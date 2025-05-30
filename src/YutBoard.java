import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 게임 화면: 논리적 경로(path + step) 기반 버튼 배열과
 * 실제 픽셀 좌표 배치를 분리하여 구현한 Yut Nori 보드.
 */
public class YutBoard extends JFrame implements GameView {
    private static final String IMG_ROOT = "img/"; // 이미지 기본 경로

    private final PlayConfig config;
    private final GameController controller;

    private final JPanel boardPanel;
    private final JPanel pieceActionPanel;      // '새 말 꺼내기' 버튼 등이 위치할 패널 (기존 piecePanel에서 이름 변경 및 역할 명확화)
    private final JLabel statusLabel;
    private final JPanel infoPanel;

    private List<Point>[] pathPoints;           // 각 경로(path)의 UI 좌표 리스트
    private JButton[][] panButtons;             // 윷판의 각 위치를 나타내는 버튼 배열
    private final int buttonSize = 30;          // 윷판 위 말/칸 버튼 크기
    private boolean canMove = false;            // 윷 던진 후 true가 되어 말 선택 가능

    private JButton rndBtn;                     // 랜덤 윷 던지기 버튼
    private JButton specBtn;                    // 지정 윷 던지기 버튼
    private JComboBox<Yut.Result> yutComboBox;  // 지정 윷 선택 콤보박스

    // YutBoard 내에서 PathConfig 정보를 사용하기 위해 해당 보드 타입에 맞는 PathConfig 인스턴스를 가짐
    private final PathConfig pathConfigInstance;


