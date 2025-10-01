package app;

import controllers.Music;
import javafx.application.Application;
import javafx.stage.Stage;
import views.MainMenu;

public class Main extends Application {

    // remember the last normal (windowed) size so exiting fullscreen feels normal
    private double lastWindowW = 1280;
    private double lastWindowH = 800;

    public void start(Stage stage) {
        stage.setTitle("Dominoes");

        // main menu now only needs the stage
        var menu = new MainMenu(stage);
        stage.setScene(menu.createScene());

        // resizable sane defaults
        stage.setResizable(true);
        stage.setMinWidth(900);
        stage.setMinHeight(870);
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

        // bgm (you can adjust later with Music.setVolume(0..10) if you added that helper)
        Music.playSongOnLoop("/assets/music/Song1.mp3", 0.6);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
