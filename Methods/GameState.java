package Methods;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class GameState {
    private static final int LAST_INITIAL_ITERATION = 2;

    private boolean gameRunning = true;
    private String systemType;
    private int systemIteration = 0;
    private int flagVerification = 0;
    private String expectedHash;
    private String logEvidence;
    private String lastLogin;

    public GameState() {
        generateSystemHash();
    }

    public boolean isGameRunning() {
        return gameRunning;
    }

    public void stopGame() {
        gameRunning = false;
    }

    public String getSystemType() {
        return systemType;
    }

    public String getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(String lastLogin) {
        this.lastLogin = lastLogin;
    }

    public void setSystemType(String systemType) {
        this.systemType = systemType;
    }

    public int getSystemIteration() {
        return systemIteration;
    }

    public int getFlagVerification() {
        return flagVerification;
    }

    String getExpectedHash() {
        return expectedHash;
    }

    public String getLogEvidence() {
        return logEvidence;
    }

    public int getDifficulty() {
        if (systemIteration == 0) {
            return 8;
        }
        if (systemIteration == 1) {
            return 16;
        }
        return 32;
    }

    public void advanceIteration() {
        if (systemIteration < LAST_INITIAL_ITERATION) {
            systemIteration += 1;
            flagVerification = systemIteration;
            generateSystemHash();
        }
        else {
            flagVerification = LAST_INITIAL_ITERATION + 1;
        }
    }

    private void generateSystemHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String secret = createSecret();
            expectedHash = hashText(secret, digest);
            logEvidence = encodeSecret(secret);
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String createSecret() {
        String token = randomToken();

        if (systemIteration == 0) {
            return "linux-admin-" + token;
        }
        if (systemIteration == 1) {
            return "windows-event-" + token;
        }
        return "mac-backup-" + token;
    }

    private String encodeSecret(String secret) {
        if (systemIteration == 0) {
            return secret;
        }
        if (systemIteration == 1) {
            return Base64.getEncoder().encodeToString(secret.getBytes(StandardCharsets.UTF_8));
        }
        return rot13(secret);
    }

    private String randomToken() {
        byte[] tokenBytes = new byte[6];
        new SecureRandom().nextBytes(tokenBytes);
        StringBuilder token = new StringBuilder();

        for (byte tokenByte : tokenBytes) {
            token.append(String.format("%02x", tokenByte));
        }

        return token.toString();
    }

    private String rot13(String value) {
        StringBuilder encodedValue = new StringBuilder();

        for (char character : value.toCharArray()) {
            if (character >= 'a' && character <= 'z') {
                encodedValue.append((char) ('a' + (character - 'a' + 13) % 26));
            }
            else {
                encodedValue.append(character);
            }
        }

        return encodedValue.toString();
    }

    private String hashText(String value, MessageDigest digest) {
        byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder hash = new StringBuilder();

        for (byte hashByte : hashBytes) {
            hash.append(String.format("%02x", hashByte));
        }

        return hash.toString();
    }
}