package views;

import java.util.ArrayList;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ObservableNumberValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import models.AvailablePieces;
import models.CDominoes;
import models.Hand;
import controllers.CPlayer;

public class CTable {
    private final Stage stage;
    private final Hand hand;
    private final AvailablePieces remainingPieces;
    private final CPlayer player;

    // navbar height, make hand bars dynamic
    private static final double NAVBAR_H = 56;
    private static final double HANDBAR_MIN = 64;
    private static final double HANDBAR_MAX = 140;
    private static final double CENTER_MIN_H = 160;

    // domino aspect ratio 
    private static final double DOMINO_ASPECT_W_OVER_H = 0.5;

    // hand strips
    public final HBox aiHandStrip = new HBox(8);
    public final HBox playerHandStrip = new HBox(8);

    // play area
    public final Canvas tableTop = new Canvas(800, 600);

    // global overlay so dragged tiles appear on top of everything
    private final Pane overlay = new Pane();

    // background image used by both hand bars
    private static final String HANDBAR_BG = "/assets/tabletop/playerhands.jpg";
    private static final String TABLE_BG   = "/assets/tabletop/table.jpg";

    public CTable(Stage stage, Hand hand, AvailablePieces remainingPieces, CPlayer player) {
        this.stage = stage;
        this.hand = hand;
        this.remainingPieces = remainingPieces;
        this.player = player;
    }

    // parent box
    public Parent createRoot() {
        AnchorPane root = new AnchorPane();
        PauseMenu pauseMenu = new PauseMenu(stage);

        root.setStyle("-fx-background-color: #151515;");

        // the main vertical box that fills the window
        VBox box = new VBox();
        box.setFillWidth(true);
        box.setSpacing(0);
        // attaches pane to the corners of the window
        AnchorPane.setTopAnchor(box, 0.0);
        AnchorPane.setRightAnchor(box, 0.0);
        AnchorPane.setBottomAnchor(box, 0.0);
        AnchorPane.setLeftAnchor(box, 0.0);

        // navbar
        HBox navbar = buildNavbar(stage, pauseMenu);

        // ai hand bar
        StackPane aiHandBar = buildHandBar(aiHandStrip, false);

        // table
        StackPane tablePane = new StackPane();
        // set table background image (cover the area; centered)
        var tableUrl = getClass().getResource(TABLE_BG);
        if (tableUrl != null) {
            Image tImg = new Image(tableUrl.toExternalForm(), true);
            BackgroundImage tBG = new BackgroundImage(
                    tImg,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, false, true) // cover
            );
            tablePane.setBackground(new Background(tBG));
        }
        tablePane.setMinHeight(CENTER_MIN_H);
        tableTop.widthProperty().bind(tablePane.widthProperty());
        tableTop.heightProperty().bind(tablePane.heightProperty());
        tablePane.getChildren().addAll(tableTop);
        VBox.setVgrow(tablePane, Priority.ALWAYS);

        // player hand bar
        StackPane playerHandBar = buildHandBar(playerHandStrip, true);

        // adds above 4 sections to the main box
        box.getChildren().addAll(navbar, aiHandBar, tablePane, playerHandBar);

        // configure the overlay as a full-window top layer
        overlay.setPickOnBounds(false); 
        AnchorPane.setTopAnchor(overlay, 0.0);
        AnchorPane.setRightAnchor(overlay, 0.0);
        AnchorPane.setBottomAnchor(overlay, 0.0);
        AnchorPane.setLeftAnchor(overlay, 0.0);

        // add box first, overlay second so it renders above everything, and then the pause above that
        root.getChildren().addAll(box, overlay, pauseMenu.getView());
        overlay.toFront();

        // attache the pause menu to the corners of the windows
        AnchorPane.setTopAnchor(pauseMenu.getView(), 0.0);
        AnchorPane.setRightAnchor(pauseMenu.getView(), 0.0);
        AnchorPane.setBottomAnchor(pauseMenu.getView(), 0.0);
        AnchorPane.setLeftAnchor(pauseMenu.getView(), 0.0);

        // populate dominoes in player hands
        displayDominoes(hand, aiHandStrip, "AI", aiHandBar);
        displayDominoes(hand, playerHandStrip, "Player", playerHandBar);

