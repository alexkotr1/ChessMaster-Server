package alexk.chess;


import alexk.chess.Pionia.Pioni;
import jakarta.websocket.Session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Game implements WebSocketMessageListener {
    public enum State { HOST_JOINED, BLACK_JOINED, GAME_STARTED, GAME_ENDED, PLAYER_EXITED}

    private static final ArrayList<Game> games = new ArrayList<>();
    private final UUID uuid;
    private final String code;
    private ChessEngine chessEngine;
    private final Session white;
    private final boolean[] playAgain = new boolean[2];
    private Session black;
    private int timerMinutes;
    public State state;
    public Game(String code, Session white, int timerMinutes) {
        this.timerMinutes = timerMinutes;
        chessEngine = new ChessEngine(white,null, timerMinutes);
        uuid = UUID.randomUUID();
        this.code = code;
        this.white = white;
        games.add(this);
        state = State.HOST_JOINED;


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
        chessEngine.playChess();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        Runnable task = chessEngine::notifyTimers;
        Runnable checkTimeOut = ()->{
            if (state == State.PLAYER_EXITED) {
                scheduler.shutdown();
                return;
            }
            chessEngine.checkTimeOut();
            if (chessEngine.chessBoard.getGameEnded()) {
                Message msg = new Message();
                msg.setCode(RequestCodes.ENEMY_MOVE);
                msg.send(white, null);
                msg.send(black, null);
                state = State.GAME_ENDED;
            }
        };
        scheduler.scheduleAtFixedRate(task, 0, 10, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(checkTimeOut, 0, 1, TimeUnit.SECONDS);
    }
    public static Game findGameBySession(Session session){
        return games.stream().filter(game -> game.getWhite().equals(session) || (game.getBlack() != null && game.getBlack().equals(session))).findFirst().orElse(null);
    }


    @Override
    public void onMessageReceived(Message message, Session session) {
        if (this.state == State.PLAYER_EXITED) return;
        System.out.println("Received Message on State " + state);
        Message res = new Message();
        try {
            switch (message.getCode()) {
                case GET_PIONIA -> {
                    ArrayList<Pioni> pionia = chessEngine.chessBoard.getPionia();
                    res.setCode(RequestCodes.GET_PIONIA_RESULT);
                    res.setData(pionia);
                    res.send(session, message);
                }
                case GET_MOVES_REMAINING -> {
                    res.setCode(RequestCodes.GET_MOVES_REMAINING_RESULT);
                    res.setData(chessEngine.chessBoard.getMovesRemaining());
                    res.send(session, message);
                }
                case GET_WHITE_TURN -> {
                    res.setCode(RequestCodes.GET_WHITE_TURN_RESULT);
                    res.setData(chessEngine.chessBoard.getWhiteTurn());
                    res.send(session, message);
                }
                case GET_PIONI_AT_POS -> {
                    int[] pos = Message.mapper.readValue(message.getData(), int[].class);
                    Pioni pioni = chessEngine.chessBoard.getPioniAt(Utilities.int2Char(pos[0]),pos[1]);
                    res.setCode(RequestCodes.GET_PIONI_AT_POS_RESULT);
                    res.setData(pioni);
                    res.send(session, message);
                }
                case IS_GAME_ENDED ->{
                    res.setCode(RequestCodes.IS_GAME_ENDED_RESULT);
                    res.setData(chessEngine.chessBoard.getGameEnded());
                    res.send(session, message);
                }
                case CHECKMATE -> {
                    res.setCode(RequestCodes.CHECKMATE_RESULT);
                    boolean white = Boolean.parseBoolean(message.getData());
                    HashMap<Pioni,ArrayList<int[]>> legalMoves = chessEngine.kingCheckMate(white);
                    res.setData(Message.mapper.writeValueAsString(legalMoves));
                    res.send(session, message);
                }
                case STALEMATE_CHECK -> {
                    res.setCode(RequestCodes.STALEMATE_CHECK_RESULT);
                    boolean white = Boolean.parseBoolean(res.getData());
                    res.setData(Message.mapper.writeValueAsString(chessEngine.stalemateCheck(white)));
                    res.send(session, message);
                }
                case REQUEST_CHESSBOARD -> {
                    res.setCode(RequestCodes.REQUEST_CHESSBOARD_RESULT);
                    res.setData(Message.mapper.writeValueAsString(chessEngine.chessBoard));
                    res.send(session, message);
                }
                case REQUEST_MOVE -> {
                    int[][] move = Message.mapper.readValue(message.getData(), int[][].class);
                    boolean currentTurn = chessEngine.chessBoard.getWhiteTurn();
                    ArrayList<Pioni> moved = null;
                    if ((currentTurn && session == white) || (!currentTurn && session == black)) {
                        moved = chessEngine.nextMove(Utilities.int2Char(move[0][0]),move[0][1],Utilities.int2Char(move[1][0]),move[1][1]);
                    }
                    res.setData(Message.mapper.writeValueAsString(moved));
                    res.setCode(RequestCodes.REQUEST_MOVE_RESULT);
                    chessEngine.chessBoard.printBoard();
                    res.send(session, message);
                    if (moved == null || moved.isEmpty()) return;
                    for (Pioni p : moved) {
                        if (ChessEngine.checkKingMat(chessEngine.chessBoard, !p.getIsWhite())) {
                            HashMap<Pioni, ArrayList<int[]>> legalMovesWhenEnemyKingThreatened = chessEngine.kingCheckMate(!p.getIsWhite());
                            if (legalMovesWhenEnemyKingThreatened == null || legalMovesWhenEnemyKingThreatened.isEmpty()) chessEngine.getBoard().setGameEnded(true,p.getIsWhite() ? ChessEngine.Winner.White : ChessEngine.Winner.Black);
                        }
                    }
                    chessEngine.checkGameEnd(currentTurn);
                    if (currentTurn != chessEngine.chessBoard.getWhiteTurn()){
                        Message notifyPlayer = new Message();
                        notifyPlayer.setCode(RequestCodes.ENEMY_MOVE);
                        notifyPlayer.setData(true);
                        notifyPlayer.send(session.equals(white) ? black : white,null);
                    }
                }
                case CHESSBOARD_STATE -> {
                    res.setCode(RequestCodes.CHESSBOARD_STATE_RESULT);
                    res.setData(chessEngine.chessBoard.getState());
                    res.send(session, message);
                }
                case REQUEST_UPGRADE -> {
                    res.setCode(RequestCodes.REQUEST_UPGRADE_RESULT);
                    int[] selection = Message.mapper.readValue(message.getData(), int[].class);
                    Pioni p = chessEngine.chessBoard.getPioniAt(Utilities.int2Char(selection[0]),selection[1]);
                    String upgradeTo = "";
                    if (selection[2] == 0) upgradeTo = "Alogo";
                    else if (selection[2] == 1) upgradeTo = "Pyrgos";
                    else if (selection[2] == 2) upgradeTo = "Stratigos";
                    else if (selection[2] == 3) upgradeTo = "Vasilissa";
                    boolean result = chessEngine.upgradePioni(p,upgradeTo);
                    res.setData(result);
                    res.send(session, message);
                    if (!result) return;
                    chessEngine.checkGameEnd(p.getIsWhite());
                    Message notifyPlayer = new Message();
                    notifyPlayer.setCode(RequestCodes.ENEMY_MOVE);
                    notifyPlayer.setData(false);
                    notifyPlayer.send(session.equals(white) ? black : white,null);
                }
                case PLAY_AGAIN -> {
                    playAgain[session.equals(white) ? 0 : 1] = true;
                    if (playAgain[0] == playAgain[1]){
                        this.state = State.PLAYER_EXITED;
                        games.remove(this);
                        chessEngine = null;
                        Game game = new Game(null,white,timerMinutes);
                        game.setBlack(black);
                        game.start();
                        Message playAgain = new Message();
                        playAgain.setCode(RequestCodes.PLAY_AGAIN_ACCEPTED);
                        playAgain.send(white,null);
                        playAgain.send(black,null);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void onCloseReceived(Session session) {
        if (session == white || session == black) {
            state = State.PLAYER_EXITED;
            games.remove(this);
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
