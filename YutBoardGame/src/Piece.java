public class Piece {
    private int x, y;
    private int stackCount;

    public Piece() {
        // 초기 좌표와 stack 수 설정 (0, 0, 1)
    }

    public int getX() {
        // 말의 x좌표 반환
        return 0;
    }

    public int getY() {
        // 말의 y좌표 반환
        return 0;
    }

    public int getStackCount() {
        // 업혀 있는 말의 수 반환
        return 0;
    }

    public void move(int moveValue) {
        // 윷 결과에 따라 말의 좌표를 변경
    }

    public void addStack(int count) {
        // 말 위에 다른 말을 업히면 count만큼 stack 증가
    }
}