import java.util.ArrayList;
import java.util.List;

/**
 * 게임 상태를 관리하는 클래스
 */
public class GameState {
    private final List<Player> players;      // 플레이어 리스트
    private final PlayConfig config;         // 게임 설정
    private final PathConfig pathConfig;     // 경로 설정
    private final int totalOuterSteps;       // 외곽 경로 전체 단계 수

    private int currentPlayerIndex = 0;      // 현재 턴 플레이어 인덱스
    private List<Yut.Result> lastThrow;      // 마지막 윷 던지기 결과
    private int throwCount = 1;              // 던질 수 있는 횟수

    public enum phase {THROW, MOVE} // 게임 진행을 던지기/이동으로 분리
    private phase currentPhase = phase.THROW; // 현재 진행 중인 단계
    private int select = 0; // 선택된 윷 인덱스

    /**
     * 생성자: 게임 설정과 플레이어명을 받아 초기화
     */
    public GameState(PlayConfig config, List<String> playerNames) {
        this.config = config;
        this.pathConfig = new PathConfig(config.getBoardType());
        this.totalOuterSteps = pathConfig.getOuterLength();
        this.players = new ArrayList<>();
        this.lastThrow = new ArrayList<>();
        for (int i = 0; i < config.getPlayerCount(); i++) {
            players.add(new Player(i, playerNames.get(i), config.getPieceCount()));
        }
    }

    /** 전체 플레이어 리스트 반환 */
    public List<Player> getPlayers() { return players; }
    /** 현재 턴 플레이어 반환 */
    public Player getCurrentPlayer() { return players.get(currentPlayerIndex); }
    /** 마지막 던지기 결과 반환 */
    public List<Yut.Result> getLastThrow() { return lastThrow; }
    /** 남은 던지기 횟수 반환*/
    public int getThrowCount() { return throwCount; }
    /** 현재 플레이어 페이즈 반환 */
    public phase getPhase() { return currentPhase; }
    /** 현재 플레이어 인덱스 반환 */
    public void setSelect(int value) { select = value; }

    /**
     * 윷 던지기 결과 적용
     * @param result 던진 결과
     */
    public void applyThrow(Yut.Result result) {
        if (result == Yut.Result.빽도) {
            for (Piece p : players.get(currentPlayerIndex).getPieces()) {
            // 보드에 나가있는 말이 있으면
                if (p.getPathIndex() != -1 && !p.isFinished()) {
                // 아무 일도 하지 않음
                }
                else {
                    nextTurn(); //나가있는 말이 없는데 빽도가 나왔으면 턴 넘김
                    return;
                }
            }
        }
        
        if(currentPhase != phase.THROW) {
            return; // 던지기 단계가 아닐 때는 무시
        }
        this.lastThrow.add(result);
        throwCount --; // 던지기 횟수 감소
        if (result == Yut.Result.모 || result == Yut.Result.윷) {
            throwCount ++; // 윷, 모가 나오면 던질 수 있는 횟수 추가
        }
        if (throwCount == 0) {
            currentPhase = phase.MOVE; // 던지기 종료 후 이동 단계로 전환
        }
    }

