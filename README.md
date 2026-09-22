# Capture the Flag

A command-line Capture the Flag (CTF) application written in Java that introduces basic cybersecurity concepts through hashing, decoding, log analysis, and challenge progression.

The program presents the player with simulated systems and security logs containing artifacts that must be analyzed, decoded, and hashed in order to verify each flag and advance to the next challenge.

## Features

* Command-line based gameplay
* Multiple system environments

  * Linux
  * Windows
  * macOS
* Randomly generated challenge secrets
* SHA-256 hashing
* Base64 decoding
* ROT13 decoding
* Security log analysis
* Hash verification
* Progressive challenge difficulty
* Interactive simulated terminal
* Input validation
* Persistent game state during a session

## How It Works

Each challenge generates a unique secret value.

The player investigates the simulated system and security logs to locate evidence related to that secret.

Depending on the current challenge, the player may need to:

1. Locate a plaintext artifact
2. Decode a Base64 value
3. Decode a ROT13 value
4. Hash the recovered text using SHA-256
5. Submit the resulting hash for verification

If the submitted SHA-256 hash matches the expected hash, the player advances to the next system.

## Challenge Progression

### Challenge 1 — Linux

The Linux security logs contain a plaintext token.

The player must:

1. Locate the token in the logs
2. Hash the token using SHA-256
3. Submit the resulting hash

### Challenge 2 — Windows

The Windows logs contain a Base64 encoded artifact.

The player must:

1. Locate the Base64 value
2. Decode it
3. Hash the decoded value using SHA-256
4. Submit the resulting hash

### Challenge 3 — macOS

The macOS logs contain a ROT13-obfuscated recovery value.

The player must:

1. Locate the encoded value
2. Decode the ROT13 text
3. Hash the decoded value using SHA-256
4. Submit the resulting hash

## Main Menu

The application provides the following options:

```text
1. View System
2. View Logs
3. Verify Flag
4. Hash Text
5. Decode Text
6. Open Terminal
7. Exit
```

## Interactive Terminal

The program also contains a simulated terminal that allows the player to interact with the challenge using commands.

Available commands include:

```text
help
system
logs
hash <text>
decode base64 <text>
decode rot13 <text>
status
clear
exit
```

Example:

```text
ctf@linux:~$ logs
ctf@linux:~$ hash linux-admin-example
ctf@linux:~$ status
```

## Project Structure

```text
CaptureTheFlag.java
Methods/
├── DecodeText.java
├── GameState.java
├── HashText.java
├── Terminal.java
├── VerifyFlag.java
├── ViewLogs.java
└── ViewSystem.java
```

### `CaptureTheFlag.java`

Contains the main method and primary gameplay loop.

It handles menu input and routes the player to the appropriate section of the program.

### `GameState.java`

Maintains the current state of the game, including:

* Current system
* Current challenge iteration
* Flag verification progress
* Expected SHA-256 hash
* Generated log evidence
* Last login information
* Challenge difficulty

It also generates randomized challenge secrets and creates the expected SHA-256 hashes.

### `ViewSystem.java`

Displays information about the current simulated system.

### `ViewLogs.java`

Displays the security evidence associated with the current challenge.

### `HashText.java`

Allows the player to generate a SHA-256 hash from entered text.

### `DecodeText.java`

Provides decoding utilities for:

* Base64
* ROT13

### `VerifyFlag.java`

Accepts a SHA-256 hash submitted by the player and compares it against the expected challenge hash.

Correct submissions advance the game.

### `Terminal.java`

Provides a simulated command-line terminal where the player can interact with the CTF using commands instead of the main numbered menu.

## Requirements

* Java
* JDK 17 or newer recommended

No external libraries are required.

The project uses standard Java libraries including:

* `java.util.Scanner`
* `java.security.MessageDigest`
* `java.security.SecureRandom`
* `java.util.Base64`
* `java.nio.charset.StandardCharsets`

## Running the Program

Compile the project from the project directory:

```bash
javac CaptureTheFlag.java Methods/*.java
```

Run the program:

```bash
java CaptureTheFlag
```

## Cybersecurity Concepts Demonstrated

This project introduces several basic cybersecurity concepts:

* Cryptographic hashing
* SHA-256
* Encoded data
* Base64
* Simple substitution ciphers
* ROT13
* Security log analysis
* Artifact discovery
* Hash verification
* Challenge-response workflows
* Command-line investigation

## Current Scope

The current version focuses on the gameplay and challenge-verification portion of a Capture the Flag system.

It currently supports a single active player/session with randomly generated challenge data.

Future development may expand the project to support competition-management functionality such as:

* Multiple teams
* Unique challenge environments per team
* Team-specific keys
* Score tracking
* Persistent scoreboards
* Automated environment setup
* External setup scripts
* Challenge completion tracking

## Purpose

This project was created as a Java-based exploration of Capture the Flag systems and introductory cybersecurity concepts.

The goal is to combine software engineering concepts such as methods, classes, state management, input handling, and command parsing with cybersecurity concepts such as hashing, encoding, decoding, and challenge verification.

## License

This project is licensed under the MIT license.
