package alexk.chess;

import alexk.chess.Pionia.*;
import jakarta.websocket.Session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ChessEngine {
    public enum Winner { White, Black, Draw }
    protected ChessBoard chessBoard = new ChessBoard();
    ArrayList<int[]> allPositions = new ArrayList<>();
    private final Session white;
    private Session black;
    private Timer whiteTimer;
    private Timer blackTimer;
    private final int timerMinutes;
    private final boolean vsAI;
    public Pioni toBeUpgraded;
    private CompletableFuture<String> upgrade;
    public ChessEngine(Session white, Session black, int timerMinutes, boolean vsAI) {
        this.white = white;
        this.black = black;
        this.vsAI = vsAI;
        this.timerMinutes = timerMinutes;
        for (int x = 1;x<=8;x++) {
            for (int y = 1; y <= 8; y++) {
                allPositions.add(new int[]{x, y});
            }
        }
    }
    public void playChess(){
        chessBoard.loadBoard();
        chessBoard.printBoard();
        whiteTimer = new Timer(1000, (long) timerMinutes * 60 * 1000) {
            @Override
            protected void onFinish() {
                chessBoard.setGameEnded(true,Winner.Black);
                Message msg = new Message();
                msg.setCode(RequestCodes.ENEMY_MOVE);
                msg.send(white, null);
                if (!vsAI) msg.send(black, null);
            }
        };
        blackTimer = new Timer(1000, (long) timerMinutes * 60 * 1000) {
            @Override
            protected void onFinish() {
                chessBoard.setGameEnded(true,Winner.White);
                Message msg = new Message();
                msg.setCode(RequestCodes.ENEMY_MOVE);
                msg.send(white, null);
                if (!vsAI) msg.send(black, null);
            }
        };
        whiteTimer.start();
        blackTimer.start();
        blackTimer.pause();
    }
    public long getWhiteRemaining(){
        return whiteTimer.getRemainingTime();
    }
    public long getBlackRemaining(){
        return blackTimer.getRemainingTime();
    }
    public boolean legalMove(char xOrig, int yOrig, char xDest, int yDest){
        Pioni p = chessBoard.getPioniAt(xOrig,yOrig);
        HashMap<Pioni, ArrayList<int[]>> legalMovesWhenKingThreatened = kingCheckMate(p.getIsWhite());
        if (legalMovesWhenKingThreatened != null && !legalMovesWhenKingThreatened.isEmpty()) {
            ArrayList<int[]> desiredMoves = legalMovesWhenKingThreatened.get(p);
            if (desiredMoves != null && desiredMoves.stream().noneMatch(arr -> arr[0] == Utilities.char2Int(xDest) && arr[1] == yDest)) {
                return false;
            } else legalMovesWhenKingThreatened.clear();
        }

        return !checkDumbMove(p, new int[]{Utilities.char2Int(xDest), yDest});
    }
    public ArrayList<Pioni> nextMove(char xOrig, int yOrig, char xDest, int yDest){
        Pioni p = chessBoard.getPioniAt(xOrig,yOrig);
        if (p == null || p.getIsWhite() != chessBoard.getWhiteTurn() || !p.isLegalMove(xDest,yDest) || getBoard().getGameEnded()) return null;
        ArrayList<Pioni> moved = new ArrayList<>();
        moved.add(p);
        Pioni pioniAtDest = chessBoard.getPioniAt(xDest,yDest);

        if (p.getType().equals("Vasilias") && pioniAtDest != null && pioniAtDest.getType().equals("Pyrgos") && p.getIsWhite() == pioniAtDest.getIsWhite()){
            moved.add(pioniAtDest);
            int[] dest = pioniAtDest.getPosition();
            int[] orig = p.getPosition();
            switchTurn();
            chessBoard.move(xOrig,yOrig,Utilities.int2Char(dest[0] > orig[0] ? orig[0] + 2 : orig[0] - 2),yOrig);
            chessBoard.move(Utilities.int2Char(dest[0]),dest[1],Utilities.int2Char(dest[0] > orig[0] ? orig[0] - 1 : orig[0] + 1),yOrig);
            getBoard().totalMoves++;
            return moved;
        }

        chessBoard.move(xOrig,yOrig,xDest,yDest);
        if (!vsAI || p.getIsWhite()) toBeUpgraded = upgradeTime(p.getIsWhite());
        if (pioniAtDest != null && p.getIsWhite() != pioniAtDest.getIsWhite()) {
            chessBoard.capture(pioniAtDest);
            chessBoard.setMovesRemaining(100);
            moved.add(pioniAtDest);
        }
        if (toBeUpgraded == null) switchTurn();
        getBoard().totalMoves++;
        return moved;
    }
    public void notifyKingCheck(boolean isWhite, boolean isChecked){
        Message message = new Message();
        message.setCode(isWhite ? RequestCodes.KING_CHECK_WHITE : RequestCodes.KING_CHECK_BLACK);
        message.setData(isChecked);
        message.send(white,null);
        if (!vsAI) message.send(black,null);
    }
    public void notifyTimers(){
        Message whiteNotification = new Message();
        whiteNotification.setCode(RequestCodes.TIMER);
        whiteNotification.setData(new long[]{whiteTimer.getRemainingTime(), blackTimer.getRemainingTime()});
        whiteNotification.send(white,null);
        if (!vsAI) whiteNotification.send(black, null);
    }
    public Pioni upgradeTime(boolean white){
        return chessBoard.getPionia().stream().filter(p -> p.getType().equals("Stratiotis") && p.getIsWhite() == white && p.getYPos() == (p.getIsWhite() ? 8 : 1)).findFirst().orElse(null);
    }
    public void switchTurn(){
        chessBoard.setWhiteTurn(!chessBoard.getWhiteTurn());
        toggleTimer();
    }
    private void toggleTimer(){
        if (chessBoard.getWhiteTurn()){
            whiteTimer.resume();
            blackTimer.pause();
            return;
        }
        whiteTimer.pause();
        blackTimer.resume();
    }
    public void checkGameEnd(boolean white){
        if (checkKingMat(chessBoard, !white)) {
            notifyKingCheck(!white,true);
            HashMap<Pioni, ArrayList<int[]>> legalMovesWhenEnemyKingThreatened = kingCheckMate(!white);
            if (legalMovesWhenEnemyKingThreatened == null || legalMovesWhenEnemyKingThreatened.isEmpty()) getBoard().setGameEnded(true,white ? Winner.White : Winner.Black);
        }
        else if (stalemateCheck(!white) || chessBoard.getMovesRemaining() == 0) {
            notifyKingCheck(!white,false);
            getBoard().setGameEnded(true, Winner.Draw);
        }
        else notifyKingCheck(!white,false);
        notifyKingCheck(white, checkKingMat(chessBoard, white));
    }
    public boolean upgradePioni(Pioni p, String type){
        if (p.getType().equals("Stratiotis") && ((p.getIsWhite() && p.getYPos() == 8) || (!p.getIsWhite() && p.getYPos() == 1))) {
            Pioni upgradedPioni;
            switch (type) {
                case "Alogo":
                    upgradedPioni = new Alogo(p.getIsWhite(), chessBoard, p.getXPos(), p.getYPos(),null,false,null,null);
                    break;
                case "Pyrgos":
                    upgradedPioni = new Pyrgos(p.getIsWhite(), chessBoard, p.getXPos(), p.getYPos(),null,false,true,false);
                    break;
                case "Stratigos":
                    upgradedPioni = new Stratigos(p.getIsWhite(), chessBoard, p.getXPos(), p.getYPos(),null,false,null,null);
                    break;
                case "Vasilissa":
                    upgradedPioni = new Vasilissa(p.getIsWhite(), chessBoard, p.getXPos(), p.getYPos(),null,false,null,null);
                    break;
                default:
                    System.err.println("Something went wrong!");
                    return false;
            }
            chessBoard.getPionia().add(upgradedPioni);
            chessBoard.getPionia().remove(p);
            return true;
        }
        return false;
    }

    public void setBlackSession(Session black){ this.black = black; }
    public static boolean checkKingMat(ChessBoard chessBoard, boolean white){
        Pioni allyKing = chessBoard.getPionia()
                .stream()
                .filter(p -> p.getIsWhite() == white && p.getType().equals("Vasilias"))
                .findFirst()
                .orElse(null);
        assert allyKing != null;
        for (Pioni p : chessBoard.getPionia().stream().filter(pioni -> !pioni.getCaptured()).collect(Collectors.toCollection(ArrayList::new))) {
            if (p.getIsWhite() != white && p.isLegalMove(allyKing.getXPos(), allyKing.getYPos())) return true;
        }
        return false;
    }
    public HashMap<Pioni,ArrayList<int[]>> kingCheckMate(boolean white) {
        HashMap<Pioni,ArrayList<int[]>> legalMovesWhenKingThreatened = new HashMap<>();
        ArrayList<Pioni> duplicatePieces = chessBoard.getPionia().stream().filter(pioni -> pioni.getIsWhite() == white && !pioni.getCaptured()).collect(Collectors.toCollection(ArrayList::new));
        for (Pioni p : duplicatePieces) {
            for (int[] pos : allPositions) {
                ChessBoard testChessBoard = chessBoard.clone();
                Pioni duplicatePioni = testChessBoard.getPioniAt(p.getXPos(), p.getYPos());
                if (!duplicatePioni.isLegalMove(Utilities.int2Char(pos[0]), pos[1])) continue;
                testChessBoard.move(p.getXPos(), p.getYPos(), Utilities.int2Char(pos[0]), pos[1]);
                if (!checkKingMat(testChessBoard, white)) {
                    Pioni origPioni = chessBoard.getPioniAt(p.getXPos(), p.getYPos());
                    ArrayList<int[]> existingRoutes = legalMovesWhenKingThreatened.get(origPioni);
                    if (existingRoutes == null) existingRoutes = new ArrayList<>();
                    existingRoutes.add(new int[]{ pos[0],pos[1] });
                    legalMovesWhenKingThreatened.put(origPioni, existingRoutes);
                }
            }
        }
        return legalMovesWhenKingThreatened;
    }
    public boolean stalemateCheck(boolean white) {
        ArrayList<Pioni> duplicatePieces = chessBoard.getPionia().stream().filter(pioni -> pioni.getIsWhite() == white && !pioni.getCaptured()).collect(Collectors.toCollection(ArrayList::new));
        for (Pioni p : duplicatePieces) {
            for (int[] pos : allPositions) {
                if (p.isLegalMove(Utilities.int2Char(pos[0]), pos[1]) && !checkDumbMove(p,pos)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean checkDumbMove(Pioni p, int[] dest){
        ChessBoard testChessBoard = chessBoard.clone();
        testChessBoard.move(p.getXPos(), p.getYPos(), Utilities.int2Char(dest[0]), dest[1]);
        return ChessEngine.checkKingMat(testChessBoard,p.getIsWhite());
    }


    public int getMinutesAllowed(){ return timerMinutes; }
    public ChessBoard getBoard() { return chessBoard; }
    public String toFen(){
        StringBuilder fen = new StringBuilder();
        for (int y = 8;y>=1;y--){
            int emptyCounter = 0;
            for (int x = 1;x<=8;x++){
                Pioni pioni = getBoard().getPioniAt(Utilities.int2Char(x),y);
                if (pioni != null) {
                    if (emptyCounter != 0) fen.append(emptyCounter);
                    fen.append(pioni.print());
                    emptyCounter = 0;
                }
                else emptyCounter++;
                if (x == 8) fen.append(emptyCounter == 0 ? "" : emptyCounter).append(y != 1 ? "/" : "");
            }
        }
        fen.append(" ").append(getBoard().getWhiteTurn() ? "w" : "b").append(" ");
        boolean whiteKingSideRights = getBoard().castlingRights(true,true);
        boolean whiteQueenSideRights = getBoard().castlingRights(true,false);
        boolean blackKingSideRights = getBoard().castlingRights(false,true);
        boolean blackQueenSideRights = getBoard().castlingRights(false,false);

        if (whiteKingSideRights) fen.append("K");
        if (whiteQueenSideRights) fen.append("Q");
        if (blackKingSideRights) fen.append("k");
        if (blackQueenSideRights) fen.append("q");
        if (!whiteKingSideRights && !whiteQueenSideRights && !blackKingSideRights && !blackQueenSideRights) fen.append("-");

        fen
                .append(" ")
                .append("-")
                .append(" ")
                .append(getBoard().getMovesRemaining())
                .append(" ")
                .append(getBoard().totalMoves);

        return fen.toString();
    }

}
