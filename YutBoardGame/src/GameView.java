public interface GameView {
    void updateBoard(GameState state);
    void showThrowResult(Yut.Result result);
    void showWinner(Player winner);
}