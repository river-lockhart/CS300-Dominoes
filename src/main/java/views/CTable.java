package views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class CTable {
    private final Stage stage;

    // boxes for players hands and table
    public final HBox aiHandStrip = new HBox(8);
    public final HBox playerHandStrip = new HBox(8);
    public final Canvas tableTop = new Canvas(800, 600);

    // heights for the three table sections
    private static final double AI_PREF = 180;
    private static final double AI_MIN  = 110;
    private static final double PLAYER_PREF = 180;
    private static final double PLAYER_MIN  = 110;
    private static final double NAVBAR_PREF = 40; 
    private static final double CENTER_MIN  = 120; 

    public CTable(Stage stage) { this.stage = stage; }

    public Scene createScene() {
        // parent panel
        VBox root = new VBox();
        root.setPadding(Insets.EMPTY);
        root.setStyle("-fx-background-color: #151515;");

        // button to show remaining pieces
        Button remainingPieces = new Button("Remaining Pieces");
        remainingPieces.setStyle("-fx-background-color: #151515; -fx-text-fill: white; -fx-border-color: white; -fx-border-width: 1px; -fx-border-radius: 5px;"); 


        // button to quit game back to menu
        Button quitButton = new Button("Quit Game");
        quitButton.setStyle("-fx-background-color: #151515; -fx-text-fill: white; -fx-border-color: white; -fx-border-width: 1px; -fx-border-radius: 5px;"); 
        quitButton.setOnAction(e -> stage.close());
        
        // navbar
        HBox navbar = new HBox(remainingPieces, quitButton);
        navbar.setAlignment(Pos.CENTER_RIGHT);
        navbar.setSpacing(10);
        navbar.setPadding(new Insets(6, 6, 4, 6));
        navbar.setMinHeight(Region.USE_PREF_SIZE);
        navbar.setPrefHeight(NAVBAR_PREF);
        navbar.setMaxHeight(Region.USE_PREF_SIZE);

        // section for ai current pieces
        ScrollPane aiHand = makeScrollArea(aiHandStrip, AI_MIN, AI_PREF);

        // table where pieces will be played
        StackPane center = new StackPane(tableTop);
        center.setPadding(Insets.EMPTY);

        // force flex on window change
        center.widthProperty().addListener((o, ow, nw) -> tableTop.setWidth(nw.doubleValue()));
        center.heightProperty().addListener((o, oh, nh) -> tableTop.setHeight(nh.doubleValue()));
        center.setMinHeight(CENTER_MIN); 

        // human players hand
        ScrollPane playerScroll = makeScrollArea(playerHandStrip, PLAYER_MIN, PLAYER_PREF);

        // panel for the three sections to go inside
        BorderPane content = new BorderPane();

        // place sections accordingly
        content.setTop(aiHand);
        content.setCenter(center);
        content.setBottom(playerScroll);

        // add navbar and play area to the scene
        root.getChildren().addAll(navbar, content);

        // flex around the play area (prevents losing player hand area)
        VBox.setVgrow(content, Priority.ALWAYS);

        // create scene
        Scene scene = new Scene(root);

        // force window width
        aiHand.setMaxWidth(Double.MAX_VALUE);
        playerScroll.setMaxWidth(Double.MAX_VALUE);
        BorderPane.setAlignment(center, Pos.CENTER);

        // should additionally prevent player hand moving out of window
        double stageMinHeight = NAVBAR_PREF + AI_MIN + PLAYER_MIN + CENTER_MIN;
        stage.setMinHeight(stageMinHeight);
        stage.setMinWidth(900);

        return scene;
    }

    // turn each players hand area scrollable
    private ScrollPane makeScrollArea(HBox playerHand, double minimumHeight, double preferredHeight) {
        playerHand.setAlignment(Pos.CENTER);
        playerHand.setPadding(new Insets(6));

        // creates a scroll area inside each players hand area
        ScrollPane scrollArea = new ScrollPane(playerHand);

        // only scroll horizontal, not vertical
        scrollArea.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollArea.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollArea.setFitToWidth(true);
        scrollArea.setFitToHeight(true);

        scrollArea.setMinHeight(minimumHeight);
        scrollArea.setPrefHeight(preferredHeight);
        scrollArea.setMaxHeight(Region.USE_PREF_SIZE);

        scrollArea.setStyle("""
            -fx-background-color: transparent;
            -fx-background: transparent;
        """);
        playerHand.setStyle("-fx-background-color: #222831;");

        return scrollArea;
    }
}
