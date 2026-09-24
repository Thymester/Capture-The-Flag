package crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class EncodingService {
    private EncodingService() {
    }

    public enum Encoding {
        PLAIN,
        BASE64,
        ROT13,
        HEX,
        REVERSED
    }

    public static String encode(String value, Encoding encoding) {
        switch (encoding) {
            case BASE64:
                return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
            case ROT13:
                return transformRot13(value);
            case HEX:
                return toHex(value);
            case REVERSED:
                return new StringBuilder(value).reverse().toString();
            case PLAIN:
            default:
                return value;
        }
    }

    public static String sha256(String value) {
        try {
            byte[] hashBytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder();

            for (byte hashByte : hashBytes) {
                hash.append(String.format("%02x", hashByte));
            }

            return hash.toString();
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public static String decodeBase64(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    public static String decodeHex(String value) {
        if (value.length() % 2 != 0 || !value.matches("[0-9a-fA-F]+")) {
            throw new IllegalArgumentException("Invalid hexadecimal text");
        }

        StringBuilder decoded = new StringBuilder();
        for (int index = 0; index < value.length(); index += 2) {
            int character = Integer.parseInt(value.substring(index, index + 2), 16);
            decoded.append((char) character);
        }
        return decoded.toString();
    }

    public static String rot13(String value) {
        return transformRot13(value);
    }

    private static String toHex(String value) {
        StringBuilder encodedValue = new StringBuilder();

        for (byte character : value.getBytes(StandardCharsets.UTF_8)) {
            encodedValue.append(String.format("%02x", character & 0xff));
        }

        return encodedValue.toString();
    }

    private static String transformRot13(String value) {
        StringBuilder encodedValue = new StringBuilder();

        for (char character : value.toCharArray()) {
            if (character >= 'a' && character <= 'z') {
                encodedValue.append((char) ('a' + (character - 'a' + 13) % 26));
            }
            else if (character >= 'A' && character <= 'Z') {
                encodedValue.append((char) ('A' + (character - 'A' + 13) % 26));
            }
            else {
                encodedValue.append(character);
            }
        }

        return encodedValue.toString();
    }
}
