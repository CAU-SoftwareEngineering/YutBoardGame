import java.util.List;

/**
 * 게임 진행 제어 클래스
 */
public class GameController {
    private final GameState state;
    private final GameView view;

    public GameController(PlayConfig config, List<String> playerNames, GameView view) {
        this.state = new GameState(config, playerNames);
        this.view  = view;
    }

    /** 게임 시작 시 보드 초기화 */
    public void startGame() {
        view.updateBoard(state);
    }

    /** 랜덤 윷 던지기 처리 */
    public void onThrowRandom() {
        Yut.Result result = Yut.throwRandom();
        state.applyThrow(result);
        view.showThrowResult(result);
    }

    /** 지정 윷 던지기 처리 */
    public void onThrowSpecified(Yut.Result specified) {
        Yut.Result result = Yut.throwSpecified(specified);
        state.applyThrow(result);
        view.showThrowResult(result);
    }

    /**
     * 보드판 위 pathIndex, stepIndex 칸을 클릭했을 때 호출.
     * 해당 위치에 있는 현재 플레이어의 말을 찾아서 이동 처리.
     *
     * @param pathIndex  경로 인덱스 (0=외곽, 1~=지름길)
     * @param stepIndex  해당 경로 위 단계 인덱스
     */
    public void onSelectPiece(int pathIndex, int stepIndex) {
        Player current = state.getCurrentPlayer();
        Piece selectedPiece = null;
        for (Piece p : current.getPieces()) {
            if (!p.isFinished() && p.getPathIndex() == pathIndex && p.getStepIndex() == stepIndex) {
                selectedPiece = p;
                break;
            }
        }

        if (selectedPiece != null) {
            state.movePiece(selectedPiece.getId()); // GameState 변경

            // GameState.movePiece() 내부에서 승리 조건, 추가 턴 등을 결정하고 상태를 변경
            // 그 최종 상태를 기반으로 UI를 업데이트
            if (state.isGameOver() && state.getWinner() != null) {
                // showWinner가 내부적으로 updateBoard를 호출하여 최종 화면을 그림
                view.showWinner(state.getWinner());
            } else {
                // 게임이 계속 진행 중이면 현재 상태로 보드 업데이트
                view.updateBoard(state);
            }
        }
        // 선택된 말이 없거나 이미 처리된 경우 아무것도 하지 않음
    }

    public void deployNewPiece() {
        Player current = state.getCurrentPlayer();
        for (Piece p : current.getPieces()) {
            // 아직 보드에 올라가지 않은 말 찾기
            if (p.getPathIndex() == -1 && !p.isFinished()) {
                // 시작 위치 세팅하고 바로 이동
                p.setPathIndex(0);
                p.setStepIndex(0);
                state.movePiece(p.getId());
                view.updateBoard(state);
                return;
            }
        }
        // 꺼낼 수 있는 말이 없는 경우
        view.showThrowResult(Yut.Result.빽도); // 예외적 알림
    }

    /** 현재 게임 상태를 반환 */
    public GameState getState() {
        return state;
    }

    /** 게임 재시작 처리: 현재 게임 뷰를 닫고, 초기 설정 화면을 뷰를 통해 다시 표시하도록 요청 */
    public void restartGame() {
        view.closeGameView();      // GameView 인터페이스를 통해 뷰 닫기 요청
        view.showInitialSetup();   // GameView 인터페이스를 통해 초기 설정 화면 표시 요청
    }
}

