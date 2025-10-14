package views;

import javafx.geometry.Pos;
import java.util.ArrayList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.scene.text.Font;
import javafx.util.Duration;

public class PauseMenu {
    private final StackPane pauseScreen;
    private final VBox menuPanel;

    private final Button resumeButton = new Button("RESUME");
    private final Button settingsButton = new Button("SETTINGS");
    private final Button resetButton = new Button("MENU");
    private final Button quitButton = new Button("QUIT");
    private final ArrayList<Button> menuButtons = new ArrayList<>();

    private SettingsMenu settingsMenu;

    public static final String FRAME_STYLE =
            "-fx-background-color: transparent; " +
            "-fx-text-fill: white; " +
            "-fx-border-color: white; " +
            "-fx-border-width: 5px; " +
            "-fx-border-radius: 10px;";

    // applies framed style and disables focus travel
    public static void applyFramedButtonStyle(Button button) {
        button.setStyle(FRAME_STYLE);
        button.setFocusTraversable(false);
    }

    // creates a dim layer that follows parent size
    public static Rectangle createDimmer(StackPane parentPane, double opacity) {
        Rectangle dimmerRect = new Rectangle();
        dimmerRect.setFill(Color.rgb(0, 0, 0, Math.max(0.0, Math.min(1.0, opacity))));
        dimmerRect.widthProperty().bind(parentPane.widthProperty());
        dimmerRect.heightProperty().bind(parentPane.heightProperty());
        return dimmerRect;
    }

    // builds the pause overlay and menu panel
    public PauseMenu(Stage stage) {
        // make overlay over the game table
        pauseScreen = new StackPane();
        pauseScreen.setVisible(false);
        pauseScreen.setPickOnBounds(true);

        // make dimmed background
        Rectangle screenDimmer = createDimmer(pauseScreen, 0.55);

        // make menu panel and center it
        menuPanel = new VBox(25, resumeButton, settingsButton, resetButton, quitButton);
        menuPanel.setAlignment(Pos.CENTER);
        menuPanel.setPadding(new Insets(20));
        menuPanel.setBackground(new Background(
                new BackgroundFill(Color.BLACK, new CornerRadii(10), Insets.EMPTY)));
        menuPanel.setBorder(new Border(new BorderStroke(
                Color.WHITE, BorderStrokeStyle.SOLID,
                new CornerRadii(10), new BorderWidths(2))));
        menuPanel.setFillWidth(false);

        // size the panel to part of the screen
        menuPanel.prefWidthProperty().bind(pauseScreen.widthProperty().multiply(0.40));
        menuPanel.prefHeightProperty().bind(pauseScreen.heightProperty().multiply(0.70));
        menuPanel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        // add dimmer and menu to overlay
        pauseScreen.getChildren().addAll(screenDimmer, menuPanel);
        StackPane.setAlignment(menuPanel, Pos.CENTER);

        // collect buttons for batch styling
        menuButtons.add(resumeButton);
        menuButtons.add(settingsButton);
        menuButtons.add(resetButton);
        menuButtons.add(quitButton);

        // style and size all buttons together
        for (Button button : menuButtons) {
            applyFramedButtonStyle(button);

            // size buttons by panel size
            button.prefWidthProperty().bind(menuPanel.widthProperty().multiply(0.50));
            button.prefHeightProperty().bind(menuPanel.heightProperty().multiply(0.20));
            button.setMinHeight(32);

            // stop vbox from stretching buttons
            button.setMaxWidth(Region.USE_PREF_SIZE);
            button.setMaxHeight(Region.USE_PREF_SIZE);

            // scale font with height
            button.heightProperty().addListener((obs, oldSize, newSize) ->
                    button.setFont(Font.font(newSize.doubleValue() * 0.25)));
        }

        // wire resume action
        resumeButton.setOnAction(e -> hide());

        // open settings overlay
        settingsButton.setOnAction(e -> {
            if (settingsMenu == null) {
                settingsMenu = new SettingsMenu(stage, this);
            }
            Parent parent = pauseScreen.getParent();
            if (parent instanceof Pane host) {
                if (!host.getChildren().contains(settingsMenu.getView())) {
                    host.getChildren().add(settingsMenu.getView());
                    if (host instanceof AnchorPane) {
                        AnchorPane.setTopAnchor(settingsMenu.getView(), 0.0);
                        AnchorPane.setRightAnchor(settingsMenu.getView(), 0.0);
                        AnchorPane.setBottomAnchor(settingsMenu.getView(), 0.0);
                        AnchorPane.setLeftAnchor(settingsMenu.getView(), 0.0);
                    }
                }
                this.hide();
                settingsMenu.show();
                settingsMenu.getView().toFront();
            }
        });

        // return to main menu
        resetButton.setOnAction(e -> {
            hide();
            Parent menuRoot = new MainMenu(stage).createRoot();
            SceneTransition.fadeIntoScene(stage, menuRoot, Duration.millis(600));
        });

        // quit the game
        quitButton.setOnAction(e -> stage.close());
    }

    // shows the pause overlay
    public void show() {
        pauseScreen.setVisible(true);
        pauseScreen.toFront();
        pauseScreen.requestFocus();
    }

    // hides the pause overlay
    public void hide() {
        pauseScreen.setVisible(false);
    }

    // returns the root overlay node
    public StackPane getView() {
        return pauseScreen;
    }

    // gets the resume button node
    public Button getResumeButton() {
        return resumeButton;
    }

    // gets the settings button node
    public Button getSettingsButton() {
        return settingsButton;
    }

    // gets the menu button node
    public Button getResetButton() {
        return resetButton;
    }

    // gets the quit button node
    public Button getQuitButton() {
        return quitButton;
    }
}
