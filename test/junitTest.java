import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * GameState와 Player의 핵심 로직을 검증하는 테스트 클래스.
 */
public class junitTest {

    private GameState state;
    private Player player0;
    private Player player1;

    @BeforeEach
    void setUp() {
        PlayConfig config = new PlayConfig(2, 4, PlayConfig.BoardType.SQUARE);
        state = new GameState(config, List.of("Player1", "Player2"));
        player0 = state.getPlayers().get(0);
        player1 = state.getPlayers().get(1);
    }

    @Test
    @DisplayName("플레이어 정보 테스트 (ID, 말 개수)")
    void playerInfoTest() {
        assertEquals(0, player0.getId());
        assertEquals(4, player0.getPieces().size());
        assertEquals(1, player1.getId());
        assertEquals(4, player1.getPieces().size());
    }

    @Test
    @DisplayName("말 이동 테스트1 (외곽 경로 및 첫번째 지름길 진입)")
    void pieceMoveShortcutTest1() {
        // given: '윷'과 '도'를 던져 이동 기회 2번 확보
        state.applyThrow(Yut.Result.윷);
        state.applyThrow(Yut.Result.도);

        Piece piece = player0.getPieces().get(0);
        piece.setPathIndex(0);
        piece.setStepIndex(0);

        // when: '윷'(4칸)으로 이동
        state.setSelect(0); // '윷' 선택
        state.movePiece(piece.getId());

        // then: P0, S4에 위치
        assertEquals(4, piece.getStepIndex(), "'윷'으로 네 칸 이동");

        // when: 남은 '도'(1칸)로 이동
        state.setSelect(0); // 남은 '도' 선택
        state.movePiece(piece.getId());

        // then: 분기점인 P0, S5에 위치
        assertEquals(5, piece.getStepIndex(), "'도'로 한 칸 더 이동하여 분기점 도착");
    }

    @Test
    @DisplayName("말 이동 테스트2 (외곽 경로 및 두번째 지름길 진입)")
    void pieceMoveShortcutTest2() {
        // given: '윷', '윷', '개'를 던져 이동 기회 3번 확보
        state.applyThrow(Yut.Result.윷);
        state.applyThrow(Yut.Result.윷);
        state.applyThrow(Yut.Result.개);

        Piece piece = player0.getPieces().get(0);
        piece.setPathIndex(0);
        piece.setStepIndex(0);

        // when: '윷'(4칸)으로 이동
        state.setSelect(0); // '윷' 선택
        state.movePiece(piece.getId());

        // then: P0, S4에 위치
        assertEquals(4, piece.getStepIndex(), "'윷'으로 네 칸 이동");

        // when: '윷'(4칸)으로 이동
        state.setSelect(0); // '윷' 선택
        state.movePiece(piece.getId());

        // then: P0, S8에 위치
        assertEquals(8, piece.getStepIndex(), "'윷'으로 네 칸 이동");

        // when: 남은 '개'(2칸)로 이동
        state.setSelect(0); // 남은 '도' 선택
        state.movePiece(piece.getId());

        // then: 분기점인 P0, S10에 위치
        assertEquals(10, piece.getStepIndex(), "'개'로 한 칸 더 이동하여 분기점 도착");
    }

    @Test
    @DisplayName("말 업기 테스트")
    void pieceGroupTest() {
        // given: 첫 번째 말(piece1)이 P0, S2 위치에 미리 자리를 잡고 있음
        Piece piece1 = player0.getPieces().get(0);
        piece1.setPathIndex(0);
        piece1.setStepIndex(2);

        // given: 두 번째 말(piece2)은 출발점에 있음
        Piece piece2 = player0.getPieces().get(1);
        piece2.setPathIndex(0);
        piece2.setStepIndex(0);

        // when: '개'(2칸)를 던져 두 번째 말을 첫 번째 말 위치로 이동
        state.applyThrow(Yut.Result.개);
        state.movePiece(piece2.getId());

        // then: 두 말 모두 같은 위치(P0, S2)에 있으므로 업힘 상태여야 함
        assertEquals(2, piece1.getStepIndex(), "첫 번째 말의 위치 확인");
        assertEquals(2, piece2.getStepIndex(), "두 번째 말의 위치 확인");
        assertTrue(piece1.isGrouped(), "첫 번째 말이 업힘 상태여야 함");
        assertTrue(piece2.isGrouped(), "두 번째 말이 업힘 상태여야 함");
    }

    @Test
    @DisplayName("말 잡기 테스트 (테스트 격리성 확보)")
    void pieceCatchTest() {
        // given: 상대(Player1)의 말이 P0, S2 위치에 있도록 직접 설정
        Piece opponentPiece = player1.getPieces().get(0);
        opponentPiece.setPathIndex(0);
        opponentPiece.setStepIndex(2);

        // when: 내(Player0)가 '개'를 던져 상대 말을 잡음
        Piece myPiece = player0.getPieces().get(0);
        myPiece.setPathIndex(0);
        myPiece.setStepIndex(0);
        state.applyThrow(Yut.Result.개);
        state.movePiece(myPiece.getId());

        // then: 잡힌 말은 시작점으로, 나는 추가 턴 획득
        assertEquals(-1, opponentPiece.getPathIndex(), "잡힌 말은 출발점으로 돌아가야 함 (path: -1)");
        assertEquals(1, state.getThrowCount(), "말을 잡으면 던질 기회가 1회 추가되어야 함");
        assertEquals(GameState.TurnEvent.CAPTURE_OCCURRED, state.getLastTurnEvent());
    }

    @Test
    @DisplayName("말 완주 및 점수 획득 테스트")
    void pieceFinishAndPointTest() {
        // given
        Piece piece = player0.getPieces().get(0);
        piece.setPathIndex(0);
        piece.setStepIndex(18); // 완주 2칸 전

        // when
        state.applyThrow(Yut.Result.개);
        state.movePiece(piece.getId());

        // then
        assertTrue(piece.isFinished(), "말이 완주 상태여야 함");
        long finishedPieces = player0.getPieces().stream().filter(Piece::isFinished).count();
        assertEquals(1, finishedPieces, "완주한 말이 1개이므로 점수는 1");
    }
}