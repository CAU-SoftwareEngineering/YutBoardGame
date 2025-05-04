import java.util.List;

public class GameState {
    private List<Player> players;
    private int currentTurn;
    private int totalPlayers;
    private int pieceCountPerPlayer;
    private int yutResult;
    private int winnerId = -1;

    public GameState(int players, int pieces) {
        // 플레이어와 말 개수 설정 및 초기화
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Player getCurrentPlayer() {
        // 현재 턴의 플레이어 반환
        return null;
    }

    public void advanceTurn() {
        // 다음 플레이어로 턴 넘김
    }

    public boolean checkWinner() {
        // 모든 말을 골인시킨 플레이어가 있으면 true
        return false;
    }

    public void setYutResult(int result) {
        // 현재 윷 결과 저장
        this.yutResult = result;
    }

    public int getYutResult() {
        // 현재 저장된 윷 결과 반환
        return yutResult;
    }
}