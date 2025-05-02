public class GameController {
    private GameState gameState;
    private YutBoard yutBoard;

    public GameController(int playerCount, int pieceCount) {
        // GameState 및 YutBoard 초기화
    }

    public void onYutThrow() {
        // Yut 클래스 통해 윷 던지고 결과 View에 표시
    }

    public void onPieceCreate() {
        // 현재 플레이어가 새 말 생성 시도, 이동 로직 포함
    }

    public void onPieceSelect(int x, int y) {
        // 특정 좌표의 말을 이동 대상으로 선택
    }

    public void onTestThrow(int fixedResult) {
        // 테스트용 윷 결과 수동 설정
    }

    private void updateView() {
        // 말 위치, 점수, 메시지 등 View 갱신 처리
    }

    private void handleCatch() {
        // 잡은 말 처리 및 메시지 출력
    }

    private void handleGoal() {
        // 골인한 말 처리 및 점수 업데이트
    }

    private void checkWin() {
        // 승리 조건 확인 후 종료 처리
    }
}