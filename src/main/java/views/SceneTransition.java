package views;

import javafx.animation.FadeTransition;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SceneTransition {
    private SceneTransition() {}

    // fades between main menu and gameplay
    public static void fadeIntoScene(Stage stage, Parent nextScene, Duration timeToFade){
        Scene scene = stage.getScene();
        if(scene == null){
            stage.setScene(new Scene(nextScene));
            return;
        }

        Parent currentScene = scene.getRoot();

        // creates a stack for scenes to be loaded into
        StackPane sceneStack = new StackPane(currentScene);
        nextScene.setOpacity(0);
        // adds scenes to stack
        sceneStack.getChildren().add(nextScene);
        // sets the stack as active
        scene.setRoot(sceneStack);

        FadeTransition fadeIn = new FadeTransition(timeToFade, nextScene);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        FadeTransition fadeOut = new FadeTransition(timeToFade, currentScene);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        //fades into the next scene
        fadeIn.setOnFinished(e -> scene.setRoot(nextScene));
        
        fadeOut.play();
        fadeIn.play();
    }
}
