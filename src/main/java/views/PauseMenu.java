package views;

import javafx.geometry.Pos;

import java.util.ArrayList;

import javafx.geometry.Insets;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.scene.text.Font;
import javafx.application.Platform;

public class PauseMenu {
    private final StackPane pauseScreen;
    private final VBox menuPanel;

    // buttons
    private final Button resumeButton = new Button("RESUME");
    private final Button settingsButton = new Button("SETTINGS");
    private final Button resetButton = new Button("RESET");
    private final Button quitButton = new Button("QUIT");
    ArrayList<Button> buttons = new ArrayList<>();

    public PauseMenu(Stage stage) {
        
        // overlay over the game table
        pauseScreen = new StackPane();
        pauseScreen.setVisible(false);
        pauseScreen.setPickOnBounds(true);

        // dimmed background
        Rectangle dimScreen = new Rectangle();
        dimScreen.setFill(Color.rgb(0, 0, 0, 0.55)); // black with 55% opacity
        dimScreen.widthProperty().bind(pauseScreen.widthProperty());
        dimScreen.heightProperty().bind(pauseScreen.heightProperty());

        // vbox to hold buttons, centered
        menuPanel = new VBox(25, resumeButton, settingsButton, resetButton, quitButton);
        menuPanel.setAlignment(Pos.CENTER);
        menuPanel.setPadding(new Insets(20));
        menuPanel.setBackground(new Background(
                new BackgroundFill(Color.BLACK, new CornerRadii(10), Insets.EMPTY)));
        menuPanel.setBorder(new Border(new BorderStroke(
                Color.WHITE, BorderStrokeStyle.SOLID,
                new CornerRadii(10), new BorderWidths(2))));
        menuPanel.setFillWidth(false); // let children keep their own pref widths

        // set menu panel to percentage of screen
        menuPanel.prefWidthProperty().bind(pauseScreen.widthProperty().multiply(0.40));
        menuPanel.prefHeightProperty().bind(pauseScreen.heightProperty().multiply(0.70)); 
        menuPanel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        // add dimmer and buttons to the overlay
        pauseScreen.getChildren().addAll(dimScreen, menuPanel);
        StackPane.setAlignment(menuPanel, Pos.CENTER);

        // add buttons to the button arraylist
        buttons.add(resumeButton);
        buttons.add(settingsButton);
        buttons.add(resetButton);
        buttons.add(quitButton);

        // style and size all buttons at once (match MainMenu vibe)
        for (Button button : buttons){
            // transparent w/ white border like your MainMenu buttons
            button.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: white; -fx-border-width: 5px; -fx-border-radius: 10px;");
            button.setFocusTraversable(false);

            
            // keep max width at pref so VBox won't force-fill
            button.setMaxWidth(Region.USE_PREF_SIZE);

            // sizes relative to the panel
            button.prefWidthProperty().bind(menuPanel.widthProperty().multiply(0.50));  // ~40% of panel width
            button.prefHeightProperty().bind(menuPanel.heightProperty().multiply(0.20)); // ~20% of panel height
            button.setMinHeight(32);
            button.setMaxHeight(Region.USE_PREF_SIZE);

            // font scales with button height (≈40% of height)
            button.heightProperty().addListener((obs, oh, nh) ->
                    button.setFont(Font.font(nh.doubleValue() * 0.25)));
        }

        // button actions
        resumeButton.setOnAction(e -> hide());
        resetButton.setOnAction(e -> {
            hide();

            // normalize window if you want before swapping scenes
            stage.setFullScreen(false);
            stage.setMaximized(false);
            stage.setResizable(true);
            stage.setMinWidth(900);
            stage.setMinHeight(870);
            stage.setWidth(1280);
            stage.setHeight(800);
            stage.centerOnScreen();

            // swap to a fresh main menu (single-arg constructor)
            stage.setScene(new MainMenu(stage).createScene());

            // ✅ enter fullscreen AFTER the new scene is attached (next pulse)
            Platform.runLater(() -> stage.setFullScreen(true));
        });
        quitButton.setOnAction(e -> stage.close());
    }

    // show/hide methods
    public void show() {
        pauseScreen.setVisible(true);
        pauseScreen.toFront();
        pauseScreen.requestFocus();
    }

    public void hide() {
        pauseScreen.setVisible(false);
    }

    // expose the root node so CTable can display
    public StackPane getView() {
        return pauseScreen;
    }

    // getters for buttons (currently unused)
    public Button getResumeButton() {
        return resumeButton;
    }

    public Button getSettingsButton() {
        return settingsButton;
    }

    public Button getResetButton() {
        return resetButton;
    }

    public Button getQuitButton() {
        return quitButton;
    }
}
