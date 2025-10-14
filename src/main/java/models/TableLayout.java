package models;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import controllers.TurnManager;

import java.util.*;

public class TableLayout {

    private final Pane overlay;
    private final Canvas tableTop;           // used to locate its parent (true visual table)
    private final TurnManager turnManager;
    private final HBox playerStrip;
    private final HBox aiStrip;

    private final Canvas hintLayer = new Canvas();

    private int rows = 0, cols = 0;
    private double cell = 48;
    private static final double MIN_CELL = 32;
    private static final double MAX_CELL = 64;

    private final Map<Cell, PlacedDomino> occupancy = new HashMap<>();
    private final List<PlacedDomino> tableDominoes = new ArrayList<>();

    private final LinkedHashSet<Anchor> anchors = new LinkedHashSet<>();

    private static final String OUTLINE_STYLE =
            "-fx-border-color: rgba(255,255,255,0.40);"
          + "-fx-border-width: 1;"
          + "-fx-border-insets: 0;"
          + "-fx-background-insets: 0;";

    private static final Color HINT_STROKE = Color.color(1, 1, 1, 0.70);
    private static final Color HINT_FILL   = Color.color(1, 1, 1, 0.08);
    private static final double HINT_ARC   = 6.0;

    // Slight shrink applied to candidate anchor rectangles during validation so
    // “just touching” a domino/anchor doesn’t count as overlap and get culled.
    private static final double ANCHOR_INSET = 1.0;

    // 👇 NEW: show outlines/hints only during the very first turn
    // (true at game start; flipped to false immediately after the first placement)
    private boolean showFirstTurnHints = true;

    public TableLayout(Pane overlay, Canvas tableTop, TurnManager turnManager,
                       HBox playerStrip, HBox aiStrip) {
        this.overlay = Objects.requireNonNull(overlay);
        this.tableTop = Objects.requireNonNull(tableTop);
        this.turnManager = Objects.requireNonNull(turnManager);
        this.playerStrip = Objects.requireNonNull(playerStrip);
        this.aiStrip = Objects.requireNonNull(aiStrip);

        hintLayer.setMouseTransparent(true);
        overlay.getChildren().add(0, hintLayer);

        bindGridToTable();
        repaintAnchorHints();
    }

    /* --- force reseed "+" after layout to guarantee true center --- */
    public void forceReseedCenterIfEmpty() {
        ensureGridReady();
        if (tableDominoes.isEmpty()) {
            anchors.clear();
            rebuildAnchors();
        }
    }

    public static final class Placement {
        public final int row, col;
        public final boolean vertical;
        public final double offsetX, offsetY;
        public Placement(int row, int col, boolean vertical, double dx, double dy) {
            this.row=row; this.col=col; this.vertical=vertical; this.offsetX=dx; this.offsetY=dy;
        }
        @Override public String toString(){ return "Placement{r="+row+", c="+col+", v="+vertical+", dx="+offsetX+", dy="+offsetY+"}"; }
    }

    private static boolean isVerticalOrientation(CDominoes d) {
        return d.getOrientation() != null && d.getOrientation().startsWith("Vertical");
    }
    private static void rotateToOrientation(CDominoes d, boolean vertical) {
        int guard = 0;
        while (isVerticalOrientation(d) != vertical && guard++ < 4) {
            CDominoes.rotateDomino(d);
        }
    }

    public boolean tryPlaceOnGrid(CDominoes domino, StackPane hitbox, HBox sourceStrip, String who) {
        ensureGridReady();
        if (hitbox.getParent() != overlay) return false;
        if (rows <= 0 || cols <= 0) return false;

        boolean wantVertical = isVerticalOrientation(domino);
        Point2D dropCenter = new Point2D(
                hitbox.getLayoutX() + hitbox.getWidth()  / 2.0,
                hitbox.getLayoutY() + hitbox.getHeight() / 2.0
        );

        Anchor pick = nearestMatchingAnchor(dropCenter, domino, wantVertical, cell * 1.6);

        // allow 90° auto-rotate if a perpendicular anchor is closer
        if (pick == null) {
            boolean altVertical = !wantVertical;
            Anchor alt = nearestMatchingAnchor(dropCenter, domino, altVertical, cell * 1.6);
            if (alt != null) {
                rotateToOrientation(domino, altVertical);
                pick = alt;
            }
        }

        if (pick == null) return false;

        commitPlacementAt(domino, hitbox, sourceStrip,
                new Placement(pick.r, pick.c, pick.vertical, pick.offsetX, pick.offsetY));
        return true;
    }

