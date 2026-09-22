package Methods;

public class ViewLogs {
    public static void viewLogs(GameState gameState) {
        System.out.println("\n--- Security Logs ---");
        System.out.println("Algorithm: SHA-256");

        if (gameState.getSystemIteration() == 0) {
            System.out.println("[INFO] Linux backup account token recovered from auth.log:");
            System.out.println(gameState.getLogEvidence());
            System.out.println("[NOTE] This evidence is plaintext. Hash the token exactly as shown.");
        }
        else if (gameState.getSystemIteration() == 1) {
            System.out.println("[WARN] Windows event export contains a Base64 credential artifact:");
            System.out.println(gameState.getLogEvidence());
            System.out.println("[NOTE] Decode Base64, then hash the decoded text.");
        }
        else if (gameState.getSystemIteration() == 2) {
            System.out.println("[ALERT] Mac backup record contains an obfuscated recovery key:");
            System.out.println(gameState.getLogEvidence());
            System.out.println("[NOTE] Decode ROT13, then hash the decoded text.");
        }
        else {
            System.out.println("[INFO] The first three iterations are complete.");
        }
    }
}