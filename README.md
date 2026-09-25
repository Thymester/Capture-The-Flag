# Capture the Flag

A command-line Capture the Flag (CTF) application written in Java. The game introduces basic cybersecurity concepts through simulated system environments, security-log analysis, encoding and decoding, SHA-256 hashing, and challenge verification.

The application supports both local solo play and LAN-based team play. In team mode, multiple players connect to a server computer, select a team and player name, and work on a shared challenge progression while the server tracks team and player statistics.

## Features

### Solo play

* Command-line gameplay
* Randomly generated challenge secrets
* Twenty-one progressive system challenges
* Simulated operating-system environments
* SHA-256 hashing
* Plaintext artifact analysis
* Base64 decoding
* ROT13 decoding
* Hexadecimal decoding
* Reversed-text decoding
* Security-log analysis
* Hash verification
* Progressive difficulty
* Interactive simulated terminal
* File navigation and search commands
* Input validation
* Game state held in memory for the current session

### Team play

* Optional team mode; solo play does not require a server
* Host computer runs the team server
* Other computers connect over HTTP
* Server-generated invite code required to join
* Session authentication for team data and scoring endpoints
* Players choose a team name and player name
* Maximum of six players per team
* One shared `GameState` per team
* Server-authoritative hash verification
* Team score and error tracking
* Individual player points and error tracking
* Completion-time tracking
* Speed bonus when a team completes the challenge sequence
* Leaderboard showing team size, score, errors, elapsed time, and highest-scoring player
* Team data written to `team-scores.json`

## How the Game Works

Each challenge creates a random secret using a system-specific prefix and random bytes. The secret is encoded according to the current system profile, hashed with SHA-256, and placed in simulated evidence files.

The player investigates the simulated system and its logs to find the artifact. Depending on the challenge, the artifact may need to be decoded before it is hashed.

The general process is:

1. View the current system and difficulty.
2. Inspect the recovered logs.
3. Locate the artifact and determine its representation.
4. Decode the artifact when necessary.
5. Generate a SHA-256 hash of the recovered text.
6. Submit the 64-character hexadecimal hash.
7. Advance to the next system after a correct verification.

Some later systems require the built-in terminal instead of the numbered menu tools.

## Game Modes

When the program starts, it displays:

```text
1. Solo Play
2. Team Play
```

### Solo Play

Solo play creates a local `GameState` and runs without a network connection or server. The solo menu is:

```text
1. View System
2. View Logs
3. Verify Flag
4. Hash Text
5. Decode Text
6. Open Terminal
7. Exit
```

Solo progress is held in memory and is lost when the program exits.

### Team Play

Team play connects to a `TeamServer` running on the host computer. Each player enters:

* Server URL
* Invite code supplied by the host
* Team name
* Player name

Players on the same team share the team’s challenge progression. A correct submission advances the team’s shared `GameState`. The player who submits the correct hash receives individual points.

The team menu is:

```text
1. View System
2. View Logs
3. Verify Flag
4. Hash Text
5. Decode Text
6. Open Terminal
7. Leaderboard
8. Exit
```

Team mode displays the current number of completed challenges and the number of players connected to the team.

## Team Scoring

The server calculates team and player scores. Clients do not decide whether a submitted hash is correct or how many points are awarded.

For each correct verification:

```text
team points  = max(10, 100 - team errors * 5)
player points = max(10, 100 - player errors * 5)
```

When a team completes the final challenge, it receives an additional speed bonus:

```text
speed bonus = max(0, 1000 - elapsed seconds)
```

The server also records:

* Team name
* Number of players
* Team score
* Team errors
* Team start time
* Team completion time
* Player names
* Individual player points
* Individual player errors

The current leaderboard displays these values and identifies the highest-scoring player on each team. The current server does not automatically sort teams or declare a winner; the displayed score, errors, completion state, and elapsed time can be used to compare teams.

## Running Team Mode

### 1. Compile the project

Run this command from the project directory:

```bash
javac $(find . -name '*.java' -print)
```

On Windows PowerShell, an equivalent command is:

```powershell
javac (Get-ChildItem -Recurse -Filter *.java | ForEach-Object { $_.FullName })
```

### 2. Start the server

Run this on the computer hosting the match:

```bash
java team.TeamServer
```

The default port is `8080`. A different port can be supplied:

```bash
java team.TeamServer 9090
```

The server reports the location of the score file when it starts. Scores are written to:

```text
team-scores.json
```

The server also prints a cryptographically random invite code. The host must give
that code only to intended players. The code is required for every join request
and is regenerated each time the server process starts.

