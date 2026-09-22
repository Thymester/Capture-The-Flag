package Methods;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class HashText {
    public static void hashText(Scanner scanner) {
        System.out.print("Enter the text to hash: ");

        if (!scanner.hasNextLine()) {
            return;
        }

        String text = scanner.nextLine();

        printHash(text);
    }

    public static void printHash(String text) {
        try {
            byte[] hashBytes = MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder();

            for (byte hashByte : hashBytes) {
                hash.append(String.format("%02x", hashByte));
            }

            System.out.println("SHA-256: " + hash);
        }
        catch (NoSuchAlgorithmException exception) {
            System.out.println("SHA-256 is not available on this system.");
        }
    }
}