    public Optional<Placement> findLegalPlacementAnywhere(CDominoes domino) {
        ensureGridReady();
        if (rows <= 0 || cols <= 0) return Optional.empty();

        for (int rot = 0; rot < 4; rot++) {
            boolean wantVertical = domino.getOrientation().startsWith("Vertical");
            for (Anchor a : anchors) {
                if (a.vertical != wantVertical) continue;
                if (!anchorFree(a)) continue;
                if (!matchesAnchor(domino, a)) continue;
                return Optional.of(new Placement(a.r, a.c, a.vertical, a.offsetX, a.offsetY));
            }
            CDominoes.rotateDomino(domino);
        }
        return Optional.empty();
    }

    public void commitPlacementAt(CDominoes domino, StackPane hitbox, HBox sourceStrip, Placement plan) {
        ensureGridReady();

        Cell tl = new Cell(plan.row, plan.col);
        Cell c2 = plan.vertical ? new Cell(plan.row+1, plan.col) : new Cell(plan.row, plan.col+1);
        if (!inBounds(tl) || !inBounds(c2)) return;
        if (occupancy.containsKey(tl) || occupancy.containsKey(c2)) return;

        Anchor gate = null;
        if (!tableDominoes.isEmpty()) {
            for (Anchor a : anchors) {
                if (a.r == plan.row && a.c == plan.col && a.vertical == plan.vertical
                        && almost(a.offsetX, plan.offsetX) && almost(a.offsetY, plan.offsetY)) {
                    gate = a; break;
                }
            }
            if (gate == null || !matchesAnchor(domino, gate)) return;
        }

        if (hitbox.getParent() != overlay) {
            sourceStrip.getChildren().remove(hitbox);
            overlay.getChildren().add(hitbox);
        }

        // if we just placed a vertical DOUBLE into a single's perpendicular anchor,
        // cap N/S expansion at that joint
        boolean capNorthSouth = false;
        if (gate != null && gate.vertical &&
                (gate.touch == Touch.PERP_NORTH || gate.touch == Touch.PERP_SOUTH) &&
                gate.incomingMustBeDouble) {
            capNorthSouth = true;
        }

        // was the table empty before this placement?
        boolean wasEmpty = tableDominoes.isEmpty();

        place(domino, hitbox, tl, plan.vertical, plan.offsetX, plan.offsetY, capNorthSouth);
        sourceStrip.getChildren().remove(hitbox);

        // after first placement, hide all outlines + anchor hints
        if (wasEmpty && showFirstTurnHints) {
            showFirstTurnHints = false;
            restyleAllPlacedDominoes();
        }

        rebuildAnchors();
        nextTurn();
    }

    /* ---------------- Anchors ---------------- */

    private enum Touch {
        NORTH, SOUTH, WEST, EAST,
        PERP_NORTH, PERP_SOUTH, PERP_WEST, PERP_EAST
    }

    private static final class Anchor {
        final int r, c;
        final boolean vertical;
        final Integer required;
        final Touch touch;
        final boolean incomingMustBeDouble;
        final double offsetX, offsetY;

        Anchor(int r, int c, boolean vertical, Integer required, Touch touch,
               boolean mustBeDouble, double offsetX, double offsetY) {
            this.r=r; this.c=c; this.vertical=vertical; this.required=required;
            this.touch=touch; this.incomingMustBeDouble=mustBeDouble;
            this.offsetX = offsetX; this.offsetY = offsetY;
        }
        @Override public boolean equals(Object o){
            if(!(o instanceof Anchor)) return false;
            Anchor a=(Anchor)o;
            return r==a.r && c==a.c && vertical==a.vertical && touch==a.touch
                    && almost(offsetX, a.offsetX) && almost(offsetY, a.offsetY);
        }
        @Override public int hashCode(){ return Objects.hash(r,c,vertical,touch,Math.rint(offsetX*1000),Math.rint(offsetY*1000)); }
    }

