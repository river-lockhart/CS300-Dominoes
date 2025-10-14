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

public class AvailablePiecesOverlay<T> {

    private final Pane hostPane;
    private final Supplier<List<T>> itemSource;
    private final Function<T, Node> itemRenderer;

    private final StackPane cardPanel = new StackPane();
    private final GridPane itemGrid = new GridPane();

    private int colCount = 4;
    private int rowCount = 2;
    private boolean isShowing = false;

    private double cellWidth = 64;
    private double cellHeight = 64;
    private double horizontalGap = 8;
    private double verticalGap = 8;

    private final javafx.event.EventHandler<MouseEvent> clickAwayFilter = e -> {
        if (!isShowing) return;
        Object target = e.getTarget();
        if (target instanceof Node) {
            Node node = (Node) target;
            if (isDescendantOf(node, cardPanel)) return;
        }
        hide();
    };

    // builds overlay card and positions it at top right
    public AvailablePiecesOverlay(Pane hostPane,
                                  Supplier<List<T>> itemSource,
                                  Function<T, Node> itemRenderer) {
        this.hostPane = Objects.requireNonNull(hostPane);
        this.itemSource = Objects.requireNonNull(itemSource);
        this.itemRenderer = Objects.requireNonNull(itemRenderer);

        cardPanel.setBackground(new Background(new BackgroundFill(
                Color.rgb(20,20,20,0.92), new CornerRadii(10), Insets.EMPTY)));
        cardPanel.setBorder(new Border(new BorderStroke(
                Color.color(1,1,1,0.08), BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(1))));
        cardPanel.setPadding(new Insets(8));
        cardPanel.setPickOnBounds(true);
        cardPanel.setMouseTransparent(false);
        cardPanel.setVisible(false);
        cardPanel.setManaged(false);

        itemGrid.setHgap(horizontalGap);
        itemGrid.setVgap(verticalGap);
        itemGrid.setAlignment(Pos.CENTER);
        cardPanel.getChildren().add(itemGrid);

        cardPanel.addEventFilter(MouseEvent.MOUSE_PRESSED, MouseEvent::consume);

        hostPane.getChildren().add(cardPanel);

        double marginTop = 10.0;
        double marginRight = 16.0;
        cardPanel.layoutYProperty().set(marginTop);

        cardPanel.layoutXProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(0.0, hostPane.getWidth() - cardPanel.getWidth() - marginRight),
                hostPane.widthProperty(), cardPanel.widthProperty()
        ));

        cardPanel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    }

    // sets grid columns and rows
    public void setGrid(int cols, int rows) {
        this.colCount = Math.max(1, cols);
        this.rowCount = Math.max(1, rows);
        if (isShowing) refresh();
    }

    // sets fixed cell width and height
    public void setCellSize(double width, double height) {
        this.cellWidth = Math.max(24, width);
        this.cellHeight = Math.max(24, height);
        if (isShowing) refresh();
    }

    // toggles overlay visibility
    public void toggle() { if (isShowing) hide(); else show(); }

    // shows overlay and installs click-away handler
    public void show() {
        if (isShowing) return;
        refresh();
        cardPanel.setVisible(true);
        isShowing = true;

        Scene scene = sceneOf(hostPane);
        if (scene != null) scene.addEventFilter(MouseEvent.MOUSE_PRESSED, clickAwayFilter);
    }

    // hides overlay and removes click-away handler
    public void hide() {
        if (!isShowing) return;
        cardPanel.setVisible(false);
        isShowing = false;

        Scene scene = sceneOf(hostPane);
        if (scene != null) scene.removeEventFilter(MouseEvent.MOUSE_PRESSED, clickAwayFilter);
    }

    // rebuilds grid to match fixed cell sizes
    // computes row and column using index math
    private void refresh() {
        itemGrid.getChildren().clear();
        itemGrid.getColumnConstraints().clear();
        itemGrid.getRowConstraints().clear();

        List<ColumnConstraints> columnRules = new ArrayList<>(colCount);
        for (int col = 0; col < colCount; col++) {
            ColumnConstraints colRule = new ColumnConstraints();
            colRule.setMinWidth(cellWidth);
            colRule.setPrefWidth(cellWidth);
            colRule.setMaxWidth(cellWidth);
            columnRules.add(colRule);
        }
        itemGrid.getColumnConstraints().addAll(columnRules);

        List<RowConstraints> rowRules = new ArrayList<>(rowCount);
        for (int row = 0; row < rowCount; row++) {
            RowConstraints rowRule = new RowConstraints();
            rowRule.setMinHeight(cellHeight);
            rowRule.setPrefHeight(cellHeight);
            rowRule.setMaxHeight(cellHeight);
            rowRules.add(rowRule);
        }
        itemGrid.getRowConstraints().addAll(rowRules);

        List<T> items = itemSource.get();
        if (items != null && !items.isEmpty()) {
            int maxItems = Math.min(items.size(), colCount * rowCount);
            for (int i = 0; i < maxItems; i++) {
                int row = i / colCount;
                int col = i % colCount;

                Node content = itemRenderer.apply(items.get(i));

                StackPane slot = new StackPane(content);
                slot.setAlignment(Pos.CENTER);
                slot.setMinSize(cellWidth, cellHeight);
                slot.setPrefSize(cellWidth, cellHeight);
                slot.setMaxSize(cellWidth, cellHeight);
                GridPane.setFillWidth(slot, false);
                GridPane.setFillHeight(slot, false);

                itemGrid.add(slot, col, row);
            }
        }

        cardPanel.applyCss();
        cardPanel.autosize();
    }

    // refreshes grid only when overlay is showing
    public void refreshIfShowing() {
        if (isShowing) refresh();
    }

    // checks if node is inside the given parent
    private static boolean isDescendantOf(Node node, Parent parent) {
        while (node != null) {
            if (node == parent) return true;
            node = node.getParent();
        }
        return false;
    }

    // returns the scene for the given node
    private static Scene sceneOf(Node node) { return node == null ? null : node.getScene(); }
}
