package team;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import game.GameState;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

public class TeamServer {
    private static final int DEFAULT_PORT = 8080;
    private static final Path SCORE_FILE = Path.of("team-scores.json");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, Team> teams = new LinkedHashMap<>();
    private final Map<String, PlayerSession> sessions = new LinkedHashMap<>();
    private final String inviteCode = createInviteCode();

    public static void main(String[] args) throws IOException {
        int port = args.length == 0 ? DEFAULT_PORT : Integer.parseInt(args[0]);
        TeamServer server = new TeamServer();
        server.chooseStartupMode();
        server.start(port);
    }

    private void chooseStartupMode() throws IOException {
        if (!Files.exists(SCORE_FILE)) {
            return;
        }

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("A previous team match was found in " + SCORE_FILE.toAbsolutePath());
            System.out.println("1. Restart from the saved match\n2. Start a new match");
            System.out.print("Choose an option: ");
            String choice = scanner.hasNextLine() ? scanner.nextLine().trim() : "1";
            if (choice.equals("1")) {
                resumeOrStartFresh();
            }
            else if (choice.equals("2")) {
                Files.deleteIfExists(SCORE_FILE);
                System.out.println("Starting a new team match.");
            }
            else {
                resumeOrStartFresh();
            }
        }
    }

    private void resumeOrStartFresh() throws IOException {
        try {
            loadScores();
        }
        catch (IOException exception) {
            Path backupFile = Path.of("team-scores.json.invalid");
            Files.move(SCORE_FILE, backupFile);
            System.out.println("The saved match could not be resumed: " + exception.getMessage());
            System.out.println("Starting a new match. The old file was saved as " + backupFile + ".");
        }
    }

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/join", this::join);
        server.createContext("/snapshot", this::snapshot);
        server.createContext("/verify", this::verify);
        server.createContext("/leaderboard", this::leaderboard);
        server.setExecutor(null);
        server.start();
        System.out.println("Capture The Flag team server listening on port " + port);
        System.out.println("Invite code: " + inviteCode);
        System.out.println("Share the invite code only with intended players.");
        System.out.println("Scores are saved to " + SCORE_FILE.toAbsolutePath());
    }

    private synchronized void join(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            respond(exchange, 405, "error=Join requires POST\n");
            return;
        }

        Map<String, String> query = form(exchange);
        if (!MessageDigest.isEqual(inviteCode.getBytes(StandardCharsets.UTF_8),
                clean(query.get("invite")).getBytes(StandardCharsets.UTF_8))) {
            respond(exchange, 403, "error=Invalid invite code\n");
            return;
        }

        String teamName = clean(query.get("team"));
        String playerName = clean(query.get("player"));

        if (teamName.isEmpty() || playerName.isEmpty()) {
            respond(exchange, 400, "error=Team and player names are required\n");
            return;
        }

        Team team = teams.computeIfAbsent(teamName, ignored -> new Team(teamName));
        Player player = team.findPlayer(playerName);
        if (player == null && team.players.size() >= 6) {
            respond(exchange, 409, "error=That team already has six players\n");
            return;
        }

        String sessionId = UUID.randomUUID().toString();
        if (player == null) {
            player = new Player(playerName);
            team.players.add(player);
        }
        sessions.put(sessionId, new PlayerSession(team, player));
        saveScores();

        respond(exchange, 200, "session=" + sessionId + "\nteam=" + encoded(team.name) + "\n"
                + snapshotValues(team, player));
    }

    private synchronized void snapshot(HttpExchange exchange) throws IOException {
        PlayerSession session = session(exchange);
        if (session == null) {
            return;
        }
        respond(exchange, 200, snapshotValues(session.team, session.player));
    }

    private synchronized void verify(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            respond(exchange, 405, "error=Verification requires POST\n");
            return;
        }

        PlayerSession session = session(exchange);
        if (session == null) {
            return;
        }

        if (session.team.gameState.isComplete()) {
            respond(exchange, 200, "result=complete\n" + snapshotValues(session.team, session.player));
            return;
        }

        String submittedHash = form(exchange).getOrDefault("hash", "");
        session.player.attempts++;
        if (!submittedHash.matches("[0-9a-fA-F]{64}")
                || !session.team.gameState.getExpectedHash().equalsIgnoreCase(submittedHash)) {
            session.player.errors++;
            session.team.errors++;
            saveScores();
            respond(exchange, 200, "result=incorrect\n" + snapshotValues(session.team, session.player));
            return;
        }

        session.player.points += Math.max(10, 100 - session.player.errors * 5);
        session.team.score += Math.max(10, 100 - session.team.errors * 5);
        session.team.gameState.advanceIteration();
        if (session.team.gameState.isComplete() && session.team.completedAt == null) {
            session.team.completedAt = Instant.now().toString();
            session.team.score += Math.max(0, 1000 - session.team.elapsedSeconds());
        }
        saveScores();
        respond(exchange, 200, "result=correct\n" + snapshotValues(session.team, session.player));
    }

    private synchronized void leaderboard(HttpExchange exchange) throws IOException {
        if (session(exchange) == null) {
            return;
        }

        StringBuilder response = new StringBuilder();
        for (Team team : teams.values()) {
            Player topPlayer = team.topPlayer();
            response.append("team=").append(encoded(team.name))
                    .append("|players=").append(team.players.size())
                    .append("|score=").append(team.score)
                    .append("|errors=").append(team.errors)
                    .append("|topPlayer=").append(encoded(topPlayer == null ? "-" : topPlayer.name))
                    .append("|topPoints=").append(topPlayer == null ? 0 : topPlayer.points)
                    .append("|elapsedSeconds=").append(team.elapsedSeconds())
                    .append("|complete=").append(team.gameState.isComplete())
                    .append("\n");
        }
        respond(exchange, 200, response.toString());
    }

    private String snapshotValues(Team team, Player player) {
        GameState state = team.gameState;
        return "iteration=" + state.getSystemIteration() + "\n"
                + "hash=" + encoded(state.getExpectedHash()) + "\n"
                + "evidence=" + encoded(state.getLogEvidence()) + "\n"
                + "login=" + encoded(state.getLastLogin()) + "\n"
                + "complete=" + state.isComplete() + "\n"
                + "teamScore=" + team.score + "\n"
                + "teamErrors=" + team.errors + "\n"
                + "playerPoints=" + player.points + "\n"
                + "playerErrors=" + player.errors + "\n"
                + "playerCount=" + team.players.size() + "\n";
    }

    private PlayerSession session(HttpExchange exchange) throws IOException {
        String sessionId = exchange.getRequestHeaders().getFirst("X-CTF-Session");
        PlayerSession session = sessions.get(sessionId);
        if (session == null) {
            respond(exchange, 401, "error=Invalid team session\n");
        }
        return session;
    }

    private Map<String, String> form(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return parsePairs(body);
    }

    private Map<String, String> parsePairs(String rawQuery) {
        Map<String, String> values = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return values;
        }
        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                values.put(parts[0], URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
            }
        }
        return values;
    }

    private String createInviteCode() {
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        StringBuilder code = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            code.append(String.format("%02x", value));
        }
        return code.toString();
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private String encoded(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private void saveScores() {
        StringBuilder json = new StringBuilder("{\n  \"teams\": [\n");
        int teamIndex = 0;
        for (Team team : teams.values()) {
            if (teamIndex++ > 0) {
                json.append(",\n");
            }
            json.append("    {\n")
                    .append("      \"name\": \"").append(jsonEscape(team.name)).append("\",\n")
                    .append("      \"score\": ").append(team.score).append(",\n")
                    .append("      \"errors\": ").append(team.errors).append(",\n")
                    .append("      \"startedAt\": \"").append(team.startedAt).append("\",\n")
                    .append("      \"completedAt\": ").append(jsonValue(team.completedAt)).append(",\n")
                    .append("      \"iteration\": ").append(team.gameState.getSystemIteration()).append(",\n")
                    .append("      \"hash\": \"").append(jsonEscape(team.gameState.getExpectedHash())).append("\",\n")
                    .append("      \"evidence\": \"").append(jsonEscape(team.gameState.getLogEvidence())).append("\",\n")
                    .append("      \"login\": \"").append(jsonEscape(team.gameState.getLastLogin())).append("\",\n")
                    .append("      \"complete\": ").append(team.gameState.isComplete()).append(",\n")
                    .append("      \"players\": [\n");
            for (int playerIndex = 0; playerIndex < team.players.size(); playerIndex++) {
                if (playerIndex > 0) {
                    json.append(",\n");
                }
                Player player = team.players.get(playerIndex);
                json.append("        {\"name\": \"").append(jsonEscape(player.name))
                        .append("\", \"points\": ").append(player.points)
                        .append(", \"errors\": ").append(player.errors).append("}");
            }
            json.append("\n      ]\n    }");
        }
        json.append("\n  ]\n}\n");
        try {
            Files.writeString(SCORE_FILE, json.toString(), StandardCharsets.UTF_8);
        }
        catch (IOException exception) {
            System.err.println("Could not save scores: " + exception.getMessage());
        }
    }

    private void loadScores() throws IOException {
        String json = Files.readString(SCORE_FILE, StandardCharsets.UTF_8);
        Object parsed = new JsonReader(json).readValue();
        if (!(parsed instanceof Map)) {
            throw new IOException("The score file does not contain a JSON object");
        }

        Object savedTeams = ((Map<?, ?>) parsed).get("teams");
        if (!(savedTeams instanceof List)) {
            throw new IOException("The score file does not contain a teams array");
        }

        teams.clear();
        for (Object savedTeam : (List<?>) savedTeams) {
            if (!(savedTeam instanceof Map)) {
                continue;
            }
            Map<?, ?> values = (Map<?, ?>) savedTeam;
            String name = stringValue(values, "name");
            int iteration = intValue(values, "iteration");
            boolean complete = booleanValue(values, "complete");
            Team team = new Team(name, iteration, stringValue(values, "hash"),
                    stringValue(values, "evidence"), stringValue(values, "login"),
                    stringValue(values, "startedAt"), complete);
            team.score = intValue(values, "score");
            team.errors = intValue(values, "errors");
            team.completedAt = nullableStringValue(values, "completedAt");

            Object savedPlayers = values.get("players");
            if (savedPlayers instanceof List) {
                for (Object savedPlayer : (List<?>) savedPlayers) {
                    if (savedPlayer instanceof Map) {
                        Map<?, ?> playerValues = (Map<?, ?>) savedPlayer;
                        Player player = new Player(stringValue(playerValues, "name"));
                        player.points = intValue(playerValues, "points");
                        player.errors = intValue(playerValues, "errors");
                        team.players.add(player);
                    }
                }
            }
            teams.put(team.name, team);
        }
        System.out.println("Resumed " + teams.size() + " saved team(s).");
    }

    private String stringValue(Map<?, ?> values, String key) throws IOException {
        Object value = values.get(key);
        if (!(value instanceof String)) {
            throw new IOException("Missing string field: " + key);
        }
        return (String) value;
    }

    private String nullableStringValue(Map<?, ?> values, String key) throws IOException {
        Object value = values.get(key);
        if (value == null) {
            return null;
        }
        if (!(value instanceof String)) {
            throw new IOException("Invalid string field: " + key);
        }
        return (String) value;
    }

    private int intValue(Map<?, ?> values, String key) throws IOException {
        Object value = values.get(key);
        if (!(value instanceof Number)) {
            throw new IOException("Missing numeric field: " + key);
        }
        return ((Number) value).intValue();
    }

    private boolean booleanValue(Map<?, ?> values, String key) throws IOException {
        Object value = values.get(key);
        if (!(value instanceof Boolean)) {
            throw new IOException("Missing boolean field: " + key);
        }
        return (Boolean) value;
    }

    private String jsonValue(String value) {
        return value == null ? "null" : "\"" + jsonEscape(value) + "\"";
    }

    private String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private static class Team {
        private final String name;
        private final GameState gameState;
        private final List<Player> players = new ArrayList<>();
        private final Instant startedAt;
        private int score;
        private int errors;
        private String completedAt;

        private Team(String name) {
            this.name = name;
            this.gameState = new GameState();
            this.startedAt = Instant.now();
        }

        private Team(String name, int iteration, String hash, String evidence, String login,
                String startedAt, boolean complete) {
            this.name = name;
            this.gameState = new GameState();
            this.gameState.applySnapshot(iteration, hash, evidence, login, complete);
            this.startedAt = Instant.parse(startedAt);
        }

        private Player findPlayer(String playerName) {
            for (Player player : players) {
                if (player.name.equals(playerName)) {
                    return player;
                }
            }
            return null;
        }

        private Player topPlayer() {
            Player result = null;
            for (Player player : players) {
                if (result == null || player.points > result.points) {
                    result = player;
                }
            }
            return result;
        }

        private long elapsedSeconds() {
            Instant end = completedAt == null ? Instant.now() : Instant.parse(completedAt);
            return Math.max(0, end.getEpochSecond() - startedAt.getEpochSecond());
        }
    }

    private static class Player {
        private final String name;
        private int points;
        private int errors;
        private int attempts;

        private Player(String name) {
            this.name = name;
        }
    }

    private static class PlayerSession {
        private final Team team;
        private final Player player;

        private PlayerSession(Team team, Player player) {
            this.team = team;
            this.player = player;
        }
    }

    private static class JsonReader {
        private final String input;
        private int index;

        private JsonReader(String input) {
            this.input = input;
        }

        private Object readValue() throws IOException {
            skipWhitespace();
            Object value = readValueInternal();
            skipWhitespace();
            if (index != input.length()) {
                throw error("Unexpected content after JSON value");
            }
            return value;
        }

        private Object readValueInternal() throws IOException {
            skipWhitespace();
            if (index >= input.length()) {
                throw error("Unexpected end of JSON");
            }
            char character = input.charAt(index);
            if (character == '{') {
                return readObject();
            }
            if (character == '[') {
                return readArray();
            }
            if (character == '"') {
                return readString();
            }
            if (input.startsWith("true", index)) {
                index += 4;
                return Boolean.TRUE;
            }
            if (input.startsWith("false", index)) {
                index += 5;
                return Boolean.FALSE;
            }
            if (input.startsWith("null", index)) {
                index += 4;
                return null;
            }
            return readNumber();
        }

        private Map<String, Object> readObject() throws IOException {
            Map<String, Object> object = new LinkedHashMap<>();
            expect('{');
            skipWhitespace();
            if (consume('}')) {
                return object;
            }
            while (true) {
                skipWhitespace();
                String key = readString();
                skipWhitespace();
                expect(':');
                object.put(key, readValueInternal());
                skipWhitespace();
                if (consume('}')) {
                    return object;
                }
                expect(',');
            }
        }

        private List<Object> readArray() throws IOException {
            List<Object> array = new ArrayList<>();
            expect('[');
            skipWhitespace();
            if (consume(']')) {
                return array;
            }
            while (true) {
                array.add(readValueInternal());
                skipWhitespace();
                if (consume(']')) {
                    return array;
                }
                expect(',');
            }
        }

        private String readString() throws IOException {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (index < input.length()) {
                char character = input.charAt(index++);
                if (character == '"') {
                    return value.toString();
                }
                if (character == '\\') {
                    if (index >= input.length()) {
                        throw error("Incomplete string escape");
                    }
                    char escape = input.charAt(index++);
                    switch (escape) {
                        case '"':
                            value.append('"');
                            break;
                        case '\\':
                            value.append('\\');
                            break;
                        case '/':
                            value.append('/');
                            break;
                        case 'b':
                            value.append('\b');
                            break;
                        case 'f':
                            value.append('\f');
                            break;
                        case 'n':
                            value.append('\n');
                            break;
                        case 'r':
                            value.append('\r');
                            break;
                        case 't':
                            value.append('\t');
                            break;
                        case 'u':
                            value.append(readUnicodeEscape());
                            break;
                        default:
                            throw error("Invalid string escape");
                    }
                }
                else {
                    value.append(character);
                }
            }
            throw error("Unterminated string");
        }

        private char readUnicodeEscape() throws IOException {
            if (index + 4 > input.length()) {
                throw error("Incomplete Unicode escape");
            }
            String hex = input.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            }
            catch (NumberFormatException exception) {
                throw error("Invalid Unicode escape");
            }
        }

        private Number readNumber() throws IOException {
            int start = index;
            while (index < input.length() && "-+0123456789.eE".indexOf(input.charAt(index)) >= 0) {
                index++;
            }
            if (start == index) {
                throw error("Invalid JSON value");
            }
            String number = input.substring(start, index);
            try {
                return number.contains(".") || number.contains("e") || number.contains("E")
                        ? Double.parseDouble(number) : Long.parseLong(number);
            }
            catch (NumberFormatException exception) {
                throw error("Invalid JSON number");
            }
        }

        private void skipWhitespace() {
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
        }

        private void expect(char expected) throws IOException {
            if (index >= input.length() || input.charAt(index) != expected) {
                throw error("Expected '" + expected + "'");
            }
            index++;
        }

        private boolean consume(char expected) {
            if (index < input.length() && input.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private IOException error(String message) {
            return new IOException(message + " at character " + index);
        }
    }
}
