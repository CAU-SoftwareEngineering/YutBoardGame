public interface GameView {
    void updatePiecePosition(int playerId, int x, int y, int stack);
    void showYutResult(int result);
    void showMessage(String message);
    void highlightPlayer(int playerId);
    void disableAllButtons();
}