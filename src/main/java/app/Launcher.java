package app;

// calls Main to launch the application from a separate class
// this is needed for JavaFX to work properly when packaged as a jar
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}
