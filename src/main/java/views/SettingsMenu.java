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

    // volume slider controls
    private final Slider volumeSlider = new Slider(0, 10, 6);
    private final Label volumeLabel = new Label("VOLUME");

    // window mode switch controls
    private final Label modeLabel = new Label("FULLSCREEN");
    private final Button caretLeft = new Button("◀");
    private final Button caretRight = new Button("▶");

    // back button
    private final Button backButton = new Button("BACK");

    // for  tick alignment
    private final StackPane sliderStack = new StackPane();
    private final Pane tickPane = new Pane();
    private static final int TICK_DIVISIONS = 10;
    private static final double TICK_WIDTH = 3.0;

    // boxes for window mode parts
    private final HBox modeRow = new HBox(12);            
    private final StackPane caretBoxLeft = new StackPane();  
    private final StackPane caretBoxRight = new StackPane(); 
    private final StackPane modeBox = new StackPane();       

    public SettingsMenu(Stage stage, PauseMenu pauseMenu) {
        // overlay over the pause menu
        pauseScreen = new StackPane();
        pauseScreen.setVisible(false);
        pauseScreen.setPickOnBounds(true);

        // dimmed background
        Rectangle dimScreen = new Rectangle();
        dimScreen.setFill(Color.rgb(0, 0, 0, 0.55));
        dimScreen.widthProperty().bind(pauseScreen.widthProperty());
        dimScreen.heightProperty().bind(pauseScreen.heightProperty());

        // panel for the settings pieces
        menuPanel = new VBox();
        menuPanel.setAlignment(Pos.CENTER);
        menuPanel.setPadding(new Insets(20));
        menuPanel.setBackground(new Background(
                new BackgroundFill(Color.BLACK, new CornerRadii(10), Insets.EMPTY)));
        menuPanel.setBorder(new Border(new BorderStroke(
                Color.WHITE, BorderStrokeStyle.SOLID,
                new CornerRadii(10), new BorderWidths(2))));
        menuPanel.setFillWidth(false);

        // set panel size to be a percent of the overlay
        menuPanel.prefWidthProperty().bind(pauseScreen.widthProperty().multiply(0.40));
        menuPanel.prefHeightProperty().bind(pauseScreen.heightProperty().multiply(0.70));
        menuPanel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        // back button 
        backButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: white; -fx-border-width: 5px; -fx-border-radius: 10px;");
        backButton.setFocusTraversable(false);
        // change size based on panel
        backButton.prefWidthProperty().bind(menuPanel.widthProperty().multiply(0.50));
        backButton.prefHeightProperty().bind(menuPanel.heightProperty().multiply(0.18));
        backButton.setMinHeight(36);

        // volume label above slider
        volumeLabel.setTextFill(Color.WHITE);
        volumeLabel.setAlignment(Pos.CENTER);
        volumeLabel.setMaxWidth(Double.MAX_VALUE);
        // chang size based on back button
        volumeLabel.prefWidthProperty().bind(backButton.widthProperty());

        // match the size of the slider with the size of the back button
        sliderStack.setPickOnBounds(false);
        sliderStack.prefWidthProperty().bind(backButton.widthProperty());
        sliderStack.minWidthProperty().bind(backButton.widthProperty());
        sliderStack.maxWidthProperty().bind(backButton.widthProperty());

        // set up how the slider functions
        volumeSlider.setBlockIncrement(1);
        volumeSlider.setMajorTickUnit(1);
        volumeSlider.setMinorTickCount(0);
        volumeSlider.setSnapToTicks(true);
        volumeSlider.setShowTickMarks(false);
        volumeSlider.setShowTickLabels(false);
        volumeSlider.setMinWidth(0);
        volumeSlider.prefWidthProperty().bind(sliderStack.widthProperty());
        sliderStack.getChildren().add(volumeSlider);

        // overlay the tick on the slider
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

        VBox volCol = new VBox(8, volumeLabel, sliderStack);
        volCol.setAlignment(Pos.CENTER);

        // change size of window mode box with back button
        modeRow.setAlignment(Pos.CENTER_LEFT);
        modeRow.prefWidthProperty().bind(backButton.widthProperty());
        modeRow.minWidthProperty().bind(backButton.widthProperty());
        modeRow.maxWidthProperty().bind(backButton.widthProperty());
        modeRow.prefHeightProperty().bind(backButton.heightProperty());
        modeRow.minHeightProperty().bind(backButton.heightProperty());
        modeRow.maxHeightProperty().bind(backButton.heightProperty());

        // add border the window mode pieces
        String boxStyle = "-fx-background-color: transparent; -fx-border-color: white; -fx-border-width: 5px; -fx-border-radius: 10px; -fx-background-radius: 10px;";
        caretBoxLeft.setStyle(boxStyle);
        caretBoxRight.setStyle(boxStyle);
        modeBox.setStyle(boxStyle);

        // style the carets (arrows that change window mode)
        String caretBtnStyle = "-fx-background-color: transparent; -fx-text-fill: white;";
        caretLeft.setStyle(caretBtnStyle);
        caretRight.setStyle(caretBtnStyle);
        caretLeft.setFocusTraversable(false);
        caretRight.setFocusTraversable(false);
        caretBoxLeft.getChildren().add(caretLeft);
        caretBoxRight.getChildren().add(caretRight);
        StackPane.setAlignment(caretLeft, Pos.CENTER);
        StackPane.setAlignment(caretRight, Pos.CENTER);

        // set height of each section
        for (Region r : new Region[]{caretBoxLeft, caretBoxRight, modeBox}) {
            r.prefHeightProperty().bind(modeRow.heightProperty());
            r.minHeightProperty().bind(modeRow.heightProperty());
            r.maxHeightProperty().bind(modeRow.heightProperty());
        }

        // make caret 10% of box and textbox 90%
        DoubleBinding usableW = Bindings.createDoubleBinding(
                () -> Math.max(0, modeRow.getWidth() - modeRow.getSpacing()),
                modeRow.widthProperty()
        );
        
        caretBoxLeft.prefWidthProperty().bind(usableW.multiply(0.10));
        caretBoxLeft.minWidthProperty().bind(usableW.multiply(0.10));
        caretBoxLeft.maxWidthProperty().bind(usableW.multiply(0.10));
        caretBoxRight.prefWidthProperty().bind(usableW.multiply(0.10));
        caretBoxRight.minWidthProperty().bind(usableW.multiply(0.10));
        caretBoxRight.maxWidthProperty().bind(usableW.multiply(0.10));
        
        modeBox.prefWidthProperty().bind(usableW.multiply(0.90));
        modeBox.minWidthProperty().bind(usableW.multiply(0.90));
        modeBox.maxWidthProperty().bind(usableW.multiply(0.90));

        // make window mode text static
        modeLabel.setTextFill(Color.WHITE);
        modeLabel.setAlignment(Pos.CENTER);
        modeLabel.setMaxWidth(Double.MAX_VALUE);
        modeBox.getChildren().add(modeLabel);
        StackPane.setAlignment(modeLabel, Pos.CENTER);

        // adds window mode text and caret to the correct row
        modeRow.getChildren().setAll(modeBox, caretBoxRight);

        // add the dimmer and the panel to the pause screen to view
        pauseScreen.getChildren().addAll(dimScreen, menuPanel);
        StackPane.setAlignment(menuPanel, Pos.CENTER);

        // adjusts space between the 3 parts (was having trouble just flexing)
        Region spacer1 = new Region();
        Region spacer2 = new Region();

        spacer1.prefHeightProperty().bind(backButton.heightProperty().multiply(0.35));
        spacer1.minHeightProperty().bind(backButton.heightProperty().multiply(0.35));
        spacer1.maxHeightProperty().bind(backButton.heightProperty().multiply(0.35));

        spacer2.prefHeightProperty().bind(backButton.heightProperty().multiply(0.35));
        spacer2.minHeightProperty().bind(backButton.heightProperty().multiply(0.35));
        spacer2.maxHeightProperty().bind(backButton.heightProperty().multiply(0.35));

        menuPanel.getChildren().addAll(volCol, spacer1, modeRow, spacer2, backButton);

        // hooks up Music function to the volume slider
        int startVol = Math.max(0, Math.min(10, Music.getVolume()));
        volumeSlider.setValue(startVol);

        // snaps the ball on the slider to each tick as it gets moved
        volumeSlider.valueProperty().addListener((obs, ov, nv) -> {
            if (volumeSlider.isValueChanging()) {
                double rounded = Math.rint(nv.doubleValue());
                if (rounded != nv.doubleValue()) {
                    volumeSlider.setValue(rounded);
                    return;
                }
            }
            Music.setVolume((int) Math.round(volumeSlider.getValue()));
        });

        // supposed to fix ticks to correct alignment (is not perfect)
        hookTrackAlignment();

        // sets default window to fullscreen
        updateWindowModeUI(stage.isFullScreen());

        // caret actions
        caretRight.setOnAction(e -> { stage.setFullScreen(false); updateWindowModeUI(false); });
        caretLeft.setOnAction(e ->  { stage.setFullScreen(true);  updateWindowModeUI(true);  });

        // forces same font size across the menu
        backButton.heightProperty().addListener((o, ov, nv) -> {
            double h = nv.doubleValue();
            if (h > 0) {
                double f = h * 0.25; // same factor as PauseMenu buttons
                backButton.setFont(Font.font(f));
                volumeLabel.setFont(Font.font(f));
                modeLabel.setFont(Font.font(f));
                caretLeft.setFont(Font.font(f * 0.8));  // arrows look nicer slightly smaller
                caretRight.setFont(Font.font(f * 0.8));
            }
        });
        Platform.runLater(() -> {
            double h = backButton.getHeight();
            if (h > 0) {
                double f = h * 0.25;
                backButton.setFont(Font.font(f));
                volumeLabel.setFont(Font.font(f));
                modeLabel.setFont(Font.font(f));
                caretLeft.setFont(Font.font(f * 0.8));
                caretRight.setFont(Font.font(f * 0.8));
            }
        });

        // return to pause menu
        backButton.setOnAction(e -> {
            hide();
            pauseMenu.show();
        });
    }

    public void show() {
        pauseScreen.setVisible(true);
        pauseScreen.toFront();
        pauseScreen.requestFocus();
    }

    public void hide() {
        pauseScreen.setVisible(false);
    }

    public StackPane getView() {
        return pauseScreen;
    }

    private void updateWindowModeUI(boolean isFullscreen) {
        modeLabel.setText(isFullscreen ? "FULLSCREEN" : "WINDOWED");

        // show one caret box and reorder row so caret sits on the side you can click to change to
        if (isFullscreen) {
            // switch to windowed
            caretBoxLeft.setManaged(false);
            caretBoxLeft.setVisible(false);
            caretBoxRight.setManaged(true);
            caretBoxRight.setVisible(true);
            modeRow.getChildren().setAll(modeBox, caretBoxRight);
        } else {
            // switch to fullscreen
            caretBoxLeft.setManaged(true);
            caretBoxLeft.setVisible(true);
            caretBoxRight.setManaged(false);
            caretBoxRight.setVisible(false);
            modeRow.getChildren().setAll(caretBoxLeft, modeBox);
        }
    }

    // builds the ticks for the volume slider
    private void buildTicks(Pane tickPane, int divisions) {
        tickPane.getChildren().clear();
        for (int i = 0; i <= divisions; i++) {
            Region line = new Region();
            line.setStyle("-fx-background-color: white;");
            line.setMinWidth(TICK_WIDTH);
            line.setPrefWidth(TICK_WIDTH);
            line.setMaxWidth(TICK_WIDTH);
            tickPane.getChildren().add(line);
        }
    }

    // aligns ticks (again, not perfect)
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

    // changes track that volume adjusts based on what is playing
    private class ChangeListener implements javafx.beans.value.ChangeListener<Object> {
        @Override public void changed(javafx.beans.value.ObservableValue<?> o, Object a, Object b) {
            Node track = volumeSlider.lookup(".track");
            if (track != null) layoutTicksToTrack(track);
        }
    }

    // adds the ticks to the slider
    private void layoutTicksToTrack(Node track) {
        if (tickPane.getChildren().isEmpty()) return;

        Bounds tbScene = track.localToScene(track.getBoundsInLocal());
        Bounds tbStack = sliderStack.sceneToLocal(tbScene);

        double trackX = tbStack.getMinX();
        double trackW = Math.max(0, tbStack.getWidth());
        double span = Math.max(0, trackW - TICK_WIDTH);

        double h = Math.max(0, tickPane.getHeight());
        double tickH = h * 0.60; // vertical length (centered)
        double y = (h - tickH) / 2.0;

        int count = TICK_DIVISIONS + 1;
        for (int i = 0; i < count; i++) {
            Region line = (Region) tickPane.getChildren().get(i);
            double x = trackX + (i / (double) TICK_DIVISIONS) * span;
            line.setLayoutX(x);
            line.setLayoutY(y);
            line.setPrefHeight(tickH);
            line.setMinHeight(tickH);
            line.setMaxHeight(tickH);
        }
    }
}
