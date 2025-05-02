import javax.swing.*;

public class YutBoard extends JFrame {
    private GameController controller;

    public YutBoard(GameController controller) {
        // UI 구성 및 버튼 이벤트 등록
    }

    public void updatePiecePosition(int playerId, int x, int y, int stack) {
        // 보드에서 해당 위치의 말 이미지를 갱신함
    }

    public void showYutResult(int result) {
        // 윷 결과를 텍스트나 이미지로 표시
    }

    public void showMessage(String message) {
        // 게임 메시지를 하단 패널에 출력
    }

    public void highlightPlayer(int playerId) {
        // 현재 턴인 플레이어를 UI 상에서 강조
    }

    public void disableAllButtons() {
        // 게임 종료 시 버튼을 비활성화
    }
}