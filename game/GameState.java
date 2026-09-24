package game;

import crypto.EncodingService;
import evidence.EvidenceFileSystem;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class GameState {
    private static final List<SystemProfile> SYSTEM_PROFILES = List.of(
        new SystemProfile("Linux", 8,
            "linux-admin-", EncodingService.Encoding.PLAIN, "INFO",
            "Linux backup account token recovered from auth.log",
            "This evidence is plaintext. Hash the token exactly as shown.", false,
            "/var/log/auth.log", "/home/analyst/.cache/session.log", "/home/analyst/.cache/.session", "SESSION_TOKEN"),
        new SystemProfile("Windows", 16,
            "windows-event-", EncodingService.Encoding.BASE64, "WARN",
            "Windows event export contains a Base64 credential artifact",
            "Decode the recovered text using the format suggested by the surrounding logs, then hash it.", false,
            "C:/Windows/Logs/System.evtx", "C:/Users/analyst/AppData/Local/Temp/event-export.txt",
            "C:/Users/analyst/AppData/Local/Temp/.credential.dat", "EVENT_DATA"),
        new SystemProfile("Mac", 32,
            "mac-backup-", EncodingService.Encoding.ROT13, "ALERT",
            "Mac backup record contains an obfuscated recovery key",
            "A note in the backup process hints that the recovered text was shifted, not encrypted.", false,
            "/var/log/system.log", "/Users/analyst/Library/Logs/backup-note.log",
            "/Users/analyst/Library/Preferences/.recovery.key", "RECOVERY_KEY"),
        new SystemProfile("Router", 48,
            "router-config-", EncodingService.Encoding.HEX, "WARN",
            "Router configuration export contains a hexadecimal access token",
            "The configuration uses a byte-oriented representation. Preserve every byte before hashing.", false,
            "/logs/firewall.log", "/config/backups/last-known-good.cfg", "/config/.secrets/access.token", "ACCESS_TOKEN"),
        new SystemProfile("Ubuntu", 64,
            "ubuntu-shell-", EncodingService.Encoding.REVERSED, "ALERT",
            "Shell history contains a reversed administrative artifact",
            "The shell history shows a command copied from right to left. Restore the original order.", false,
            "/var/log/auth.log", "/home/analyst/.bash_history", "/home/analyst/.local/share/.backup.key", "BACKUP_KEY"),
        new SystemProfile("Kali", 80,
            "kali-pentest-", EncodingService.Encoding.BASE64, "CRITICAL",
            "Encrypted-looking terminal capture contains a Base64 artifact",
            "The final artifact is buried in a terminal capture. Identify the encoding from the capture metadata.", true,
            "/var/log/secure", "/root/.cache/terminal-capture.log", "/root/.ssh/.capture", "CAPTURE_DATA")
    );

    private static final DateTimeFormatter LOGIN_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH);

    private final SecureRandom random = new SecureRandom();
    private boolean gameRunning = true;
    private int systemIteration;
    private int flagVerification;
    private String expectedHash;
    private String logEvidence;
    private String lastLogin;
    private EvidenceFileSystem evidenceFileSystem;

    public GameState() {
        generateChallenge();
    }

    public boolean isGameRunning() {
        return gameRunning;
    }

    public void stopGame() {
        gameRunning = false;
    }

    public String getSystemType() {
        return getCurrentProfile().getSystemType();
    }

    public String getLastLogin() {
        return lastLogin;
    }

    public int getSystemIteration() {
        return systemIteration;
    }

    public int getFlagVerification() {
        return flagVerification;
    }

    public String getExpectedHash() {
        return expectedHash;
    }

    public String getLogEvidence() {
        return logEvidence;
    }

    public int getDifficulty() {
        return getCurrentProfile().getDifficulty();
    }

    public boolean requiresTerminal() {
        return getCurrentProfile().isTerminalOnly() && !isComplete();
    }

    public SystemProfile getCurrentProfile() {
        return SYSTEM_PROFILES.get(systemIteration);
    }

    public EvidenceFileSystem getEvidenceFileSystem() {
        return evidenceFileSystem;
    }

    public boolean isComplete() {
        return flagVerification >= SYSTEM_PROFILES.size();
    }

    public void advanceIteration() {
        if (systemIteration < SYSTEM_PROFILES.size() - 1) {
            systemIteration += 1;
            flagVerification = systemIteration;
            generateChallenge();
        }
        else {
            flagVerification = SYSTEM_PROFILES.size();
        }
    }

    private void generateChallenge() {
        String secret = createSecret();
        expectedHash = EncodingService.sha256(secret);
        logEvidence = EncodingService.encode(secret, getCurrentProfile().getEncoding());
        lastLogin = randomLogin();
        evidenceFileSystem = new EvidenceFileSystem(getCurrentProfile(), logEvidence, lastLogin);
    }

    private String createSecret() {
        byte[] tokenBytes = new byte[6];
        random.nextBytes(tokenBytes);
        StringBuilder token = new StringBuilder();

        for (byte tokenByte : tokenBytes) {
            token.append(String.format("%02x", tokenByte));
        }

        return getCurrentProfile().getSecretPrefix() + token;
    }

    private String randomLogin() {
        long daysAgo = random.nextInt(91);
        LocalDateTime login = LocalDateTime.now()
                .minusDays(daysAgo)
                .minusHours(random.nextInt(24))
                .minusMinutes(random.nextInt(60));
        return login.format(LOGIN_FORMAT);
    }

    public static class SystemProfile {
        private final String systemType;
        private final int difficulty;
        private final String secretPrefix;
        private final EncodingService.Encoding encoding;
        private final String logLevel;
        private final String logMessage;
        private final String logNote;
        private final boolean terminalOnly;
        private final String logPath;
        private final String leadPath;
        private final String artifactPath;
        private final String artifactLabel;

        private SystemProfile(String systemType, int difficulty,
                String secretPrefix, EncodingService.Encoding encoding, String logLevel,
                String logMessage, String logNote, boolean terminalOnly,
                String logPath, String leadPath, String artifactPath, String artifactLabel) {
            this.systemType = systemType;
            this.difficulty = difficulty;
            this.secretPrefix = secretPrefix;
            this.encoding = encoding;
            this.logLevel = logLevel;
            this.logMessage = logMessage;
            this.logNote = logNote;
            this.terminalOnly = terminalOnly;
            this.logPath = logPath;
            this.leadPath = leadPath;
            this.artifactPath = artifactPath;
            this.artifactLabel = artifactLabel;
        }

        public String getSystemType() {
            return systemType;
        }

        public int getDifficulty() {
            return difficulty;
        }

        public String getSecretPrefix() {
            return secretPrefix;
        }

        public EncodingService.Encoding getEncoding() {
            return encoding;
        }

        public String getLogLevel() {
            return logLevel;
        }

        public String getLogMessage() {
            return logMessage;
        }

        public String getLogNote() {
            return logNote;
        }

        public boolean isTerminalOnly() {
            return terminalOnly;
        }

        public String getLogPath() {
            return logPath;
        }

        public String getLeadPath() {
            return leadPath;
        }

        public String getArtifactPath() {
            return artifactPath;
        }

        public String getArtifactLabel() {
            return artifactLabel;
        }
    }
}
