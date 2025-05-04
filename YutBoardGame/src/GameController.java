public class GameController {
    private GameState gameState;
    private GameView view;

    public GameController(int playerCount, int pieceCount, GameView view) {
        // GameState 및 View 주입
        this.gameState = new GameState(playerCount, pieceCount);
        this.view = view;
    }

    // 윷을 던졌을 때 호출되는 메서드: 랜덤 결과 생성 후 View에 표시
    public void onYutThrow() {
        int result = Yut.throwRandom();
        gameState.setYutResult(result);
        view.showYutResult(result);
        view.showMessage("윷 결과: " + result);
    }

    // 테스트용 윷 던지기: 고정된 값으로 설정
    public void onTestThrow(int fixedResult) {
        gameState.setYutResult(fixedResult);
        view.showYutResult(fixedResult);
        view.showMessage("테스트 결과: " + fixedResult);
    }

    // 새 말을 생성하려고 시도하는 메서드
    public void onPieceCreate() {
        Player current = gameState.getCurrentPlayer();
        boolean created = current.createPiece();
        if (created) {
            view.showMessage("새 말이 생성되었습니다.");
            updateView();
        } else {
            view.showMessage("더 이상 만들 수 있는 말이 없습니다.");
        }
    }

    // 특정 좌표의 말을 선택해 이동시키는 메서드
    public void onPieceSelect(int x, int y) {
        Player current = gameState.getCurrentPlayer();
        boolean moved = current.movePiece(x, y, gameState.getYutResult());
        if (moved) {
            view.showMessage("말이 이동했습니다.");
            current.checkCatch(x, y); // 상대 말 잡기
            current.checkGoalIn();   // 골인 체크
            updateView();            // UI 갱신

            // 승리 여부 확인 후 처리
            if (gameState.checkWinner()) {
                view.showMessage("플레이어 " + current.getId() + " 승리!");
                view.disableAllButtons();
            } else {
                gameState.advanceTurn();
                view.highlightPlayer(gameState.getCurrentPlayer().getId());
            }
        } else {
            view.showMessage("이동할 수 없습니다. 올바른 말을 선택하세요.");
        }
    }

    // 말의 위치 상태를 기반으로 전체 View 갱신
    private void updateView() {
        for (Player p : gameState.getPlayers()) {
            for (Piece piece : p.getPieces()) {
                view.updatePiecePosition(p.getId(), piece.getX(), piece.getY(), piece.getStackCount());
            }
        }
    }
}