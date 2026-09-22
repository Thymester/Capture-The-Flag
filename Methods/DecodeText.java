package Methods;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Scanner;

public class DecodeText {
    public static void decodeText(Scanner scanner) {
        System.out.println("Choose a decoding method:\n1. Base64\n2. ROT13");
        System.out.print("Decoder: ");

        if (!scanner.hasNextLine()) {
            return;
        }

        String decoder = scanner.nextLine().trim();
        System.out.print("Enter the text to decode: ");

        if (!scanner.hasNextLine()) {
            return;
        }

        String text = scanner.nextLine();

        if (decoder.equals("1")) {
            printDecodedBase64(text);
        }
        else if (decoder.equals("2")) {
            System.out.println("Decoded text: " + rot13(text));
        }
        else {
            System.out.println("Unknown decoder. Choose 1 for Base64 or 2 for ROT13.");
        }
    }

    public static void printDecodedBase64(String text) {
        try {
            String decoded = new String(Base64.getDecoder().decode(text), StandardCharsets.UTF_8);
            System.out.println("Decoded text: " + decoded);
        }
        catch (IllegalArgumentException exception) {
            System.out.println("Invalid Base64 text.");
        }
    }

    public static String rot13(String value) {
        StringBuilder decoded = new StringBuilder();

        for (char character : value.toCharArray()) {
            if (character >= 'a' && character <= 'z') {
                decoded.append((char) ('a' + (character - 'a' + 13) % 26));
            }
            else if (character >= 'A' && character <= 'Z') {
                decoded.append((char) ('A' + (character - 'A' + 13) % 26));
            }
            else {
                decoded.append(character);
            }
        }

        return decoded.toString();
    }
}