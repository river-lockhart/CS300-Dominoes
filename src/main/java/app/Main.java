package app;

import controllers.CPlayer;
import controllers.Music;
import javafx.application.Application;
import javafx.stage.Stage;
import models.AvailablePieces;
import models.Hand;
import views.MainMenu;

public class Main extends Application {

    // remember the last normal (windowed) size so exiting fullscreen feels normal
    private double lastWindowW = 1280;
    private double lastWindowH = 800;

    // create deck of dominoes per game
    Hand gameDeck = new Hand();
    final private CPlayer player = new CPlayer();

    // retrieve the dominoes not added to player hands
    AvailablePieces leftoverDominoes = new AvailablePieces(gameDeck);

    public void start(Stage stage) {
        stage.setTitle("Dominoes");

        // pass domino hands, remaining pieces, and the player to the ui
        var menu = new MainMenu(stage, gameDeck, leftoverDominoes, player);
        stage.setScene(menu.createScene());

        // resizable sane defaults
        stage.setResizable(true);
        stage.setMinWidth(680);  // your requested min width (+~40px)
        stage.setMinHeight(500); // was 480; about 20px bigger
        stage.setWidth(lastWindowW);
        stage.setHeight(lastWindowH);

        stage.show();

        // start fullscreen if you want
        stage.setFullScreen(true);

        // restore window size when leaving fullscreen
        stage.fullScreenProperty().addListener((obs, wasFull, isFull) -> {
            if (isFull) {
                lastWindowW = stage.getWidth();
                lastWindowH = stage.getHeight();
            } else {
                stage.setResizable(true);
                stage.setMaximized(false);
                stage.setWidth(Math.max(lastWindowW, stage.getMinWidth()));
                stage.setHeight(Math.max(lastWindowH, stage.getMinHeight()));
                stage.centerOnScreen();
            }
        });

        Music.playSongOnLoop("/assets/music/Song1.mp3", 0.6);
        
    }

    public static void main(String[] args) {
        launch(args);
    }
}