    private void rebuildAnchors() {
        anchors.clear();

        if (tableDominoes.isEmpty()) {
            if (rows <= 0 || cols <= 0) return;

            CenterSeed seed = computeCenterSeed();
            if (anchorCellsFree(true, seed.vTL))
                addAnchorIfValid(new Anchor(seed.vTL.r, seed.vTL.c, true, null, Touch.SOUTH, false, seed.vDX, seed.vDY));
            if (anchorCellsFree(false, seed.hTL))
                addAnchorIfValid(new Anchor(seed.hTL.r, seed.hTL.c, false, null, Touch.EAST, false, seed.hDX, seed.hDY));
            repaintAnchorHints();
            return;
        }

        for (PlacedDomino pd : tableDominoes) {
            if (pd.vertical) {
                // chain ends (unless capped by a perpendicular double)
                if (!pd.capNorthSouth) {
                    Integer topVal = pd.model.getTopValue();
                    Cell above = new Cell(pd.topLeft.r - 2, pd.topLeft.c);
                    if (topVal != null && inBoundsPair(true, above) && anchorCellsFree(true, above))
                        addAnchorIfValid(new Anchor(above.r, above.c, true, topVal, Touch.SOUTH, false, pd.offsetX, pd.offsetY));

                    Integer bottomVal = pd.model.getBottomValue();
                    Cell below = new Cell(pd.topLeft.r + 2, pd.topLeft.c);
                    if (bottomVal != null && inBoundsPair(true, below) && anchorCellsFree(true, below))
                        addAnchorIfValid(new Anchor(below.r, below.c, true, bottomVal, Touch.NORTH, false, pd.offsetX, pd.offsetY));
                }

                // perpendiculars
                if (pd.isVerticalDouble()) {
                    int rTop = pd.topLeft.r;
                    int cCol = pd.topLeft.c;

                    // left / right (horizontal singles attach here)
                    Cell leftPerp = new Cell(rTop, cCol - 2);
                    if (inBoundsPair(false, leftPerp) && anchorCellsFree(false, leftPerp)) {
                        double dy = pd.offsetY + cell/2.0;
                        Integer req = pd.model.getTopValue();
                        addAnchorIfValid(new Anchor(leftPerp.r, leftPerp.c, false, req, Touch.PERP_EAST, false, pd.offsetX, dy));
                    }
                    Cell rightPerp = new Cell(rTop, cCol + 1);
                    if (inBoundsPair(false, rightPerp) && anchorCellsFree(false, rightPerp)) {
                        double dy = pd.offsetY + cell/2.0;
                        Integer req = pd.model.getTopValue();
                        addAnchorIfValid(new Anchor(rightPerp.r, rightPerp.c, false, req, Touch.PERP_WEST, false, pd.offsetX, dy));
                    }

                    // top/bottom allow horizontal singles to cross the center of the double
                    if (!pd.capNorthSouth) {
                        Cell abovePerpH = new Cell(pd.topLeft.r - 1, pd.topLeft.c - 1);
                        if (inBoundsPair(false, abovePerpH) && anchorCellsFree(false, abovePerpH)) {
                            double dx = pd.offsetX + cell/2.0;
                            Integer req = pd.model.getTopValue();
                            addAnchorIfValid(new Anchor(abovePerpH.r, abovePerpH.c, false, req, Touch.PERP_SOUTH, false, dx, pd.offsetY));
                        }
                        Cell belowPerpH = new Cell(pd.topLeft.r + 2, pd.topLeft.c - 1);
                        if (inBoundsPair(false, belowPerpH) && anchorCellsFree(false, belowPerpH)) {
                            double dx = pd.offsetX + cell/2.0;
                            Integer req = pd.model.getBottomValue();
                            addAnchorIfValid(new Anchor(belowPerpH.r, belowPerpH.c, false, req, Touch.PERP_NORTH, false, dx, pd.offsetY));
                        }
                    }
                } else {
                    // vertical single: allow ONLY perpendicular **doubles** at its ends
                    Cell abovePerp = new Cell(pd.topLeft.r - 1, pd.topLeft.c - 1);
                    if (!pd.capNorthSouth && inBoundsPair(false, abovePerp) && anchorCellsFree(false, abovePerp)) {
                        double dx = pd.offsetX + cell/2.0;
                        Integer req = pd.model.getTopValue();
                        addAnchorIfValid(new Anchor(abovePerp.r, abovePerp.c, false, req, Touch.PERP_SOUTH, true, dx, pd.offsetY));
                    }
                    Cell belowPerp = new Cell(pd.topLeft.r + 2, pd.topLeft.c - 1);
                    if (!pd.capNorthSouth && inBoundsPair(false, belowPerp) && anchorCellsFree(false, belowPerp)) {
                        double dx = pd.offsetX + cell/2.0;
                        Integer req = pd.model.getBottomValue();
                        addAnchorIfValid(new Anchor(belowPerp.r, belowPerp.c, false, req, Touch.PERP_NORTH, true, dx, pd.offsetY));
                    }
                }

            } else {
                // horizontal domino
                Integer leftVal = pd.model.getLeftValue();
                Cell left = new Cell(pd.topLeft.r, pd.topLeft.c - 2);
                if (leftVal != null && inBoundsPair(false,left) && anchorCellsFree(false,left))
                    addAnchorIfValid(new Anchor(left.r, left.c, false, leftVal, Touch.EAST, false, pd.offsetX, pd.offsetY));

                Integer rightVal = pd.model.getRightValue();
                Cell right = new Cell(pd.topLeft.r, pd.topLeft.c + 2);
                if (rightVal != null && inBoundsPair(false,right) && anchorCellsFree(false,right))
                    addAnchorIfValid(new Anchor(right.r, right.c, false, rightVal, Touch.WEST, false, pd.offsetX, pd.offsetY));

                if (pd.isHorizontalDouble()) {
                    // horizontal DOUBLE: one cell above, one below (centered)
                    int rRow = pd.topLeft.r;
                    int cLeft = pd.topLeft.c;

                    Cell topPerp = new Cell(rRow - 2, cLeft);
                    if (inBoundsPair(true, topPerp) && anchorCellsFree(true, topPerp)) {
                        double dx = pd.offsetX + cell/2.0;
                        Integer req = pd.model.getLeftValue();
                        addAnchorIfValid(new Anchor(topPerp.r, topPerp.c, true, req, Touch.PERP_SOUTH, false, dx, pd.offsetY));
                    }
                    Cell bottomPerp = new Cell(rRow + 1, cLeft);
                    if (inBoundsPair(true, bottomPerp) && anchorCellsFree(true, bottomPerp)) {
                        double dx = pd.offsetX + cell/2.0;
                        Integer req = pd.model.getLeftValue();
                        addAnchorIfValid(new Anchor(bottomPerp.r, bottomPerp.c, true, req, Touch.PERP_NORTH, false, dx, pd.offsetY));
                    }

                } else {
                    // HORIZONTAL SINGLE:
                    // allow vertical DOUBLES on LEFT and RIGHT sides ONLY (no top/bottom)
                    int rRow  = pd.topLeft.r;
                    int cLeft = pd.topLeft.c;
                    int cRight = cLeft + 1;

                    // LEFT side (centered beside the single)
                    Cell leftSidePerp  = new Cell(rRow - 1, cLeft - 1);
                    if (inBoundsPair(true, leftSidePerp) && anchorCellsFree(true, leftSidePerp)) {
                        Integer req = pd.model.getLeftValue();
                        addAnchorIfValid(new Anchor(
                                leftSidePerp.r, leftSidePerp.c,
                                true, req, Touch.PERP_EAST, true,
                                pd.offsetX, pd.offsetY + cell/2.0
                        ));
                    }

                    // RIGHT side (centered beside the single)
                    Cell rightSidePerp = new Cell(rRow - 1, cRight + 1);
                    if (inBoundsPair(true, rightSidePerp) && anchorCellsFree(true, rightSidePerp)) {
                        Integer req = pd.model.getRightValue();
                        addAnchorIfValid(new Anchor(
                                rightSidePerp.r, rightSidePerp.c,
                                true, req, Touch.PERP_WEST, true,
                                pd.offsetX, pd.offsetY + cell/2.0
                        ));
                    }
                }
            }
        }

        repaintAnchorHints();
    }

