package terminal;

import evidence.EvidenceFileSystem;
import game.GameState;
import java.util.Scanner;
import ui.DecodeText;
import ui.HashText;
import ui.VerifyFlag;
import ui.ViewLogs;
import ui.ViewSystem;

public class Terminal {
    private final GameState gameState;
    private final VerifyFlag verifier;

    public Terminal(GameState gameState) {
        this.gameState = gameState;
        this.verifier = new VerifyFlag(gameState);
    }

    public void open(Scanner scanner) {
        System.out.println("\nConnected to ctf-terminal.");
        System.out.println("Type 'help' for available commands or 'exit' to return to the menu.");

        while (scanner.hasNextLine()) {
            String promptName = gameState.getSystemType().toLowerCase();
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
                case "pwd":
                    System.out.println(getFileSystem().getCurrentDirectory());
                    break;
                case "ls":
                    listFiles(commandParts);
                    break;
                case "cd":
                    changeDirectory(commandParts);
                    break;
                case "cat":
                    readFile(commandParts);
                    break;
                case "grep":
                    searchFiles(commandParts);
                    break;
                case "find":
                    findFiles(commandParts);
                    break;
                case "hash":
                    runHash(commandParts);
                    break;
                case "decode":
                    runDecode(commandParts);
                    break;
                case "verify":
                    verifyHash(commandParts);
                    break;
                case "status":
                    printStatus();
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

    private void printHelp() {
        System.out.println("Available commands:");
        System.out.println("  help                    Show this command list");
        System.out.println("  system                  View system information");
        System.out.println("  logs                    Print recovered log files");
        System.out.println("  pwd                     Show the current directory");
        System.out.println("  ls [-a]                 List files in the current directory");
        System.out.println("  cd <directory>          Change directory");
        System.out.println("  cat <file>              Read a file");
        System.out.println("  grep <text>             Search all recovered files");
        System.out.println("  find [directory]        Find files below a directory");
        System.out.println("  hash <text>             Calculate a SHA-256 hash");
        System.out.println("  decode <type> <text>    Decode an artifact");
        System.out.println("  verify <SHA-256 Hash>   Verify the artifact");
        System.out.println("  status                  View challenge progress");
        System.out.println("  clear                   Clear the terminal display");
        System.out.println("  exit                    Return to the main menu");
    }

    private void listFiles(String[] commandParts) {
        boolean showHidden = commandParts.length > 1 && commandParts[1].equals("-a");
        for (String entry : getFileSystem().listDirectory(showHidden)) {
            System.out.println(entry);
        }
    }

    private void changeDirectory(String[] commandParts) {
        if (commandParts.length < 2) {
            System.out.println("Usage: cd <directory>");
            return;
        }

        if (!getFileSystem().changeDirectory(commandParts[1])) {
            System.out.println("cd: directory not found");
        }
    }

    private void readFile(String[] commandParts) {
        if (commandParts.length < 2) {
            System.out.println("Usage: cat <file>");
            return;
        }

        String contents = getFileSystem().readFile(commandParts[1]);
        if (contents == null) {
            System.out.println("cat: file not found");
            return;
        }
        System.out.println(contents);
    }

    private void searchFiles(String[] commandParts) {
        if (commandParts.length < 2) {
            System.out.println("Usage: grep <text>");
            return;
        }

        for (String path : getFileSystem().grepFiles(commandParts[1])) {
            System.out.println(path);
        }
    }

    private void findFiles(String[] commandParts) {
        String startingPath = commandParts.length < 2 ? "/" : commandParts[1];
        for (String path : getFileSystem().findFiles(startingPath)) {
            System.out.println(path);
        }
    }

    private void verifyHash(String[] commandParts) {
        if (commandParts.length < 2) {
            System.out.println("Usage: verify <SHA-256 Hash>");
            return;
        }

        verifier.verifyFlag(commandParts[1]);
    }

    private void runHash(String[] commandParts) {
        if (commandParts.length < 2) {
            System.out.println("Usage: hash <text>");
            return;
        }

        String text = commandParts.length == 2
                ? commandParts[1]
                : commandParts[1] + " " + commandParts[2];
        HashText.printHash(text);
    }

    private void runDecode(String[] commandParts) {
        if (commandParts.length < 3) {
            System.out.println("Usage: decode <type> <text>");
            return;
        }

        String decoder = commandParts[1].toLowerCase();
        String text = commandParts[2];

        switch (decoder) {
            case "base64":
                DecodeText.printDecodedBase64(text);
                break;
            case "rot13":
                System.out.println("Decoded text: " + DecodeText.rot13(text));
                break;
            case "hex":
                DecodeText.printDecodedHex(text);
                break;
            case "reverse":
                System.out.println("Decoded text: " + DecodeText.reverse(text));
                break;
            default:
                System.out.println("Unknown decoder. Use base64, rot13, hex, or reverse.");
        }
    }

    private void printStatus() {
        System.out.println("Flag verifications: " + gameState.getFlagVerification());
        System.out.println("Security difficulty: " + gameState.getDifficulty());
        if (gameState.requiresTerminal()) {
            System.out.println("Access mode: Built-in terminal required");
        }
    }

    private EvidenceFileSystem getFileSystem() {
        return gameState.getEvidenceFileSystem();
    }

    private void clearScreen() {
        for (int line = 0; line < 30; line++) {
            System.out.println();
        }
    }
}
