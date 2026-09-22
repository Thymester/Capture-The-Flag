package Methods;

public class ViewSystem {
    public static void viewSystem(GameState gameState) {
        if (gameState.getSystemIteration() == 0) {
            gameState.setSystemType("Linux");
            gameState.setLastLogin("Jan. 6th at 3:34pm");
        }
        else if (gameState.getSystemIteration() == 1) {
            gameState.setSystemType("Windows");
            gameState.setLastLogin("Sep. 22 at 1:50am");
        }
        else if (gameState.getSystemIteration() == 2) {
            gameState.setSystemType("Mac");
            gameState.setLastLogin("Aug. 1 at 4:48pm");
        }

        System.out.println("\nThe system is below:");
        System.out.println("Operating System: " + gameState.getSystemType());
        System.out.println("Security Difficulty: " + gameState.getDifficulty());
        System.out.println("Last Login: " + gameState.getLastLogin() + "\n");
    }
}