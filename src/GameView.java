import java.util.List;

public interface GameView {
    void updateBoard(GameState state);
    void showThrowResult(Yut.Result result);    // 윷결과 표시 메소드
    void showWinner(Player winner);             // 승리자 표시 메소드
    void closeGameView();                       // 현재 게임 뷰를 닫는 메소드
    void showInitialSetup();                    // 초기 설정 화면(FirstPage)을 표시하는 메소드
}