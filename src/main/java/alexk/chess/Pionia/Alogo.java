package alexk.chess.Pionia;

import alexk.chess.ChessBoard;
import alexk.chess.Utilities;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Arrays;

public class Alogo extends Pioni implements Serializable {


    @JsonCreator
    public Alogo(
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
        if (!isWithinBounds(x,y)) return false;
        int destX = Utilities.char2Int(x);
        int destY = y;
        int currentX = Utilities.char2Int(getXPos());
        int currentY = getYPos();
        final int[][] allowed = {{2,-1},{-1,-2},{1,-2},{2,-1},{2,1},{1,2},{-1,2},{-2,1},{-2,-1}};
        return Arrays.stream(allowed).anyMatch(arr -> currentX + arr[0] == destX && currentY + arr[1] == destY) && (this.getChessBoard().getPioniAt(x,y) == null || this.getChessBoard().getPioniAt(x,y).getIsWhite() != getIsWhite());
    }
}