package controllers;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class Music {
    private static MediaPlayer player;

    private Music(){}

    public static void playSongOnLoop(String resourcePath, double volume){
        // stops current song
        stop();
        // retrieves song url
        var songURL = Music.class.getResource(resourcePath);

        // stops media player if no song is found
        if(songURL == null){
            System.out.println("Can't find a song to play");
            return;
        }

        // inserts song url into media player
        player = new MediaPlayer(new Media(songURL.toExternalForm()));
        // loops the song
        player.setCycleCount(MediaPlayer.INDEFINITE);
        player.setVolume(volume);
        player.play();
    }

    public static void stop(){
        // stops the song and removes from media player if there is a song playing
        if(player != null){
            try{
                player.stop();
            } catch(Exception ignored){}
            try{
                player.dispose();
            } catch(Exception ignored){}

            player = null;
        }
    }

    // adjusts volume
    public static void setVolume(int level){
        if(player != null){
            // volume goes 0-10
            int volumeScale = Math.max(0, Math.min(10, level));
            double scaledForMediaPlayer = volumeScale / 10.0;
            player.setVolume(scaledForMediaPlayer);
        }
    }

    // get current volume
    public static int getVolume(){
        if(player != null){
            // retrieves on 0-10 scale for slider
            return (int)Math.round(player.getVolume() * 10);
        }
        return 0;
    }
}
