import java.util.List;

public class Player {
    private int id;
    private List<Piece> pieces;
    private int point;
    private int remainingPieces;

    public Player(int id, int pieceCount) {
        // 플레이어 ID와 초기 말 개수 설정
    }

    public int getId() {
        // 플레이어 ID 반환
        return 0;
    }

    public int getPoint() {
        // 점수 반환
        return 0;
    }

    public int getRemainingPieces() {
        // 아직 출전하지 않은 말의 수 반환
        return 0;
    }

    public List<Piece> getPieces() {
        // 현재 보드 위의 말 리스트 반환
        return null;
    }

    public boolean createPiece() {
        // 남은 말이 있으면 새로운 말 생성하여 보드에 추가
        return false;
    }

    public boolean movePiece(int x, int y, int moveValue) {
        // 해당 좌표의 말을 찾아 moveValue만큼 이동시킴
        return false;
    }

    public boolean checkCatch(int x, int y) {
        // 상대방 말과 좌표가 겹치는지 확인하고 겹치면 제거 처리
        return false;
    }

    public boolean checkGoalIn() {
        // 도착 지점에 도달한 말이 있다면 점수 증가 및 제거
        return false;
    }

    public boolean checkStack() {
        // 같은 좌표에 있는 말들을 병합하고 stack 수 증가
        return false;
    }
}