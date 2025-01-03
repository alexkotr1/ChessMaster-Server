package alexk.chess.Pionia;

import alexk.chess.ChessBoard;
import alexk.chess.Utilities;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;

public class Vasilias extends Pioni implements Serializable {
    private boolean moved;
    @JsonCreator
    public Vasilias(
            @JsonProperty("isWhite") Boolean isWhite,
            @JsonProperty("chessBoard") ChessBoard chessBoard,
            @JsonProperty("xpos") char initialX,
            @JsonProperty("ypos") int initialY,
            @JsonProperty("id") String id,
            @JsonProperty("captured") Boolean captured
    ) {
        super(isWhite, chessBoard, initialX, initialY, id, captured);
    }

    @Override
    public boolean isLegalMove(char x, int y) {
        int currentPositionX = getPosition()[0];
        int currentPositionY = getPosition()[1];
        int nextPositionX = Utilities.char2Int(x);
        int xDiff = Math.abs(currentPositionX - nextPositionX);
        int yDiff = Math.abs(currentPositionY - y);
        Pioni pioniAtDestination = getChessBoard().getPioniAt(x,y);
        if (pioniAtDestination != null && pioniAtDestination.getIsWhite() == getIsWhite() && pioniAtDestination.getType().equals("Pyrgos")) {
            boolean rookMoved = ((Pyrgos) pioniAtDestination).getMoved();
            boolean kingMoved = getMoved();
            if (!rookMoved && !kingMoved) {
                ArrayList<int[]> route = pioniAtDestination.getRoute(Utilities.char2Int(pioniAtDestination.getXPos()), pioniAtDestination.getYPos(), getPosition()[0], getPosition()[1]);
                if (route != null && !route.isEmpty() && route.stream().noneMatch(r->{
                    Pioni pAtDest = getChessBoard().getPioniAt(Utilities.int2Char(r[0]),r[1]);
                    return getChessBoard().isDangerousPosition(Utilities.int2Char(r[0]),r[1],getIsWhite()) || (pAtDest != null && !pAtDest.getType().equals("Pyrgos") && !pAtDest.getType().equals("Vasilias"));
                })) return true;
            }
        }
        return xDiff <= 1 && yDiff <= 1 && (xDiff != 0 || yDiff != 0) && (this.getChessBoard().getPioniAt(x,y) == null || this.getChessBoard().getPioniAt(x,y).getIsWhite() != getIsWhite());
    }

    public void setMoved(boolean moved) {this.moved = moved;}
    public boolean getMoved() {return moved;}
}
