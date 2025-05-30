/**
 * 게임 말(Piece) 정보
 */
public class Piece {
    private final int id;            // 말 ID
    private final Player owner;      // 소유자

    private int pathIndex;           // 경로 인덱스(0=외곽,1~=지름길)
    private int stepIndex;           // 경로상 단계 인덱스
    private int stack;               // 업힌 말 개수

    private boolean grouped;         // 그룹핑 여부
    private boolean finished;        // 완주 여부

    public Piece(int id, Player owner) {
        this.id = id;
        this.owner = owner;
        this.pathIndex = -1;
        this.stepIndex = -1;
        this.stack = 1;
        this.grouped = false;
        this.finished = false;
    }

    public int getId() { return id; }
    public Player getOwner() { return owner; }
    public int getPathIndex() { return pathIndex; }
    public void setPathIndex(int pathIndex) { this.pathIndex = pathIndex; }
    public int getStepIndex() { return stepIndex; }
    public void setStepIndex(int stepIndex) { this.stepIndex = stepIndex; }
    public boolean isGrouped() { return grouped; }
    public int getStack() { return stack; }
    public void setGrouped(boolean grouped) { this.grouped = grouped; }
    public boolean isFinished() { return finished; }
    public void setFinished(boolean finished) { this.finished = finished; }
}