        return root;
    }

    public Scene createScene() {
        Parent root = createRoot();

        // scene
        Scene scene = new Scene(root);

        // min sizes for scene
        stage.setMinWidth(900);
        stage.setMinHeight(870);

        return scene;
    }

    private HBox buildNavbar(Stage stage, PauseMenu pauseMenu) {
        // create and style buttons on play table
        Button remainingPiecesBtn = new Button("Remaining Pieces");
        remainingPiecesBtn.setStyle("-fx-background-color: #151515; -fx-text-fill: white; -fx-border-color: white; -fx-border-width: 1px; -fx-border-radius: 6px;");
        remainingPiecesBtn.setFocusTraversable(false);

        Button menuButton = new Button("Menu");
        menuButton.setStyle("-fx-background-color: #151515; -fx-text-fill: white; -fx-border-color: white; -fx-border-width: 1px; -fx-border-radius: 6px;");
        menuButton.setFocusTraversable(false);
        menuButton.setOnAction(e -> pauseMenu.show());

        // flex navbar
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // flex buttons in navbar
        HBox bar = new HBox(10, spacer, remainingPiecesBtn, menuButton);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPadding(new Insets(8, 10, 8, 10));
        bar.setStyle("-fx-background-color: #151515;");
        bar.setMinHeight(NAVBAR_H);
        bar.setPrefHeight(NAVBAR_H);
        bar.setMaxHeight(NAVBAR_H);
        bar.setMinWidth(0);
        bar.setPrefWidth(Region.USE_COMPUTED_SIZE);
        bar.setMaxWidth(Double.MAX_VALUE);

        return bar;
    }

    // creates sections for ai player hand and player hand
    private StackPane buildHandBar(HBox strip, boolean alignBottom) {
        // style both hands
        strip.setAlignment(Pos.CENTER);
        strip.setFillHeight(true);
        strip.setPadding(new Insets(6));
        strip.setSpacing(8);
        strip.setMinWidth(0);
        strip.setPrefWidth(Region.USE_COMPUTED_SIZE);
        strip.setMaxWidth(Double.MAX_VALUE);

        // flex strips
        HBox.setHgrow(strip, Priority.ALWAYS);

        // we need the image *behind* the tiles and clipped to the bar's size
        StackPane bar = new StackPane();
        bar.setPadding(Insets.EMPTY);

        // background image view
        ImageView bgView = null;
        var bgUrl = getClass().getResource(HANDBAR_BG);
        if (bgUrl != null) {
            Image bgImg = new Image(bgUrl.toExternalForm(), true);
            bgView = new ImageView(bgImg);
            bgView.setPreserveRatio(true);
            bgView.setSmooth(true);
            bgView.setCache(true);

            // fill the width of the bar; height will be whatever keeps aspect
            bgView.fitWidthProperty().bind(bar.widthProperty());

            // anchor to TOP for AI, BOTTOM for Player
            StackPane.setAlignment(bgView, alignBottom ? Pos.BOTTOM_CENTER : Pos.TOP_CENTER);

            // clip the whole bar area so only the edge we want is visible
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
            clip.widthProperty().bind(bar.widthProperty());
            clip.heightProperty().bind(bar.heightProperty());
            bar.setClip(clip);

            bar.getChildren().add(bgView);
        } else {
            // fallback color if image missing
            bar.setStyle("-fx-background-color: #222831;");
        }

        // tiles sit above the background
        bar.getChildren().add(strip);
        StackPane.setAlignment(strip, Pos.CENTER);

        bar.setMinHeight(HANDBAR_MIN);
        bar.setMaxHeight(HANDBAR_MAX);

        final double childHPad = 8;
        final double stripSidePad = 12;
        final double spacing = strip.getSpacing();

        // flexes dominoes inside of the hand strips
        ObservableNumberValue tileCount = Bindings.size(strip.getChildren());
        DoubleBinding handBarPrefH = Bindings.createDoubleBinding(() -> {
            int n = Math.max(1, tileCount.intValue());
            double barW = Math.max(bar.getWidth(), 1);
            double usedByGaps = (n - 1) * spacing + (n * childHPad) + stripSidePad;
            double usableW = Math.max(0, barW - usedByGaps);
            double perTileW = usableW / n;

            double heightFromWidth = (DOMINO_ASPECT_W_OVER_H > 0)
                    ? (perTileW / DOMINO_ASPECT_W_OVER_H) + 12
                    : HANDBAR_MIN;

            return Math.max(HANDBAR_MIN, Math.min(HANDBAR_MAX, heightFromWidth));
        }, bar.widthProperty(), tileCount);

        bar.prefHeightProperty().bind(handBarPrefH);
        return bar;
    }

    // shows dominos in the hand strips
    private void displayDominoes(Hand hand, HBox strip, String who, StackPane bar) {
        ArrayList<CDominoes> sideOfTable = "AI".equals(who) ? hand.getAiHand() : hand.getPlayerHand();
        strip.getChildren().clear();

        // remove any previous edge line
        bar.getChildren().removeIf(n -> "handEdge".equals(n.getId())); 

        for (CDominoes domino : sideOfTable) {
            // gets image resource
            var url = getClass().getResource(domino.getImage());
            if (url == null) continue;

            // sets images
            Image image = new Image(url.toExternalForm(), true);
            ImageView view = new ImageView(image);
            view.setPreserveRatio(true);
            view.setSmooth(true);
            view.setCache(true);

            // domino height tracks bar height
            view.fitHeightProperty().bind(bar.heightProperty().subtract(12));

            // rotates domino image
            view.setRotate(domino.getRotationDegrees());

            // set up pane to act as hitbox for grabbing dominoes
            StackPane hitbox = new StackPane(view);
            hitbox.setPadding(new Insets(4));
            HBox.setHgrow(hitbox, Priority.NEVER);

            // defines player actions for each domino
            if ("Player".equals(who)) {
                player.definePlayerMovement(domino, overlay, hitbox, strip);
            }

            // adds each hitbox to the strip, over the dominoes
            strip.getChildren().add(hitbox);
        }

        // creates a border
        javafx.scene.shape.Rectangle edge = new javafx.scene.shape.Rectangle();
        edge.setId("handEdge");
        edge.setManaged(false);              
        edge.setMouseTransparent(true);
        edge.setFill(javafx.scene.paint.Color.WHITE);
        edge.setHeight(1);
        edge.widthProperty().bind(bar.widthProperty());

        // player = top border, AI = bottom border
        if ("Player".equals(who)) {
            edge.setLayoutY(0);
        } else {
            edge.layoutYProperty().bind(bar.heightProperty().subtract(1));
        }

        // adds border to the bar
        bar.getChildren().add(edge);
        edge.toFront(); 
    }
}
