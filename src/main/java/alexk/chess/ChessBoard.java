package alexk.chess;

import alexk.chess.Pionia.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.util.ArrayList;
import java.util.UUID;

public class ChessBoard {
    @JsonManagedReference
    private final ArrayList<Pioni> pionia = new ArrayList<>();
    private boolean whiteTurn = true;
    private int movesRemaining = 100;
    private Boolean gameEnded = false;
    private ChessEngine.Winner winner = null;
    private String state = UUID.randomUUID().toString();
    @JsonIgnore
    public int totalMoves = 1;
    public void placePioniAt(Pioni p, char xPos, int yPos){
        p.setXPos(xPos);
        p.setYPos(yPos);
        refreshState();
    }
    public boolean isDangerousPosition(char xOrig, int yOrig, boolean white){
        for (Pioni p : getPionia().stream().filter(pioni -> pioni.getIsWhite() != white).toList()) {
            if (p.isLegalMove(xOrig,yOrig)) return true;
        }
        return false;
    }
    public void loadBoard(){
        for (int x = 1;x<=16;x++){
            pionia.add(new Stratiotis(x < 9,this, Utilities.int2Char(x < 9 ? x : x - 8 ),x < 9 ? 2 : 7,null,false,null,null));
        }
        for (int x = 0;x<2;x++){
            for (int y = 0;y<4;y++){
                switch (y){
                    case 0:{
                        pionia.add(new Pyrgos(x == 0,this,'A',x == 0 ? 1 : 8,null,false, false, false));
                        pionia.add(new Pyrgos(x == 0,this,'H',x == 0 ? 1 : 8,null,false,false, true));
                        break;
                    }
                    case 1:{
                        pionia.add(new Alogo(x == 0,this,'B',x == 0 ? 1 : 8,null,false,null,null));
                        pionia.add(new Alogo(x == 0,this,'G',x == 0 ? 1 : 8,null,false,null,null));
                        break;
                    }
                    case 2:{
                        pionia.add(new Stratigos(x == 0,this,'C',x == 0 ? 1 : 8,null,false,null,null));
                        pionia.add(new Stratigos(x == 0,this,'F',x == 0 ? 1 : 8,null,false,null,null));
                        break;
                    }
                    case 3:{
                        pionia.add(new Vasilissa(x == 0,this,'D',x == 0 ? 1 : 8,null,false,false,null));
                        pionia.add(new Vasilias(x == 0,this,'E',x == 0 ? 1 : 8,null,false,null,null));
                        break;
                    }
                }
            }
        }
        refreshState();
    }
    public Pioni getPioniAt(char xPos, int yPos){
        return pionia.stream().filter(pioni -> Utilities.int2Char(pioni.getPosition()[0]) == xPos && pioni.getPosition()[1] == yPos && !pioni.getCaptured()).findFirst().orElse(null);
    }
    public void refreshState(){
        this.state = UUID.randomUUID().toString();
    }
    public ArrayList<Pioni> getPionia(){
        return pionia;
    }
    public void move(char xOrig, int yOrig, char xDest,int yDest){
        movesRemaining--;
        Pioni p = getPioniAt(xOrig,yOrig);
        Pioni pDest = getPioniAt(xDest,yDest);
        if (p.getType().equals("Stratiotis")) movesRemaining = 100;
        if (pDest != null) capture(pDest);
        placePioniAt(p,xDest,yDest);
        if (p.getType().equals("Pyrgos")) ((Pyrgos) p).setMoved(true);
        else if (p.getType().equals("Vasilias")) ((Vasilias) p).setMoved(true);
    }
    public Boolean getWhiteTurn(){
        return whiteTurn;
    }
    public void setWhiteTurn(boolean whiteTurn){
        this.whiteTurn = whiteTurn;
        refreshState();
    }
    public String getState(){ return state; }
    public void setMovesRemaining(int movesRemaining){
        this.movesRemaining = movesRemaining;
        refreshState();
    }
    public int getMovesRemaining(){ return movesRemaining; }
    public void setGameEnded(Boolean gameEnded, ChessEngine.Winner winner) {
        this.gameEnded = gameEnded;
        this.winner = winner;
        refreshState();
    }
    public Boolean getGameEnded() { return gameEnded;}
    public ChessEngine.Winner getWinner() { return winner;}
    public void printBoard(){
        System.out.println("   a  b  c  d  e  f  g  h  \n  ------------------------");
        for (int y = 8;y>=1;y--){
            System.out.printf("%d  %s  %s  %s  %s  %s  %s  %s  %s %d%n",y,
                    this.getPioniAt('A',y) == null ? " " : this.getPioniAt('A',y).print(),
                    this.getPioniAt('B',y) == null ? " " : this.getPioniAt('B',y).print(),
                    this.getPioniAt('C',y) == null ? " " : this.getPioniAt('C',y).print(),
                    this.getPioniAt('D',y) == null ? " " : this.getPioniAt('D',y).print(),
                    this.getPioniAt('E',y) == null ? " " : this.getPioniAt('E',y).print(),
                    this.getPioniAt('F',y) == null ? " " : this.getPioniAt('F',y).print(),
                    this.getPioniAt('G',y) == null ? " " : this.getPioniAt('G',y).print(),
                    this.getPioniAt('H',y) == null ? " " : this.getPioniAt('H',y).print(),
                    y);
        }
        System.out.println("  ------------------------\n   a  b  c  d  e  f  g  h");

    }
    public boolean castlingRights(boolean white, boolean kingSide){
        Vasilias king = null;
        Pyrgos rook = null;
        for (Pioni p : getPionia()){
            if (p.getIsWhite() == white){
                if (p.getType().equals("Vasilias")) king = (Vasilias) p;
                else if (p.getType().equals("Pyrgos")){
                    if (((Pyrgos) p).getKingSide() == kingSide) rook = (Pyrgos) p;
                }
            }
        }
        return king != null && rook != null && !king.getMoved() && !rook.getMoved() && !rook.getCaptured();
    }
    @Override
    protected ChessBoard clone() {
        ChessBoard chessBoard = new ChessBoard();
        chessBoard.whiteTurn = whiteTurn;
        for (Pioni p : pionia){
            Pioni clone = p.clone();
            clone.setChessBoard(chessBoard);
            chessBoard.pionia.add(clone);
        }
        return chessBoard;
    }
    public void capture(Pioni p){
        p.setCaptured(true);
        refreshState();
    }
}
