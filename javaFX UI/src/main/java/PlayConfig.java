/**
 * 게임 설정 정보
 */
public class PlayConfig {
    public enum BoardType { SQUARE, PENTAGON, HEXAGON }

    private int playerCount;    // 플레이어 수 (2~4)
    private int pieceCount;     // 말 개수 (2~5)
    private BoardType boardType; // 판 종류

    /** 기본 생성자: 2명, 2말, 사각형 */
    public PlayConfig() {
        this.playerCount = 2;
        this.pieceCount  = 2;
        this.boardType   = BoardType.SQUARE;
    }

    /** 전체 생성자 */
    public PlayConfig(int playerCount, int pieceCount, BoardType boardType) {
        this.playerCount = playerCount;
        this.pieceCount  = pieceCount;
        this.boardType   = boardType;
    }

    // --- getters & setters ---

    public int getPlayerCount() { return playerCount; }
    public void setPlayerCount(int playerCount) {
        if (playerCount < 2 || playerCount > 4)
            throw new IllegalArgumentException("플레이어 수는 2~4 사이여야 합니다.");
        this.playerCount = playerCount;
    }

    public int getPieceCount() { return pieceCount; }
    public void setPieceCount(int pieceCount) {
        if (pieceCount < 2 || pieceCount > 5)
            throw new IllegalArgumentException("말 개수는 2~5 사이여야 합니다.");
        this.pieceCount = pieceCount;
    }

    public BoardType getBoardType() { return boardType; }

    /** 라디오 버튼 텍스트("사각형" 등)로부터 설정 */
    public void setBoardType(String label) {
        switch (label) {
            case "사각형": this.boardType = BoardType.SQUARE;   break;
            case "오각형": this.boardType = BoardType.PENTAGON; break;
            case "육각형": this.boardType = BoardType.HEXAGON;  break;
            default:
                throw new IllegalArgumentException("알 수 없는 판 종류: " + label);
        }
    }
    public void setBoardType(BoardType boardType) {
        this.boardType = boardType;
    }
}