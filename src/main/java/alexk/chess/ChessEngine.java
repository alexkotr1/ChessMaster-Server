package alexk.chess;

import alexk.chess.Pionia.*;
import jakarta.websocket.Session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.stream.Collectors;

public class ChessEngine {
    public enum Winner { White, Black, Draw }
    protected ChessBoard chessBoard = new ChessBoard();
    ArrayList<int[]> allPositions = new ArrayList<>();
    private final Session white;
    private Session black;
    private long whiteTimeRemaining;
    private long blackTimeRemaining;
    private boolean whiteTimerRunning = false;
    private long timerStart;
    public ChessEngine(Session white, Session black, int timerMinutes) {
        this.white = white;
        this.black = black;
        blackTimeRemaining = timerMinutes * 60000L;
        whiteTimeRemaining = timerMinutes * 60000L;
        for (int x = 1;x<=8;x++) {
            for (int y = 1; y <= 8; y++) {
                allPositions.add(new int[]{x, y});
            }
        }
    }
    public void playChess(){
        chessBoard.loadBoard();
        chessBoard.printBoard();
        startWhiteTimer();
    }
    public ArrayList<Pioni> nextMove(char xOrig, int yOrig, char xDest, int yDest){
        Pioni p = chessBoard.getPioniAt(xOrig,yOrig);
        if (p == null || p.getIsWhite() != chessBoard.getWhiteTurn() || !p.isLegalMove(xDest,yDest) || getBoard().getGameEnded()) return null;
        ArrayList<Pioni> moved = new ArrayList<>();
        moved.add(p);
        Pioni pioniAtDest = chessBoard.getPioniAt(xDest,yDest);
        //1
        HashMap<Pioni, ArrayList<int[]>> legalMovesWhenKingThreatened = kingCheckMate(p.getIsWhite());
        if (legalMovesWhenKingThreatened != null && !legalMovesWhenKingThreatened.isEmpty()) {
            ArrayList<int[]> desiredMoves = legalMovesWhenKingThreatened.get(p);
            if (desiredMoves != null && desiredMoves.stream().noneMatch(arr -> arr[0] == Utilities.char2Int(xDest) && arr[1] == yDest)) {
                return null;
            } else legalMovesWhenKingThreatened.clear();
        }
        //2
        if (checkDumbMove(p, new int[]{Utilities.char2Int(xDest), yDest})) return null;

        if (p.getType().equals("Vasilias") && pioniAtDest != null && pioniAtDest.getType().equals("Pyrgos") && p.getIsWhite() == pioniAtDest.getIsWhite()){
            moved.add(pioniAtDest);
            int[] dest = pioniAtDest.getPosition();
            int[] orig = p.getPosition();
            chessBoard.setWhiteTurn(!chessBoard.getWhiteTurn());
            chessBoard.move(xOrig,yOrig,Utilities.int2Char(dest[0] > orig[0] ? orig[0] + 2 : orig[0] - 2),yOrig);
            chessBoard.move(Utilities.int2Char(dest[0]),dest[1],Utilities.int2Char(dest[0] > orig[0] ? orig[0] - 1 : orig[0] + 1),yOrig);
            toggleTimer();
            return moved;
        }

        chessBoard.move(xOrig,yOrig,xDest,yDest);
        if (pioniAtDest != null && p.getIsWhite() != pioniAtDest.getIsWhite()) {
            chessBoard.capture(pioniAtDest);
            chessBoard.setMovesRemaining(100);
            moved.add(pioniAtDest);
        }
        if (!moved.isEmpty()) {
            chessBoard.setWhiteTurn(!chessBoard.getWhiteTurn());
            toggleTimer();
        }
        return moved;
    }
    public void checkTimeOut(){
        updateTimers();
        if (whiteTimeRemaining <= 0){
            chessBoard.setGameEnded(true,Winner.Black);
        } else if (blackTimeRemaining <= 0){
            chessBoard.setGameEnded(true,Winner.White);
        }
    }
    public void notifyKingCheck(boolean isWhite, boolean isChecked){
        Message message = new Message();
        message.setCode(isWhite ? RequestCodes.KING_CHECK_WHITE : RequestCodes.KING_CHECK_BLACK);
        message.setData(isChecked);
        message.send(white,null);
        message.send(black,null);
    }
    public void notifyTimers(){
        updateTimers();
        Message whiteNotification = new Message();
        whiteNotification.setCode(RequestCodes.TIMER);
        whiteNotification.setData(new long[]{whiteTimeRemaining, blackTimeRemaining});
        whiteNotification.send(white,null);
        whiteNotification.send(black, null);
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
                    upgradedPioni = new Alogo(p.getIsWhite(), chessBoard, p.getXPos(), p.getYPos(),null,false);
                    break;
                case "Pyrgos":
                    upgradedPioni = new Pyrgos(p.getIsWhite(), chessBoard, p.getXPos(), p.getYPos(),null,false);
                    break;
                case "Stratigos":
                    upgradedPioni = new Stratigos(p.getIsWhite(), chessBoard, p.getXPos(), p.getYPos(),null,false);
                    break;
                case "Vasilissa":
                    upgradedPioni = new Vasilissa(p.getIsWhite(), chessBoard, p.getXPos(), p.getYPos(),null,false);
                    break;
                default:
                    System.err.println("Something went wrong!");
                    return false;
            }
            Iterator<Pioni> iterator = chessBoard.getPionia().iterator();
            while (iterator.hasNext()) {
                Pioni pioni = iterator.next();
                if (pioni.getXPos() == upgradedPioni.getXPos() && pioni.getYPos() == upgradedPioni.getYPos()) {
                    iterator.remove();
                    chessBoard.getPionia().add(upgradedPioni);
                    return true;
                }
            }
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

    private void startWhiteTimer() {
        if (!chessBoard.getWhiteTurn()) updateTimers();
        timerStart = System.currentTimeMillis();
    }

    private void startBlackTimer() {
        if (chessBoard.getWhiteTurn()) updateTimers();
        timerStart = System.currentTimeMillis();
    }


    private void updateTimers() {
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - timerStart;

        if (whiteTimerRunning) {
            whiteTimeRemaining -= elapsed;
            if (whiteTimeRemaining <= 0) {
                chessBoard.setGameEnded(true, Winner.Black);
            }
        } else {
            blackTimeRemaining -= elapsed;
            if (blackTimeRemaining <= 0) {
                chessBoard.setGameEnded(true, Winner.White);
            }
        }

        timerStart = currentTime;
    }
    private void toggleTimer() {
        updateTimers();
        if (chessBoard.getWhiteTurn()) {
            whiteTimerRunning = true;
            startWhiteTimer();
        } else {
            whiteTimerRunning = false;
            startBlackTimer();
        }
    }
    public int getMinutesAllowed(){
        return (int) (whiteTimeRemaining / 60000L);
    }
    public ChessBoard getBoard() { return chessBoard; }


}