    @SuppressWarnings("unchecked")
    public YutBoard(PlayConfig config, List<String> playerNames) {
        super("윷놀이 게임");
        this.config = config;
        // 현재 보드 타입에 맞는 PathConfig 인스턴스 생성
        this.pathConfigInstance = new PathConfig(config.getBoardType());
        this.controller = new GameController(config, playerNames, this);


        setLayout(new BorderLayout());

        // --- 중앙 보드 패널 ---
        boardPanel = new JPanel(null) { // null 레이아웃 사용, 직접 좌표 배치
            private Image bg = null;
            { // 인스턴스 초기화 블록에서 이미지 로드 시도
                ImageIcon icon = loadIcon(IMG_ROOT + "background.png");
                if (icon != null && icon.getImage() != null) {
                    bg = icon.getImage();
                } else {
                    System.err.println("배경 이미지 로드 실패: " + IMG_ROOT + "background.png");
                }
            }
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bg != null) {
                    g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
                } else { // 이미지가 없을 경우 단색 배경
                    g.setColor(new Color(210, 180, 140));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        boardPanel.setPreferredSize(new Dimension(600, 600));
        // 창 크기 변경 시 보드 UI 다시 그리기
        boardPanel.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                boardPanel.removeAll(); // 기존 버튼 제거 후 다시 초기화
                initBoardGeometry();    // 보드 기하학적 구조 계산
                initBoardUI();          // 보드 UI 요소(버튼) 생성 및 배치
                updateBoard(controller.getState()); // 현재 게임 상태로 보드 다시 그리기
                boardPanel.revalidate();
                boardPanel.repaint();
            }
        });
        add(boardPanel, BorderLayout.CENTER);

        // --- 우측 플레이어 정보 패널 ---
        infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setPreferredSize(new Dimension(220, 600));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(infoPanel, BorderLayout.EAST);

        // --- 하단 컨트롤 패널 ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        pieceActionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); // 중앙 정렬
        statusLabel = new JLabel("게임을 시작해주세요.", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        bottomPanel.add(pieceActionPanel, BorderLayout.CENTER);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        // --- 상단 윷 던지기 버튼 패널 ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); // 중앙 정렬
        rndBtn = new JButton("랜덤 윷 던지기");
        yutComboBox = new JComboBox<>(Yut.Result.values()); // 빽도, 도, 개, 걸, 윷, 모
        specBtn = new JButton("지정 윷 던지기");

        rndBtn.addActionListener(e -> controller.onThrowRandom());
        specBtn.addActionListener(e -> controller.onThrowSpecified((Yut.Result) yutComboBox.getSelectedItem()));

        topPanel.add(rndBtn);
        topPanel.add(yutComboBox);
        topPanel.add(specBtn);
        add(topPanel, BorderLayout.NORTH);

        enableYutButtons(false); // 초기 버튼 상태 (게임 시작 전 비활성화)

        pack(); // 컴포넌트 크기에 맞게 창 크기 조절
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 화면 중앙에 배치
        setVisible(true);

        // 컨트롤러를 통해 게임 시작 (UI가 준비된 후 호출)
        controller.startGame(); // -> GameState 초기화 및 updateBoard 호출 유도
    }

    /**
     * 보드상의 논리적 좌표(pathPoints)를 현재 패널 크기에 맞춰 계산합니다.
     */
    @SuppressWarnings("unchecked")
    private void initBoardGeometry() {
        PlayConfig.BoardType boardType = config.getBoardType();
        int sides; // 보드의 변의 수

        double w = boardPanel.getWidth();
        double h = boardPanel.getHeight();
        if (w == 0 || h == 0) {
            Dimension preferredSize = boardPanel.getPreferredSize();
            w = preferredSize.width;
            h = preferredSize.height;
            if (w == 0 || h == 0) {
                System.err.println("Board panel size is zero, cannot init geometry.");
                return;
            }
        }

        double centerX = w / 2.0;
        double centerY = h / 2.0;
        Point centerPoint = new Point((int) centerX, (int) centerY);

        List<Point> outerPath = new ArrayList<>();
        // pathPoints[0]은 외곽 경로, pathPoints[1]부터 지름길
        // 지름길 개수는 PathConfig의 branchCount - 1 (0번 인덱스는 출발점 자체를 의미)
        int numberOfShortcuts = this.pathConfigInstance.getBranchCount() - 1;
        this.pathPoints = new List[1 + numberOfShortcuts];


        if (boardType == PlayConfig.BoardType.SQUARE) {
            sides = 4;
            double mainRadius = Math.min(w, h) * 0.4; // 사각형 모양 조정을 위한 반지름

            // 1. 외곽 경로 (20개 지점) - 시계방향, 출발점(우하단)부터
            Point v0 = new Point((int)(centerX + mainRadius), (int)(centerY + mainRadius)); // 0 (우하단, 출발)
            Point v1 = new Point((int)(centerX + mainRadius), (int)(centerY - mainRadius)); // 5 (우상단)
            Point v2 = new Point((int)(centerX - mainRadius), (int)(centerY - mainRadius)); // 10 (좌상단)
            Point v3 = new Point((int)(centerX - mainRadius), (int)(centerY + mainRadius)); // 15 (좌하단)

            outerPath.add(v0); // 0
            for (int i = 1; i <= 4; i++) outerPath.add(interpolate(v0, v1, i / 5.0)); // 1-4
            outerPath.add(v1); // 5
            for (int i = 1; i <= 4; i++) outerPath.add(interpolate(v1, v2, i / 5.0)); // 6-9
            outerPath.add(v2); // 10
            for (int i = 1; i <= 4; i++) outerPath.add(interpolate(v2, v3, i / 5.0)); // 11-14
            outerPath.add(v3); // 15
            for (int i = 1; i <= 4; i++) outerPath.add(interpolate(v3, v0, i / 5.0)); // 16-19

            this.pathPoints[0] = outerPath;

            // 지름길 1: 5번(우상단) -> 중앙 -> 15번(좌하단)
            // PathConfig에서 첫 번째 실제 지름길(인덱스 1)은 5번에서 분기
            List<Point> shortcutPath_5_to_15 = new ArrayList<>();
            shortcutPath_5_to_15.add(interpolate(v1, centerPoint, 0.33));
            shortcutPath_5_to_15.add(interpolate(v1, centerPoint, 0.66));
            shortcutPath_5_to_15.add(centerPoint);
            shortcutPath_5_to_15.add(interpolate(centerPoint, v3, 0.33));
            shortcutPath_5_to_15.add(interpolate(centerPoint, v3, 0.66));
            this.pathPoints[1] = shortcutPath_5_to_15; // PathConfig의 지름길 인덱스 1에 해당

            // 지름길 2: 10번(좌상단) -> 중앙 -> 0번(우하단/출발점)
            // PathConfig에서 두 번째 실제 지름길(인덱스 2)은 10번에서 분기
            List<Point> shortcutPath_10_to_0 = new ArrayList<>();
            shortcutPath_10_to_0.add(interpolate(v2, centerPoint, 0.33));
            shortcutPath_10_to_0.add(interpolate(v2, centerPoint, 0.66));
            shortcutPath_10_to_0.add(centerPoint);
            shortcutPath_10_to_0.add(interpolate(centerPoint, v0, 0.33));
            shortcutPath_10_to_0.add(interpolate(centerPoint, v0, 0.66));
            this.pathPoints[2] = shortcutPath_10_to_0; // PathConfig의 지름길 인덱스 2에 해당

        } else { // 오각형, 육각형
            sides = (boardType == PlayConfig.BoardType.PENTAGON) ? 5 : 6;
            double radius = Math.min(w, h) * 0.35;
            double angleOffsetRadians = (sides == 5) ? -Math.PI / 2 : ( (sides == 6) ? 0 : -Math.PI/4 ); // 기본값은 사각형 오프셋

            Point[] vertices = new Point[sides];
            for (int i = 0; i < sides; i++) {
                double angle = 2 * Math.PI * i / sides + angleOffsetRadians;
                int x = (int) (centerX + radius * Math.cos(angle));
                int y = (int) (centerY + radius * Math.sin(angle));
                vertices[i] = new Point(x, y);
            }

            // 외곽 경로 계산
            int startVertexIndex = switch (sides) {
                case 4 -> 1; // 사각형은 위에서 이미 처리됨
                case 5 -> 2;
                case 6 -> 1;
                default -> 0;
            };

            for (int i = 0; i < sides; i++) {
                int currentVertexIdx = (startVertexIndex - i + sides) % sides;
                int nextVertexIdx = (startVertexIndex - i - 1 + sides) % sides;
                Point pA = vertices[currentVertexIdx];
                Point pB = vertices[nextVertexIdx];
                for (int k = 0; k < 5; k++) {
                    outerPath.add(interpolate(pA, pB, k / 5.0));
                }
            }
            this.pathPoints[0] = outerPath;

            // 지름길 생성
            for (int scIndex = 0; scIndex < numberOfShortcuts; scIndex++) {
                // PathConfig의 branchPoint는 0(출발점), 5(첫번째모서리), 10(두번째모서리) ... 이런식
                // 실제 지름길은 5, 10, ... 에서 시작하므로, PathConfig의 지름길 인덱스는 1부터 시작
                int branchOuterStep = this.pathConfigInstance.getBranchPoint(scIndex + 1);
                // outerPath에서 해당 인덱스가 유효한지 확인
                if (branchOuterStep >= outerPath.size()) {
                    System.err.println("Error in initBoardGeometry: branchOuterStep " + branchOuterStep + " is out of bounds for outerPath size " + outerPath.size());
                    continue; // 이 지름길은 생성할 수 없음
                }
                Point shortcutStartPoint = outerPath.get(branchOuterStep);

                // 지름길의 끝점은 해당 지름길이 PathConfig에 정의된 exitOffset을 따름
                int exitOuterStepForThisSC = this.pathConfigInstance.getExitOffset(scIndex + 1);
                Point actualShortcutEndTarget; // 시각적 목표 지점

                if (exitOuterStepForThisSC > outerPath.size()) {
                    // exitOuterStepForThisSC 값이 outerPath.size()보다 큰 경우 (배열 범위를 벗어남)
                    System.err.println("Critical Error in initBoardGeometry: exitOuterStepForThisSC " + exitOuterStepForThisSC +
                            " is strictly greater than outerPath size " + outerPath.size() + ". Skipping this shortcut.");
                    continue; // 이 지름길은 생성할 수 없음
                } else if (exitOuterStepForThisSC == outerPath.size()) {
                    // exitOuterStepForThisSC 값이 outerPath.size()와 같은 경우 (예: outerLength와 같음)
                    // 이는 논리적으로 "완주"를 의미할 수 있으며, 시각적으로는 출발/도착점(인덱스 0)을 향하도록 처리
                    System.out.println("Info in initBoardGeometry: exitOuterStepForThisSC " + exitOuterStepForThisSC +
                            " equals outerPath size " + outerPath.size() +
                            ". Visually targeting outerPath.get(0) for shortcut exit.");
                    if (outerPath.isEmpty()) { // 예외적인 경우 방어
                        System.err.println("Error: outerPath is empty when trying to handle exitOuterStepForThisSC == outerPath.size(). Skipping shortcut.");
                        continue;
                    }
                    actualShortcutEndTarget = outerPath.get(0); // 출발/도착점을 시각적 목표로 설정
                } else {
                    // exitOuterStepForThisSC < outerPath.size() 인 경우 (유효한 인덱스)
                    actualShortcutEndTarget = outerPath.get(exitOuterStepForThisSC);
                }

                List<Point> shortcutPath = new ArrayList<>();
                // 지름길은 5개의 점으로 구성: 시작점쪽 2개, 중앙 1개, 끝점쪽 2개
                shortcutPath.add(interpolate(shortcutStartPoint, centerPoint, 0.33));
                shortcutPath.add(interpolate(shortcutStartPoint, centerPoint, 0.66));
                shortcutPath.add(centerPoint); // 중앙점
                shortcutPath.add(interpolate(centerPoint, actualShortcutEndTarget, 0.33));
                shortcutPath.add(interpolate(centerPoint, actualShortcutEndTarget, 0.66));

                if (this.pathPoints.length > scIndex + 1) { // 배열 범위 확인
                    this.pathPoints[scIndex + 1] = shortcutPath;
                } else {
                    System.err.println("Error in initBoardGeometry: scIndex+1 " + (scIndex+1) +
                            " is out of bounds for pathPoints length " + this.pathPoints.length);
                }
            }
        }
    }

    /** pathPoints를 기반으로 윷판 UI(버튼)를 생성하고 배치 */
    private void initBoardUI() {
        if (pathPoints == null) {
            System.err.println("initBoardUI: pathPoints가 null입니다. initBoardGeometry()를 먼저 호출해야 합니다.");
            return;
        }

        // 기존 버튼들 제거 (componentResized 등에서 재호출될 수 있으므로)
        boardPanel.removeAll();

        panButtons = new JButton[pathPoints.length][];
        for (int pathIdx = 0; pathIdx < pathPoints.length; pathIdx++) {
            List<Point> currentPathCoords = pathPoints[pathIdx];
            if (currentPathCoords == null) { // 해당 경로가 정의되지 않았으면 건너뜀
                System.err.println("initBoardUI: pathPoints[" + pathIdx + "] is null.");
                continue;
            }
            panButtons[pathIdx] = new JButton[currentPathCoords.size()];
            for (int stepIdx = 0; stepIdx < currentPathCoords.size(); stepIdx++) {
                Point p = currentPathCoords.get(stepIdx);
                String iconName;

                boolean isOuterPath = (pathIdx == 0);
                // 출발/도착점 (외곽 경로의 0번 인덱스)
                boolean isStartFinishPoint = isOuterPath && stepIdx == 0;
                // 꼭짓점 (외곽 경로의 각 변이 시작되는 지점, 출발/도착점 제외)
                boolean isVertex = isOuterPath && (stepIdx % 5 == 0) && !isStartFinishPoint;
                // 중앙 노드 (지름길의 중앙, pathConfigInstance.getMergeStep() 인덱스에 해당)
                boolean isCenterNode = !isOuterPath && (stepIdx == this.pathConfigInstance.getMergeStep());

                if (isStartFinishPoint) {
                    iconName = "startcircle.jpg";
                } else if (isVertex || isCenterNode) {
                    iconName = "bigcircle.jpg";
                } else {
                    iconName = "circle.jpg";
                }

                ImageIcon icon = loadIcon(IMG_ROOT + iconName);
                JButton btn = createBoardButton(icon, p.x, p.y);

                final int fPathIdx = pathIdx;
                final int fStepIdx = stepIdx;
                btn.addActionListener(e -> {
                    if (!canMove) {
                        statusLabel.setText("먼저 윷을 던져주세요!");
                        return;
                    }
                    GameState currentState = controller.getState(); // 항상 최신 상태 참조
                    if (currentState.getLastThrow() != null && currentState.getLastThrow().size() > 1) {
                        Object[] yutOptions = currentState.getLastThrow().stream()
                                .map(Yut.Result::toString).toArray();
                        int choice = JOptionPane.showOptionDialog(this,
                                "어떤 윷으로 이동하시겠습니까?", "이동할 윷 선택",
                                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                                null, yutOptions, yutOptions[0]);
                        if (choice == JOptionPane.CLOSED_OPTION) return; // 사용자가 창을 닫은 경우
                        currentState.setSelect(choice); // GameState에 선택된 윷 결과 인덱스 저장
                    } else if (currentState.getLastThrow() != null && currentState.getLastThrow().size() == 1){
                        currentState.setSelect(0); // 결과가 하나면 자동 선택 (인덱스 0)
                    }
                    // GameController의 기존 메소드명 사용
                    controller.onSelectPiece(fPathIdx, fStepIdx);
                });
                boardPanel.add(btn);
                panButtons[pathIdx][stepIdx] = btn;
            }
        }
        boardPanel.revalidate(); // 버튼 추가 후 패널 갱신
        boardPanel.repaint();
    }

    @Override
    public void updateBoard(GameState state) {
        SwingUtilities.invokeLater(() -> {
            if (panButtons == null || pathPoints == null) { // UI가 아직 준비되지 않았다면
                if(boardPanel.getWidth() > 0 && boardPanel.getHeight() > 0 && (pathPoints == null || panButtons == null) ) {
                    // 창은 그려졌는데, 내부 요소들이 초기화 안된 경우 시도
                    initBoardGeometry();
                    initBoardUI();
                }
                if (panButtons == null) { // 그래도 null이면 진행 불가
                    System.err.println("updateBoard: panButtons is null, cannot update.");
                    return;
                }
            }

            pieceActionPanel.removeAll(); // 하단 패널의 기존 버튼들 제거
            JButton newPieceBtn = new JButton("새 말 꺼내기");
            newPieceBtn.addActionListener(e -> {
                GameState currentState = controller.getState(); // 최신 상태 가져오기
                if (currentState.getLastThrow() != null && currentState.getLastThrow().size() > 1) {
                    Object[] yutOptions = currentState.getLastThrow().stream().map(Yut.Result::toString).toArray();
                    int choice = JOptionPane.showOptionDialog(this,
                            "어떤 윷으로 새 말을 이동하시겠습니까?", "새 말 이동 윷 선택",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                            null, yutOptions, yutOptions[0]);
                    if (choice == JOptionPane.CLOSED_OPTION) return;
                    currentState.setSelect(choice);
                } else if (currentState.getLastThrow() != null && currentState.getLastThrow().size() == 1){
                    currentState.setSelect(0);
                }
                controller.deployNewPiece();
            });
            pieceActionPanel.add(newPieceBtn);

            // 모든 칸의 버튼을 기본 아이콘으로 초기화하고 활성화 상태 설정
            for (int pIdx = 0; pIdx < panButtons.length; pIdx++) {
                if (panButtons[pIdx] == null) continue;
                for (int sIdx = 0; sIdx < panButtons[pIdx].length; sIdx++) {
                    if (panButtons[pIdx][sIdx] == null) continue; // 해당 버튼이 생성되지 않았으면 건너뜀

                    String baseIconName;
                    boolean isOuterPath = (pIdx == 0);
                    boolean isStartFinishPoint = isOuterPath && sIdx == 0;
                    boolean isVertex = isOuterPath && (sIdx % 5 == 0) && !isStartFinishPoint;
                    boolean isCenterNode = !isOuterPath && (sIdx == this.pathConfigInstance.getMergeStep());

                    if (isStartFinishPoint) {
                        baseIconName = "startcircle.jpg";
                    } else if (isVertex || isCenterNode) {
                        baseIconName = "bigcircle.jpg";
                    } else {
                        baseIconName = "circle.jpg";
                    }
                    panButtons[pIdx][sIdx].setIcon(loadIcon(IMG_ROOT + baseIconName));
                    // 말 선택 가능 상태(canMove)에 따라 버튼 활성화
                    // 추가적으로, 해당 위치에 현재 플레이어의 말이 있거나, 새 말을 놓을 수 있는 출발점인 경우 등
                    // 더 구체적인 활성화 조건은 GameController 또는 GameState에서 판단 정보를 받아올 수 있음
                }
            }

            // 각 플레이어의 말들을 보드에 표시 (업힌 말 고려)
            // 위치별 말 스택 수 계산 (같은 플레이어의 말만)
            Map<String, Map<Player, Integer>> pieceStackCounts = new HashMap<>();
            for (Player player : state.getPlayers()) {
                for (Piece piece : player.getPieces()) {
                    if (piece.isFinished() || piece.getPathIndex() < 0 || piece.getStepIndex() < 0) continue;
                    String locationKey = piece.getPathIndex() + "-" + piece.getStepIndex();
                    pieceStackCounts.computeIfAbsent(locationKey, k -> new HashMap<>())
                            .merge(player, 1, Integer::sum); // 해당 위치, 해당 플레이어의 말 개수 +1
                }
            }

            for (Player player : state.getPlayers()) {
                for (Piece piece : player.getPieces()) {
                    if (piece.isFinished() || piece.getPathIndex() < 0 || piece.getStepIndex() < 0) continue;

                    // panButtons 배열 범위 체크
                    if (piece.getPathIndex() >= panButtons.length || panButtons[piece.getPathIndex()] == null ||
                            piece.getStepIndex() >= panButtons[piece.getPathIndex()].length ||
                            panButtons[piece.getPathIndex()][piece.getStepIndex()] == null) {
                        System.err.printf("잘못된 말 위치 참조: Player %d, Piece %d, Path %d, Step %d\n",
                                player.getId(), piece.getId(), piece.getPathIndex(), piece.getStepIndex());
                        continue;
                    }

                    String locationKey = piece.getPathIndex() + "-" + piece.getStepIndex();
                    int displayStack = pieceStackCounts.getOrDefault(locationKey, new HashMap<>())
                            .getOrDefault(player, 1);

                    displayStack = Math.min(displayStack, 5); // 이미지 파일은 5스택까지만 있다고 가정
                    if (displayStack <= 0) displayStack = 1; // 최소 1개

                    // 말 이미지 파일명 결정 로직
                    String iconFileName;
                    boolean isOuterPath = (piece.getPathIndex() == 0);
                    // 출발/도착점은 꼭짓점 정의에서 제외됨 (isVertex 조건 참고)
                    boolean isStartFinishPoint = isOuterPath && (piece.getStepIndex() == 0);
                    // 꼭짓점: 외곽 경로이면서, 5의 배수 번째 스텝이고, 출발/도착점이 아닌 경우
                    boolean isVertex = isOuterPath && (piece.getStepIndex() % 5 == 0) && !isStartFinishPoint;
                    // 중앙 노드: 외곽 경로가 아니면서, 해당 지름길의 중앙 스텝인 경우
                    boolean isCenterNode = !isOuterPath && (piece.getStepIndex() == 2);

                    if (isVertex || isCenterNode) {
                        // 꼭짓점 또는 중앙일 경우 "big" 이미지를 사용 (예: "bigblue1.jpg")
                        iconFileName = "big" + player.getColor() + displayStack + ".jpg";
                        System.out.println(iconFileName);
                    } else {
                        // 일반적인 경우 (예: "blue1.jpg")
                        iconFileName = player.getColor() + displayStack + ".jpg";
                    }

                    // for 중앙 노드에서 말이 사라지는 버그
                    ImageIcon pieceIcon = loadIcon(IMG_ROOT + iconFileName);
                    JButton targetButton = panButtons[piece.getPathIndex()][piece.getStepIndex()];
                    targetButton.setIcon(pieceIcon);
                    if (isCenterNode) {
                        System.out.println("Bringing central button to front.");
                        boardPanel.setComponentZOrder(targetButton, 0); // 0이 가장 위
                    }
                }
            }

            // GameState의 phase enum 케이싱
            if (state.getPhase() == GameState.phase.THROW) {
                // 1. 기본 메시지를 변수에 먼저 저장
                String baseMessage = "Player " + state.getCurrentPlayer().getId() + ": 윷을 던지세요. (남은 횟수: " + state.getThrowCount() + ")";

                // 2. 조건에 따라 앞에 붙일 추가 안내 메시지를 설정
                String prefix = "";
                Yut.Result lastThrownResult = state.getLastThrow().isEmpty() ? null : state.getLastThrow().get(state.getLastThrow().size() - 1);

                if (state.getLastTurnEvent() == GameState.TurnEvent.YUT_OR_MO_THROWN && lastThrownResult != null) {
                    prefix = "'" + lastThrownResult.toString() + "'이(가) 나와 한 번 더 던지세요!\n";
                } else if (state.getLastTurnEvent() == GameState.TurnEvent.CAPTURE_OCCURRED) {
                    prefix = "상대 말을 잡았습니다! 한 번 더 던지세요.\n";
                }

                // 3. 최종적으로 조합된 메시지를 statusLabel에 한 번만 설정
                statusLabel.setText(prefix + baseMessage);

                enableYutButtons(true);
                newPieceBtn.setEnabled(false); // 윷 던지기 페이즈에는 새 말 꺼내기 비활성화
                canMove = false; // 아직 말 선택 불가
            } else if (state.getPhase() == GameState.phase.MOVE) {
                String yutResultsStr = state.getLastThrow().stream()
                        .map(Yut.Result::toString).collect(Collectors.joining(", "));
                statusLabel.setText("Player " + state.getCurrentPlayer().getId() + ": 말을 선택하세요. (결과: " + yutResultsStr + ")");
                enableYutButtons(false);
                // 새 말 꺼내기 버튼은 윷 던지기 결과가 있을 때만 활성화
                newPieceBtn.setEnabled(state.getLastThrow() != null && !state.getLastThrow().isEmpty());
                canMove = true; // 말 선택 가능
            }

            // GameState에 getWinner()가 있고, winner가 null이 아니면 게임 종료 처리
            if (state.isGameOver() && state.getWinner() != null) {
                statusLabel.setText("Player " + state.getWinner().getId() + " ("+ state.getWinner().getColor()+")"+" 승리! 게임 종료.");
                enableYutButtons(false);
                newPieceBtn.setEnabled(false); // 새 말 꺼내기 버튼 비활성화 확인
                canMove = false;

                // pieceActionPanel에 기존 버튼이 있다면 제거 (예: "새 말 꺼내기")
                pieceActionPanel.removeAll();

                JButton restartButton = new JButton("게임 재시작");
                restartButton.addActionListener(e -> controller.restartGame());
                pieceActionPanel.add(restartButton);

                JButton exitButton = new JButton("게임 종료");
                exitButton.addActionListener(e -> System.exit(0)); // 게임 창만 닫기
                pieceActionPanel.add(exitButton);

                pieceActionPanel.revalidate();
                pieceActionPanel.repaint();
            } else {
                // 게임 종료 상태가 아닐 경우 "새 말 꺼내기" 버튼 등이 다시 표시되도록 처리
                // 예를 들어, newPieceBtn이 제거되었거나 현재 보이지 않는 경우 다시 추가
                if (pieceActionPanel.getComponentCount() == 0 && state.getPhase() == GameState.phase.MOVE) {
                    newPieceBtn.setEnabled(state.getLastThrow() != null && !state.getLastThrow().isEmpty());
                    pieceActionPanel.add(newPieceBtn);
                    pieceActionPanel.revalidate();
                    pieceActionPanel.repaint();
                }
            }

            updateInfoPanel(state); // 플레이어 정보 패널 업데이트

            boardPanel.revalidate(); boardPanel.repaint();
            pieceActionPanel.revalidate(); pieceActionPanel.repaint();
            // infoPanel도 내용 변경 시 revalidate/repaint 필요 (updateInfoPanel 내부에서 처리)
        });
    }

    /** 플레이어 정보 패널(우측)을 현재 게임 상태에 따라 업데이트 */
    private void updateInfoPanel(GameState state) {
        infoPanel.removeAll(); // 기존 정보 제거

        JLabel titleLabel = new JLabel("플레이어 정보");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // 중앙 정렬
        infoPanel.add(titleLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 10))); // 간격

        for (Player player : state.getPlayers()) {
            JPanel playerBox = new JPanel();
            playerBox.setLayout(new BoxLayout(playerBox, BoxLayout.Y_AXIS));
            // 플레이어 ID와 색상을 제목으로 사용
            playerBox.setBorder(BorderFactory.createTitledBorder("Player " + player.getId() + " (" + player.getColor() + ")"));
            playerBox.setAlignmentX(Component.LEFT_ALIGNMENT);

            // 현재 턴 플레이어 표시 (테두리 제목 색상 변경)
            if (player == state.getCurrentPlayer() && !(state.isGameOver() && state.getWinner() != null) ) {
                ((javax.swing.border.TitledBorder)playerBox.getBorder()).setTitleColor(Color.BLUE);
            }


            // 1. 플레이어가 사용하는 말 이미지
            ImageIcon playerIcon = loadIcon(IMG_ROOT + player.getColor() + ".jpg"); // 예: blue.jpg
            JLabel iconLabel = new JLabel(playerIcon != null ? playerIcon : createPlaceholderIcon(Color.LIGHT_GRAY, 20,20)); // null 방지
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            playerBox.add(iconLabel);
            playerBox.add(Box.createRigidArea(new Dimension(0, 5)));

            // 2. 남은 말 개수 (윷판에 아직 올라가지 않은 말)
            long piecesNotYetOnBoard = player.getPieces().stream()
                    .filter(p -> p.getPathIndex() == -1 && !p.isFinished())
                    .count();
            JLabel remainingPiecesLabel = new JLabel("대기 중인 말: " + piecesNotYetOnBoard);
            remainingPiecesLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            playerBox.add(remainingPiecesLabel);

            // 3. 점수 (완주한 말 개수)
            long score = player.getPieces().stream().filter(Piece::isFinished).count();
            JLabel scoreLabel = new JLabel("완주한 말: " + score + " / " + config.getPieceCount()); // 전체 말 개수 함께 표시
            scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            playerBox.add(scoreLabel);

            infoPanel.add(playerBox);
            infoPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }
        infoPanel.revalidate();
        infoPanel.repaint();
    }


    @Override
    public void showThrowResult(Yut.Result result) { // GameView 인터페이스와 시그니처 일치
        SwingUtilities.invokeLater(() -> {
            System.out.println("윷 던짐 결과 (View): " + result);
            updateBoard(controller.getState()); // 상태 변경에 따른 전체 UI 업데이트
        });
    }

    @Override
    public void showWinner(Player winner) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                    "Player " + winner.getId() + " (" + winner.getColor() + ") 님 승리!",
                    "게임 종료", JOptionPane.INFORMATION_MESSAGE);
            // GameState에 winner 정보가 설정된 후 updateBoard가 호출되어 UI 최종 정리
            updateBoard(controller.getState());
        });
    }

    @Override
    public void closeGameView() {
        this.dispose();
    }

    @Override
    public void showInitialSetup() {
        // FirstPage를 다시 표시
        SwingUtilities.invokeLater(FirstPage::new);
    }

    /** 이미지 리소스 로드 헬퍼 (null 반환 가능성 처리) */
    private ImageIcon loadIcon(String path) {
        URL url = getClass().getClassLoader().getResource(path);
        if (url == null) {
            System.err.println("아이콘 파일 누락 또는 경로 문제: " + path);
            return createPlaceholderIcon(Color.GRAY, buttonSize, buttonSize);
        }
        return new ImageIcon(url);
    }

    /** 플레이스홀더 아이콘 생성 (이미지 로드 실패 시) */
    private ImageIcon createPlaceholderIcon(Color color, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(color);
        g2d.fillOval(0, 0, width, height); // 원형 플레이스홀더
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawOval(0, 0, width - 1, height - 1);
        g2d.dispose();
        return new ImageIcon(img);
    }

    /** 보드 버튼 생성 헬퍼 (버튼 크기 고정, 아이콘 크기에 따라 스케일링) */
    private JButton createBoardButton(ImageIcon icon, int centerX, int centerY) {
        JButton btn = new JButton();
        if (icon != null && icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
            btn.setIcon(icon);
            // 버튼 크기를 아이콘 크기로 설정
            btn.setSize(icon.getIconWidth(), icon.getIconHeight());
            // 버튼 위치는 (centerX, centerY)를 아이콘의 중심으로 하여 설정
            btn.setLocation(centerX - icon.getIconWidth() / 2, centerY - icon.getIconHeight() / 2);
        } else {
            // 아이콘 로드 실패 시, 기본 크기 및 플레이스홀더 설정
            int defaultSize = 20; // 아이콘 없을 때 기본 크기
            btn.setSize(defaultSize, defaultSize);
            btn.setLocation(centerX - defaultSize / 2, centerY - defaultSize / 2);
            btn.setIcon(createPlaceholderIcon(Color.LIGHT_GRAY, defaultSize, defaultSize));
        }
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setMargin(new Insets(0,0,0,0));
        return btn;
    }

    /** 두 점 사이를 t 비율로 내분하는 점을 반환 */
    private Point interpolate(Point p1, Point p2, double t) {
        return new Point((int) (p1.x * (1 - t) + p2.x * t), (int) (p1.y * (1 - t) + p2.y * t));
    }

    /** 윷 던지기 관련 버튼 활성화/비활성화 */
    private void enableYutButtons(boolean enable) {
        rndBtn.setEnabled(enable);
        specBtn.setEnabled(enable);
        yutComboBox.setEnabled(enable);
    }
}