    /* ---------------- Hint rendering + filters ---------------- */

    private boolean addAnchorIfValid(Anchor cand) {
        Point2D tl = cellTopLeftOnHint(new Cell(cand.r, cand.c));
        double w = cand.vertical ? cell : cell * 2.0;
        double h = cand.vertical ? cell * 2.0 : cell;
        double x = tl.getX() + cand.offsetX;
        double y = tl.getY() + cand.offsetY;

        // Inset candidate rect slightly for hit-tests to avoid “touching” being treated as overlap
        double ix = x + ANCHOR_INSET;
        double iy = y + ANCHOR_INSET;
        double iw = w - 2 * ANCHOR_INSET;
        double ih = h - 2 * ANCHOR_INSET;

        if (!rectFullyInsideHint(ix, iy, iw, ih)) return false;
        if (rectOverlapsExistingAnchor(ix, iy, iw, ih, cand.vertical)) return false;
        if (rectOverlapsHandBars(ix, iy, iw, ih)) return false;
        if (rectOverlapsPlacedDominoes(ix, iy, iw, ih)) return false;

        anchors.add(cand);
        return true;
    }

    private boolean rectFullyInsideHint(double x, double y, double w, double h) {
        double W = hintLayer.getWidth();
        double H = hintLayer.getHeight();
        return x >= 0.0 && y >= 0.0 && (x + w) <= W && (y + h) <= H;
    }

