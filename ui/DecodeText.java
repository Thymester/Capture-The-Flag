package ui;

import crypto.EncodingService;
import java.util.Scanner;

public class DecodeText {
    public static void decodeText(Scanner scanner) {
        System.out.println("Choose a decoding method:\n1. Base64\n2. ROT13\n3. Hexadecimal\n4. Reverse");
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
        else if (decoder.equals("3")) {
            printDecodedHex(text);
        }
        else if (decoder.equals("4")) {
            System.out.println("Decoded text: " + reverse(text));
        }
        else {
            System.out.println("Unknown decoder. Choose a number from 1 to 4.");
        }
    }

    public static void printDecodedBase64(String text) {
        try {
            System.out.println("Decoded text: " + EncodingService.decodeBase64(text));
        }
        catch (IllegalArgumentException exception) {
            System.out.println("Invalid Base64 text.");
        }
    }

    public static void printDecodedHex(String text) {
        try {
            System.out.println("Decoded text: " + EncodingService.decodeHex(text));
        }
        catch (IllegalArgumentException exception) {
            System.out.println("Invalid hexadecimal text.");
        }
    }

    public static String reverse(String value) {
        return new StringBuilder(value).reverse().toString();
    }

    public static String rot13(String value) {
        return EncodingService.rot13(value);
    }
}
