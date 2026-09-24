package ui;

import crypto.EncodingService;
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
        System.out.println("SHA-256: " + EncodingService.sha256(text));
    }
}