    private boolean rectOverlapsExistingAnchor(double x, double y, double w, double h, boolean candVertical) {
        for (Anchor a : anchors) {
            if (a.vertical != candVertical) continue;
            Point2D tl = cellTopLeftOnHint(new Cell(a.r, a.c));
            double aw = a.vertical ? cell : cell * 2.0;
            double ah = a.vertical ? cell * 2.0 : cell;
            // inset existing anchors too, consistently
            double ax = tl.getX() + a.offsetX + ANCHOR_INSET;
            double ay = tl.getY() + a.offsetY + ANCHOR_INSET;
            double aiw = aw - 2 * ANCHOR_INSET;
            double aih = ah - 2 * ANCHOR_INSET;

            if (rectsOverlap(ax, ay, aiw, aih, x, y, w, h)) return true;
        }
        return false;
    }

    private boolean rectOverlapsHandBars(double xHint, double yHint, double w, double h) {
        Bounds tb = tableAreaOnOverlay();
        double ox = tb.getMinX() + xHint;
        double oy = tb.getMinY() + yHint;

        Bounds playerB = nodeBoundsOnOverlay(playerStrip);
        Bounds aiB     = nodeBoundsOnOverlay(aiStrip);

        return (playerB != null && rectsOverlap(ox, oy, w, h, playerB.getMinX(), playerB.getMinY(), playerB.getWidth(), playerB.getHeight()))
            || (aiB     != null && rectsOverlap(ox, oy, w, h, aiB.getMinX(),     aiB.getMinY(),     aiB.getWidth(),     aiB.getHeight()));
    }

    private boolean rectOverlapsPlacedDominoes(double xHint, double yHint, double w, double h) {
        if (tableDominoes.isEmpty()) return false;
        Bounds tb = tableAreaOnOverlay();
        double ax = tb.getMinX() + xHint;
        double ay = tb.getMinY() + yHint;

        for (PlacedDomino pd : tableDominoes) {
            Node n = pd.node;
            double bx = n.getLayoutX();
            double by = n.getLayoutY();
            double bw = n.getLayoutBounds().getWidth();
            double bh = n.getLayoutBounds().getHeight();
            if (rectsOverlap(ax, ay, w, h, bx, by, bw, bh)) return true;
        }
        return false;
    }

