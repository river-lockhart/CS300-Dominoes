package controllers;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class Music {
    private static MediaPlayer player;

    private Music(){}

    public static void playSongOnLoop(String resourcePath, double volume){
        stop();
        var songURL = Music.class.getResource(resourcePath);

        if(songURL == null){
            System.out.println("Can't find a song to play");
            return;
        }

        player = new MediaPlayer(new Media(songURL.toExternalForm()));
        player.setCycleCount(MediaPlayer.INDEFINITE);
        player.setVolume(volume);
        player.play();
    }

    public static void stop(){
        if(player != null){
            try{
                player.stop();
            }catch(Exception ignored){}
            try{
                player.dispose();
            }catch(Exception ignored){}

            player = null;
        }
    }
}
