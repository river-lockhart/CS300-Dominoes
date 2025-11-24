package util;

import controllers.TurnManager;
import models.CDominoes;
import models.TableLayout;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public final class ConsoleLogger {

    private static final DateTimeFormatter clockFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final List<String> moveLines = new ArrayList<>();
    private static final List<String> chainOrder = new ArrayList<>();

    private static CDominoes carryoverRunnerUp = null;

    private static final Object logLock = new Object();

    private ConsoleLogger() {}

    // resets all logs for a fresh game
    public static void startGame() {
        synchronized (logLock) {
            moveLines.clear();
            chainOrder.clear();
            carryoverRunnerUp = null;
        }
        println("");
        println("=== domino game started @ " + now() + " ===");
    }

    // prints a player drawing one tile
    public static void logDraw(TurnManager.Side side, CDominoes tile) {
        String who = sideLabel(side);
        String piece = tileAscii(tile);
        String line = stamp("DRAW  | " + padRight(who, 7) + " drew " + piece);
        synchronized (logLock) {
            moveLines.add(line);
        }
        println(line);
    }

    // prints a player passing their turn
    public static void logPass(TurnManager.Side side) {
        String who = sideLabel(side);
        String line = stamp("PASS  | " + padRight(who, 7) + " passed (no move)");
        synchronized (logLock) {
            moveLines.add(line);
        }
        println(line);
    }

    // prints first placement only, adds to chain silently
    public static void logFirstPlacement(TurnManager.Side side, CDominoes tile) {
        String who = sideLabel(side);
        String piece = tileAscii(tile);
        String line = stamp("PLACE | " + padRight(who, 7) + " placed " + piece);
        synchronized (logLock) {
            moveLines.add(line);
            chainOrder.add(piece);
        }
        println(line);
    }

    // prints a normal placement with coordinates, adds to chain silently
    public static void logMovePlaced(TurnManager.Side side, CDominoes tile, TableLayout.Placement plan) {
        String who = sideLabel(side);
        String piece = tileAscii(tile);
        String face = plan.vertical ? "vertical" : "horizontal";
        String place = "(row " + plan.row + ", col " + plan.col + ", " + face + ")";
        String line = stamp("PLACE | " + padRight(who, 7) + " placed " + piece + " " + place);
        synchronized (logLock) {
            moveLines.add(line);
            chainOrder.add(piece);
        }
        println(line);
    }

    // prints "placed [a] against [b]" and adds [a] to chain silently
    public static void logPlacedAgainst(TurnManager.Side side, CDominoes placed, CDominoes matched) {
        String who = sideLabel(side);
        String a = tileAscii(placed);
        String b = tileAscii(matched);
        String line = stamp("MATCH | " + padRight(who, 7) + " placed " + a + " against " + b);
        synchronized (logLock) {
            moveLines.add(line);
            chainOrder.add(a);
        }
        println(line);
    }

    // prints a short winner summary
    public static void logWinnerSimple(String winnerLabel) {
        println(stamp("FINAL | winner: " + winnerLabel));
    }

    // set carryover tile for runner-up (edge case)
    public static void setCarryoverForRunnerUp(CDominoes tile) {
        synchronized (logLock) {
            carryoverRunnerUp = tile;
        }
    }

    // clear carryover
    public static void clearCarryover() {
        synchronized (logLock) {
            carryoverRunnerUp = null;
        }
    }

    // prints the final required summary (only place the chain here)
    public static void logFinalResult(
            String winnerLabel,
            String runnerUpLabel,
            List<CDominoes> runnerUpTiles
    ) {
        List<CDominoes> shown = new ArrayList<>();
        CDominoes carryCopy;
        synchronized (logLock) {
            if (runnerUpTiles != null) shown.addAll(runnerUpTiles);
            if (carryoverRunnerUp != null) shown.add(carryoverRunnerUp);
            carryCopy = carryoverRunnerUp;
        }

        String countText = String.valueOf(shown.size());
        String tilesText = tilesAscii(shown);

        println("");
        println("=== Final Result :) @ " + now() + " ===");
        println("WINNER: " + winnerLabel);
        println("SECOND: " + runnerUpLabel);
        println(runnerUpLabel + " kept: " + countText);
        println(runnerUpLabel + "'s leftover tiles: " + tilesText);
        println("Moves made: " + renderChain());
        println("===============================");
    }

    // returns the whole move log
    public static List<String> getMoveLines() {
        synchronized (logLock) {
            return Collections.unmodifiableList(new ArrayList<>(moveLines));
        }
    }

    // returns the chain in order
    public static List<String> getChainOrder() {
        synchronized (logLock) {
            return Collections.unmodifiableList(new ArrayList<>(chainOrder));
        }
    }

    // builds ascii for one tile
    private static String tileAscii(CDominoes tile) {
        Integer a = valueA(tile);
        Integer b = valueB(tile);
        String left = a == null ? "?" : String.valueOf(a);
        String right = b == null ? "?" : String.valueOf(b);
        return "[" + left + "|" + right + "]";
    }

    // picks first value by current facing
    private static Integer valueA(CDominoes tile) {
        boolean isVertical = tile.getOrientation() != null && tile.getOrientation().startsWith("Vertical");
        return isVertical ? tile.getTopValue() : tile.getLeftValue();
    }

    // picks second value by current facing
    private static Integer valueB(CDominoes tile) {
        boolean isVertical = tile.getOrientation() != null && tile.getOrientation().startsWith("Vertical");
        return isVertical ? tile.getBottomValue() : tile.getRightValue();
    }

    // builds ascii for many tiles
    private static String tilesAscii(List<CDominoes> tiles) {
        if (tiles == null || tiles.isEmpty()) return "[]";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < tiles.size(); i++) {
            if (i > 0) out.append(" ");
            out.append(tileAscii(tiles.get(i)));
        }
        return out.toString();
    }

    // renders the chain with arrows
    private static String renderChain() {
        synchronized (logLock) {
            if (chainOrder.isEmpty()) return "—";
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < chainOrder.size(); i++) {
                if (i > 0) out.append(" -> ");
                out.append(chainOrder.get(i));
            }
            return out.toString();
        }
    }

    // prints a time stamped line
    private static String stamp(String text) {
        return "[" + now() + "] " + text;
    }

    // returns a simple time string
    private static String now() {
        return LocalTime.now().format(clockFmt);
    }

    // pads a small label for neat columns
    private static String padRight(String text, int width) {
        if (text.length() >= width) return text;
        StringBuilder sb = new StringBuilder(text);
        while (sb.length() < width) sb.append(' ');
        return sb.toString();
    }

    // maps side to a friendly label
    private static String sideLabel(TurnManager.Side side) {
        if (side == null) return "Unknown";
        return side == TurnManager.Side.PLAYER ? "Player" : "Computer";
    }

    // prints to console
    private static void println(String s) {
        System.out.println(s);
    }
}
