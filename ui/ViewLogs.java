package ui;

import game.GameState;
import java.util.Map;

public class ViewLogs {
    public static void viewLogs(GameState gameState) {
        System.out.println("\n--- Security Logs ---");
        System.out.println("Algorithm: SHA-256");

        if (gameState.isComplete()) {
            System.out.println("[INFO] You have already completed the game.");
            return;
        }

        System.out.println("Recovered files from the current system:");
        for (Map.Entry<String, String> file : gameState.getEvidenceFileSystem().getFiles().entrySet()) {
            System.out.println("\n--- " + file.getKey() + " ---");
            System.out.println(file.getValue());
        }
    }
}