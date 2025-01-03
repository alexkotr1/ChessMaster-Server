package alexk.chess.Pionia;

import alexk.chess.ChessBoard;
import alexk.chess.Utilities;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;

public class Stratiotis extends Pioni implements Serializable {


    @JsonCreator
    public Stratiotis(
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
        int destX = Utilities.char2Int(x);
        int currentX = Utilities.char2Int(getXPos());
        int currentY = getYPos();
        if (!isWithinBounds(x,y) || (getIsWhite() && currentY > y) || (!getIsWhite() && currentY < y)) return false;
        if (destX != currentX){
            return Math.abs(destX - currentX) == 1 && Math.abs(currentY - y) == 1 && getChessBoard().getPioniAt(x, y) != null && getChessBoard().getPioniAt(x, y).getIsWhite() != getIsWhite();
        }
        if (y != currentY && getChessBoard().getPioniAt(x, y) != null) {return false;}

        if (Math.abs(y - currentY) > 2) {
            return false;
        }

        if (Math.abs(y - currentY) == 2) {
            if ((getIsWhite() && currentY != 2) || (!getIsWhite() && currentY != 7)) {
                return false;
            }
        }
        ArrayList<int[]> route = getRoute(currentX,currentY,destX,y);
        return (route != null && !route.isEmpty() && route.getLast()[0] == destX && route.getLast()[1] == y) && (this.getChessBoard().getPioniAt(x,y) == null || this.getChessBoard().getPioniAt(x,y).getIsWhite() != getIsWhite());
    }
}
