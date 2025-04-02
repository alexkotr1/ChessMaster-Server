# ChessServer

A modern Java WebSocket chess server implementation with AI support. Live at [chessmaster.gr](https://chessmaster.gr)

## Overview

ChessServer is a full-featured chess server implemented in Java that allows players to compete against each other or against an AI opponent. The server handles all chess game logic, player matching, and communication via WebSockets for real-time gameplay.

## Features

- **Real-time gameplay** via WebSocket connections
- **Complete chess rule implementation** including:
    - Standard piece movement
    - Castling
    - Pawn promotion
    - Check and checkmate detection
    - Stalemate detection
    - 50-move draw rule
- **Game timing** with configurable time controls
- **AI opponent** using a remote chess engine API
- **Multiple simultaneous games**
- **In-game chat**

## Technical Architecture

### Core Components

- **WebSocket Server**: Built using the Tyrus WebSocket implementation
- **Game Engine**: Complete chess logic implementation
- **AI Integration**: Connects to a remote chess API service for AI moves
- **Messaging System**: JSON-based communication protocol using Jackson for serialization

### Class Structure

- `WebSocket`: Entry point and WebSocket endpoint implementation
- `Game`: Manages game state, players, and communication
- `ChessEngine`: Implements chess rules and game logic
- `ChessBoard`: Represents the board state and piece positions
- `Pioni`: Base class for chess pieces with specific implementations:
    - `Vasilias` (King)
    - `Vasilissa` (Queen)
    - `Pyrgos` (Rook)
    - `Alogo` (Knight)
    - `Stratigos` (Bishop)
    - `Stratiotis` (Pawn)
- `Timer`: Handles chess clock functionality
- `RemoteClient`: Connects to a remote chess engine API for AI moves

## Installation

### Prerequisites

- Java 21 or higher
- Maven

### Building

1. Clone the repository
   ```
   git clone https://github.com/yourusername/ChessServer.git
   cd ChessServer
   ```

2. Build with Maven
   ```
   mvn clean package
   ```

This will create a JAR file with all dependencies in the `target` directory.

### Running

Run the server with:

```
java -jar target/ChessServer-1.0-jar-with-dependencies.jar
```

The server will start on port 8080 by default.

## Configuration

Server configuration can be managed through:
- `log4j2.json` for logging settings
- WebSocket server configuration in `WebSocket.java`

## API Documentation

### WebSocket Communication

All communication uses a JSON message format:

```json
{
  "code": "REQUEST_CODE",
  "data": "JSON_DATA",
  "messageID": "UUID",
  "fen": "OPTIONAL_FEN_STRING"
}
```

### Request Codes

The server handles various request codes for game actions:

- Game Setup: `HOST_GAME`, `JOIN_GAME`, `START_AI_GAME`
- Game Play: `REQUEST_MOVE`, `REQUEST_UPGRADE`, `ENEMY_MOVE`
- Game State: `GET_PIONIA`, `GET_WHITE_TURN`, `IS_GAME_ENDED`
- Analysis: `REQUEST_FEN`, `REQUEST_BOARD_STATE`
- Communication: `CHAT_MESSAGE`

## Frontend Integration

Frontend clients can connect to the WebSocket server and authenticate through the connection handshake. Once connected, clients can send and receive JSON messages to control the game.

## Chess AI

The system integrates with a remote chess AI service through a WebSocket connection. The AI service analyzes board positions using FEN notation and returns optimal moves.

## Deployment

The service is currently deployed and available at [chessmaster.gr](https://chessmaster.gr).

## License

MIT License with Attribution

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following condition:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software, and appropriate attribution must be given to the original creators when using or adapting this work.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.