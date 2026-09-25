package team;

import game.GameState;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import terminal.Terminal;
import ui.DecodeText;
import ui.HashText;
import ui.VerifyFlag;
import ui.ViewLogs;
import ui.ViewSystem;

public class TeamClient {
    public static void play(Scanner scanner) {
        try {
            System.out.print("Server URL [http://localhost:8080]: ");
            String serverUrl = scanner.nextLine().trim();
            if (serverUrl.isEmpty()) {
                serverUrl = "http://localhost:8080";
            }

            System.out.print("Invite code: ");
            String inviteCode = scanner.nextLine().trim();
            System.out.print("Team name: ");
            String teamName = scanner.nextLine().trim();
            System.out.print("Your player name: ");
            String playerName = scanner.nextLine().trim();

            TeamClient client = new TeamClient(serverUrl);
            Map<String, String> join = client.request("/join", null,
                    "invite=" + encoded(inviteCode) + "&team=" + encoded(teamName)
                            + "&player=" + encoded(playerName));
            String sessionId = required(join, "session");
            GameState gameState = new GameState();
            applySnapshot(gameState, join);

            System.out.println("Joined team " + teamName + " with "
                    + join.getOrDefault("playerCount", "1") + " player(s).");
            client.gameplayLoop(scanner, sessionId, gameState);
        }
        catch (Exception exception) {
            System.out.println("Could not connect to the team server: " + exception.getMessage());
        }
    }

    private final String serverUrl;

    private TeamClient(String serverUrl) {
        this.serverUrl = serverUrl.endsWith("/")
                ? serverUrl.substring(0, serverUrl.length() - 1)
                : serverUrl;
    }

    private void gameplayLoop(Scanner scanner, String sessionId, GameState gameState)
            throws IOException {
        RemoteVerifier verifier = new RemoteVerifier(this, sessionId, gameState);
        while (gameState.isGameRunning()) {
            System.out.println("\nTeam status: " + gameState.getFlagVerification()
                    + " challenge(s) completed | " + "players: " + getValue(sessionId, "playerCount"));
            System.out.println("1. View System\n2. View Logs\n3. Verify Flag\n4. Hash Text\n"
                    + "5. Decode Text\n6. Open Terminal\n7. Leaderboard\n8. Exit");
            System.out.print("Choice: ");
            if (!scanner.hasNextLine()) {
                return;
            }

            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                continue;
            }
            switch (input) {
                case "1":
                    ViewSystem.viewSystem(gameState);
                    break;
                case "2":
                    ViewLogs.viewLogs(gameState);
                    break;
                case "3":
                    verifier.verifyFlag(scanner);
                    break;
                case "4":
                    HashText.hashText(scanner);
                    break;
                case "5":
                    DecodeText.decodeText(scanner);
                    break;
                case "6":
                    new Terminal(gameState, verifier).open(scanner);
                    break;
                case "7":
                    printLeaderboard(sessionId);
                    break;
                case "8":
                    gameState.stopGame();
                    break;
                default:
                    System.out.println("Choose an option from 1 to 8.");
            }
        }
    }

    private String getValue(String sessionId, String key) throws IOException {
        return request("/snapshot", sessionId).getOrDefault(key, "0");
    }

    private void printLeaderboard(String sessionId) throws IOException {
        System.out.println("\n--- Team Leaderboard ---");
        String response = requestRaw("/leaderboard", "GET", null, sessionId);
        for (String line : response.split("\\R")) {
            if (!line.isBlank()) {
                Map<String, String> values = parse(line.replace('|', '\n'));
                System.out.println(decode(values.get("team")) + " | players: "
                        + values.get("players") + " | points: " + values.get("score")
                        + " | errors: " + values.get("errors")
                    + " | top player: " + decode(values.get("topPlayer"))
                    + " (" + values.get("topPoints") + " points)"
                        + " | time: " + values.get("elapsedSeconds") + "s"
                        + " | complete: " + values.get("complete"));
            }
        }
    }

    private Map<String, String> request(String path, String sessionId) throws IOException {
        return request(path, sessionId, null);
    }

    private Map<String, String> request(String path, String sessionId, String formBody)
            throws IOException {
        String method = formBody == null ? "GET" : "POST";
        return parse(requestRaw(path, method, formBody, sessionId));
    }

    private String requestRaw(String path, String method, String formBody, String sessionId)
            throws IOException {
        URL url = URI.create(serverUrl + path).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        if (sessionId != null) {
            connection.setRequestProperty("X-CTF-Session", sessionId);
        }
        if (formBody != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.getOutputStream().write(formBody.getBytes(StandardCharsets.UTF_8));
        }
        int responseCode = connection.getResponseCode();
        String response = new String((responseCode >= 400
                ? connection.getErrorStream() : connection.getInputStream()).readAllBytes(),
                StandardCharsets.UTF_8);
        if (responseCode >= 400) {
            throw new IOException(response.replace("error=", ""));
        }
        return response;
    }

    private static Map<String, String> parse(String response) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String line : response.split("\\R")) {
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                values.put(parts[0], parts[1]);
            }
        }
        return values;
    }

    private static void applySnapshot(GameState gameState, Map<String, String> values) {
        gameState.applySnapshot(Integer.parseInt(required(values, "iteration")),
                decode(required(values, "hash")), decode(required(values, "evidence")),
            decode(required(values, "login")), Boolean.parseBoolean(required(values, "complete")));
    }

    private static String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Server response did not contain " + key);
        }
        return value;
    }

    private static String encoded(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String decode(String value) {
        if (value == null) {
            return "";
        }
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static String getValue(Map<String, String> values, String key) {
        return values.getOrDefault(key, "0");
    }

    private static class RemoteVerifier extends VerifyFlag {
        private final TeamClient client;
        private final String sessionId;
        private final GameState gameState;

        private RemoteVerifier(TeamClient client, String sessionId, GameState gameState) {
            super(gameState);
            this.client = client;
            this.sessionId = sessionId;
            this.gameState = gameState;
        }

        @Override
        public void verifyFlag(String submittedHash) {
            try {
                Map<String, String> response = client.request("/verify", sessionId,
                    "hash=" + encoded(submittedHash));
                applySnapshot(gameState, response);
                if ("correct".equals(response.get("result"))) {
                    System.out.println("Correct. The team advanced to the next challenge.");
                    System.out.println("Team points: " + getValue(response, "teamScore")
                            + " | Your points: " + getValue(response, "playerPoints"));
                    if (gameState.isComplete()) {
                        System.out.println("Your team completed the game!");
                        gameState.stopGame();
                    }
                }
                else if ("complete".equals(response.get("result"))) {
                    System.out.println("This team has already completed the game.");
                    gameState.stopGame();
                }
                else {
                    System.out.println("Incorrect hash. Team errors: "
                            + getValue(response, "teamErrors"));
                }
            }
            catch (IOException | IllegalArgumentException exception) {
                System.out.println("Verification failed: " + exception.getMessage());
            }
        }
    }
}
