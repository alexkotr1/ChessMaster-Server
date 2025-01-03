package alexk.chess.Pionia;

import alexk.chess.ChessBoard;
import alexk.chess.Utilities;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Stratiotis.class, name = "Stratiotis"),
        @JsonSubTypes.Type(value = Pyrgos.class, name = "Pyrgos"),
        @JsonSubTypes.Type(value = Alogo.class, name = "Alogo"),
        @JsonSubTypes.Type(value = Stratigos.class, name = "Stratigos"),
        @JsonSubTypes.Type(value = Vasilissa.class, name = "Vasilissa"),
        @JsonSubTypes.Type(value = Vasilias.class, name = "Vasilias")
})

public abstract class Pioni {
    protected boolean isWhite;
    protected String type;
    protected int[] position = new int[2];
    @JsonBackReference
    protected ChessBoard chessBoard;
    private final String id;
    private String imagePath;
    private boolean captured;
    public Pioni(Boolean isWhite, ChessBoard chessBoard, char initialX, int initialY, String id, Boolean captured) {
        this.id = id == null ? UUID.randomUUID().toString() : id;
        position[0] = Utilities.char2Int(initialX);
        position[1] = initialY;
        this.type = this.getClass().getSimpleName();
        this.isWhite = isWhite;
        this.chessBoard = chessBoard;
        if (captured != null) this.captured = captured;

        switch (this.type){
            case "Alogo":
                this.imagePath = this.isWhite ? "white knight.png" : "black knight.png";
                break;
            case "Pyrgos":
                this.imagePath = this.isWhite ? "white rook.png" : "black rook.png";
                break;
            case "Stratigos":
                this.imagePath = this.isWhite ? "white bishop.png" : "black bishop.png";
                break;
            case "Stratiotis":
                this.imagePath = this.isWhite ? "white pawn.png" : "black pawn.png";
                break;
            case "Vasilias":
                this.imagePath = this.isWhite ? "white king.png" : "black king.png";
                break;
            case "Vasilissa":
                this.imagePath = this.isWhite ? "white queen.png" : "black queen.png";
                break;
        }
    }
    public abstract boolean isLegalMove(char x, int y);
    protected boolean isWithinBounds(char x, int y){
        int intX = Utilities.char2Int(x);
        return intX > 0 && intX <= 8 && y > 0 && y <= 8;
    }
    public ArrayList<int[]> getRoute(int x1, int y1, int x2, int y2){
        if (x2 < 0 || x2 > 8 || y2 < 0 || y2 > 8) return null;
        ArrayList<int[]> route = new ArrayList<>();
        //straight movement
        if ((y1 == y2 || x1 == x2) && (type.equals("Pyrgos") || type.equals("Vasilissa") || type.equals("Stratiotis"))){
            boolean x1Equalsx2 = x1 == x2;
            int diff = Math.abs((x1 == x2 ? y1 - y2 : x1 - x2));
            for (int i = 0;i<=diff;i++){
                int[] dest = x1Equalsx2 ? new int[]{x1, y1 + (y1 > y2 ? -i : i)} : new int[]{x1 + (x1 > x2 ? -i : i),y1};
                route.add(dest);
                if (i != 0 && i != diff && chessBoard.getPioniAt(Utilities.int2Char(dest[0]),dest[1]) != null) break;
            }
        }
        //diagonal movement
        else if (x1 != x2 && y1 != y2 && (type.equals("Vasilissa") || type.equals("Stratigos"))){
            int diff = Math.abs(x1 - x2);
            if (Math.abs(x1 - x2) != Math.abs(y1 - y2)) return null;
            for (int i = 0;i<=diff;i++){
                int destX = x1 + (x1 > x2 ? -i : i);
                int destY = y1 + (y1 > y2 ? -i : i);
                route.add(new int[]{destX,destY});
                if (i != 0 && i != diff && chessBoard.getPioniAt(Utilities.int2Char(destX),destY) != null) break;
            }
        }
        return route;
    }
    public String print(){
        String printChar = "";
        switch (type){
            case "Vasilias":
                printChar = "k";
                break;
            case "Vasilissa":
                printChar = "q";
                break;
            case "Pyrgos":
                printChar = "r";
                break;
            case "Alogo":
                printChar = "n";
                break;
            case "Stratigos":
                printChar = "b";
                break;
            case "Stratiotis":
                printChar = "p";
                break;
            default:
                System.err.println("Something went wrong!");
                break;
        }
        return isWhite ? printChar.toUpperCase() : printChar;
    }
    public void setPosition(char x, int y){
        position[0] = Utilities.char2Int(x);
        position[1] = y;
    }
    public int[] getPosition(){
        return position;
    }
    public void setXPos(char x){
        position[0] = Utilities.char2Int(x);
    }
    public void setYPos(int y){
        position[1] = y;
    }
    public char getXPos(){
        return Utilities.int2Char(position[0]);
    }
    public int getYPos(){
        return position[1];
    }
    public void setIsWhite(boolean isWhite){
        this.isWhite = isWhite;
    }
    public boolean getIsWhite(){
        return isWhite;
    }
    public void setChessBoard(ChessBoard chessBoard){ this.chessBoard = chessBoard; }
    public ChessBoard getChessBoard(){ return chessBoard; }
    public void setType(String type){
        this.type = type;
    }
    public String getType(){
        return type;
    }
    public void setImagePath(String imagePath){ this.imagePath = imagePath; }
    public String getImagePath(){ return imagePath; }
    public void setCaptured(Boolean captured){ this.captured = captured; }
    public Boolean getCaptured(){ return captured; }
    public String getID(){ return id; }
    public static void printRoute(ArrayList<int[]> route){
        if (route == null || route.isEmpty()) {
            return;
        }
        for (int i = 0;i<route.size();i++){
            System.out.printf("%d:[%c,%s]%n",i,Utilities.int2Char(route.get(i)[0]),route.get(i)[1]);
        }
    }
    @JsonIgnore
    @Override
    public String toString(){
        return String.format("Type: %s Position: [%c,%d] ID: %s", type, Utilities.int2Char(position[0]), position[1],this.id);
    }
    @Override
    public Pioni clone() {
        try {
            Pioni cloned = this.getClass()
                    .getConstructor(Boolean.class, ChessBoard.class, char.class, int.class, String.class, Boolean.class)
                    .newInstance(this.isWhite, null, Utilities.int2Char(this.position[0]), this.position[1], this.id, this.captured);
            cloned.setImagePath(this.getImagePath());
            return cloned;
        } catch (Exception e) {
            throw new AssertionError("Clone operation failed", e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pioni pioni = (Pioni) o;
        return Objects.equals(id, pioni.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
