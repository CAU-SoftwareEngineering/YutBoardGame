/**
 * 판 종류별 경로(branching, merging, exit) 설정
 */
public class PathConfig {
    private final PlayConfig.BoardType boardType;
    private final int[] branchPoints;    // 외곽 경로에서 지름길 진입 스텝
    private final int mergeShortcut;     // 중앙 합류 후 진입할 지름길 인덱스
    private final int mergeStep;         // 중앙 합류 지점의 단계 인덱스
    private final int[] exitOffsets;     // 지름길 종료 후 외곽 경로 복귀 보정값
    private final int outerLength;       // 외곽 경로 전체 단계 수
    private final int[] shortcutLengths;   // 지름길 최대 단계 수

    public PathConfig(PlayConfig.BoardType boardType) {
        this.boardType = boardType;
        switch (boardType) {
            case SQUARE:
                branchPoints    = new int[]{0, 5, 10};
                mergeShortcut   = 2;
                mergeStep       = 2;
                exitOffsets     = new int[]{0, 10, 15};
                outerLength     = 20;
                shortcutLengths = 5;
                break;
            case PENTAGON:
                branchPoints    = new int[]{0, 5, 10, 15};
                mergeShortcut   = 3;
                mergeStep       = 2;
                exitOffsets     = new int[]{0, 10, 15, 20};
                outerLength     = 25;
                shortcutLengths = 5;
                break;
            case HEXAGON:
                branchPoints    = new int[]{0, 5, 10, 15, 20};
                mergeShortcut   = 4;
                mergeStep       = 2;
                exitOffsets     = new int[]{0, 5, 15, 20, 25};
                outerLength     = 30;
                shortcutLengths = 5;
                break;
            default:
                throw new IllegalArgumentException("Unknown board type");
        }
    }

    /** 지름길 진입 단계 수 */
    public int getBranchCount() { return branchPoints.length; }
    /** 특정 지름길 진입 지점 */
    public int getBranchPoint(int idx) { return branchPoints[idx]; }
    /** 중앙 합류 후 진입할 지름길 인덱스 */
    public int getMergeShortcut() { return mergeShortcut; }
    /** 중앙 합류 단계 인덱스 */
    public int getMergeStep() { return mergeStep; }
    /** 지름길 종료 후 외곽 보정값 */
    public int getExitOffset(int idx) { return exitOffsets[idx]; }
    /** 외곽 경로 전체 단계 수 */
    public int getOuterLength() { return outerLength; }
    /** 특정 지름길의 최대 단계 수 */
    public int getShortcutLength(int idx) { return shortcutLengths; }
}
