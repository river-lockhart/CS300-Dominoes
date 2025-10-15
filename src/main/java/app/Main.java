//AUTHORED BY RIVER LOCKHART, KHUSBU BHUSHAL, GABRIEL BOAFO

package app;

import controllers.Music;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import views.MainMenu;

public class Main extends Application {

    // remember the last window size 
    private double lastWindowW = 1280;
    private double lastWindowH = 800;

    
    public void start(Stage stage) {
        stage.setTitle("Dominoes");

        // create main menu
        var menu = new MainMenu(stage);
        stage.setScene(menu.createScene());

        // stage sizing
        stage.setResizable(true);
        stage.setMinWidth(900);
        stage.setMinHeight(870);
        stage.setWidth(lastWindowW);
        stage.setHeight(lastWindowH);

        // disable hint and using esc to exit fullscreen
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

        // force fullscreen after scene changes
        stage.sceneProperty().addListener((obs, oldScene, newScene) ->
            Platform.runLater(() -> {
                stage.toFront();
                stage.requestFocus();
                stage.setFullScreen(true);
            })
        );

        stage.show();

        // start fullscreen
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

        // background music
        Music.playSongOnLoop("/assets/music/Song1.mp3", 0.2);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
