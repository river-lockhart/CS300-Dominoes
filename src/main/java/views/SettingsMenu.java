package views;

import controllers.Music;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.scene.text.Font;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;

public class SettingsMenu {
    private final StackPane pauseScreen;
    private final VBox menuPanel;

    private final Slider volumeSlider = new Slider(0, 10, 6);
    private final Label volumeLabel = new Label("VOLUME");

    private final Label modeLabel = new Label("FULLSCREEN");
    private final Button arrowLeft = new Button("◀");
    private final Button arrowRight = new Button("▶");

    private final Button backButton = new Button("BACK");

    private final StackPane sliderStack = new StackPane();
    private final Pane tickPane = new Pane();
    private static final int TICK_DIVISIONS = 10;
    private static final double TICK_WIDTH = 3.0;

    private final HBox modeRow = new HBox(12);
    private final StackPane arrowBoxLeft = new StackPane();
    private final StackPane arrowBoxRight = new StackPane();
    private final StackPane modeBox = new StackPane();

    // builds the settings overlay and connects behaviors
    public SettingsMenu(Stage stage, PauseMenu pauseMenu) {
        pauseScreen = new StackPane();
        pauseScreen.setVisible(false);
        pauseScreen.setPickOnBounds(true);

        Rectangle dimScreen = PauseMenu.createDimmer(pauseScreen, 0.55);

        menuPanel = new VBox();
        menuPanel.setAlignment(Pos.CENTER);
        menuPanel.setPadding(new Insets(20));
        menuPanel.setBackground(new Background(
                new BackgroundFill(Color.BLACK, new CornerRadii(10), Insets.EMPTY)));
        menuPanel.setBorder(new Border(new BorderStroke(
                Color.WHITE, BorderStrokeStyle.SOLID,
                new CornerRadii(10), new BorderWidths(2))));
        menuPanel.setFillWidth(false);

        menuPanel.prefWidthProperty().bind(pauseScreen.widthProperty().multiply(0.40));
        menuPanel.prefHeightProperty().bind(pauseScreen.heightProperty().multiply(0.70));
        menuPanel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        PauseMenu.applyFramedButtonStyle(backButton);
        backButton.prefWidthProperty().bind(menuPanel.widthProperty().multiply(0.50));
        backButton.prefHeightProperty().bind(menuPanel.heightProperty().multiply(0.18));
        backButton.setMinHeight(36);

        volumeLabel.setTextFill(Color.WHITE);
        volumeLabel.setAlignment(Pos.CENTER);
        volumeLabel.setMaxWidth(Double.MAX_VALUE);
        volumeLabel.prefWidthProperty().bind(backButton.widthProperty());

        sliderStack.setPickOnBounds(false);
        sliderStack.prefWidthProperty().bind(backButton.widthProperty());
        sliderStack.minWidthProperty().bind(backButton.widthProperty());
        sliderStack.maxWidthProperty().bind(backButton.widthProperty());

        volumeSlider.setBlockIncrement(1);
        volumeSlider.setMajorTickUnit(1);
        volumeSlider.setMinorTickCount(0);
        volumeSlider.setSnapToTicks(true);
        volumeSlider.setShowTickMarks(false);
        volumeSlider.setShowTickLabels(false);
        volumeSlider.setMinWidth(0);
        volumeSlider.prefWidthProperty().bind(sliderStack.widthProperty());
        sliderStack.getChildren().add(volumeSlider);

        tickPane.setMouseTransparent(true);
        tickPane.prefWidthProperty().bind(sliderStack.widthProperty());
        tickPane.minWidthProperty().bind(sliderStack.widthProperty());
        tickPane.maxWidthProperty().bind(sliderStack.widthProperty());
        tickPane.prefHeightProperty().bind(volumeSlider.heightProperty());
        tickPane.minHeightProperty().bind(volumeSlider.heightProperty());
        tickPane.maxHeightProperty().bind(volumeSlider.heightProperty());
        StackPane.setAlignment(tickPane, Pos.CENTER_LEFT);
        sliderStack.getChildren().add(tickPane);
        buildTicks(tickPane, TICK_DIVISIONS);

        VBox volumeColumn = new VBox(8, volumeLabel, sliderStack);
        volumeColumn.setAlignment(Pos.CENTER);

        modeRow.setAlignment(Pos.CENTER_LEFT);
        modeRow.prefWidthProperty().bind(backButton.widthProperty());
        modeRow.minWidthProperty().bind(backButton.widthProperty());
        modeRow.maxWidthProperty().bind(backButton.widthProperty());
        modeRow.prefHeightProperty().bind(backButton.heightProperty());
        modeRow.minHeightProperty().bind(backButton.heightProperty());
        modeRow.maxHeightProperty().bind(backButton.heightProperty());

        String boxStyle = "-fx-background-color: transparent; -fx-border-color: white; -fx-border-width: 5px; -fx-border-radius: 10px; -fx-background-radius: 10px;";
        arrowBoxLeft.setStyle(boxStyle);
        arrowBoxRight.setStyle(boxStyle);
        modeBox.setStyle(boxStyle);

        String arrowButtonStyle = "-fx-background-color: transparent; -fx-text-fill: white;";
        arrowLeft.setStyle(arrowButtonStyle);
        arrowRight.setStyle(arrowButtonStyle);
        arrowLeft.setFocusTraversable(false);
        arrowRight.setFocusTraversable(false);
        arrowBoxLeft.getChildren().add(arrowLeft);
        arrowBoxRight.getChildren().add(arrowRight);
        StackPane.setAlignment(arrowLeft, Pos.CENTER);
        StackPane.setAlignment(arrowRight, Pos.CENTER);

        for (Region region : new Region[]{arrowBoxLeft, arrowBoxRight, modeBox}) {
            region.prefHeightProperty().bind(modeRow.heightProperty());
            region.minHeightProperty().bind(modeRow.heightProperty());
            region.maxHeightProperty().bind(modeRow.heightProperty());
        }

        DoubleBinding usableWidth = Bindings.createDoubleBinding(
                () -> Math.max(0, modeRow.getWidth() - modeRow.getSpacing()),
                modeRow.widthProperty()
        );

        arrowBoxLeft.prefWidthProperty().bind(usableWidth.multiply(0.10));
        arrowBoxLeft.minWidthProperty().bind(usableWidth.multiply(0.10));
        arrowBoxLeft.maxWidthProperty().bind(usableWidth.multiply(0.10));
        arrowBoxRight.prefWidthProperty().bind(usableWidth.multiply(0.10));
        arrowBoxRight.minWidthProperty().bind(usableWidth.multiply(0.10));
        arrowBoxRight.maxWidthProperty().bind(usableWidth.multiply(0.10));

        modeBox.prefWidthProperty().bind(usableWidth.multiply(0.90));
        modeBox.minWidthProperty().bind(usableWidth.multiply(0.90));
        modeBox.maxWidthProperty().bind(usableWidth.multiply(0.90));

        modeLabel.setTextFill(Color.WHITE);
        modeLabel.setAlignment(Pos.CENTER);
        modeLabel.setMaxWidth(Double.MAX_VALUE);
        modeBox.getChildren().add(modeLabel);
        StackPane.setAlignment(modeLabel, Pos.CENTER);

        modeRow.getChildren().setAll(modeBox, arrowBoxRight);

        pauseScreen.getChildren().addAll(dimScreen, menuPanel);
        StackPane.setAlignment(menuPanel, Pos.CENTER);

        Region spacerOne = new Region();
        Region spacerTwo = new Region();

        spacerOne.prefHeightProperty().bind(backButton.heightProperty().multiply(0.35));
        spacerOne.minHeightProperty().bind(backButton.heightProperty().multiply(0.35));
        spacerOne.maxHeightProperty().bind(backButton.heightProperty().multiply(0.35));

        spacerTwo.prefHeightProperty().bind(backButton.heightProperty().multiply(0.35));
        spacerTwo.minHeightProperty().bind(backButton.heightProperty().multiply(0.35));
        spacerTwo.maxHeightProperty().bind(backButton.heightProperty().multiply(0.35));

        menuPanel.getChildren().addAll(volumeColumn, spacerOne, modeRow, spacerTwo, backButton);

        int startVolume = Math.max(0, Math.min(10, Music.getVolume()));
        volumeSlider.setValue(startVolume);

        volumeSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            // snaps slider to nearest tick
            double rounded = Math.rint(newValue.doubleValue());
            if (volumeSlider.isValueChanging() && rounded != newValue.doubleValue()) {
                volumeSlider.setValue(rounded);
                return;
            }
            Music.setVolume((int) Math.round(volumeSlider.getValue()));
        });

        hookTrackAlignment();

        updateWindowModeUI(stage.isFullScreen());

        arrowRight.setOnAction(e -> { stage.setFullScreen(false); updateWindowModeUI(false); });
        arrowLeft.setOnAction(e ->  { stage.setFullScreen(true);  updateWindowModeUI(true);  });

        backButton.heightProperty().addListener((o, oldHeight, newHeight) -> {
            // keeps font sizes in sync
            double h = newHeight.doubleValue();
            if (h > 0) {
                double f = h * 0.25;
                backButton.setFont(Font.font(f));
                volumeLabel.setFont(Font.font(f));
                modeLabel.setFont(Font.font(f));
                arrowLeft.setFont(Font.font(f * 0.8));
                arrowRight.setFont(Font.font(f * 0.8));
            }
        });
        Platform.runLater(() -> {
            // applies font size after first layout
            double h = backButton.getHeight();
            if (h > 0) {
                double f = h * 0.25;
                backButton.setFont(Font.font(f));
                volumeLabel.setFont(Font.font(f));
                modeLabel.setFont(Font.font(f));
                arrowLeft.setFont(Font.font(f * 0.8));
                arrowRight.setFont(Font.font(f * 0.8));
            }
        });

        backButton.setOnAction(e -> {
            // returns to the pause menu
            hide();
            pauseMenu.show();
        });
    }

    // shows the settings overlay
    public void show() {
        pauseScreen.setVisible(true);
        pauseScreen.toFront();
        pauseScreen.requestFocus();
    }

    // hides the settings overlay
    public void hide() {
        pauseScreen.setVisible(false);
    }

    // returns the overlay node for layout
    public StackPane getView() {
        return pauseScreen;
    }

    // updates text and arrows based on window mode
    private void updateWindowModeUI(boolean isFullscreen) {
        modeLabel.setText(isFullscreen ? "FULLSCREEN" : "WINDOWED");

        if (isFullscreen) {
            arrowBoxLeft.setManaged(false);
            arrowBoxLeft.setVisible(false);
            arrowBoxRight.setManaged(true);
            arrowBoxRight.setVisible(true);
            modeRow.getChildren().setAll(modeBox, arrowBoxRight);
        } else {
            arrowBoxLeft.setManaged(true);
            arrowBoxLeft.setVisible(true);
            arrowBoxRight.setManaged(false);
            arrowBoxRight.setVisible(false);
            modeRow.getChildren().setAll(arrowBoxLeft, modeBox);
        }
    }

    // builds tick marks for the slider
    private void buildTicks(Pane targetPane, int divisions) {
        targetPane.getChildren().clear();
        for (int index = 0; index <= divisions; index++) {
            Region line = new Region();
            line.setStyle("-fx-background-color: white;");
            line.setMinWidth(TICK_WIDTH);
            line.setPrefWidth(TICK_WIDTH);
            line.setMaxWidth(TICK_WIDTH);
            targetPane.getChildren().add(line);
        }
    }

    // aligns ticks with the slider track
    private void hookTrackAlignment() {
        Runnable tryHook = new Runnable() {
            @Override public void run() {
                volumeSlider.applyCss();
                Node track = volumeSlider.lookup(".track");
                if (track == null) {
                    Platform.runLater(this);
                    return;
                }
                ChangeListener relayout = new ChangeListener();
                track.boundsInLocalProperty().addListener(relayout);
                track.boundsInParentProperty().addListener(relayout);
                volumeSlider.widthProperty().addListener(relayout);
                volumeSlider.heightProperty().addListener(relayout);
                sliderStack.widthProperty().addListener(relayout);
                sliderStack.heightProperty().addListener(relayout);
                pauseScreen.widthProperty().addListener(relayout);
                pauseScreen.heightProperty().addListener(relayout);
                layoutTicksToTrack(track);
            }
        };
        Platform.runLater(tryHook);
    }

    // watches size changes and realigns the ticks
    private class ChangeListener implements javafx.beans.value.ChangeListener<Object> {
        @Override public void changed(javafx.beans.value.ObservableValue<?> o, Object a, Object b) {
            Node track = volumeSlider.lookup(".track");
            if (track != null) layoutTicksToTrack(track);
        }
    }

    // positions ticks to match the slider track
    private void layoutTicksToTrack(Node track) {
        if (tickPane.getChildren().isEmpty()) return;

        Bounds trackInScene = track.localToScene(track.getBoundsInLocal());
        Bounds trackInStack = sliderStack.sceneToLocal(trackInScene);

        double trackStartX = trackInStack.getMinX();
        double trackWidth = Math.max(0, trackInStack.getWidth());
        double slideSpan = Math.max(0, trackWidth - TICK_WIDTH);

        // sets simple sizes for tick layout math
        double paneHeight = Math.max(0, tickPane.getHeight());
        double tickHeight = paneHeight * 0.60;
        double offsetY = (paneHeight - tickHeight) / 2.0;

        int tickCount = TICK_DIVISIONS + 1;
        for (int index = 0; index < tickCount; index++) {
            Region line = (Region) tickPane.getChildren().get(index);
            double posX = trackStartX + (index / (double) TICK_DIVISIONS) * slideSpan;
            line.setLayoutX(posX);
            line.setLayoutY(offsetY);
            line.setPrefHeight(tickHeight);
            line.setMinHeight(tickHeight);
            line.setMaxHeight(tickHeight);
        }
    }
}
