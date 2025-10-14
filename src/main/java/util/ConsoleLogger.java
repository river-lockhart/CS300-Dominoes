package util;

import controllers.TurnManager;
import models.CDominoes;
import models.TableLayout;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
    note: comments are only above functions, lower-case, ≤10 words
*/
public final class ConsoleLogger {

    private static final DateTimeFormatter clockFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final List<String> moveLines = new ArrayList<>();
    private static final List<String> chainOrder = new ArrayList<>();

    // carryover runner-up tile (edge case: both hands 0, boneyard 1)
    private static CDominoes carryoverRunnerUp = null;

    private ConsoleLogger() {}

    // resets all logs for a fresh game
    public static void startGame() {
        moveLines.clear();
        chainOrder.clear();
        carryoverRunnerUp = null;
        println("");
        println("=== domino game started @ " + now() + " ===");
    }

    // prints a player drawing one tile
    public static void logDraw(TurnManager.Side side, CDominoes tile) {
        String who = sideLabel(side);
        String piece = tileAscii(tile);
        String line = stamp("DRAW  | " + padRight(who, 7) + " drew " + piece);
        moveLines.add(line);
        println(line);
    }

    // prints a player passing their turn
    public static void logPass(TurnManager.Side side) {
        String who = sideLabel(side);
        String line = stamp("PASS  | " + padRight(who, 7) + " passed (no move)");
        moveLines.add(line);
        println(line);
    }

    // prints first placement only, adds to chain silently
    public static void logFirstPlacement(TurnManager.Side side, CDominoes tile) {
        String who = sideLabel(side);
        String piece = tileAscii(tile);
        String line = stamp("PLACE | " + padRight(who, 7) + " placed " + piece);
        moveLines.add(line);
        println(line);

        // record order, but do NOT print chain here
        chainOrder.add(piece);
    }

    // prints a normal placement with coordinates, adds to chain silently
    public static void logMovePlaced(TurnManager.Side side, CDominoes tile, TableLayout.Placement plan) {
        String who = sideLabel(side);
        String piece = tileAscii(tile);
        String face = plan.vertical ? "vertical" : "horizontal";
        String place = "(row " + plan.row + ", col " + plan.col + ", " + face + ")";
        String line = stamp("PLACE | " + padRight(who, 7) + " placed " + piece + " " + place);
        moveLines.add(line);
        println(line);

        // record order, but do NOT print chain here
        chainOrder.add(piece);
    }

    // prints "placed [a] against [b]" and adds [a] to chain silently
    public static void logPlacedAgainst(TurnManager.Side side, CDominoes placed, CDominoes matched) {
        String who = sideLabel(side);
        String a = tileAscii(placed);
        String b = tileAscii(matched);
        String line = stamp("MATCH | " + padRight(who, 7) + " placed " + a + " against " + b);
        moveLines.add(line);
        println(line);

        // record order, but do NOT print chain here
        chainOrder.add(a);
    }

    // prints a short winner summary
    public static void logWinnerSimple(String winnerLabel) {
        println(stamp("FINAL | winner: " + winnerLabel));
    }

    // set carryover tile for runner-up (edge case)
    public static void setCarryoverForRunnerUp(CDominoes tile) {
        carryoverRunnerUp = tile;
    }

    // clear carryover
    public static void clearCarryover() {
        carryoverRunnerUp = null;
    }

    // prints the final required summary (only place the chain here)
    public static void logFinalResult(
            String winnerLabel,
            String runnerUpLabel,
            List<CDominoes> runnerUpTiles
    ) {
        // stitch carryover into runner-up list if present
        List<CDominoes> shown = new ArrayList<>();
        if (runnerUpTiles != null) shown.addAll(runnerUpTiles);
        if (carryoverRunnerUp != null) shown.add(carryoverRunnerUp);

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
        return Collections.unmodifiableList(moveLines);
    }

    // returns the chain in order
    public static List<String> getChainOrder() {
        return Collections.unmodifiableList(chainOrder);
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
        if (chainOrder.isEmpty()) return "—";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < chainOrder.size(); i++) {
            if (i > 0) out.append(" -> ");
            out.append(chainOrder.get(i));
        }
        return out.toString();
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
