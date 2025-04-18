package alexk.chess.Pionia;

import alexk.chess.ChessBoard;
import alexk.chess.Utilities;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;

public class Vasilissa extends Pioni implements Serializable {
    @JsonCreator
    public Vasilissa(
            @JsonProperty("isWhite") Boolean isWhite,
            @JsonProperty("chessBoard") ChessBoard chessBoard,
            @JsonProperty("xpos") char initialX,
            @JsonProperty("ypos") int initialY,
            @JsonProperty("id") String id,
            @JsonProperty("captured") Boolean captured,
            @JsonProperty("moved") Boolean moved,
            @JsonProperty("kingSide") Boolean kingSide
    ) {
        super(isWhite, chessBoard, initialX, initialY, id, captured, null, null);
    }

    @Override
    public boolean isLegalMove(char x, int y) {
        if (!isWithinBounds(x,y)) return false;
        int destX = Utilities.char2Int(x);
        int currentX = Utilities.char2Int(getXPos());
        int currentY = getYPos();
        ArrayList<int[]> route = getRoute(currentX,currentY,destX,y);
        return (route != null && !route.isEmpty() && route.getLast()[0] == destX && route.getLast()[1] == y) && (this.chessBoard.getPioniAt(x,y) == null || this.chessBoard.getPioniAt(x,y).getIsWhite() != getIsWhite());
    }
}