### 3. Connect players

Run this on every player computer:

```bash
java CaptureTheFlag
```

Select `2. Team Play`, then enter the server URL, invite code, team name, and
player name. On the same computer as the server, the default URL is:

```text
http://localhost:8080
```

For other computers on the same network, replace `localhost` with the host computer’s local IP address, for example:

```text
http://192.168.1.25:8080
```

The host computer’s firewall must allow incoming connections on the selected port. The server must remain running while team clients are connected.

### Connecting Across Modern Networks

The application cannot safely bypass NAT, firewalls, carrier-grade NAT, or
enterprise network policy. Those controls exist specifically to prevent unsolicited
connections. If direct LAN addressing is unreliable, use a private overlay network
such as Tailscale or ZeroTier. Install the same provider on the host and players,
join the same private network, start `TeamServer` on the host, and use the host’s
overlay IP address in the client URL, for example:

```text
http://100.x.y.z:8080
```

This avoids router port forwarding and limits reachability to the private overlay,
but the invite code is still required. Do not expose port `8080` to the public
Internet or configure unsolicited port forwarding for this game.

Team endpoints require the session token returned after a successful join. The
client sends it in the `X-CTF-Session` header rather than placing it in a URL.
The transport is still plain HTTP, so use the game only on a trusted LAN or private
overlay. HTTPS would be required for untrusted networks.

## Team Score File

The server writes a JSON file named `team-scores.json` in its current working directory. The file contains team and player score information, including errors and completion timestamps.

When the server starts and `team-scores.json` exists, it asks whether to resume the saved match or start a new match:

```text
1. Restart from the saved match
2. Start a new match
```

Resuming restores team scores, errors, players, player points, the current challenge, challenge evidence, completion state, and timing information. Starting a new match removes the active score file and creates fresh in-memory teams when players join.

If the saved file is invalid or uses an older format, the server starts a new match and renames the old file to `team-scores.json.invalid`.

## Challenge Progression

The game defines the following system profiles in `game/GameState.java`:

| # | System | Difficulty | Artifact representation | Terminal required |
|---:|---|---|---|---|
| 1 | Linux | Beginner | Plaintext | No |
| 2 | Windows | Beginner | Base64 | No |
| 3 | Mac | Beginner | ROT13 | No |
| 4 | Router | Easy | Hexadecimal | Yes |
| 5 | Ubuntu | Easy | Reversed text | Yes |
| 6 | Kali | Easy | Base64 | Yes |
| 7 | FreeBSD | Moderate | ROT13 | Yes |
| 8 | Android | Moderate | Hexadecimal | Yes |
| 9 | Solaris | Moderate | Reversed text | Yes |
| 10 | OpenBSD | Moderate | Base64 | Yes |
| 11 | Debian | Hard | Plaintext | Yes |
| 12 | Fedora | Hard | Base64 | Yes |
| 13 | Raspberry Pi | Hard | ROT13 | Yes |
| 14 | Docker Host | Hard | Hexadecimal | Yes |
| 15 | Azure VM | Very Hard | Reversed text | Yes |
| 16 | AWS Instance | Very Hard | Base64 | Yes |
| 17 | Kubernetes Node | Very Hard | Hexadecimal | Yes |
| 18 | SCADA Controller | Impossible | ROT13 | Yes |
| 19 | Satellite Link | Impossible | Reversed text | Yes |
| 20 | Zero-Day Lab | Impossible | Base64 | Yes |

The source currently contains 20 listed `SystemProfile` entries. The challenge progression is indexed from zero internally, so the final successful verification sets the game’s completion state after the final profile.

## Main Menu Tools

Solo mode provides these numbered tools:

```text
1. View System
2. View Logs
3. Verify Flag
4. Hash Text
5. Decode Text
6. Open Terminal
7. Exit
```

### View System

Displays:

* Current operating system or simulated system type
* Security difficulty
* Last login timestamp

### View Logs

Displays the recovered files and their contents for the current challenge. The evidence includes a security log, a lead file, an artifact file, and a temporary README file.

### Verify Flag

Accepts a submitted SHA-256 hash. The input must contain exactly 64 hexadecimal characters. A correct hash advances the current game state.

### Hash Text

Calculates and displays the SHA-256 hash of entered text.

### Decode Text

The menu decoder supports:

```text
1. Base64
2. ROT13
3. Hexadecimal
4. Reverse
```

## Interactive Terminal

The simulated terminal is available from the main menu and from team mode. It provides these commands:

