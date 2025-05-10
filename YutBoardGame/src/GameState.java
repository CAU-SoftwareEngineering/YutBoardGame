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
    private Yut.Result lastThrow;            // 마지막 윷 던지기 결과

    /**
     * 생성자: 게임 설정과 플레이어명을 받아 초기화
     */
    public GameState(PlayConfig config, List<String> playerNames) {
        this.config = config;
        this.pathConfig = new PathConfig(config.getBoardType());
        this.totalOuterSteps = pathConfig.getOuterLength();
        this.players = new ArrayList<>();
        for (int i = 0; i < config.getPlayerCount(); i++) {
            players.add(new Player(i, playerNames.get(i), config.getPieceCount()));
        }
    }

    /** 전체 플레이어 리스트 반환 */
    public List<Player> getPlayers() { return players; }
    /** 현재 턴 플레이어 반환 */
    public Player getCurrentPlayer() { return players.get(currentPlayerIndex); }
    /** 마지막 던지기 결과 반환 */
    public Yut.Result getLastThrow() { return lastThrow; }

    /**
     * 윷 던지기 결과 적용
     * @param result 던진 결과
     */
    public void applyThrow(Yut.Result result) {
        this.lastThrow = result;
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
        int move = lastThrow.ordinal();

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
                        pStep = 0;
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
                pPath = pathConfig.getMergeShortcut() + 1;
                pStep = 0;
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

        // 잡기: 같은 칸의 상대 말 초기 위치로 리셋
        for (Player op : players) {
            if (op == current) continue;
            for (Piece opPiece : op.getPieces()) {
                if (!opPiece.isFinished()
                        && opPiece.getPathIndex() == path
                        && opPiece.getStepIndex() == step) {
                    opPiece.setPathIndex(-1);
                    opPiece.setStepIndex(-1);
                    opPiece.setGrouped(false);
                }
            }
        }

        // 그룹핑: 같은 칸 내 말이 2개 이상이면 grouped=true
        int count = 0;
        for (Piece my : current.getPieces()) {
            my.setGrouped(false);
            if (!my.isFinished()
                    && my.getPathIndex() == path
                    && my.getStepIndex() == step) {
                count++;
            }
        }
        if (count > 1) {
            for (Piece my : current.getPieces()) {
                if (!my.isFinished()
                        && my.getPathIndex() == path
                        && my.getStepIndex() == step) {
                    my.setGrouped(true);
                }
            }
        }

        // 말 이동이 끝난 뒤, 마지막 윷 결과가 윷(YUT) 또는 모(MO)이 아니면 턴 변경
        if (lastThrow != Yut.Result.윷 && lastThrow != Yut.Result.모) {
            nextTurn();
        }
    }

    /** 턴 넘기기 */
    public void nextTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
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