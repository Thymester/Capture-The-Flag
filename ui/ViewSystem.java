package ui;

import game.GameState;
public class ViewSystem {
    public static void viewSystem(GameState gameState) {
        System.out.println("\nThe system is below:");
        System.out.println("Operating System: " + gameState.getSystemType());
        System.out.println("Security Difficulty: " + gameState.getDifficulty());
        System.out.println("Last Login: " + gameState.getLastLogin() + "\n");
    }
}
