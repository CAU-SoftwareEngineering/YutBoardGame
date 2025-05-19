import java.util.ArrayList;
import java.util.List;

/**
 * 플레이어 정보
 */
public class Player {
    private final int id;                // 플레이어 ID
    private final String color;          // 말 색상
    private final List<Piece> pieces;    // 소유한 말 리스트

    public Player(int id, String name, int pieceCount) {
        this.id = id;
        this.color = getColorName(id);
        this.pieces = new ArrayList<>();
        for (int i = 0; i < pieceCount; i++) {
            pieces.add(new Piece(i, this));
        }
    }

    private String getColorName(int id) {
        return switch (id % 4) {
            case 0 -> "blue";
            case 1 -> "red";
            case 2 -> "green";
            case 3 -> "yellow";
            default -> "gray";
        };
    }

    public int getId() { return id; }
    public String getColor() { return color; }
    public List<Piece> getPieces() { return pieces; }
}