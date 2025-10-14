package views;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Minimal floating overlay: a compact card in the top-right that shows a grid of items.
 * - No fullscreen sheet; just a small panel sized to fixed cells (so background matches content).
 * - Click outside the panel to dismiss (scene-level filter).
 * - No title/close button; toggle via your button.
 */
public class AvailablePiecesOverlay<T> {

    private final Pane host;                      // layered above the table (e.g., CTable root AnchorPane)
    private final Supplier<List<T>> supplier;     // provides current items
    private final Function<T, Node> renderer;     // renders a tiny domino (no borders)

    private final StackPane panel = new StackPane();
    private final GridPane grid = new GridPane();

    private int cols = 4;
    private int rows = 2;
    private boolean showing = false;

    // Fixed cell size so the panel background always matches the domino footprint
    private double CELL_W = 64;
    private double CELL_H = 64;
    private double HGAP   = 8;
    private double VGAP   = 8;

    // scene-level click-away filter (no big transparent node)
    private final javafx.event.EventHandler<MouseEvent> clickAway = e -> {
        if (!showing) return;
        Object tgt = e.getTarget();
        if (tgt instanceof Node) {
            Node n = (Node) tgt;
            if (isDescendantOf(n, panel)) return; // click inside panel → ignore
        }
        hide();
    };

    public AvailablePiecesOverlay(Pane host,
                                  Supplier<List<T>> supplier,
                                  Function<T, Node> renderer) {
        this.host = Objects.requireNonNull(host);
        this.supplier = Objects.requireNonNull(supplier);
        this.renderer = Objects.requireNonNull(renderer);

        // tiny floating card; sized strictly to content
        panel.setBackground(new Background(new BackgroundFill(
                Color.rgb(20,20,20,0.92), new CornerRadii(10), Insets.EMPTY)));
        panel.setBorder(new Border(new BorderStroke(
                Color.color(1,1,1,0.08), BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(1))));
        panel.setPadding(new Insets(8));
        panel.setPickOnBounds(true);
        panel.setMouseTransparent(false);
        panel.setVisible(false);
        panel.setManaged(false); // manual positioning

        // grid inside the card
        grid.setHgap(HGAP);
        grid.setVgap(VGAP);
        grid.setAlignment(Pos.CENTER);
        panel.getChildren().add(grid);

        // clicks inside the panel shouldn't close it
        panel.addEventFilter(MouseEvent.MOUSE_PRESSED, MouseEvent::consume);

        // mount panel once; keep hidden until show()
        host.getChildren().add(panel);

        // robust top-right positioning (works for any Pane)
        final double MARGIN_TOP   = 10.0;
        final double MARGIN_RIGHT = 16.0; // nudge so the 4th column stays visible
        panel.layoutYProperty().set(MARGIN_TOP);

        // clamp X to avoid peeking off the right, even as images load
        panel.layoutXProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(0.0, host.getWidth() - panel.getWidth() - MARGIN_RIGHT),
                host.widthProperty(), panel.widthProperty()
        ));

        // keep the panel as small as the grid + padding
        panel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    }

    /** Configure grid size (defaults 4×2). */
    public void setGrid(int cols, int rows) {
        this.cols = Math.max(1, cols);
        this.rows = Math.max(1, rows);
        if (showing) refresh();
    }

    /** Optionally tweak the fixed cell size (defaults 64×64). */
    public void setCellSize(double w, double h) {
        this.CELL_W = Math.max(24, w);
        this.CELL_H = Math.max(24, h);
        if (showing) refresh();
    }

    public void toggle() { if (showing) hide(); else show(); }

    public void show() {
        if (showing) return;
        refresh();
        panel.setVisible(true);
        showing = true;

        Scene sc = sceneOf(host);
        if (sc != null) sc.addEventFilter(MouseEvent.MOUSE_PRESSED, clickAway);
    }

    public void hide() {
        if (!showing) return;
        panel.setVisible(false);
        showing = false;

        Scene sc = sceneOf(host);
        if (sc != null) sc.removeEventFilter(MouseEvent.MOUSE_PRESSED, clickAway);
    }

    /** Rebuild grid with fixed-size slots so background always matches content. */
    private void refresh() {
        grid.getChildren().clear();
        grid.getColumnConstraints().clear();
        grid.getRowConstraints().clear();

        // build fixed constraints for a predictable panel size
        List<ColumnConstraints> cc = new ArrayList<>(cols);
        for (int c = 0; c < cols; c++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setMinWidth(CELL_W);
            col.setPrefWidth(CELL_W);
            col.setMaxWidth(CELL_W);
            cc.add(col);
        }
        grid.getColumnConstraints().addAll(cc);

        List<RowConstraints> rc = new ArrayList<>(rows);
        for (int r = 0; r < rows; r++) {
            RowConstraints row = new RowConstraints();
            row.setMinHeight(CELL_H);
            row.setPrefHeight(CELL_H);
            row.setMaxHeight(CELL_H);
            rc.add(row);
        }
        grid.getRowConstraints().addAll(rc);

        List<T> items = supplier.get();
        if (items != null && !items.isEmpty()) {
            int max = Math.min(items.size(), cols * rows);
            for (int i = 0; i < max; i++) {
                int r = i / cols;
                int c = i % cols;

                Node content = renderer.apply(items.get(i));
                // wrap in a fixed-size cell so the panel width/height are correct immediately
                StackPane slot = new StackPane(content);
                slot.setAlignment(Pos.CENTER);
                slot.setMinSize(CELL_W, CELL_H);
                slot.setPrefSize(CELL_W, CELL_H);
                slot.setMaxSize(CELL_W, CELL_H);
                GridPane.setFillWidth(slot, false);
                GridPane.setFillHeight(slot, false);

                grid.add(slot, c, r);
            }
        }

        // snap to final size so position clamp uses accurate width
        panel.applyCss();
        panel.autosize();
    }

    /** Allow host to live-refresh while open (e.g., after draws). */
    public void refreshIfShowing() {
        if (showing) refresh();
    }

    private static boolean isDescendantOf(Node n, Parent p) {
        while (n != null) {
            if (n == p) return true;
            n = n.getParent();
        }
        return false;
    }

    private static Scene sceneOf(Node n) { return n == null ? null : n.getScene(); }
}
