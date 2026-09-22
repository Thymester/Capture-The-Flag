import Methods.DecodeText;
import Methods.GameState;
import Methods.HashText;
import Methods.Terminal;
import Methods.VerifyFlag;
import Methods.ViewLogs;
import Methods.ViewSystem;
import java.util.Scanner;

public class CaptureTheFlag {
    public static void main (String[] args ) {
        GameState gameState = new GameState();
        gameplayLoop(gameState);
    }

    private static void gameplayLoop(GameState gameState) {
        try (Scanner scanner = new Scanner(System.in)) {
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

                switch (userChoice) {
                    case 1: 
                        ViewSystem.viewSystem(gameState);
                        break;
                    case 2:
                        ViewLogs.viewLogs(gameState);
                        break;
                    case 3:
                        VerifyFlag.verifyFlag(scanner, gameState);
                        break;
                    case 4:
                        HashText.hashText(scanner);
                        break;
                    case 5:
                        DecodeText.decodeText(scanner);
                        break;
                    case 6:
                        Terminal.open(scanner, gameState);
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
}
