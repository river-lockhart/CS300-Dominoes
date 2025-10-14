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

    // buttons
    private final Button resumeButton = new Button("RESUME");
    private final Button settingsButton = new Button("SETTINGS");
    private final Button resetButton = new Button("MENU");
    private final Button quitButton = new Button("QUIT");
    ArrayList<Button> buttons = new ArrayList<>();

    // lazily-created settings overlay
    private SettingsMenu settings;

    /** Shared white-framed transparent button style (used by SettingsMenu too). */
    public static final String FRAME_BUTTON_STYLE =
            "-fx-background-color: transparent; " +
            "-fx-text-fill: white; " +
            "-fx-border-color: white; " +
            "-fx-border-width: 5px; " +
            "-fx-border-radius: 10px;";

    /** Apply standard framed style and disable focus-traversal. */
    public static void applyFramedButtonStyle(Button b) {
        b.setStyle(FRAME_BUTTON_STYLE);
        b.setFocusTraversable(false);
    }

    /** Create a dimmer that auto-binds to the given parent’s size. */
    public static Rectangle createDimmer(StackPane parent, double alpha) {
        Rectangle dim = new Rectangle();
        dim.setFill(Color.rgb(0, 0, 0, Math.max(0.0, Math.min(1.0, alpha))));
        dim.widthProperty().bind(parent.widthProperty());
        dim.heightProperty().bind(parent.heightProperty());
        return dim;
    }

    public PauseMenu(Stage stage) {

        // overlay over the game table
        pauseScreen = new StackPane();
        pauseScreen.setVisible(false);
        pauseScreen.setPickOnBounds(true);

        // dimmed background (shared helper)
        Rectangle dimScreen = createDimmer(pauseScreen, 0.55);

        // vbox to hold buttons, centered
        menuPanel = new VBox(25, resumeButton, settingsButton, resetButton, quitButton);
        menuPanel.setAlignment(Pos.CENTER);
        menuPanel.setPadding(new Insets(20));
        menuPanel.setBackground(new Background(
                new BackgroundFill(Color.BLACK, new CornerRadii(10), Insets.EMPTY)));
        menuPanel.setBorder(new Border(new BorderStroke(
                Color.WHITE, BorderStrokeStyle.SOLID,
                new CornerRadii(10), new BorderWidths(2))));
        // prevents children from getting incorrect sizing
        menuPanel.setFillWidth(false);

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

        // style and size all buttons at once
        for (Button button : buttons){
            applyFramedButtonStyle(button);

            // sizes buttons relative to the panel
            button.prefWidthProperty().bind(menuPanel.widthProperty().multiply(0.50));
            button.prefHeightProperty().bind(menuPanel.heightProperty().multiply(0.20));
            button.setMinHeight(32);

            // keeps VBox from autofilling buttons
            button.setMaxWidth(Region.USE_PREF_SIZE);
            button.setMaxHeight(Region.USE_PREF_SIZE);

            // font scales with button height
            button.heightProperty().addListener((obs, oh, nh) ->
                    button.setFont(Font.font(nh.doubleValue() * 0.25)));
        }

        // button actions
        resumeButton.setOnAction(e -> hide());

        // open settings overlay
        settingsButton.setOnAction(e -> {
            if (settings == null) {
                settings = new SettingsMenu(stage, this);
            }
            Parent parent = pauseScreen.getParent();
            if (parent instanceof Pane host) {
                if (!host.getChildren().contains(settings.getView())) {
                    host.getChildren().add(settings.getView());
                    if (host instanceof AnchorPane) {
                        AnchorPane.setTopAnchor(settings.getView(), 0.0);
                        AnchorPane.setRightAnchor(settings.getView(), 0.0);
                        AnchorPane.setBottomAnchor(settings.getView(), 0.0);
                        AnchorPane.setLeftAnchor(settings.getView(), 0.0);
                    }
                }
                this.hide();
                settings.show();
                settings.getView().toFront();
            }
        });

        resetButton.setOnAction(e -> {
            hide();

            // swap to a fresh main menu with a frozen crossfade (prevents window from resizing weird when crossfading)
            Parent menuRoot = new MainMenu(stage).createRoot();
            SceneTransition.fadeIntoScene(stage, menuRoot, Duration.millis(600));
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
