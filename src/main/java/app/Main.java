package app;

import javafx.application.Application;
import javafx.stage.Stage;
import views.MainMenu;

public class Main extends Application {
    public void start(Stage stage) {
        stage.setTitle("Dominoes");
        var menu = new MainMenu(stage);
        stage.setScene(menu.createScene());
        stage.setFullScreen(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
