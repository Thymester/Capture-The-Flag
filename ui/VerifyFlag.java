package ui;

import game.GameState;
import java.util.Scanner;

public class VerifyFlag {
    private final GameState gameState;

    public VerifyFlag(GameState gameState) {
        this.gameState = gameState;
    }

    public void verifyFlag(Scanner scanner) {
        System.out.print("Enter the SHA-256 hash you calculated: ");

        if (!scanner.hasNextLine()) {
            return;
        }

        verifyFlag(scanner.nextLine().trim());
    }

    public void verifyFlag(String submittedHash) {
        if (gameState.isComplete()) {
            System.out.println("The game is already complete.");
            return;
        }

        if (!submittedHash.matches("[0-9a-fA-F]{64}")) {
            System.out.println("Invalid hash format. A SHA-256 hash must contain 64 hexadecimal characters.");
            return;
        }

        if (gameState.getExpectedHash().equalsIgnoreCase(submittedHash)) {
            System.out.println("Correct. The artifact was verified.");
            gameState.advanceIteration();
        }
        else {
            System.out.println("Incorrect hash. Review the evidence and try again.");
        }
    }
}
