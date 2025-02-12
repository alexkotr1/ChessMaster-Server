package alexk.chess.Pionia;

import alexk.chess.ChessBoard;
import alexk.chess.Utilities;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class Vasilias extends Pioni implements Serializable {
    @JsonCreator
    public Vasilias(
            @JsonProperty("isWhite") Boolean isWhite,
            @JsonProperty("chessBoard") ChessBoard chessBoard,
            @JsonProperty("xpos") char initialX,
            @JsonProperty("ypos") int initialY,
            @JsonProperty("id") String id,
            @JsonProperty("captured") Boolean captured,
            @JsonProperty("moved") Boolean moved,
            @JsonProperty("kingSide") Boolean kingSide
    ) {
        super(isWhite, chessBoard, initialX, initialY, id, captured, moved, null);
    }

    @Override
    public boolean isLegalMove(char x, int y) {
        int currentPositionX = getPosition()[0];
        int currentPositionY = getPosition()[1];
        int nextPositionX = Utilities.char2Int(x);
        int xDiff = Math.abs(currentPositionX - nextPositionX);
        int yDiff = Math.abs(currentPositionY - y);
        boolean kingSide = nextPositionX > currentPositionX;
        Pyrgos pyrgos = null;
        try{
            pyrgos = (Pyrgos) getChessBoard().getPionia().stream()
                    .filter(pioni -> pioni.getIsWhite() == getIsWhite() &&
                            pioni.getType().equals("Pyrgos") &&
                            pioni.getKingSide() == kingSide && !pioni.getCaptured())
                    .toList().getFirst();
        } catch(NoSuchElementException ignored){}

        if (pyrgos != null && xDiff == 2  && yDiff == 0 && chessBoard.castlingRights(getIsWhite(),kingSide)) {
            boolean rookMoved = pyrgos.getMoved();
            boolean kingMoved = getMoved();
            System.out.println("Castling!");
            if (!rookMoved && !kingMoved) {
                int rookPositionX = Utilities.char2Int(pyrgos.getXPos());
                int rookPositionY = pyrgos.getYPos();
                System.out.println("Rook Position: " + rookPositionX + ", " + rookPositionY + " King Position: " + getPosition()[0] + ", " + getPosition()[1]);
                ArrayList<int[]> route = getRoute(currentPositionX, currentPositionY, pyrgos.getPosition()[0], pyrgos.getPosition()[1]);
                System.out.println("Route: " + route);
                printRoute(route);
                if (route != null && !route.isEmpty() && route.getLast()[0] == rookPositionX && route.getLast()[1] == rookPositionY) {

                    for (int i = 1; i < route.size() - 1; i++) {
                        int[] pos = route.get(i);
                        Pioni pioni = chessBoard.getPioniAt(Utilities.int2Char(pos[0]),pos[1]);
                        if (pioni != null || chessBoard.isDangerousPosition(Utilities.int2Char(pos[0]),pos[1],getIsWhite())) return false;
                    }
                    return true;
                } else return false;
            }
        }
        return xDiff <= 1 && yDiff <= 1 && (xDiff != 0 || yDiff != 0) && (this.getChessBoard().getPioniAt(x,y) == null || this.getChessBoard().getPioniAt(x,y).getIsWhite() != getIsWhite());
    }
}
