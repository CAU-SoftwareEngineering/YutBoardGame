import java.util.ArrayList;
import java.util.List;

/**
 * 게임 상태를 관리하는 클래스
 */
public class GameState {
    // 추가 턴 발생 등 특별한 게임 이벤트를 정의하는 열거형
    public enum TurnEvent {
        NORMAL,             // 일반 상태
        YUT_OR_MO_THROWN,   // 윷 또는 모를 던져서 추가 턴 획득
        CAPTURE_OCCURRED,    // 상대 말을 잡아서 추가 턴 획득
        BAEKDO_TURN_PASS    // 빽도가 나왔지만 말이 없어 턴이 넘어감
    }

    private final List<Player> players;
    private final PlayConfig config;
    private final PathConfig pathConfig;
    private final int totalOuterSteps;

    private int currentPlayerIndex = 0;
    private List<Yut.Result> lastThrow;
    private int throwCount = 1;

    public enum phase {THROW, MOVE}
    private phase currentPhase = phase.THROW;
    private int select = 0;
    private Player winner = null;

    private TurnEvent lastTurnEvent = TurnEvent.NORMAL; // 마지막 턴 이벤트 상태 변수

    /**
     * 생성자: 게임 설정과 플레이어명을 받아 초기화
     */
    public GameState(PlayConfig config, List<String> playerNames) {
        this.config = config; //
        this.pathConfig = new PathConfig(config.getBoardType()); //
        this.totalOuterSteps = pathConfig.getOuterLength(); //
        this.players = new ArrayList<>(); //
        this.lastThrow = new ArrayList<>(); //
        for (int i = 0; i < config.getPlayerCount(); i++) { //
            players.add(new Player(i, playerNames.get(i), config.getPieceCount())); //
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
    /** 이벤트 상태 getter */
    public TurnEvent getLastTurnEvent() { return lastTurnEvent; }
    /** 승리 플레이어 getter*/
    public Player getWinner() { return winner; }

    /**
     * 윷 던지기 결과 적용
     * @param result 던진 결과
     */
    public void applyThrow(Yut.Result result) {
        this.lastTurnEvent = TurnEvent.NORMAL; // 이벤트를 기본적으로 '일반'으로 초기화

        if (result == Yut.Result.빽도) {
            // 현재 플레이어의 말 중 하나라도 판 위에 있는지 확인
            boolean hasPieceOnBoard = false;
            for (Piece p : getCurrentPlayer().getPieces()) {
                if (p.getPathIndex() != -1 && !p.isFinished()) {
                    hasPieceOnBoard = true;
                    break;
                }
            }
            // 판에 말이 하나도 없는 상태에서 빽도가 나온 경우에만 턴을 넘김
            if (!hasPieceOnBoard) {
                this.lastTurnEvent = TurnEvent.BAEKDO_TURN_PASS; // 턴 넘어감 이벤트 설정
                nextTurn(); // 다음 플레이어에게 턴 넘김
                return; // 메소드 종료
            }
        }

        // 윷 던지기 단계가 아니면 로직을 실행하지 않음
        if(currentPhase != phase.THROW) {
            return;
        }

        // 윷 결과를 저장하고 던질 횟수를 차감
        this.lastThrow.add(result);
        throwCount --;

        // 윷 또는 모가 나오면 던질 기회 추가
        if (result == Yut.Result.모 || result == Yut.Result.윷) {
            throwCount ++;
            this.lastTurnEvent = TurnEvent.YUT_OR_MO_THROWN;
        }

        // 더 이상 던질 기회가 없으면 말 이동 단계로 전환
        if (throwCount == 0) {
            currentPhase = phase.MOVE;
        }
    }

    /**
     * 특정 말 이동 처리
     * @param pieceId 이동할 말의 ID (플레이어의 말 리스트에서의 인덱스)
     */
    public void movePiece(int pieceId) {
        this.lastTurnEvent = TurnEvent.NORMAL;
        Player current = getCurrentPlayer();

        if (lastThrow.isEmpty()) {
            if (throwCount == 0 && currentPhase == phase.MOVE) nextTurn();
            return;
        }

        Yut.Result yutResult = lastThrow.get(select);

        int move;
        switch (yutResult) {
            case 빽도: move = -1; break;
            case 도:  move = 1;  break;
            case 개:  move = 2;  break;
            case 걸:  move = 3;  break;
            case 윷:  move = 4;  break;
            case 모:  move = 5;  break;
            default:   move = 0;  break; // 예외 상황
        }

        lastThrow.remove(select); // 사용한 윷 결과 제거
        select = 0; // 다음 선택을 위해 초기화

        // 함께 이동할 말들 선택 (업힌 말 그룹)
        // 기준이 되는 말(selectedPieceLogic)의 시작 위치를 알아야 함.
        // GameController에서 pieceId를 넘겨주므로, 해당 ID의 말이 기준이 됨.
        Piece representativePiece = null;
        for(Piece p : current.getPieces()){
            if(p.getId() == pieceId) {
                representativePiece = p;
                break;
            }
        }
        if (representativePiece == null || representativePiece.isFinished()) {
            System.err.println("오류: 선택된 말을 찾을 수 없거나 이미 완주한 말입니다. pieceId: " + pieceId);
            // 만약 이 상황에서 lastThrow가 비어있지 않으면 다른 말을 선택해야 함.
            // 또는 오류로 간주하고 턴을 어떻게 처리할지 결정.
            // 현재는 그냥 리턴.
            if (lastThrow.isEmpty() && throwCount == 0 && currentPhase == phase.MOVE) {
                // 잡기가 발생하지 않았고, 던질 기회도 없고, 남은 윷도 없으면 턴 넘김
                boolean canMoveOtherPiece = false;
                for(Piece p : current.getPieces()) {
                    if(!p.isFinished() && p.getPathIndex() != -1) { // 움직일 수 있는 다른 말이 있는 경우
                        canMoveOtherPiece = true;
                        break;
                    }
                }
                if(!canMoveOtherPiece && current.getPieces().stream().allMatch(p -> p.isFinished() || p.getPathIndex() == -1)) {
                    // 모든 말이 시작점에 있거나 완주했고, 새 말을 놓을 수 없는 상황 (윷 결과에 따라)
                    // 또는 움직일 말이 없는 상황
                }
                // nextTurn(); // 무조건 넘기기보다 상태에 따라 결정. 여기선 일단 턴 넘김 방지.
            }
            return;
        }

        List<Piece> groupToMove = new ArrayList<>();
        int startPathForGroup = representativePiece.getPathIndex();
        int startStepForGroup = representativePiece.getStepIndex();

        // 만약 시작점에 있는 말을 처음 움직이는 경우 (새 말 꺼내기)
        if (startPathForGroup == -1 && !representativePiece.isFinished()) {
            // 새 말을 출발점으로 설정 (0,0)
            // GameController의 deployNewPiece에서 (0,0)으로 설정 후 movePiece를 호출하는 구조라면
            // 여기서는 startPathForGroup이 0, startStepForGroup이 0으로 넘어올 것임.
            // 만약 여기서 -1, -1 상태로 넘어온다면, 여기서 (0,0)으로 초기화.
            // 사용자의 GameController.deployNewPiece()는 p.setPathIndex(0); p.setStepIndex(0); 후 state.movePiece(p.getId());를 호출.
            // 따라서 이 부분은 GameController에서 이미 처리되었을 것이므로, startPathForGroup은 0, startStepForGroup은 0으로 가정.
        }


        for (Piece p : current.getPieces()) {
            if (!p.isFinished() && p.getPathIndex() == startPathForGroup && p.getStepIndex() == startStepForGroup) {
                groupToMove.add(p);
            }
        }
        if (groupToMove.isEmpty() && !representativePiece.isFinished()){ // 업히지 않은 단독 말인 경우
            groupToMove.add(representativePiece);
        }


        for (Piece pieceToMove : groupToMove) {
            if (pieceToMove.isFinished()) continue;

            System.out.printf("이동 전: ID %d, P%d S%d. 이동량: %d (%s)\n",
                    pieceToMove.getId(), pieceToMove.getPathIndex(), pieceToMove.getStepIndex(), move, yutResult.toString());

            int currentPath = pieceToMove.getPathIndex();
            int currentStep = pieceToMove.getStepIndex();

            int nextPath = currentPath; // 이동 후 경로 (초기값은 현재 경로)
            int nextStep = currentStep; // 이동 후 단계 (초기값은 현재 단계)

            boolean stoppedAtBranchThisMove = false; // 전진 시 분기점에 멈춰 대기하는지 여부

            if (move == -1) { // 빽도 (-1) 처리
                System.out.printf("빽도 처리 시작: ID %d, 현재 P%d S%d\n", pieceToMove.getId(), currentPath, currentStep);
                if (currentPath == 0) { // 현재 외곽 경로에 있을 때
                    // 수정된 로직: 위치 1에 있는 말이 빽도를 만나면 완주
                    if (currentStep == 1) {
                        System.out.printf("빽도: ID %d, 외곽 P0 S1에서 뒤로 가 완주 처리.\n", pieceToMove.getId());
                        pieceToMove.setFinished(true);
                        nextPath = 0;
                        nextStep = 0; // 시각적 위치를 위해 명시적으로 설정
                    } else if (currentStep > 1) { // 외곽 경로의 다른 지점 (위치 0은 제외)
                        nextPath = 0;
                        nextStep = currentStep - 1; // 한 칸 뒤로
                        System.out.printf("빽도: ID %d, 외곽 P%d S%d -> P%d S%d\n",
                                pieceToMove.getId(), currentPath, currentStep, nextPath, nextStep);
                    }
                    // 만약 currentStep == 0 이면, 아무것도 하지 않고 현재 위치를 유지
                } else { // 현재 지름길(currentPath > 0)에 있을 때
                    if (currentStep == 0) {
                        int correspondingBranchPoint = pathConfig.getBranchPoint(currentPath);
                        nextPath = 0;
                        nextStep = correspondingBranchPoint;
                        System.out.printf("빽도: ID %d, 지름길 P%d S0 -> 외곽 P%d S%d (분기점 복귀)\n",
                                pieceToMove.getId(), currentPath, nextPath, nextStep);
                    } else {
                        nextPath = currentPath;
                        nextStep = currentStep - 1;
                        System.out.printf("빽도: ID %d, 지름길 P%d S%d -> P%d S%d\n",
                                pieceToMove.getId(), currentPath, currentStep, nextPath, nextStep);
                    }
                }
            } else if (move > 0) { // 전진 (move > 0) 처리
                boolean startedFromOuterBranch = false;
                // 시나리오 1: 외곽 경로의 분기점에서 출발하는 경우
                if (currentPath == 0) {
                    for (int i = 1; i < pathConfig.getBranchCount(); i++) { // pathConfig.branchPoints[0]은 시작점이므로 실제 분기점은 1부터
                        if (currentStep == pathConfig.getBranchPoint(i)) {
                            startedFromOuterBranch = true;
                            nextPath = i; // 해당 지름길 경로 인덱스로 변경
                            nextStep = move - 1; // 지름길 내부는 0부터 시작 (예: '도'는 0번 칸)
                            System.out.printf("분기점 출발->지름길 진입: ID %d, 외곽S%d -> P%d S%d (이동량:%d)\n",
                                    pieceToMove.getId(), currentStep, nextPath, nextStep, move);
                            break;
                        }
                    }
                }

                // 시나리오 2 & 3: 분기점에서 출발하지 않았거나, 이미 지름길에 있는 경우
                if (!startedFromOuterBranch) {
                    if (currentPath == 0) { // 외곽 경로에서 일반 이동 중 분기점 도착 확인
                        int potentialOuterLandingStep = currentStep + move;
                        boolean landedExactlyOnBranch = false;
                        for (int i = 1; i < pathConfig.getBranchCount(); i++) {
                            if (potentialOuterLandingStep == pathConfig.getBranchPoint(i)) {
                                nextPath = 0; // 외곽 경로 유지
                                nextStep = potentialOuterLandingStep; // 분기점에 멈춤
                                stoppedAtBranchThisMove = true; // 현재 윷 이동은 여기서 종료 (대기)
                                landedExactlyOnBranch = true;
                                System.out.printf("분기점 도착 및 대기: ID %d, 외곽S%d -> 외곽S%d (이동량:%d)\n",
                                        pieceToMove.getId(), currentStep, nextStep, move);
                                break;
                            }
                        }
                        if (!landedExactlyOnBranch) { // 분기점에 멈추지 않았다면 일반 외곽 이동
                            nextPath = 0;
                            nextStep = potentialOuterLandingStep;
                        }
                    } else { // 지름길(currentPath > 0)에서 계속 이동
                        nextPath = currentPath; // 현재 지름길 경로 유지 (탈출 전까지)
                        nextStep = currentStep + move;
                    }
                }

                // 전진 이동 시, 분기점 대기가 아니라면 추가 경로 규칙(중앙,탈출,완주) 적용
                if (!stoppedAtBranchThisMove) {
                    int tempPath = nextPath; // 위에서 계산된 경로/단계를 임시 변수에 할당
                    int tempStep = nextStep;

                    // 지름길 위에서의 로직
                    if (tempPath > 0) { // 현재 계산된 위치가 지름길인 경우
                        // 중앙 노드(갈림길 합류점) 도착 시, 합류 후의 지름길로 경로 변경
                        if (tempPath != pathConfig.getMergeShortcut() && tempStep == pathConfig.getMergeStep()) {
                            System.out.printf("중앙 노드 도착(지름길): ID %d, P%d S%d -> P%d (합류지름길) S%d 유지\n",
                                    pieceToMove.getId(), tempPath, tempStep, pathConfig.getMergeShortcut(), tempStep);
                            tempPath = pathConfig.getMergeShortcut();
                        }

                        // 지름길 완주 및 외곽 경로로 복귀
                        // 주의: tempPath가 위에서 mergeShortcut으로 변경되었을 수 있으므로, 변경 후의 tempPath를 기준으로 길이 체크
                        if (tempPath > 0 && tempStep >= pathConfig.getShortcutLength()) {
                            int overshotSteps = tempStep - pathConfig.getShortcutLength();
                            // 어떤 지름길(nextPath)에서 탈출하는지 명확히 하기 위해, 탈출 전 지름길 인덱스(nextPath)를 사용
                            int targetOuterStep = pathConfig.getExitOffset(nextPath);

                            System.out.printf("지름길 탈출: ID %d, 원래P%d S%d -> 외곽P0 S%d (목표S%d + 초과%d)\n",
                                    pieceToMove.getId(), nextPath, tempStep, (targetOuterStep + overshotSteps), targetOuterStep, overshotSteps);
                            tempPath = 0; // 외곽 경로로 변경
                            tempStep = targetOuterStep + overshotSteps;
                        }
                    }

                    // 외곽 경로 위에서의 로직 또는 지름길 탈출 후 외곽 경로에서의 로직
                    if (tempPath == 0) {
                        if (tempStep >= totalOuterSteps) { // 완주 (골인)
                            System.out.printf("완주(외곽): ID %d, P%d S%d\n", pieceToMove.getId(), tempPath, tempStep);
                            pieceToMove.setFinished(true);
                        }
                    }
                    // 규칙 적용된 경로/단계를 nextPath/nextStep에 최종 반영
                    nextPath = tempPath;
                    nextStep = tempStep;
                }
            } // end of 전진 처리 (move > 0)

            // 최종 위치 업데이트
            if (pieceToMove.isFinished()) {
                pieceToMove.setPathIndex(-1);
                pieceToMove.setStepIndex(-1);
            } else {
                pieceToMove.setPathIndex(nextPath);
                pieceToMove.setStepIndex(nextStep);
            }

            System.out.printf("이동 후 최종: ID %d, P%d S%d, 완주:%b\n",
                    pieceToMove.getId(), pieceToMove.getPathIndex(), pieceToMove.getStepIndex(), pieceToMove.isFinished());

        } // end of for (Piece pieceToMove : groupToMove)


        // --- 잡기 (Capture) ---
        // 이동 후 기준 좌표: 그룹 내 첫 번째 살아있는 말의 최종 위치
        int finalPathForAction = -1, finalStepForAction = -1;
        Piece firstAlivePieceInGroup = null;
        for (Piece p : groupToMove) {
            if (!p.isFinished()) {
                firstAlivePieceInGroup = p;
                finalPathForAction = p.getPathIndex();
                finalStepForAction = p.getStepIndex();
                break;
            }
        }

        boolean capturedOpponentPiece = false;
        if (finalPathForAction != -1 && firstAlivePieceInGroup != null) { // 그룹의 어떤 말이든 아직 말판 위에 있다면
            // 그리고 그 위치가 특수 지역(예: 시작점 0,0에서 잡기 불가 등)이 아닌 경우에만 잡기
            // 잡을 수 있는 위치인지 확인 (예: 상대방 말이 있는 곳에 도착해야 함)
            // 윷놀이 규칙상, 상대방 말이 없는 빈칸에 도착하면 잡는 것이 아님.
            // 따라서, finalPathForAction, finalStepForAction에 상대방 말이 "있는지" 확인해야 함.
            for (Player opponent : players) {
                if (opponent == current) continue;

                List<Piece> opponentPiecesToReset = new ArrayList<>(); // 동시 수정 오류 방지
                for (Piece opPiece : opponent.getPieces()) {
                    if (!opPiece.isFinished() && opPiece.getPathIndex() == finalPathForAction && opPiece.getStepIndex() == finalStepForAction) {
                        opponentPiecesToReset.add(opPiece);
                    }
                }

                if (!opponentPiecesToReset.isEmpty()) {
                    capturedOpponentPiece = true;
                    for (Piece opPieceToReset : opponentPiecesToReset) {
                        System.out.printf("말 잡힘!: P%d의 말 ID%d (소유자 %s)이 P%d에게 P%d S%d에서 잡힘\n",
                                opponent.getId(), opPieceToReset.getId(), opPieceToReset.getOwner().getColor(), current.getId(), finalPathForAction, finalStepForAction);
                        opPieceToReset.setPathIndex(-1); // 시작 지점으로 돌아감 (말판 밖 대기)
                        opPieceToReset.setStepIndex(-1);
                        opPieceToReset.setFinished(false); // 잡힌 경우 완료 상태가 아님을 명확히 함
                        opPieceToReset.setGrouped(false); // 업힌 상태 리셋
                    }
                }
            }
        }

        // --- 잡기 성공 시 처리 ---
        if (capturedOpponentPiece) {
            throwCount++; // 한 번 더 던질 기회 획득
            currentPhase = phase.THROW; // 단계를 THROW로 다시 전환
            this.lastTurnEvent = TurnEvent.CAPTURE_OCCURRED; // '말 잡기' 이벤트 발생 기록
            System.out.println("상대 말 잡음! 한 번 더 던지기. 남은 던질 기회: " + throwCount); //
        }

        // --- 업기 (Grouping) ---
        if (finalPathForAction != -1 && firstAlivePieceInGroup != null && !capturedOpponentPiece) { // 상대 말을 잡은 위치에서는 업기 불가
            int piecesAtLocation = 0;
            for (Piece myPiece : current.getPieces()) {
                if (!myPiece.isFinished() && myPiece.getPathIndex() == finalPathForAction && myPiece.getStepIndex() == finalStepForAction) {
                    piecesAtLocation++;
                }
            }
            boolean isNowGrouped = piecesAtLocation > 1;
            for (Piece myPiece : current.getPieces()) {
                if (!myPiece.isFinished() && myPiece.getPathIndex() == finalPathForAction && myPiece.getStepIndex() == finalStepForAction) {
                    myPiece.setGrouped(isNowGrouped);
                    // if(isNowGrouped) System.out.println("말 ID " + myPiece.getId() + "이(가) " + finalPathForAction + "," + finalStepForAction + "에서 업힘(총 " + piecesAtLocation + "개).");
                }
            }
        }


        // --- 다음 턴 또는 추가 던지기 ---
        if (isGameOver()) {
            System.out.println("Player " + current.getId() + "님이 모든 말을 완주시켰습니다! 게임 종료.");
            currentPhase = phase.THROW; // 게임 종료 상태로 두지만, UI에서 버튼 비활성화 등 처리
            // GameController에서 winner를 view에 알리고 UI를 최종 업데이트해야 함.
            return;
        }

        // 윷/모를 던졌거나 말을 잡아서 추가 기회가 생긴 경우가 아니면서, 현재 이동할 윷 결과가 다 떨어졌으면 턴 넘김
        if (lastThrow.isEmpty() && currentPhase == phase.MOVE) { // capturedOpponentPiece 조건은 이미 currentPhase를 THROW로 바꿈
            if (throwCount == 0) { // 이전 윷/모로 인한 남은 던지기 횟수가 없다면
                nextTurn();
            } else { // 아직 던질 기회가 남아있다면 (예: 이전 윷/모 때문이지만 이번엔 잡지 못함)
                currentPhase = phase.THROW;
                System.out.println("남은 던질 기회가 있습니다 (" + throwCount + "번). Phase: THROW");
            }
        } else if (!lastThrow.isEmpty() && currentPhase == phase.MOVE) {
            // 아직 현재 턴에 적용할 윷 결과가 남아있다면 MOVE 상태 유지 (다른 말 선택 가능)
            System.out.println("적용할 윷 결과가 남아있습니다. Phase: MOVE");
        }
        // currentPhase가 THROW가 되었다면 (잡기 또는 보류된 던지기 때문), THROW 상태 유지.
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
        this.winner = getCurrentPlayer(); // 승자 설정
        return true;
    }
}