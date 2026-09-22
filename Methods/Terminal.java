package Methods;

import java.util.Scanner;

public class Terminal {
    public static void open(Scanner scanner, GameState gameState) {
        System.out.println("\nConnected to ctf-terminal.");
        System.out.println("Type 'help' for available commands or 'exit' to return to the menu.");

        while (scanner.hasNextLine()) {
            String systemName = gameState.getSystemType();
            String promptName = systemName == null ? "unknown" : systemName.toLowerCase();
            System.out.print("ctf@" + promptName + ":~$ ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            String[] commandParts = input.split("\\s+", 3);
            String command = commandParts[0].toLowerCase();

            switch (command) {
                case "help":
                    printHelp();
                    break;
                case "system":
                    ViewSystem.viewSystem(gameState);
                    break;
                case "logs":
                    ViewLogs.viewLogs(gameState);
                    break;
                case "hash":
                    runHash(commandParts);
                    break;
                case "decode":
                    runDecode(commandParts);
                    break;
                case "status":
                    System.out.println("Flag verifications: " + gameState.getFlagVerification());
                    System.out.println("Security difficulty: " + gameState.getDifficulty());
                    break;
                case "clear":
                    clearScreen();
                    break;
                case "exit":
                    System.out.println("Closing terminal.");
                    return;
                default:
                    System.out.println("Command not found. Type 'help' for available commands.");
            }
        }
    }

    private static void printHelp() {
        System.out.println("Available commands:");
        System.out.println("  help                    Show this command list");
        System.out.println("  system                  View system information");
        System.out.println("  logs                    View security logs");
        System.out.println("  hash <text>             Calculate a SHA-256 hash");
        System.out.println("  decode base64 <text>    Decode Base64 text");
        System.out.println("  decode rot13 <text>     Decode ROT13 text");
        System.out.println("  status                  View challenge progress");
        System.out.println("  clear                   Clear the terminal display");
        System.out.println("  exit                    Return to the main menu");
    }

    private static void runHash(String[] commandParts) {
        if (commandParts.length < 2) {
            System.out.println("Usage: hash <text>");
            return;
        }

        String text = commandParts.length == 2
            ? commandParts[1]
            : commandParts[1] + " " + commandParts[2];
        HashText.printHash(text);
    }

    private static void runDecode(String[] commandParts) {
        if (commandParts.length < 3) {
            System.out.println("Usage: decode base64 <text> or decode rot13 <text>");
            return;
        }

        String decoder = commandParts[1].toLowerCase();
        String text = commandParts[2];

        if (decoder.equals("base64")) {
            DecodeText.printDecodedBase64(text);
        }
        else if (decoder.equals("rot13")) {
            System.out.println("Decoded text: " + DecodeText.rot13(text));
        }
        else {
            System.out.println("Unknown decoder. Use 'base64' or 'rot13'.");
        }
    }

    private static void clearScreen() {
        for (int line = 0; line < 30; line++) {
            System.out.println();
        }
    }
}