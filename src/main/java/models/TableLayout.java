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
import util.ConsoleLogger;

import java.util.*;

public class TableLayout {

    private final Pane overlay;
    private final Canvas tableTop;
    private final TurnManager turnManager;
    private final HBox playerStrip;
    private final HBox aiStrip;

    private final Canvas hintLayer = new Canvas();

    private int rowCount = 0, colCount = 0;
    private double cellSize = 48;
    private static final double MIN_CELL_SIZE = 32;
    private static final double MAX_CELL_SIZE = 64;

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

    private static final double ANCHOR_INSET = 1.0;

    private boolean showFirstTurnHints = true;

    // builds layout helpers and starts listeners
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

    // reseeds a fresh center if the table is empty
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
        public Placement(int row, int col, boolean vertical, double offsetX, double offsetY) {
            this.row = row; this.col = col; this.vertical = vertical; this.offsetX = offsetX; this.offsetY = offsetY;
        }
        @Override public String toString() {
            return "Placement{r=" + row + ", c=" + col + ", v=" + vertical + ", dx=" + offsetX + ", dy=" + offsetY + "}";
        }
    }

    // checks if the domino is currently vertical
    private static boolean isVerticalOrientation(CDominoes domino) {
        return domino.getOrientation() != null && domino.getOrientation().startsWith("Vertical");
    }

    // rotates a domino until it matches the needed facing
    private static void rotateToOrientation(CDominoes domino, boolean wantVertical) {
        int rotateGuard = 0;
        while (isVerticalOrientation(domino) != wantVertical && rotateGuard++ < 4) {
            CDominoes.rotateDomino(domino);
        }
    }

    // tries to snap a dragged tile to a legal anchor
    public boolean tryPlaceOnGrid(CDominoes domino, StackPane hitbox, HBox sourceStrip, String who) {
        ensureGridReady();
        if (hitbox.getParent() != overlay) return false;
        if (rowCount <= 0 || colCount <= 0) return false;

        boolean wantVertical = isVerticalOrientation(domino);
        Point2D dropCenter = new Point2D(
                hitbox.getLayoutX() + hitbox.getWidth()  / 2.0,
                hitbox.getLayoutY() + hitbox.getHeight() / 2.0
        );

        Anchor pick = nearestMatchingAnchor(dropCenter, domino, wantVertical, cellSize * 1.6);

        // allows auto-rotate if the other facing is closer
        if (pick == null) {
            boolean altVertical = !wantVertical;
            Anchor altPick = nearestMatchingAnchor(dropCenter, domino, altVertical, cellSize * 1.6);
            if (altPick != null) {
                rotateToOrientation(domino, altVertical);
                pick = altPick;
            }
        }

        if (pick == null) return false;

        commitPlacementAt(domino, hitbox, sourceStrip,
                new Placement(pick.row, pick.col, pick.vertical, pick.offsetX, pick.offsetY));
        return true;
    }

    // finds any legal placement for this domino
    public Optional<Placement> findLegalPlacementAnywhere(CDominoes domino) {
        ensureGridReady();
        if (rowCount <= 0 || colCount <= 0) return Optional.empty();

        for (int rotation = 0; rotation < 4; rotation++) {
            boolean wantVertical = domino.getOrientation().startsWith("Vertical");
            for (Anchor anchorItem : anchors) {
                if (anchorItem.vertical != wantVertical) continue;
                if (!anchorFree(anchorItem)) continue;
                if (!matchesAnchor(domino, anchorItem)) continue;
                return Optional.of(new Placement(anchorItem.row, anchorItem.col, anchorItem.vertical, anchorItem.offsetX, anchorItem.offsetY));
            }
            CDominoes.rotateDomino(domino);
        }
        return Optional.empty();
    }

    // commits a placement onto the grid and advances turn
    public void commitPlacementAt(CDominoes domino, StackPane hitbox, HBox sourceStrip, Placement plan) {
        ensureGridReady();

        Cell topLeftCell = new Cell(plan.row, plan.col);
        Cell secondCell = plan.vertical ? new Cell(plan.row + 1, plan.col) : new Cell(plan.row, plan.col + 1);
        if (!inBounds(topLeftCell) || !inBounds(secondCell)) return;
        if (occupancy.containsKey(topLeftCell) || occupancy.containsKey(secondCell)) return;

        Anchor matchedAnchor = null;
        if (!tableDominoes.isEmpty()) {
            for (Anchor anchorItem : anchors) {
                if (anchorItem.row == plan.row && anchorItem.col == plan.col && anchorItem.vertical == plan.vertical
                        && almost(anchorItem.offsetX, plan.offsetX) && almost(anchorItem.offsetY, plan.offsetY)) {
                    matchedAnchor = anchorItem; break;
                }
            }
            if (matchedAnchor == null || !matchesAnchor(domino, matchedAnchor)) return;
        }

        if (hitbox.getParent() != overlay) {
            sourceStrip.getChildren().remove(hitbox);
            overlay.getChildren().add(hitbox);
        }

        boolean wasEmpty = tableDominoes.isEmpty();

        // first placement: simple "placed [x]"
        if (wasEmpty) {
            ConsoleLogger.logFirstPlacement(turnManager.getTurn(), domino);
        }

        boolean capNorthSouth = false;
        if (matchedAnchor != null && matchedAnchor.vertical &&
                (matchedAnchor.touch == Touch.PERP_NORTH || matchedAnchor.touch == Touch.PERP_SOUTH) &&
                matchedAnchor.incomingMustBeDouble) {
            capNorthSouth = true;
        }

        place(domino, hitbox, topLeftCell, plan.vertical, plan.offsetX, plan.offsetY, capNorthSouth);
        sourceStrip.getChildren().remove(hitbox);

        // non-first placements: "placed [x] against [y]"
        if (!wasEmpty && matchedAnchor != null) {
            PlacedDomino neighbor = findNeighborFor(plan, matchedAnchor);
            if (neighbor != null) {
                ConsoleLogger.logPlacedAgainst(turnManager.getTurn(), domino, neighbor.model);
            } else {
                // graceful fallback if no neighbor found
                ConsoleLogger.logFirstPlacement(turnManager.getTurn(), domino);
            }
        }

        if (wasEmpty && showFirstTurnHints) {
            showFirstTurnHints = false;
            restyleAllPlacedDominoes();
        }

        rebuildAnchors();
        nextTurn();
    }

    // tries to resolve the existing neighbor you matched
    private PlacedDomino findNeighborFor(Placement plan, Anchor gate) {
        // candidate neighbor top-left cells to probe (broad net; first hit wins)
        List<Cell> probes = new ArrayList<>();

        if (plan.vertical) {
            // straight attachments
            probes.add(new Cell(plan.row - 2, plan.col)); // above
            probes.add(new Cell(plan.row + 2, plan.col)); // below
            // side-perpendicular possibilities
            probes.add(new Cell(plan.row,     plan.col + 2));
            probes.add(new Cell(plan.row,     plan.col - 1));
            probes.add(new Cell(plan.row - 1, plan.col - 1));
            probes.add(new Cell(plan.row + 2, plan.col - 1));
        } else {
            // straight attachments
            probes.add(new Cell(plan.row, plan.col - 2)); // left
            probes.add(new Cell(plan.row, plan.col + 2)); // right
            // top/bottom perpendicular possibilities
            probes.add(new Cell(plan.row - 2, plan.col));
            probes.add(new Cell(plan.row + 1, plan.col));
            probes.add(new Cell(plan.row - 1, plan.col));
            probes.add(new Cell(plan.row + 2, plan.col));
        }

        for (Cell c : probes) {
            PlacedDomino pd = occupancy.get(c);
            if (pd != null) return pd;
        }
        return null;
    }

    private enum Touch {
        NORTH, SOUTH, WEST, EAST,
        PERP_NORTH, PERP_SOUTH, PERP_WEST, PERP_EAST
    }

    private static final class Anchor {
        final int row, col;
        final boolean vertical;
        final Integer required;
        final Touch touch;
        final boolean incomingMustBeDouble;
        final double offsetX, offsetY;

        Anchor(int row, int col, boolean vertical, Integer required, Touch touch,
               boolean mustBeDouble, double offsetX, double offsetY) {
            this.row = row; this.col = col; this.vertical = vertical; this.required = required;
            this.touch = touch; this.incomingMustBeDouble = mustBeDouble;
            this.offsetX = offsetX; this.offsetY = offsetY;
        }
        @Override public boolean equals(Object o) {
            if (!(o instanceof Anchor)) return false;
            Anchor a = (Anchor) o;
            return row == a.row && col == a.col && vertical == a.vertical && touch == a.touch
                    && almost(offsetX, a.offsetX) && almost(offsetY, a.offsetY);
        }
        @Override public int hashCode() {
            return Objects.hash(row, col, vertical, touch, Math.rint(offsetX * 1000), Math.rint(offsetY * 1000));
        }
    }

    // rebuilds all anchors from current board state
    private void rebuildAnchors() {
        anchors.clear();

        if (tableDominoes.isEmpty()) {
            if (rowCount <= 0 || colCount <= 0) return;

            CenterSeed seed = computeCenterSeed();
            if (anchorCellsFree(true, seed.verticalTopLeft))
                addAnchorIfValid(new Anchor(seed.verticalTopLeft.row, seed.verticalTopLeft.col, true, null, Touch.SOUTH, false, seed.verticalOffsetX, seed.verticalOffsetY));
            if (anchorCellsFree(false, seed.horizontalTopLeft))
                addAnchorIfValid(new Anchor(seed.horizontalTopLeft.row, seed.horizontalTopLeft.col, false, null, Touch.EAST, false, seed.horizontalOffsetX, seed.horizontalOffsetY));
            repaintAnchorHints();
            return;
        }

        for (PlacedDomino placedDomino : tableDominoes) {
            if (placedDomino.vertical) {
                if (!placedDomino.capNorthSouth) {
                    Integer topVal = placedDomino.model.getTopValue();
                    Cell above = new Cell(placedDomino.topLeft.row - 2, placedDomino.topLeft.col);
                    if (topVal != null && inBoundsPair(true, above) && anchorCellsFree(true, above))
                        addAnchorIfValid(new Anchor(above.row, above.col, true, topVal, Touch.SOUTH, false, placedDomino.offsetX, placedDomino.offsetY));

                    Integer bottomVal = placedDomino.model.getBottomValue();
                    Cell below = new Cell(placedDomino.topLeft.row + 2, placedDomino.topLeft.col);
                    if (bottomVal != null && inBoundsPair(true, below) && anchorCellsFree(true, below))
                        addAnchorIfValid(new Anchor(below.row, below.col, true, bottomVal, Touch.NORTH, false, placedDomino.offsetX, placedDomino.offsetY));
                }

                if (placedDomino.isVerticalDouble()) {
                    int startRow = placedDomino.topLeft.row;
                    int startCol = placedDomino.topLeft.col;

                    Cell leftPerp = new Cell(startRow, startCol - 2);
                    if (inBoundsPair(false, leftPerp) && anchorCellsFree(false, leftPerp)) {
                        double dy = placedDomino.offsetY + cellSize / 2.0;
                        Integer req = placedDomino.model.getTopValue();
                        addAnchorIfValid(new Anchor(leftPerp.row, leftPerp.col, false, req, Touch.PERP_EAST, false, placedDomino.offsetX, dy));
                    }
                    Cell rightPerp = new Cell(startRow, startCol + 1);
                    if (inBoundsPair(false, rightPerp) && anchorCellsFree(false, rightPerp)) {
                        double dy = placedDomino.offsetY + cellSize / 2.0;
                        Integer req = placedDomino.model.getTopValue();
                        addAnchorIfValid(new Anchor(rightPerp.row, rightPerp.col, false, req, Touch.PERP_WEST, false, placedDomino.offsetX, dy));
                    }

                    if (!placedDomino.capNorthSouth) {
                        Cell abovePerpH = new Cell(placedDomino.topLeft.row - 1, placedDomino.topLeft.col - 1);
                        if (inBoundsPair(false, abovePerpH) && anchorCellsFree(false, abovePerpH)) {
                            double dx = placedDomino.offsetX + cellSize / 2.0;
                            Integer req = placedDomino.model.getTopValue();
                            addAnchorIfValid(new Anchor(abovePerpH.row, abovePerpH.col, false, req, Touch.PERP_SOUTH, false, dx, placedDomino.offsetY));
                        }
                        Cell belowPerpH = new Cell(placedDomino.topLeft.row + 2, placedDomino.topLeft.col - 1);
                        if (inBoundsPair(false, belowPerpH) && anchorCellsFree(false, belowPerpH)) {
                            double dx = placedDomino.offsetX + cellSize / 2.0;
                            Integer req = placedDomino.model.getBottomValue();
                            addAnchorIfValid(new Anchor(belowPerpH.row, belowPerpH.col, false, req, Touch.PERP_NORTH, false, dx, placedDomino.offsetY));
                        }
                    }
                } else {
                    Cell abovePerp = new Cell(placedDomino.topLeft.row - 1, placedDomino.topLeft.col - 1);
                    if (!placedDomino.capNorthSouth && inBoundsPair(false, abovePerp) && anchorCellsFree(false, abovePerp)) {
                        double dx = placedDomino.offsetX + cellSize / 2.0;
                        Integer req = placedDomino.model.getTopValue();
                        addAnchorIfValid(new Anchor(abovePerp.row, abovePerp.col, false, req, Touch.PERP_SOUTH, true, dx, placedDomino.offsetY));
                    }
                    Cell belowPerp = new Cell(placedDomino.topLeft.row + 2, placedDomino.topLeft.col - 1);
                    if (!placedDomino.capNorthSouth && inBoundsPair(false, belowPerp) && anchorCellsFree(false, belowPerp)) {
                        double dx = placedDomino.offsetX + cellSize / 2.0;
                        Integer req = placedDomino.model.getBottomValue();
                        addAnchorIfValid(new Anchor(belowPerp.row, belowPerp.col, false, req, Touch.PERP_NORTH, true, dx, placedDomino.offsetY));
                    }
                }

            } else {
                Integer leftVal = placedDomino.model.getLeftValue();
                Cell left = new Cell(placedDomino.topLeft.row, placedDomino.topLeft.col - 2);
                if (leftVal != null && inBoundsPair(false, left) && anchorCellsFree(false, left))
                    addAnchorIfValid(new Anchor(left.row, left.col, false, leftVal, Touch.EAST, false, placedDomino.offsetX, placedDomino.offsetY));

                Integer rightVal = placedDomino.model.getRightValue();
                Cell right = new Cell(placedDomino.topLeft.row, placedDomino.topLeft.col + 2);
                if (rightVal != null && inBoundsPair(false, right) && anchorCellsFree(false, right))
                    addAnchorIfValid(new Anchor(right.row, right.col, false, rightVal, Touch.WEST, false, placedDomino.offsetX, placedDomino.offsetY));

                if (placedDomino.isHorizontalDouble()) {
                    int startRow = placedDomino.topLeft.row;
                    int leftCol = placedDomino.topLeft.col;

                    Cell topPerp = new Cell(startRow - 2, leftCol);
                    if (inBoundsPair(true, topPerp) && anchorCellsFree(true, topPerp)) {
                        double dx = placedDomino.offsetX + cellSize / 2.0;
                        Integer req = placedDomino.model.getLeftValue();
                        addAnchorIfValid(new Anchor(topPerp.row, topPerp.col, true, req, Touch.PERP_SOUTH, false, dx, placedDomino.offsetY));
                    }
                    Cell bottomPerp = new Cell(startRow + 1, leftCol);
                    if (inboundsPairAndFreeTrue(bottomPerp))
                    {
                        double dx = placedDomino.offsetX + cellSize / 2.0;
                        Integer req = placedDomino.model.getLeftValue();
                        addAnchorIfValid(new Anchor(bottomPerp.row, bottomPerp.col, true, req, Touch.PERP_NORTH, false, dx, placedDomino.offsetY));
                    }

                } else {
                    int rRow = placedDomino.topLeft.row;
                    int cLeft = placedDomino.topLeft.col;
                    int cRight = cLeft + 1;

                    Cell leftSidePerp = new Cell(rRow - 1, cLeft - 1);
                    if (inBoundsPair(true, leftSidePerp) && anchorCellsFree(true, leftSidePerp)) {
                        Integer req = placedDomino.model.getLeftValue();
                        addAnchorIfValid(new Anchor(
                                leftSidePerp.row, leftSidePerp.col,
                                true, req, Touch.PERP_EAST, true,
                                placedDomino.offsetX, placedDomino.offsetY + cellSize / 2.0
                        ));
                    }

                    Cell rightSidePerp = new Cell(rRow - 1, cRight + 1);
                    if (inBoundsPair(true, rightSidePerp) && anchorCellsFree(true, rightSidePerp)) {
                        Integer req = placedDomino.model.getRightValue();
                        addAnchorIfValid(new Anchor(
                                rightSidePerp.row, rightSidePerp.col,
                                true, req, Touch.PERP_WEST, true,
                                placedDomino.offsetX, placedDomino.offsetY + cellSize / 2.0
                        ));
                    }
                }
            }
        }

        repaintAnchorHints();
    }

    // tiny helper for readability (horizontal double bottom anchor)
    private boolean inboundsPairAndFreeTrue(Cell cell) {
        return inBoundsPair(true, cell) && anchorCellsFree(true, cell);
    }

    // adds an anchor if the slot is safe
    private boolean addAnchorIfValid(Anchor candidate) {
        Point2D topLeftHint = cellTopLeftOnHint(new Cell(candidate.row, candidate.col));
        double w = candidate.vertical ? cellSize : cellSize * 2.0;
        double h = candidate.vertical ? cellSize * 2.0 : cellSize;
        double x = topLeftHint.getX() + candidate.offsetX;
        double y = topLeftHint.getY() + candidate.offsetY;

        double ix = x + ANCHOR_INSET;
        double iy = y + ANCHOR_INSET;
        double iw = w - 2 * ANCHOR_INSET;
        double ih = h - 2 * ANCHOR_INSET;

        if (!rectFullyInsideHint(ix, iy, iw, ih)) return false;
        if (rectOverlapsExistingAnchor(ix, iy, iw, ih, candidate.vertical)) return false;
        if (rectOverlapsHandBars(ix, iy, iw, ih)) return false;
        if (rectOverlapsPlacedDominoes(ix, iy, iw, ih)) return false;

        anchors.add(candidate);
        return true;
    }

    // checks if a rectangle fits inside the hint layer
    private boolean rectFullyInsideHint(double x, double y, double w, double h) {
        double W = hintLayer.getWidth();
        double H = hintLayer.getHeight();
        return x >= 0.0 && y >= 0.0 && (x + w) <= W && (y + h) <= H;
    }

    // checks overlap with anchors of same facing
    private boolean rectOverlapsExistingAnchor(double x, double y, double w, double h, boolean candidateVertical) {
        for (Anchor a : anchors) {
            if (a.vertical != candidateVertical) continue;
            Point2D topLeft = cellTopLeftOnHint(new Cell(a.row, a.col));
            double aw = a.vertical ? cellSize : cellSize * 2.0;
            double ah = a.vertical ? cellSize * 2.0 : cellSize;
            double ax = topLeft.getX() + a.offsetX + ANCHOR_INSET;
            double ay = topLeft.getY() + a.offsetY + ANCHOR_INSET;
            double aiw = aw - 2 * ANCHOR_INSET;
            double aih = ah - 2 * ANCHOR_INSET;

            if (rectsOverlap(ax, ay, aiw, aih, x, y, w, h)) return true;
        }
        return false;
    }

    // checks overlap with the hand bars
    private boolean rectOverlapsHandBars(double xHint, double yHint, double w, double h) {
        Bounds tableBounds = tableAreaOnOverlay();
        double ox = tableBounds.getMinX() + xHint;
        double oy = tableBounds.getMinY() + yHint;

        Bounds playerBounds = nodeBoundsOnOverlay(playerStrip);
        Bounds aiBounds     = nodeBoundsOnOverlay(aiStrip);

        return (playerBounds != null && rectsOverlap(ox, oy, w, h, playerBounds.getMinX(), playerBounds.getMinY(), playerBounds.getWidth(), playerBounds.getHeight()))
            || (aiBounds     != null && rectsOverlap(ox, oy, w, h, aiBounds.getMinX(),     aiBounds.getMinY(),     aiBounds.getWidth(),     aiBounds.getHeight()));
    }

    // checks overlap with already placed tiles
    private boolean rectOverlapsPlacedDominoes(double xHint, double yHint, double w, double h) {
        if (tableDominoes.isEmpty()) return false;
        Bounds tableBounds = tableAreaOnOverlay();
        double ax = tableBounds.getMinX() + xHint;
        double ay = tableBounds.getMinY() + yHint;

        for (PlacedDomino placedDomino : tableDominoes) {
            Node node = placedDomino.node;
            double bx = node.getLayoutX();
            double by = node.getLayoutY();
            double bw = node.getLayoutBounds().getWidth();
            double bh = node.getLayoutBounds().getHeight();
            if (rectsOverlap(ax, ay, w, h, bx, by, bw, bh)) return true;
        }
        return false;
    }

    // gets node bounds in overlay space safely
    private Bounds nodeBoundsOnOverlay(Node node) {
        try {
            Bounds sceneBounds = node.localToScene(node.getBoundsInLocal());
            return overlay.sceneToLocal(sceneBounds);
        } catch (Exception e) {
            return null;
        }
    }

    // tests rectangle overlap with a small epsilon
    private boolean rectsOverlap(double ax, double ay, double aw, double ah,
                                 double bx, double by, double bw, double bh) {
        final double EPS = 0.5;
        return ax < (bx + bw - EPS) &&
               (ax + aw) > (bx + EPS) &&
               ay < (by + bh - EPS) &&
               (ay + ah) > (by + EPS);
    }

    // draws anchor hints during the first turn
    private void repaintAnchorHints() {
        Bounds tableBounds = tableAreaOnOverlay();
        hintLayer.setManaged(false);
        hintLayer.setLayoutX(tableBounds.getMinX());
        hintLayer.setLayoutY(tableBounds.getMinY());
        hintLayer.setWidth(tableBounds.getWidth());
        hintLayer.setHeight(tableBounds.getHeight());

        GraphicsContext g = hintLayer.getGraphicsContext2D();
        g.clearRect(0, 0, hintLayer.getWidth(), hintLayer.getHeight());

        if (!showFirstTurnHints) return;

        g.setLineWidth(1.0);
        g.setStroke(HINT_STROKE);
        g.setFill(HINT_FILL);

        for (Anchor a : anchors) {
            Point2D px = cellTopLeftOnHint(new Cell(a.row, a.col));
            double w = a.vertical ? cellSize : cellSize * 2.0;
            double h = a.vertical ? cellSize * 2.0 : cellSize;

            double x = px.getX() + a.offsetX;
            double y = px.getY() + a.offsetY;

            g.fillRoundRect(x, y, w, h, HINT_ARC, HINT_ARC);
            g.strokeRoundRect(x, y, w, h, HINT_ARC, HINT_ARC);
        }
    }

    // returns true if a two-cell slot is in bounds
    private boolean inBoundsPair(boolean vertical, Cell topLeftCell) {
        return inBounds(topLeftCell) && inBounds(vertical ? new Cell(topLeftCell.row + 1, topLeftCell.col) : new Cell(topLeftCell.row, topLeftCell.col + 1));
    }

    // returns true if both cells are free
    private boolean anchorCellsFree(boolean vertical, Cell topLeftCell) {
        return !occupancy.containsKey(topLeftCell)
                && !occupancy.containsKey(vertical ? new Cell(topLeftCell.row + 1, topLeftCell.col) : new Cell(topLeftCell.row, topLeftCell.col + 1));
    }

    // finds the nearest matching anchor within a radius
    private Anchor nearestMatchingAnchor(Point2D dropCenter, CDominoes domino, boolean wantVertical, double radius) {
        double bestDist2 = radius * radius;
        Anchor best = null;
        for (Anchor a : anchors) {
            if (a.vertical != wantVertical) continue;
            if (!anchorFree(a)) continue;
            if (!matchesAnchor(domino, a)) continue;

            Point2D center = anchorCenter(a);
            double dx = center.getX() - dropCenter.getX();
            double dy = center.getY() - dropCenter.getY();
            double dist2 = dx * dx + dy * dy;
            if (dist2 < bestDist2) { bestDist2 = dist2; best = a; }
        }
        return best;
    }

    // returns true if the anchor cells are free
    private boolean anchorFree(Anchor a) {
        Cell topLeftCell = new Cell(a.row, a.col);
        return anchorCellsFree(a.vertical, topLeftCell);
    }

    // computes an anchor's center point on the overlay
    private Point2D anchorCenter(Anchor a) {
        double w = a.vertical ? cellSize : cellSize * 2;
        double h = a.vertical ? cellSize * 2 : cellSize;
        Point2D px = cellTopLeftOnOverlay(new Cell(a.row, a.col));
        return new Point2D(px.getX() + a.offsetX + w / 2.0, px.getY() + a.offsetY + h / 2.0);
    }

    // checks if a domino's values match the anchor
    private boolean matchesAnchor(CDominoes domino, Anchor a) {
        if (a.required == null) return true;

        if (a.vertical) {
            Integer top = domino.getTopValue();
            Integer bottom = domino.getBottomValue();
            boolean isDouble = (top != null && bottom != null && top.equals(bottom));

            switch (a.touch) {
                case NORTH:      return top != null && top.equals(a.required);
                case SOUTH:      return bottom != null && bottom.equals(a.required);
                case PERP_EAST:
                case PERP_WEST:
                    if (a.incomingMustBeDouble && !isDouble) return false;
                    return (top != null && top.equals(a.required)) || (bottom != null && bottom.equals(a.required));
                case PERP_SOUTH:
                    if (a.incomingMustBeDouble && !isDouble) return false;
                    return bottom != null && bottom.equals(a.required);
                case PERP_NORTH:
                    if (a.incomingMustBeDouble && !isDouble) return false;
                    return top != null && top.equals(a.required);
                default: return false;
            }
        } else {
            Integer left = domino.getLeftValue();
            Integer right = domino.getRightValue();
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

    // places a domino node and locks its events
    private void place(CDominoes domino, StackPane node, Cell topLeft, boolean vertical,
                       double offsetX, double offsetY, boolean capNorthSouthFlag) {
        PlacedDomino placedDomino = new PlacedDomino(domino, node, topLeft, vertical, offsetX, offsetY, capNorthSouthFlag);
        tableDominoes.add(placedDomino);

        occupancy.put(topLeft, placedDomino);
        occupancy.put(vertical ? new Cell(topLeft.row + 1, topLeft.col) : new Cell(topLeft.row, topLeft.col + 1), placedDomino);

        layoutOne(placedDomino);

        node.setOnMousePressed(null);
        node.setOnMouseDragged(null);
        node.setOnMouseReleased(null);
        node.setFocusTraversable(false);
        node.setMouseTransparent(true);
    }

    // lays out one placed domino on the overlay
    private void layoutOne(PlacedDomino placedDomino) {
        Cell topLeft = placedDomino.topLeft;
        boolean vertical = placedDomino.vertical;
        StackPane node = placedDomino.node;
        CDominoes domino = placedDomino.model;

        Point2D px = cellTopLeftOnOverlay(topLeft);
        double w = vertical ? cellSize : cellSize * 2;
        double h = vertical ? cellSize * 2 : cellSize;

        double x = px.getX() + placedDomino.offsetX;
        double y = px.getY() + placedDomino.offsetY;

        node.setPadding(Insets.EMPTY);
        node.setManaged(false);
        node.resizeRelocate(x, y, w, h);

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

    // restyles all placed tiles when hints turn off
    private void restyleAllPlacedDominoes() {
        if (tableDominoes.isEmpty()) return;
        for (PlacedDomino placedDomino : tableDominoes) {
            if (placedDomino.node != null) placedDomino.node.setStyle(showFirstTurnHints ? OUTLINE_STYLE : null);
        }
        repaintAnchorHints();
    }

    // binds grid math to table size changes
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

    // re-lays out all placed tiles
    private void relayoutAllPlaced() {
        if (tableDominoes.isEmpty()) return;
        for (PlacedDomino placedDomino : tableDominoes) layoutOne(placedDomino);
    }

    // sizes and positions the hint canvas to the table
    private void layoutHintCanvas() {
        Bounds tableBounds = tableAreaOnOverlay();
        hintLayer.setLayoutX(tableBounds.getMinX());
        hintLayer.setLayoutY(tableBounds.getMinY());
        hintLayer.setWidth(tableBounds.getWidth());
        hintLayer.setHeight(tableBounds.getHeight());
    }

    // computes grid cell size and counts
    private void computeGrid() {
        double w = hintLayer.getWidth();
        double h = hintLayer.getHeight();
        if (w <= 0 || h <= 0) { rowCount = colCount = 0; return; }

        double target = Math.min(w, h) / 18.0;
        cellSize = clamp(target, MIN_CELL_SIZE, MAX_CELL_SIZE);
        colCount = Math.max(3, (int) Math.floor(w / cellSize));
        rowCount = Math.max(3, (int) Math.floor(h / cellSize));
    }

    // ensures grid math and anchors exist
    private void ensureGridReady() {
        if (rowCount <= 0 || colCount <= 0) {
            layoutHintCanvas();
            computeGrid();
            relayoutAllPlaced();
            repaintAnchorHints();
        }
        if (tableDominoes.isEmpty() && anchors.isEmpty() && rowCount > 0 && colCount > 0) {
            rebuildAnchors();
        }
    }

    private static final class CenterSeed {
        final Cell verticalTopLeft, horizontalTopLeft;
        final double verticalOffsetX, verticalOffsetY, horizontalOffsetX, horizontalOffsetY;
        CenterSeed(Cell vTopLeft, Cell hTopLeft, double vDX, double vDY, double hDX, double hDY) {
            this.verticalTopLeft = vTopLeft; this.horizontalTopLeft = hTopLeft;
            this.verticalOffsetX = vDX; this.verticalOffsetY = vDY;
            this.horizontalOffsetX = hDX; this.horizontalOffsetY = hDY;
        }
    }

    // computes center-aligned seeds for the very first move
    private CenterSeed computeCenterSeed() {
        Bounds tableBounds = tableAreaOnOverlay();
        double ox = tableBounds.getMinX();
        double oy = tableBounds.getMinY();
        double centerX = ox + tableBounds.getWidth()  / 2.0;
        double centerY = oy + tableBounds.getHeight() / 2.0;

        double vTopLeftX = centerX - cellSize / 2.0;
        double vTopLeftY = centerY - cellSize;
        int vCol = clampIndex((int) Math.floor((vTopLeftX - ox) / cellSize), 0, Math.max(0, colCount - 1));
        int vRow = clampIndex((int) Math.floor((vTopLeftY - oy) / cellSize), 0, Math.max(0, rowCount - 2));
        Cell vTopLeft = new Cell(vRow, vCol);
        Point2D vTopLeftPx = cellTopLeftOnOverlay(vTopLeft);
        double vCenterX = vTopLeftPx.getX() + cellSize / 2.0;
        double vCenterY = vTopLeftPx.getY() + cellSize;
        double vDX = centerX - vCenterX;
        double vDY = centerY - vCenterY;

        double hTopLeftX = centerX - cellSize;
        double hTopLeftY = centerY - cellSize / 2.0;
        int hCol = clampIndex((int) Math.floor((hTopLeftX - ox) / cellSize), 0, Math.max(0, colCount - 2));
        int hRow = clampIndex((int) Math.floor((hTopLeftY - oy) / cellSize), 0, Math.max(0, rowCount - 1));
        Cell hTopLeft = new Cell(hRow, hCol);
        Point2D hTopLeftPx = cellTopLeftOnOverlay(hTopLeft);
        double hCenterX = hTopLeftPx.getX() + cellSize;
        double hCenterY = hTopLeftPx.getY() + cellSize / 2.0;
        double hDX = centerX - hCenterX;
        double hDY = centerY - hCenterY;

        return new CenterSeed(vTopLeft, hTopLeft, vDX, vDY, hDX, hDY);
    }

    // clamps index between safe bounds
    private int clampIndex(int value, int low, int high) {
        if (high < low) return low;
        return Math.max(low, Math.min(high, value));
    }

    // checks single cell is in grid bounds
    private boolean inBounds(Cell cell) {
        return cell.row >= 0 && cell.col >= 0 && cell.row < rowCount && cell.col < colCount;
    }

    // computes visible table bounds in overlay space
    private Bounds tableAreaOnOverlay() {
        Node tablePane = tableTop.getParent();
        Bounds tableScene = (tablePane != null ? tablePane.localToScene(tablePane.getBoundsInLocal())
                                               : tableTop.localToScene(tableTop.getBoundsInLocal()));
        return overlay.sceneToLocal(tableScene);
    }

    // gets top-left pixel of a cell on overlay
    private Point2D cellTopLeftOnOverlay(Cell cell) {
        Bounds tableBounds = tableAreaOnOverlay();
        return new Point2D(tableBounds.getMinX() + cell.col * cellSize, tableBounds.getMinY() + cell.row * cellSize);
    }

    // gets top-left pixel of a cell on hint layer
    private Point2D cellTopLeftOnHint(Cell cell) {
        return new Point2D(cell.col * cellSize, cell.row * cellSize);
    }

    // advances to the next side
    private void nextTurn() {
        turnManager.next();
    }

    // clamps a number within limits
    private static double clamp(double value, double low, double high) { return Math.max(low, Math.min(high, value)); }

    // compares doubles with tiny tolerance
    private static boolean almost(double a, double b) { return Math.abs(a - b) < 0.0001; }

    private static final class Cell {
        final int row, col;
        Cell(int row, int col) { this.row = row; this.col = col; }
        @Override public boolean equals(Object o) { if (!(o instanceof Cell)) return false; Cell x = (Cell) o; return x.row == row && x.col == col; }
        @Override public int hashCode() { return Objects.hash(row, col); }
        @Override public String toString() { return "(" + row + "," + col + ")"; }
    }

    private static final class PlacedDomino {
        final CDominoes model;
        final StackPane node;
        final Cell topLeft;
        final boolean vertical;
        final double offsetX, offsetY;
        final boolean capNorthSouth;

        PlacedDomino(CDominoes model, StackPane node, Cell topLeft, boolean vertical, double offsetX, double offsetY, boolean capNorthSouth) {
            this.model = model; this.node = node; this.topLeft = topLeft; this.vertical = vertical; this.offsetX = offsetX; this.offsetY = offsetY;
            this.capNorthSouth = vertical && capNorthSouth;
        }
        boolean isVerticalDouble() {
            if (!vertical) return false;
            Integer t = model.getTopValue(), b = model.getBottomValue();
            return t != null && b != null && t.equals(b);
        }
        boolean isHorizontalDouble() {
            if (vertical) return false;
            Integer l = model.getLeftValue(), r = model.getRightValue();
            return l != null && r != null && l.equals(r);
        }
    }

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

    // returns a read-only list of current anchor needs
    public List<AnchorHint> snapshotAnchors() {
        List<AnchorHint> list = new ArrayList<>(anchors.size());
        for (Anchor a : anchors) {
            list.add(new AnchorHint(a.vertical, a.required, a.touch.name(), a.incomingMustBeDouble));
        }
        return Collections.unmodifiableList(list);
    }
}
