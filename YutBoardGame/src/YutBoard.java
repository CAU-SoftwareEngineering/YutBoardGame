import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 게임 화면: 논리적 경로(path + step) 기반 버튼 배열과
 * 실제 픽셀 좌표 배치를 분리하여 구현한 Yut Nori 보드.
 */
public class YutBoard extends JFrame implements GameView {
    private static final String IMG_ROOT = "img/";

    private final PlayConfig config;         // 게임 설정
    private final GameController controller; // 게임 컨트롤러

    private final JPanel boardPanel;         // 보드 그래픽 패널
    private final JPanel piecePanel;         // 말 선택 패널
    private final JLabel statusLabel;        // 상태/결과 표시 레이블
    private final JPanel infoPanel;          // 우측 플레이어 정보 패널

    private List<Point>[] pathPoints;
    private JButton[][] panButtons;
    private final int buttonSize = 30;
    private boolean canMove = false;         // 윷 던진 후에만 말 이동 허용

    /**
     * @param config       PlayConfig 객체
     * @param playerNames  플레이어 이름 목록
     */
    @SuppressWarnings("unchecked")
    public YutBoard(PlayConfig config, List<String> playerNames) {
        super("윷놀이 게임");
        this.config = config;
        this.controller = new GameController(config, playerNames, this);

        setLayout(new BorderLayout());

        // --- 중앙 보드 패널 ---
        boardPanel = new JPanel(null) {
            private final Image bg = loadIcon(IMG_ROOT + "background.png").getImage();
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(bg, 0, 0, getWidth(), getHeight(), null);
            }
        };
        boardPanel.setPreferredSize(new Dimension(600, 600));
        boardPanel.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                boardPanel.removeAll();
                initBoardGeometry();
                initBoardUI();
                boardPanel.revalidate();
                boardPanel.repaint();
            }
        });
        add(boardPanel, BorderLayout.CENTER);

        // --- 우측 플레이어 정보 패널 ---
        infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setPreferredSize(new Dimension(200, 600));
        add(infoPanel, BorderLayout.EAST);

        // --- 하단 말 선택 및 상태 ---
        JPanel bottom = new JPanel(new BorderLayout());
        piecePanel = new JPanel();
        statusLabel = new JLabel("윷을 던져주세요.", SwingConstants.CENTER);
        bottom.add(piecePanel, BorderLayout.CENTER);
        bottom.add(statusLabel, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        // 윷 던지기 버튼
        JPanel top = new JPanel();
        JButton rndBtn = new JButton("랜덤 윷 던지기");
        JComboBox<Yut.Result> combo = new JComboBox<>(Yut.Result.values());
        JButton specBtn = new JButton("지정 윷 던지기");
        rndBtn.addActionListener(e -> controller.onThrowRandom());
        specBtn.addActionListener(e -> controller.onThrowSpecified((Yut.Result)combo.getSelectedItem()));
        top.add(rndBtn); top.add(combo); top.add(specBtn);
        add(top, BorderLayout.NORTH);

        // 초기 지오메트리 및 UI 생성
        initBoardGeometry();
        initBoardUI();

        pack();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);

        controller.startGame();
    }

    /**
     * 보드상의 논리적 좌표 계산:
     * - 외곽 경로: sides*5칸 (각 변을 5등분)
     * - 지름길: each branch point → 중심까지 5단계
     */
    @SuppressWarnings("unchecked")
    private void initBoardGeometry() {
        int sides = switch (config.getBoardType()) {
            case PENTAGON -> 5;
            case HEXAGON  -> 6;
            default       -> 4;
        };

        double w = boardPanel.getWidth();
        double h = boardPanel.getHeight();
        double cx = w / 2.0, cy = h / 2.0;
        double r = Math.min(w, h) / 3.5;
        Point center = new Point((int) cx, (int) cy);

        // 꼭짓점 좌표 계산
        Point[] vertices = new Point[sides];
        double angleOffset = (sides == 4) ? -Math.PI / 4
                : (sides == 6) ? 0
                : -Math.PI / 2;
        for (int i = 0; i < sides; i++) {
            double angle = 2 * Math.PI * i / sides + angleOffset;
            int x = (int) (cx + r * Math.cos(angle));
            int y = (int) (cy + r * Math.sin(angle));
            vertices[i] = new Point(x, y);
        }

        // 외곽 경로 계산 (각 변 5등분)
        List<Point> outer = new ArrayList<>();
        // 시작점이 오른쪽 아래가 되도록 꼭짓점 배열을 시계 방향으로 회전
        int startVertexIndex = switch (sides) {
            case 4 -> 1; // 사각형의 오른쪽 아래 꼭짓점 (0~3 기준)
            case 5 -> 2; // 오각형의 오른쪽 아래에 해당하는 꼭짓점
            case 6 -> 1; // 육각형의 오른쪽 아래
            default -> 0;
        };

        for (int i = 0; i < sides; i++) {
            // 시계방향: startVertexIndex에서 i만큼 뒤로(역순) 이동
            int idxA = (startVertexIndex - i + sides) % sides;
            int idxB = (startVertexIndex - i - 1 + sides) % sides;
            Point A = vertices[idxA];
            Point B = vertices[idxB];
            for (int k = 0; k < 5; k++) {
                double t = k / 5.0;
                outer.add(interp(A, B, t));
            }
        }

        // 지름길 생성 (sides - 2개): A → Center → B (총 5스텝)
        int shortcutCount = sides - 2;
        List<Point>[] pts = new List[1 + shortcutCount];
        pts[0] = outer; // 외곽 경로

        for (int i = 1; i <= shortcutCount; i++) {
            int branchIdx = i * 5;
            Point A = outer.get(branchIdx);

            // 반대쪽 꼭짓점 인덱스
            int oppIdx = ((i + sides / 2) % sides) * 5;
            Point B = outer.get(oppIdx);

            List<Point> shortcut = new ArrayList<>();
            // A → Center (2스텝)
            shortcut.add(interp(A, center, 1.0 / 3));
            shortcut.add(interp(A, center, 2.0 / 3));
            // Center (중앙 노드)
            shortcut.add(center);
            // Center → B (2스텝)
            shortcut.add(interp(center, B, 1.0 / 3));
            shortcut.add(interp(center, B, 2.0 / 3));

            pts[i] = shortcut;
        }

        this.pathPoints = pts;
    }

    /** pathPoints 기반으로 panButtons 생성 및 배치 */
    private void initBoardUI() {
        panButtons = new JButton[pathPoints.length][];
        for(int pi=0;pi<pathPoints.length;pi++){
            List<Point> pts=pathPoints[pi];
            panButtons[pi]=new JButton[pts.size()];
            for(int si=0;si<pts.size();si++){
                Point p=pts.get(si);
                String icon=(pi==0&&si==0)?"startcircle.jpg":((pi==0&&si%5==0)||(pi>0&&si==2))?"bigcircle.jpg":"circle.jpg";
                JButton b=createBoardBtn(loadIcon(IMG_ROOT+icon),p.x-buttonSize/2,p.y-buttonSize/2,buttonSize,buttonSize);
                final int fpi=pi, fsi=si;
                b.addActionListener(e->{ if(!canMove) return; controller.onSelectPiece(fpi,fsi); canMove=false; updateBoard(controller.getState()); });
                boardPanel.add(b);
                panButtons[pi][si]=b;
            }
        }
    }

    @Override
    public void updateBoard(GameState state) {
        SwingUtilities.invokeLater(() -> {
            piecePanel.removeAll();
            Map<String, Integer> stackMap = new HashMap<>();
            for (Piece q : state.getCurrentPlayer().getPieces()) {
                if (q.isFinished()) continue;
                String key = q.getPathIndex() + "-" + q.getStepIndex();
                stackMap.put(key, stackMap.getOrDefault(key, 0) + 1);
            }

            JButton newPieceBtn = new JButton("새 말 꺼내기");
            newPieceBtn.addActionListener(e -> controller.deployNewPiece());
            piecePanel.add(newPieceBtn);

            // 모든 칸에 기본 보드 아이콘(흰 원 / 큰 원 / 시작 원) 복원
            for (int pi = 0; pi < panButtons.length; pi++) {
                for (int si = 0; si < panButtons[pi].length; si++) {
                    String baseIcon;
                    if (pi == 0 && si == 0) {
                        baseIcon = "startcircle.jpg";            // 시작 칸
                    }
                    else if ((pi == 0 && si % 5 == 0)           // 외곽 모서리
                            || (pi > 0 && si == 2)) {             // 지름길 합류 지점
                        baseIcon = "bigcircle.jpg";              // 큰 원
                    }
                    else {
                        baseIcon = "circle.jpg";                 // 일반 원
                    }
                    panButtons[pi][si].setIcon(loadIcon(IMG_ROOT + baseIcon));
                    panButtons[pi][si].setEnabled(canMove); // 활성/비활성 설정
                }
            }

            for (Player p : state.getPlayers()) {
                for (Piece piece : p.getPieces()) {
                    if (piece.isFinished()) continue;
                    int pi = piece.getPathIndex();
                    int si = piece.getStepIndex();
                    if (pi < 0 || si < 0) continue;
                    String key = pi + "-" + si;
                    int stack = stackMap.getOrDefault(key, 1);
                    String color = piece.getOwner().getColor();
                    String prefix = (pi == 0 && si == 0) ? "start" :
                            ((pi == 0 && si % 5 == 0) || (pi > 0 && si == 2)) ? "big" : "";
                    String iconName = prefix + color + stack + ".jpg";
                    panButtons[pi][si].setIcon(loadIcon(IMG_ROOT + iconName));
                }
            }
            statusLabel.setText("Player " + state.getCurrentPlayer().getId() + " | Result: " + state.getLastThrow());
            piecePanel.revalidate();
            piecePanel.repaint();
        });
    }

    private void updateInfoPanel(GameState state){
        infoPanel.removeAll();
        for(Player p: state.getPlayers()){
            JPanel pP=new JPanel(new FlowLayout(FlowLayout.LEFT));
            // 말 이미지
            for(Piece pc: p.getPieces()) if(!pc.isFinished()) pP.add(new JLabel(loadIcon(IMG_ROOT+(p.getId()%4==0?"blue":"red")+".jpg")));
            pP.add(new JLabel("남은말: "+p.getPieces().stream().filter(q->!q.isFinished()).count()));
            pP.add(new JLabel("포인트: "+p.getPieces().stream().filter(Piece::isFinished).count()));
            infoPanel.add(pP);
        }
        infoPanel.revalidate(); infoPanel.repaint();
    }


    @Override
    public void showThrowResult(Yut.Result result) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Result: " + result);
            // 윷 던진 후에야 말 이동 허용
            canMove = true;
            updateBoard(controller.getState());
        });
    }

    @Override
    public void showWinner(Player winner) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, winner.getId() + " 승리!")
        );
    }

    /** 이미지 리소스 로드 헬퍼 */
    private ImageIcon loadIcon(String path) {
        URL url = getClass().getClassLoader().getResource(path);
        if (url == null) throw new RuntimeException(path + " not found");
        return new ImageIcon(url);
    }

    /** 보드 버튼 생성 헬퍼 */
    private JButton createBoardBtn(
            ImageIcon icon, int x, int y, int w, int h
    ) {
        JButton btn = new JButton(icon);
        btn.setBounds(x, y, w, h);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        return btn;
    }

    /** A와 B를 t:(1-t) 비율로 보간 */
    private Point interp(Point A, Point B, double t) {
        int x = (int)(A.x * (1 - t) + B.x * t);
        int y = (int)(A.y * (1 - t) + B.y * t);
        return new Point(x, y);
    }
}