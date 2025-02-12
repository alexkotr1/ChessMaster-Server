package alexk.chess;


import alexk.chess.Pionia.Pioni;
import alexk.chess.Stockfish.Client;
import alexk.chess.Stockfish.LocalClient;
import alexk.chess.Stockfish.RemoteClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.websocket.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.rmi.Remote;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Game implements WebSocketMessageListener {
    public enum State { HOST_JOINED, BLACK_JOINED, GAME_STARTED, GAME_ENDED, PLAYER_EXITED}
    private static final Logger logger = LogManager.getLogger(Game.class);
    private static final ArrayList<Game> games = new ArrayList<>();
    private final UUID uuid;
    private final String code;
    private ChessEngine chessEngine;
    private final Session white;
    private final boolean[] playAgain = new boolean[2];
    private Session black;
    private final int timerMinutes;
    private Boolean vsAI;
    public State state;
    private RemoteClient client;
    public Game(String code, Session white, int timerMinutes, boolean vsAI) {
        this.timerMinutes = timerMinutes;
        chessEngine = new ChessEngine(white,null, timerMinutes, vsAI);
        uuid = UUID.randomUUID();
        this.code = code;
        this.white = white;
        this.vsAI = vsAI;
        games.add(this);
        state = State.HOST_JOINED;
        logger.info("New Game with ID: {} Code: {} vsAI: {}", uuid, code, vsAI);
        if (vsAI){
            client = new RemoteClient();
            client.startEngine();
        }
    }
    public UUID getUuid() {
        return uuid;
    }
    public String getCode() {
        return code;
    }
    public ChessEngine getChessEngine() {
        return chessEngine;
    }

    public Session getWhite() {
        return white;
    }
    public Session getBlack() {
        return black;
    }
    public void setBlack(Session black) { this.black = black; chessEngine.setBlackSession(black);  }
    public static Game join(Session black, String code){
        ArrayList<Game> res = games.stream().filter(game -> game.state != State.PLAYER_EXITED && game.getCode().equals(code)).collect(Collectors.toCollection(ArrayList::new));
        if (res.size() != 1) return null;
        Game game = res.getFirst();
        if (game.state != State.HOST_JOINED) return null;
        game.setBlack(black);
        return game;
    }
    public void start(){
        logger.info("Game with ID: {} started", uuid);
        chessEngine.playChess();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        Runnable task = chessEngine::notifyTimers;
        Runnable checkGameEnd = () -> {
            if (!chessEngine.chessBoard.getGameEnded()) return;
            logger.info("Game with ID: {} ended", uuid);
            state = State.GAME_ENDED;
            scheduler.shutdownNow();
        };
        scheduler.scheduleAtFixedRate(checkGameEnd, 0, 1, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(task, 0, 10, TimeUnit.SECONDS);
    }
    public static Game findGameBySession(Session session){
        logger.info("Searching game by session.");
        return games.stream().filter(game -> game.getWhite().equals(session) || (game.getBlack() != null && game.getBlack().equals(session))).findFirst().orElse(null);
    }


    @Override
    public void onMessageReceived(Message message, Session session) {
        logger.info("Message received. ID: {} requestCode: {} Data:{} in Game: {}", message.getMessageID(), message.getCode(), message.getData(),getCode());
        if (state == State.GAME_ENDED) {
            logger.info("State for Game Code: {} is ended", getCode());
            if (vsAI){
                this.state = State.PLAYER_EXITED;
                games.remove(this);
                chessEngine = null;
                Game game = new Game(generateCode(),white,timerMinutes,true);
                game.start();
                Message playAgain = new Message();
                playAgain.setCode(RequestCodes.PLAY_AGAIN_ACCEPTED);
                playAgain.send(white,null);
                return;
            }
            if (message.getCode() == RequestCodes.PLAY_AGAIN){
                playAgain[session.equals(white) ? 0 : 1] = true;
                if (playAgain[0] == playAgain[1]){
                    this.state = State.PLAYER_EXITED;
                    games.remove(this);
                    chessEngine = null;
                    Game game = new Game(generateCode(),white,timerMinutes,false);
                    game.setBlack(black);
                    game.start();
                    Message playAgain = new Message();
                    playAgain.setCode(RequestCodes.PLAY_AGAIN_ACCEPTED);
                    playAgain.send(white,null);
                    playAgain.send(black,null);
                }
            }
            return;
        }
        Message res = new Message();
        try {
            switch (message.getCode()) {
                case GET_PIONIA -> {
                    logger.info("Starting GET_PIONIA for Game: {}", getCode());
                    ArrayList<Pioni> pionia = chessEngine.chessBoard.getPionia();
                    res.setCode(RequestCodes.GET_PIONIA_RESULT);
                    res.setData(pionia);
                    res.send(session, message);
                }
                case REQUEST_FEN ->{
                    logger.info("Starting REQUEST_FEN for Game: {}", getCode());
                    res.setCode(RequestCodes.REQUEST_FEN_RESULT);
                    res.setData(chessEngine.toFen());
                    res.send(session, message);
                }
                case REQUEST_BOARD_STATE ->{
                    logger.info("Starting REQUEST_BOARD_STATE for Game: {}", getCode());
                    res.setCode(RequestCodes.REQUEST_FEN_RESULT);
                    res.setData(chessEngine.getBoardState());
                    res.send(session, message);
                }
                case GET_MOVES_REMAINING -> {
                    logger.info("Starting GET_MOVES_REMAINING for Game: {}", getCode());
                    res.setCode(RequestCodes.GET_MOVES_REMAINING_RESULT);
                    res.setData(chessEngine.chessBoard.getMovesRemaining());
                    res.send(session, message);
                }
                case GET_WHITE_TURN -> {
                    logger.info("Starting GET_WHITE_TURN for Game: {}", getCode());
                    res.setCode(RequestCodes.GET_WHITE_TURN_RESULT);
                    res.setData(chessEngine.chessBoard.getWhiteTurn());
                    res.send(session, message);
                }
                case GET_PIONI_AT_POS -> {
                    logger.info("Starting GET_PIONI_AT_POS for Game: {}", getCode());
                    int[] pos = Message.mapper.readValue(message.getData(), int[].class);
                    Pioni pioni = chessEngine.chessBoard.getPioniAt(Utilities.int2Char(pos[0]),pos[1]);
                    res.setCode(RequestCodes.GET_PIONI_AT_POS_RESULT);
                    res.setData(pioni);
                    res.send(session, message);
                }
                case IS_GAME_ENDED ->{
                    logger.info("Starting IS_GAME_ENDED for Game: {}", getCode());
                    res.setCode(RequestCodes.IS_GAME_ENDED_RESULT);
                    res.setData(chessEngine.chessBoard.getGameEnded() ? chessEngine.chessBoard.getWinner() : "false");
                    res.send(session, message);
                }
                case CHECKMATE -> {
                    logger.info("Starting CHECKMATE for Game: {}", getCode());
                    res.setCode(RequestCodes.CHECKMATE_RESULT);
                    boolean white = Boolean.parseBoolean(message.getData());
                    HashMap<Pioni,ArrayList<int[]>> legalMoves = chessEngine.kingCheckMate(white);
                    res.setData(Message.mapper.writeValueAsString(legalMoves));
                    res.send(session, message);
                }
                case STALEMATE_CHECK -> {
                    logger.info("Starting STALEMATE_CHECK for Game: {}", getCode());
                    res.setCode(RequestCodes.STALEMATE_CHECK_RESULT);
                    boolean white = Boolean.parseBoolean(res.getData());
                    res.setData(Message.mapper.writeValueAsString(chessEngine.stalemateCheck(white)));
                    res.send(session, message);
                }
                case REQUEST_CHESSBOARD -> {
                    logger.info("Starting REQUEST_CHESSBOARD for Game: {}", getCode());
                    res.setCode(RequestCodes.REQUEST_CHESSBOARD_RESULT);
                    res.setData(Message.mapper.writeValueAsString(chessEngine.chessBoard));
                    res.send(session, message);
                }
                case REQUEST_MOVE -> {
                    logger.info("Starting REQUEST_MOVE for Game: {}", getCode());
                    int[][] move = Message.mapper.readValue(message.getData(), int[][].class);
                    boolean currentTurn = chessEngine.chessBoard.getWhiteTurn();
                    ArrayList<Pioni> moved = null;
                    if ((currentTurn && session != white) || (currentTurn && session == black)) {
                        finishUpMove(message, session, res, moved);
                        return;
                    }
                        char origX = Utilities.int2Char(move[0][0]);
                        int origY = move[0][1];
                        char destX = Utilities.int2Char(move[1][0]);
                        int destY = move[1][1];
                        Pioni p = chessEngine.getBoard().getPioniAt(origX, origY);
                        if (!chessEngine.legalMove(origX, origY, destX, destY)) {
                            finishUpMove(message, session, res, moved);
                            return;
                        }
                        moved = chessEngine.nextMove(origX,origY,destX,destY);
                        Pioni upgradable = chessEngine.toBeUpgraded;
                        finishUpMove(message, session, res, moved);
                        if (upgradable == null) return;
                        CompletableFuture<String> future = requestUpgrade(p.getIsWhite());
                        future.thenAcceptAsync(str->{
                            logger.info("Upgrading Pioni: {} in Game: {}", str, getCode());
                            try {
                                if (chessEngine.upgradePioni(upgradable,future.get())){
                                    chessEngine.switchTurn();
                                    notifyEnemyMove(true,false);
                                    notifyEnemyMove(false,false);
                                    chessEngine.toBeUpgraded = null;
                                    chessEngine.checkGameEnd(session.equals(white));
                                    if (vsAI) AIMove();
                                }
                            } catch (InterruptedException | ExecutionException e) {
                                logger.error("Error trying to upgrade Pioni:", e);
                            }
                        });
                }
                case CHESSBOARD_STATE -> {
                    logger.info("Starting CHESSBOARD_STATE for Game: {}", getCode());
                    res.setCode(RequestCodes.CHESSBOARD_STATE_RESULT);
                    res.setData(chessEngine.chessBoard.getState());
                    res.send(session, message);
                }
                case CHAT_MESSAGE -> {
                    logger.info("Starting CHAT_MESSAGE for Game: {}", getCode());
                    res.setCode(RequestCodes.CHAT_MESSAGE_NOTIFICATION);
                    res.setData(message.getData());
                    res.send(session.equals(white) ? black : white,null);
                }
            }
        } catch (Exception e) {
           throw  new RuntimeException(e);
        }
    }

    private void finishUpMove(Message message, Session session, Message res, ArrayList<Pioni> moved){
        try {
            res.setData(Message.mapper.writeValueAsString(moved));
            res.setCode(RequestCodes.REQUEST_MOVE_RESULT);
            res.setFen(chessEngine.toFen());
            res.send(session, message);
            if (moved == null || moved.isEmpty()) return;
            chessEngine.checkGameEnd(session.equals(white));
            if (session.equals(white) != chessEngine.chessBoard.getWhiteTurn() && !vsAI) notifyEnemyMove(session.equals(white),true);
            else if (vsAI && chessEngine.toBeUpgraded == null) AIMove();
        } catch (JsonProcessingException e){
            logger.error("Error trying to finish up move:", e);
        }
    }

    public void AIMove(){
        Thread thread = new Thread(() -> {
            boolean currentTurn = chessEngine.chessBoard.getWhiteTurn();
            String bestMove = client.getBestMove(chessEngine.toFen(),chessEngine.getWhiteRemaining(), chessEngine.getBlackRemaining());
            if (bestMove == null) {
                logger.error("Error trying to get best move");
                return;
            }
            bestMove = bestMove.toUpperCase();
            System.out.println(bestMove);
            char xOrig = bestMove.charAt(0);
            int yOrig = Character.getNumericValue(bestMove.charAt(1));
            char xDest = bestMove.charAt(2);
            int yDest = Character.getNumericValue(bestMove.charAt(3));

            chessEngine.nextMove(xOrig,yOrig,xDest,yDest);
            if (bestMove.length() > 4) {
                Map<Character, String> promotionMap = Map.of(
                        'Q', "Vasilissa",
                        'B', "Stratigos",
                        'N', "Alogo",
                        'R', "Pyrgos"
                );
                char upgrade = bestMove.charAt(4);
                String promotionType = promotionMap.get(upgrade);
                if (promotionType != null) {
                    Pioni p = chessEngine.getBoard().getPioniAt(xDest, yDest);
                    chessEngine.upgradePioni(p, promotionType);
                } else logger.error("Invalid promotion piece: {}", upgrade);
            }
            chessEngine.checkGameEnd(currentTurn);
            Message notifyPlayer = new Message();
            notifyPlayer.setCode(RequestCodes.ENEMY_MOVE);
            notifyPlayer.setData(true);
            notifyPlayer.send(white,null);

        });
        thread.start();

    }
    public CompletableFuture<String> requestUpgrade(boolean white){
        CompletableFuture<String> future = new CompletableFuture<>();
        Message message = new Message();
        message.setCode(RequestCodes.REQUEST_UPGRADE);
        message.setData(white);
        message.send(white ? this.white : this.black,null);
        message.onReply(res-> future.complete(res.getData()));
        return future;
    }
    public void notifyEnemyMove(boolean isWhite, boolean shouldSwitch){
        Message notifyPlayer = new Message();
        notifyPlayer.setCode(RequestCodes.ENEMY_MOVE);
        notifyPlayer.setData(shouldSwitch);
        notifyPlayer.setFen(chessEngine.toFen());
        notifyPlayer.send(isWhite ? black : white,null);
    }
    public void setVsAI(boolean vsAI){ this.vsAI = vsAI; }
    public boolean getVsAI(){ return vsAI; }
    public static String generateCode() {
        StringBuilder word = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            char randomChar = (char) ('A' + random.nextInt(26));
            word.append(randomChar);
        }
        return word.toString();
    }
    @Override
    public void onCloseReceived(Session session) {
        if (session == white || session == black) {
            System.out.println("Closing session");
            state = State.PLAYER_EXITED;
            games.remove(this);
            if (vsAI) client.stopEngine();
        }
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Game game = (Game) o;
        return Objects.equals(uuid,game.getUuid());
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
