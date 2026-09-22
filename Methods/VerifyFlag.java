package Methods;

import java.util.Scanner;

public class VerifyFlag {
    public static void verifyFlag(Scanner scanner, GameState gameState) {
        System.out.print("Enter the SHA-256 hash you calculated: ");
        if (!scanner.hasNextLine()) {
            return;
        }

        String submittedHash = scanner.nextLine().trim();

        if (!submittedHash.matches("[0-9a-fA-F]{64}")) {
            System.out.println("Invalid hash format. A SHA-256 hash must contain 64 hexadecimal characters.");
            return;
        }

        if (gameState.getExpectedHash().equalsIgnoreCase(submittedHash)) {
            System.out.println("Correct. The artifact was verified.");
            gameState.advanceIteration();

            if (gameState.getFlagVerification() < 3) {
                System.out.println("The system is becoming more difficult.");
            }
            else {
                System.out.println("You reached the end of the first three iterations.");
            }
        }
        else {
            System.out.println("Incorrect hash. Review the evidence and try again.");
        }
    }
}