    /**
     * 특정 말 이동 처리
     * @param pieceId 이동할 말의 ID
     */
    public void movePiece(int pieceId) {
        Player current = getCurrentPlayer();
        Piece selected = current.getPieces().get(pieceId);
        int path = selected.getPathIndex();
        int step = selected.getStepIndex();
        int move = lastThrow.get(select).ordinal();  // 선택된 결과를 사용용
        if (lastThrow.get(select) == Yut.Result.빽도){
            move = -1; // 빽도는 -1로 처리
        }
        lastThrow.remove(select); // 던지기 결과 사용 후 제거
        select = 0; // 다음 이동에 사용할 수 있도록 초기화


        // 함께 이동할 말들 선택 (같은 위치에 있는 그룹)
        List<Piece> groupToMove = new ArrayList<>();
        for (Piece p : current.getPieces()) {
            if (!p.isFinished()
                    && p.getPathIndex() == path
                    && p.getStepIndex() == step) {
                groupToMove.add(p);
            }
        }

        for (Piece p : groupToMove) {
            int pPath = p.getPathIndex();
            int pStep = p.getStepIndex();

            // 지름길 진입 여부
            if (pPath == 0) {
                for (int i = 1; i < pathConfig.getBranchCount(); i++) {
                    if (pStep == pathConfig.getBranchPoint(i)) {
                        pPath = i;
                        pStep = -1;
                        break;
                    }
                }
            }

            // 단계 이동
            int newStep = pStep + move;
            int pathLength = (pPath == 0)
                    ? totalOuterSteps
                    : pathConfig.getShortcutLength(pPath);
            // 경로 길이를 벗어나면 완주로 처리하고 더 이상 이동하지 않음
            if (newStep >= pathLength) {
                p.setFinished(true);
                p.setPathIndex(-1);
                p.setStepIndex(-1);
                continue;
            }
            pStep = newStep;

            // 중앙 합류 처리
            if (pPath == pathConfig.getMergeShortcut() && pStep == pathConfig.getMergeStep()) {
                pPath = pathConfig.getMergeShortcut();
                pStep = 2;
            }
            // 지름길 종료 → 외곽 복귀
            else if (pPath > 0 && pStep > pathConfig.getShortcutLength(pPath)) {
                int offset = pathConfig.getExitOffset(pPath);
                pStep += offset;
                pPath = 0;
            }

            // 위치 업데이트
            p.setPathIndex(pPath);
            p.setStepIndex(pStep);
        }

        // 이동 후 기준 좌표: 첫 번째 살아있는 말의 최종 위치
        int finalPath = -1, finalStep = -1;
        for (Piece p : groupToMove) {
            if (!p.isFinished()) {
                finalPath = p.getPathIndex();
                finalStep = p.getStepIndex();
                break;
            }
        }

        // --- 잡기 ---
        boolean isCaptured = false;
        for (Player op : players) {
            if (op == current) continue;
            for (Piece opPiece : op.getPieces()) {
                if (!opPiece.isFinished()
                        && opPiece.getPathIndex() == finalPath
                        && opPiece.getStepIndex() == finalStep
                        && opPiece.getPathIndex() != -1
                        && opPiece.getStepIndex() != -1) {
                    opPiece.setPathIndex(-1);
                    opPiece.setStepIndex(-1);
                    opPiece.setGrouped(false);
                    isCaptured = true;
                }
            }
        }
        if (isCaptured) {
            throwCount++; // 잡으면 던지기 횟수 증가
            currentPhase = phase.THROW; // 던지기 단계로 전환
            isCaptured = false; // 잡은 상태 초기화
        }

        // 그룹핑
        int count = 0;
        for (Piece my : current.getPieces()) {
            my.setGrouped(false);
            if (!my.isFinished()
                    && my.getPathIndex() == finalPath
                    && my.getStepIndex() == finalStep) {
                count++;
            }
        }
        if (count > 1) {
            for (Piece my : current.getPieces()) {
                if (!my.isFinished()
                        && my.getPathIndex() == finalPath
                        && my.getStepIndex() == finalStep) {
                    my.setGrouped(true);
                }
            }
        }

        if(lastThrow.isEmpty() && currentPhase == phase.MOVE && !isCaptured) {
            nextTurn(); // 전부 이동 후에는 턴 넘기기
        }
    }

    /** 턴 넘기기 */
    public void nextTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        throwCount = 1; // 턴마다 던지기 횟수 초기화
        currentPhase = phase.THROW; // 턴 넘기면 던지기 단계로 전환
    }

    /**
     * 게임 종료 여부 확인
     * @return 모든 말이 완주했으면 true
     */
    public boolean isGameOver() {
        for (Piece p : getCurrentPlayer().getPieces()) {
            if (!p.isFinished()) {
                return false;
            }
        }
        return true;
    }
}