    private Bounds nodeBoundsOnOverlay(Node n) {
        try {
            Bounds sceneB = n.localToScene(n.getBoundsInLocal());
            return overlay.sceneToLocal(sceneB);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean rectsOverlap(double ax, double ay, double aw, double ah,
                                 double bx, double by, double bw, double bh) {
        final double EPS = 0.5;
        return ax < (bx + bw - EPS) &&
               (ax + aw) > (bx + EPS) &&
               ay < (by + bh - EPS) &&
               (ay + ah) > (by + EPS);
    }

    private void repaintAnchorHints() {
        Bounds tb = tableAreaOnOverlay();
        hintLayer.setManaged(false);
        hintLayer.setLayoutX(tb.getMinX());
        hintLayer.setLayoutY(tb.getMinY());
        hintLayer.setWidth(tb.getWidth());
        hintLayer.setHeight(tb.getHeight());

        GraphicsContext g = hintLayer.getGraphicsContext2D();
        g.clearRect(0, 0, hintLayer.getWidth(), hintLayer.getHeight());

        // 👇 only draw during the first turn
        if (!showFirstTurnHints) return;

        g.setLineWidth(1.0);
        g.setStroke(HINT_STROKE);
        g.setFill(HINT_FILL);

        for (Anchor a : anchors) {
            Point2D px = cellTopLeftOnHint(new Cell(a.r, a.c));
            double w = a.vertical ? cell : cell * 2.0;
            double h = a.vertical ? cell * 2.0 : cell;

            double x = px.getX() + a.offsetX;
            double y = px.getY() + a.offsetY;

            g.fillRoundRect(x, y, w, h, HINT_ARC, HINT_ARC);
            g.strokeRoundRect(x, y, w, h, HINT_ARC, HINT_ARC);
        }
    }

    /* ---------------- Utility ---------------- */

    private boolean inBoundsPair(boolean vertical, Cell tl) {
        return inBounds(tl) && inBounds(vertical ? new Cell(tl.r+1, tl.c) : new Cell(tl.r, tl.c+1));
    }
    private boolean anchorCellsFree(boolean vertical, Cell tl) {
        return !occupancy.containsKey(tl)
                && !occupancy.containsKey(vertical ? new Cell(tl.r+1, tl.c) : new Cell(tl.r, tl.c+1));
    }

    private Anchor nearestMatchingAnchor(Point2D dropCenter, CDominoes d, boolean wantVertical, double radius) {
        double bestD2 = radius * radius;
        Anchor best = null;
        for (Anchor a : anchors) {
            if (a.vertical != wantVertical) continue;
            if (!anchorFree(a)) continue;
            if (!matchesAnchor(d, a)) continue;

            Point2D ac = anchorCenter(a);
            double dx = ac.getX() - dropCenter.getX();
            double dy = ac.getY() - dropCenter.getY();
            double d2 = dx*dx + dy*dy;
            if (d2 < bestD2) { bestD2 = d2; best = a; }
        }
        return best;
    }

    private boolean anchorFree(Anchor a) {
        Cell tl = new Cell(a.r, a.c);
        return anchorCellsFree(a.vertical, tl);
    }

    private Point2D anchorCenter(Anchor a) {
        double w = a.vertical ? cell : cell*2;
        double h = a.vertical ? cell*2 : cell;
        Point2D px = cellTopLeftOnOverlay(new Cell(a.r, a.c));
        return new Point2D(px.getX() + a.offsetX + w/2.0, px.getY() + a.offsetY + h/2.0);
    }

    private boolean matchesAnchor(CDominoes d, Anchor a) {
        if (a.required == null) return true;

        if (a.vertical) {
            Integer top = d.getTopValue();
            Integer bot = d.getBottomValue();
            boolean isDouble = (top != null && bot != null && top.equals(bot));

            switch (a.touch) {
                case NORTH:      return top != null && top.equals(a.required);
                case SOUTH:      return bot != null && bot.equals(a.required);

                // side-perpendiculars for vertical piece meeting a horizontal single
                case PERP_EAST:
                case PERP_WEST:
                    if (a.incomingMustBeDouble && !isDouble) return false;
                    return (top != null && top.equals(a.required)) || (bot != null && bot.equals(a.required));

                case PERP_SOUTH:
                    if (a.incomingMustBeDouble && !isDouble) return false;
                    return bot != null && bot.equals(a.required);
                case PERP_NORTH:
                    if (a.incomingMustBeDouble && !isDouble) return false;
                    return top != null && top.equals(a.required);
                default: return false;
            }
        } else {
            Integer left = d.getLeftValue();
            Integer right = d.getRightValue();
            boolean isDouble = (left != null && right != null && left.equals(right));

            switch (a.touch) {
                case WEST:  return left  != null && left.equals(a.required);
                case EAST:  return right != null && right.equals(a.required);
                case PERP_EAST:
                    if (a.incomingMustBeDouble && !isDouble) return false;
                    return right != null && right.equals(a.required);
                case PERP_WEST:
                    if (a.incomingMustBeDouble && !isDouble) return false;
                    return left  != null && left.equals(a.required);
                case PERP_NORTH:
                case PERP_SOUTH:
                    if (a.incomingMustBeDouble && !isDouble) return false;
                    return (left != null && left.equals(a.required)) || (right != null && right.equals(a.required));
                default: return false;
            }
        }
    }

    private void place(CDominoes domino, StackPane node, Cell topLeft, boolean vertical,
                       double offsetX, double offsetY, boolean capNS) {
        PlacedDomino pd = new PlacedDomino(domino, node, topLeft, vertical, offsetX, offsetY, capNS);
        tableDominoes.add(pd);

        occupancy.put(topLeft, pd);
        occupancy.put(vertical ? new Cell(topLeft.r+1, topLeft.c) : new Cell(topLeft.r, topLeft.c+1), pd);

        layoutOne(pd);

        node.setOnMousePressed(null);
        node.setOnMouseDragged(null);
        node.setOnMouseReleased(null);
        node.setFocusTraversable(false);
        node.setMouseTransparent(true);
    }

    private void layoutOne(PlacedDomino pd) {
        Cell tl = pd.topLeft;
        boolean vertical = pd.vertical;
        StackPane node = pd.node;
        CDominoes domino = pd.model;

        Point2D px = cellTopLeftOnOverlay(tl);
        double w = vertical ? cell : cell * 2;
        double h = vertical ? cell * 2 : cell;

        double x = px.getX() + pd.offsetX;
        double y = px.getY() + pd.offsetY;

        node.setPadding(Insets.EMPTY);
        node.setManaged(false);
        node.resizeRelocate(x, y, w, h);

        // 👇 only show outlines on first turn
        node.setStyle(showFirstTurnHints ? OUTLINE_STYLE : null);

        if (!node.getChildren().isEmpty() && node.getChildren().get(0) instanceof ImageView) {
            ImageView iv = (ImageView) node.getChildren().get(0);
            try { iv.fitHeightProperty().unbind(); } catch (Exception ignore) {}
            try { iv.fitWidthProperty().unbind(); } catch (Exception ignore) {}
            iv.setPreserveRatio(true);
            if (vertical) {
                iv.setFitWidth(w);
                iv.setFitHeight(h);
            } else {
                iv.setFitWidth(h);
                iv.setFitHeight(w);
            }
            iv.setRotate(domino.getRotationDegrees());
        }

        node.toFront();
    }

    // 👇 restyle existing placed tiles when we turn hints off after first placement
    private void restyleAllPlacedDominoes() {
        if (tableDominoes.isEmpty()) return;
        for (PlacedDomino pd : tableDominoes) {
            if (pd.node != null) pd.node.setStyle(showFirstTurnHints ? OUTLINE_STYLE : null);
        }
        repaintAnchorHints();
    }

    private void bindGridToTable() {
        InvalidationListener relayout = obs -> {
            layoutHintCanvas();
            computeGrid();
            relayoutAllPlaced();
            repaintAnchorHints();
            if (tableDominoes.isEmpty() && anchors.isEmpty()) rebuildAnchors();
        };
        overlay.layoutBoundsProperty().addListener(relayout);

        Node tablePane = tableTop.getParent();
        if (tablePane != null) {
            tablePane.layoutBoundsProperty().addListener(relayout);
        }
        tableTop.widthProperty().addListener(relayout);
        tableTop.heightProperty().addListener(relayout);

        layoutHintCanvas();
        computeGrid();
        relayoutAllPlaced();
        repaintAnchorHints();
        if (tableDominoes.isEmpty() && anchors.isEmpty()) rebuildAnchors();

        Platform.runLater(this::forceReseedCenterIfEmpty);
    }

    private void relayoutAllPlaced() {
        if (tableDominoes.isEmpty()) return;
        for (PlacedDomino pd : tableDominoes) layoutOne(pd);
    }

    private void layoutHintCanvas() {
        Bounds tb = tableAreaOnOverlay();
        hintLayer.setLayoutX(tb.getMinX());
        hintLayer.setLayoutY(tb.getMinY());
        hintLayer.setWidth(tb.getWidth());
        hintLayer.setHeight(tb.getHeight());
    }

    private void computeGrid() {
        double w = hintLayer.getWidth();
        double h = hintLayer.getHeight();
        if (w <= 0 || h <= 0) { rows = cols = 0; return; }

        double target = Math.min(w, h) / 18.0;
        cell = clamp(target, MIN_CELL, MAX_CELL);
        cols = Math.max(3, (int)Math.floor(w / cell));
        rows = Math.max(3, (int)Math.floor(h / cell));
    }

    private void ensureGridReady() {
        if (rows <= 0 || cols <= 0) {
            layoutHintCanvas();
            computeGrid();
            relayoutAllPlaced();
            repaintAnchorHints();
        }
        if (tableDominoes.isEmpty() && anchors.isEmpty() && rows > 0 && cols > 0) {
            rebuildAnchors();
        }
    }

    private static final class CenterSeed {
        final Cell vTL, hTL;
        final double vDX, vDY, hDX, hDY;
        CenterSeed(Cell vTL, Cell hTL, double vDX, double vDY, double hDX, double hDY){
            this.vTL=vTL; this.hTL=hTL; this.vDX=vDX; this.vDY=vDY; this.hDX=hDX; this.hDY=hDY;
        }
    }

    private CenterSeed computeCenterSeed() {
        Bounds tb = tableAreaOnOverlay();
        double ox = tb.getMinX();
        double oy = tb.getMinY();
        double cx = ox + tb.getWidth()  / 2.0;
        double cy = oy + tb.getHeight() / 2.0;

        double v_tlx = cx - cell/2.0;
        double v_tly = cy - cell;
        int vC = clampIndex((int)Math.floor((v_tlx - ox) / cell), 0, Math.max(0, cols - 1));
        int vR = clampIndex((int)Math.floor((v_tly - oy) / cell), 0, Math.max(0, rows - 2));
        Cell vTL = new Cell(vR, vC);
        Point2D vTLpx = cellTopLeftOnOverlay(vTL);
        double vCenterX = vTLpx.getX() + cell/2.0;
        double vCenterY = vTLpx.getY() + cell;
        double vDX = cx - vCenterX;
        double vDY = cy - vCenterY;

        double h_tlx = cx - cell;
        double h_tly = cy - cell/2.0;
        int hC = clampIndex((int)Math.floor((h_tlx - ox) / cell), 0, Math.max(0, cols - 2));
        int hR = clampIndex((int)Math.floor((h_tly - oy) / cell), 0, Math.max(0, rows - 1));
        Cell hTL = new Cell(hR, hC);
        Point2D hTLpx = cellTopLeftOnOverlay(hTL);
        double hCenterX = hTLpx.getX() + cell;
        double hCenterY = hTLpx.getY() + cell/2.0;
        double hDX = cx - hCenterX;
        double hDY = cy - hCenterY;

        return new CenterSeed(vTL, hTL, vDX, vDY, hDX, hDY);
    }

    private int clampIndex(int v, int lo, int hi) { if (hi < lo) return lo; return Math.max(lo, Math.min(hi, v)); }
    private boolean inBounds(Cell c) { return c.r >= 0 && c.c >= 0 && c.r < rows && c.c < cols; }

    private Bounds tableAreaOnOverlay() {
        Node tablePane = tableTop.getParent(); // the StackPane we actually see
        Bounds tableScene = (tablePane != null ? tablePane.localToScene(tablePane.getBoundsInLocal())
                                               : tableTop.localToScene(tableTop.getBoundsInLocal()));
        return overlay.sceneToLocal(tableScene);
    }

    private Point2D cellTopLeftOnOverlay(Cell c) {
        Bounds tb = tableAreaOnOverlay();
        return new Point2D(tb.getMinX() + c.c * cell, tb.getMinY() + c.r * cell);
    }
    private Point2D cellTopLeftOnHint(Cell c) { return new Point2D(c.c * cell, c.r * cell); }

    private void nextTurn() {
        turnManager.next();
        System.out.println("[Turn->next()] New side: " + turnManager.getTurn());
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
    private static boolean almost(double a, double b){ return Math.abs(a-b) < 0.0001; }

    private static final class Cell {
        final int r, c;
        Cell(int r, int c){ this.r=r; this.c=c; }
        @Override public boolean equals(Object o){ if(!(o instanceof Cell)) return false; Cell x=(Cell)o; return x.r==r && x.c==c; }
        @Override public int hashCode(){ return Objects.hash(r,c); }
        @Override public String toString(){ return "("+r+","+c+")"; }
    }

    private static final class PlacedDomino {
        final CDominoes model;
        final StackPane node;
        final Cell topLeft;
        final boolean vertical;
        final double offsetX, offsetY;
        final boolean capNorthSouth;

        PlacedDomino(CDominoes m, StackPane n, Cell tl, boolean v, double dx, double dy, boolean capNS){
            this.model=m; this.node=n; this.topLeft=tl; this.vertical=v; this.offsetX=dx; this.offsetY=dy;
            this.capNorthSouth = v && capNS;
        }
        boolean isVerticalDouble() { if (!vertical) return false; Integer t=model.getTopValue(), b=model.getBottomValue(); return t!=null&&b!=null&&t.equals(b); }
        boolean isHorizontalDouble(){ if (vertical) return false; Integer l=model.getLeftValue(), r=model.getRightValue(); return l!=null&&r!=null&&l.equals(r); }
    }

    /* ------------ read-only snapshot of current anchor requirements ----------- */

    public static final class AnchorHint {
        public final boolean vertical;
        public final Integer required;
        public final String touch;
        public final boolean mustBeDouble;

        private AnchorHint(boolean vertical, Integer required, String touch, boolean mustBeDouble) {
            this.vertical = vertical;
            this.required = required;
            this.touch = touch;
            this.mustBeDouble = mustBeDouble;
        }
    }

    /** Returns an immutable snapshot of current anchors; does not expose positions/offsets. */
    public List<AnchorHint> snapshotAnchors() {
        List<AnchorHint> list = new ArrayList<>(anchors.size());
        for (Anchor a : anchors) {
            list.add(new AnchorHint(a.vertical, a.required, a.touch.name(), a.incomingMustBeDouble));
        }
        return Collections.unmodifiableList(list);
    }
}