```text
help
system
logs
pwd
ls [-a]
cd <directory>
cat <file>
grep <text>
find [directory]
hash <text>
decode <type> <text>
verify <SHA-256 hash>
status
clear
exit
```

Supported terminal decoder names are:

```text
base64
rot13
hex
reverse
```

Example:

```text
ctf@linux:~$ logs
ctf@linux:~$ cat /home/analyst/.cache/.session
ctf@linux:~$ hash linux-admin-example
ctf@linux:~$ status
```

For terminal-only challenges, the main menu informs the player that the built-in terminal is required.

## Project Structure

```text
Capture The Flag/
├── CaptureTheFlag.java
├── crypto/
│   └── EncodingService.java
├── evidence/
│   └── EvidenceFileSystem.java
├── game/
│   └── GameState.java
├── team/
│   ├── TeamClient.java
│   └── TeamServer.java
├── terminal/
│   └── Terminal.java
└── ui/
    ├── DecodeText.java
    ├── HashText.java
    ├── VerifyFlag.java
    ├── ViewLogs.java
    └── ViewSystem.java
```

### `CaptureTheFlag.java`

Contains the application entry point. It lets the user choose solo or team mode and runs the solo gameplay loop when solo play is selected.

### `crypto/EncodingService.java`

Provides SHA-256 hashing and encoding or decoding support for Base64, ROT13, hexadecimal, reversed text, and plaintext values.

### `game/GameState.java`

Maintains the current challenge, generated secret evidence, expected hash, challenge iteration, difficulty, system profile, and completion state. It also applies challenge snapshots received by team clients.

### `evidence/EvidenceFileSystem.java`

Creates the simulated files used by each challenge and provides directory navigation, file reading, file listing, recursive file finding, and text searching.

### `team/TeamServer.java`

Runs the host-side HTTP server. It creates teams, accepts players, maintains one shared game state per team, verifies submitted hashes, updates scores, serves leaderboard information, and writes `team-scores.json`.

### `team/TeamClient.java`

Connects team players to the server, applies the team’s current challenge snapshot, forwards verification requests, displays team status, and displays the leaderboard.

### `terminal/Terminal.java`

Implements the simulated terminal and routes terminal commands to the game, evidence filesystem, hashing, decoding, and verification functionality.

### `ui/ViewSystem.java`

Displays the current system type, difficulty, and last login information.

### `ui/ViewLogs.java`

Displays the recovered evidence files for the current challenge.

### `ui/HashText.java`

Calculates SHA-256 hashes for text entered by the player.

### `ui/DecodeText.java`

Provides interactive Base64, ROT13, hexadecimal, and reverse decoding.

### `ui/VerifyFlag.java`

Validates submitted hashes and advances a local game state after a correct verification. Team mode uses a server-backed verifier through this same interface.

## Requirements

* Java Development Kit (JDK) 11 or newer
* JDK 17 or newer recommended
* A local network connection for cross-computer team play

The project uses standard Java libraries only. No external dependencies or build tools are required.

Important standard-library APIs include:

* `java.util.Scanner`
* `java.security.MessageDigest`
* `java.security.SecureRandom`
* `java.util.Base64`
* `java.nio.charset.StandardCharsets`
* `java.net.HttpURLConnection`
* `com.sun.net.httpserver.HttpServer`
* `java.nio.file.Files`
* `java.time.Instant`

## Cybersecurity Concepts Demonstrated

* Cryptographic hashing
* SHA-256
* Encoded data
* Base64
* ROT13
* Hexadecimal representation
* Reversed-text obfuscation
* Security-log analysis
* Artifact discovery
* Hash verification
* Challenge-response workflows
* Command-line investigation
* Client/server communication
* Server-authoritative scoring

## Current Scope and Limitations

The application currently supports local solo play and LAN team play through a lightweight Java HTTP server.

Current team-mode limitations include:

* The server must remain running for the match.
* Team sessions are held in server memory.
* There is no authentication beyond the generated client session ID.
* There is no automatic team ranking or winner announcement.
* The server is intended for trusted local-network play rather than public internet deployment.
* The team server currently uses an in-memory challenge state for each team.

## Future Development

Possible future improvements include:

* Loading match and score state from `team-scores.json` on server startup
* Automatic leaderboard sorting and winner announcement
* Match codes and explicit match lifecycle management
* Authentication and stronger session management
* Reconnection support
* Persistent challenge state across server restarts
* HTTPS or a more secure network protocol
* A configurable scoring system
* A graphical or web-based client
* Automated tests and build configuration

## License

MIT License. Refer to the repo license file to learn more.
