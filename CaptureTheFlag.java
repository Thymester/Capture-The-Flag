import game.GameState;
import java.util.Scanner;
import team.TeamClient;
import terminal.Terminal;
import ui.DecodeText;
import ui.HashText;
import ui.VerifyFlag;
import ui.ViewLogs;
import ui.ViewSystem;

public class CaptureTheFlag {
    public static void main (String[] args ) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Welcome to Capture the Flag!");
            System.out.println("1. Solo Play\n2. Team Play");
            System.out.print("Choose a mode: ");

            if (scanner.hasNextLine() && scanner.nextLine().trim().equals("2")) {
                TeamClient.play(scanner);
                return;
            }

            GameState gameState = new GameState();
            gameplayLoop(scanner, gameState, new VerifyFlag(gameState));
        }
    }

    private static void gameplayLoop(Scanner scanner, GameState gameState, VerifyFlag verifier) {
        while (gameState.isGameRunning()) {

                System.out.println("Welcome to Capture the Flag!");
                System.out.println("Navigate the menu to slowly figure out how to capture the flag.");

                System.out.println("What do you wish to do?\n\n1. View System\n2. View Logs\n3. Verify Flag\n4. Hash Text\n5. Decode Text\n6. Open Terminal\n7. Exit");
                System.out.flush();

                if (!scanner.hasNextLine()) {
                    break;
                }

                String menuInput = scanner.nextLine().trim();
                if (menuInput.isEmpty()) {
                    continue;
                }

                int userChoice;

                try {
                    userChoice = Integer.parseInt(menuInput);
                }
                catch (NumberFormatException exception) {
                    System.out.println("Invalid input. Enter a number from 1 to 7.");
                    continue;
                }

                if (gameState.requiresTerminal() && userChoice >= 1 && userChoice <= 5) {
                    System.out.println("This system rejects menu tools. Open the built-in terminal with option 6.");
                    continue;
                }

                switch (userChoice) {
                    case 1: 
                        ViewSystem.viewSystem(gameState);
                        break;
                    case 2:
                        ViewLogs.viewLogs(gameState);
                        break;
                    case 3:
                        verifier.verifyFlag(scanner);
                        break;
                    case 4:
                        HashText.hashText(scanner);
                        break;
                    case 5:
                        DecodeText.decodeText(scanner);
                        break;
                    case 6:
                        new Terminal(gameState).open(scanner);
                        break;
                    case 7:
                        System.out.println("Thank you for playing Capture the Flag!");
                        gameState.stopGame();
                        break;
                    default:
                        System.out.println("You did not enter a valid input.");
                }
        }
    }
}
