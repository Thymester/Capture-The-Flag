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
        new SystemProfile("Linux", "Beginner",
            "linux-admin-", EncodingService.Encoding.PLAIN, "INFO",
            "Linux backup account token recovered from auth.log",
            "This evidence is plaintext. Hash the token exactly as shown.", false,
            "/var/log/auth.log", "/home/analyst/.cache/session.log", "/home/analyst/.cache/.session", "SESSION_TOKEN"),
        new SystemProfile("Windows", "Beginner",
            "windows-event-", EncodingService.Encoding.BASE64, "WARN",
            "Windows event export contains a Base64 credential artifact",
            "Decode the recovered text using the format suggested by the surrounding logs, then hash it.", false,
            "C:/Windows/Logs/System.evtx", "C:/Users/analyst/AppData/Local/Temp/event-export.txt",
            "C:/Users/analyst/AppData/Local/Temp/.credential.dat", "EVENT_DATA"),
        new SystemProfile("Mac", "Beginner",
            "mac-backup-", EncodingService.Encoding.ROT13, "ALERT",
            "Mac backup record contains an obfuscated recovery key",
            "A note in the backup process hints that the recovered text was shifted, not encrypted.", false,
            "/var/log/system.log", "/Users/analyst/Library/Logs/backup-note.log",
            "/Users/analyst/Library/Preferences/.recovery.key", "RECOVERY_KEY"),
        new SystemProfile("Router", "Easy",
            "router-config-", EncodingService.Encoding.HEX, "WARN",
            "Router configuration export contains a hexadecimal access token",
            "The configuration uses a byte-oriented representation. Preserve every byte before hashing.", true,
            "/logs/firewall.log", "/config/backups/last-known-good.cfg", "/config/.secrets/access.token", "ACCESS_TOKEN"),
        new SystemProfile("Ubuntu", "Easy",
            "ubuntu-shell-", EncodingService.Encoding.REVERSED, "ALERT",
            "Shell history contains a reversed administrative artifact",
            "The shell history shows a command copied from right to left. Restore the original order.", true,
            "/var/log/auth.log", "/home/analyst/.bash_history", "/home/analyst/.local/share/.backup.key", "BACKUP_KEY"),
        new SystemProfile("Kali", "Easy",
            "kali-pentest-", EncodingService.Encoding.BASE64, "CRITICAL",
            "Encrypted-looking terminal capture contains a Base64 artifact",
            "The final artifact is buried in a terminal capture. Identify the encoding from the capture metadata.", true,
            "/var/log/secure", "/root/.cache/terminal-capture.log", "/root/.ssh/.capture", "CAPTURE_DATA"),
        new SystemProfile("FreeBSD", "Moderate",
            "freebsd-jail-", EncodingService.Encoding.ROT13, "WARN",
            "FreeBSD jail audit contains a rotated service credential",
            "The jail report identifies a rotation offset. Decode the credential before hashing it.", true,
            "/var/log/security", "/usr/local/etc/jail.conf", "/usr/local/etc/.service.key", "JAIL_TOKEN"),
        new SystemProfile("Android", "Moderate",
            "android-debug-", EncodingService.Encoding.HEX, "ALERT",
            "Android debug logs contain a hexadecimal recovery token",
            "The debug bridge copied the artifact as bytes. Decode the complete value before hashing it.", true,
            "/data/logs/system.log", "/sdcard/Download/debug-note.txt", "/data/misc/.recovery.token", "DEBUG_TOKEN"),
        new SystemProfile("Solaris", "Moderate",
            "solaris-zone-", EncodingService.Encoding.REVERSED, "WARN",
            "Solaris zone history contains a reversed maintenance key",
            "The zone export was written in reverse order. Restore the key exactly before hashing it.", true,
            "/var/adm/messages", "/zones/maintenance/history.log", "/zones/.maintenance.key", "ZONE_KEY"),
        new SystemProfile("OpenBSD", "Moderate",
            "openbsd-firewall-", EncodingService.Encoding.BASE64, "CRITICAL",
            "OpenBSD firewall records contain a Base64 rule artifact",
            "The firewall snapshot marks the artifact as encoded. Decode it and hash the recovered value.", true,
            "/var/log/pflog", "/etc/pf/rules.snapshot", "/etc/pf/.access.dat", "RULE_DATA"),
        new SystemProfile("Debian", "Hard",
            "debian-root-", EncodingService.Encoding.PLAIN, "ALERT",
            "Debian package logs contain a root account artifact",
            "The token is plaintext, but it is surrounded by decoy values. Hash only the labeled artifact.", true,
            "/var/log/apt/history.log", "/var/cache/apt/restore-note.log", "/var/lib/.root-token", "ROOT_TOKEN"),
        new SystemProfile("Fedora", "Hard",
            "fedora-selinux-", EncodingService.Encoding.BASE64, "WARN",
            "SELinux audit output contains an encoded policy credential",
            "Use the policy audit trail to locate the relevant Base64 value before decoding it.", true,
            "/var/log/audit/audit.log", "/var/lib/selinux/policy-note.log", "/var/lib/selinux/.policy.key", "POLICY_KEY"),
        new SystemProfile("Raspberry Pi", "Hard",
            "pi-sensor-", EncodingService.Encoding.ROT13, "ALERT",
            "Sensor backup logs contain an obfuscated device secret",
            "The backup note says the secret was shifted. Decode the labeled value and hash the result.", true,
            "/var/log/daemon.log", "/home/pi/backups/sensor-note.log", "/home/pi/.sensor.secret", "SENSOR_SECRET"),
        new SystemProfile("Docker Host", "Hard",
            "docker-host-", EncodingService.Encoding.HEX, "CRITICAL",
            "Container runtime metadata contains a hexadecimal registry token",
            "The registry export uses byte notation. Preserve leading zeroes while decoding it.", true,
            "/var/log/docker.log", "/var/lib/docker/metadata/registry.log", "/var/lib/docker/.registry.token", "REGISTRY_TOKEN"),
        new SystemProfile("Azure VM", "Very Hard",
            "azure-vm-", EncodingService.Encoding.REVERSED, "WARN",
            "Virtual machine diagnostics contain a reversed deployment secret",
            "The diagnostic collector reversed the artifact during export. Restore its original order.", true,
            "/var/log/azure/agent.log", "/var/lib/waagent/deployment-note.log", "/var/lib/waagent/.deploy.key", "DEPLOY_KEY"),
        new SystemProfile("AWS Instance", "Very Hard",
            "aws-instance-", EncodingService.Encoding.BASE64, "ALERT",
            "Cloud-init output contains a Base64 instance credential",
            "Find the credential in the cloud-init capture, decode it, and hash only the recovered text.", true,
            "/var/log/cloud-init.log", "/var/lib/cloud/instance/boot-note.log", "/var/lib/cloud/.instance.dat", "INSTANCE_DATA"),
        new SystemProfile("Kubernetes Node", "Very Hard",
            "k8s-node-", EncodingService.Encoding.HEX, "CRITICAL",
            "Kubernetes node diagnostics contain a hexadecimal bootstrap token",
            "The bootstrap record stores the token as bytes. Decode every byte before hashing it.", true,
            "/var/log/kubelet.log", "/var/lib/kubelet/bootstrap-note.log", "/var/lib/kubelet/.bootstrap.token", "BOOTSTRAP_TOKEN"),
        new SystemProfile("SCADA Controller", "Impossible",
            "scada-control-", EncodingService.Encoding.ROT13, "ALERT",
            "Industrial controller logs contain a rotated safety credential",
            "The maintenance record hints at a shifted artifact hidden among operational readings.", true,
            "/var/log/scada/events.log", "/opt/scada/maintenance-note.log", "/opt/scada/.safety.key", "SAFETY_KEY"),
        new SystemProfile("Satellite Link", "Impossible",
            "sat-link-", EncodingService.Encoding.REVERSED, "CRITICAL",
            "Satellite link telemetry contains a reversed uplink artifact",
            "Telemetry was serialized from right to left. Reconstruct the original artifact before hashing it.", true,
            "/var/log/telemetry.log", "/opt/link/uplink-note.log", "/opt/link/.uplink.key", "UPLINK_KEY"),
        new SystemProfile("Zero-Day Lab", "Impossible",
            "zero-day-lab-", EncodingService.Encoding.BASE64, "CRITICAL",
            "The final lab capture contains a Base64 challenge artifact",
            "The artifact is buried in the terminal capture. Identify its format, decode it, and verify the hash.", true,
            "/var/log/lab/incident.log", "/opt/lab/final-capture.log", "/opt/lab/.final.artifact", "FINAL_DATA")
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

    public String getDifficulty() {
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

    public void applySnapshot(int iteration, String snapshotHash, String snapshotEvidence,
            String snapshotLogin) {
        applySnapshot(iteration, snapshotHash, snapshotEvidence, snapshotLogin, false);
    }

    public void applySnapshot(int iteration, String snapshotHash, String snapshotEvidence,
            String snapshotLogin, boolean complete) {
        if (iteration < 0 || iteration >= SYSTEM_PROFILES.size()) {
            throw new IllegalArgumentException("Invalid challenge iteration");
        }

        systemIteration = iteration;
        flagVerification = complete ? SYSTEM_PROFILES.size() : iteration;
        expectedHash = snapshotHash;
        logEvidence = snapshotEvidence;
        lastLogin = snapshotLogin;
        evidenceFileSystem = new EvidenceFileSystem(getCurrentProfile(), logEvidence, lastLogin);
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
        private final String difficulty;
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

        private SystemProfile(String systemType, String difficulty,
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

        public String getDifficulty() {
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
