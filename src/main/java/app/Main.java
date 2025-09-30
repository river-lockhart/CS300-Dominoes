package app;

import controllers.CPlayer;
import controllers.Music;
import javafx.application.Application;
import javafx.stage.Stage;
import models.AvailablePieces;
import models.Hand;
import views.MainMenu;

public class Main extends Application {

    //create deck of dominoes per game
    Hand gameDeck = new Hand();
    final private CPlayer player = new CPlayer();

    // retrieve the dominoes not added to player hands
    AvailablePieces leftoverDominoes = new AvailablePieces(gameDeck);

    public void start(Stage stage) {
        stage.setTitle("Dominoes");
        //pass domino hands and remaining pieces to the ui
        var menu = new MainMenu(stage, gameDeck, leftoverDominoes, player);
        stage.setScene(menu.createScene());
        stage.setFullScreen(true);
        
        stage.show();

        Music.playSongOnLoop("/assets/music/Song1.mp3", 0.6);

        System.out.println("Leftover dominoes:");
        var leftovers = leftoverDominoes.getLeftoverDominoes();
        System.out.println("count = " + leftovers.size());
        for (var d : leftovers) {
            System.out.println(d.getImage() + " [" + d.getLeftValue() + "|" + d.getRightValue() + "]");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
