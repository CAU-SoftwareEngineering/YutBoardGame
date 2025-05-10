import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * 초기 설정 화면: 플레이어 수, 말 개수, 판 종류 선택
 */
public class FirstPage extends JFrame {
    private static final int MAX_PLAYER = 4;
    private static final int MAX_PIECE  = 5;
    private static final String[] BOARD_LABELS = {"사각형", "오각형", "육각형"};

    private final PlayConfig playConfig = new PlayConfig();
    private final PlayerAdapter playerAdapter = new PlayerAdapter(playConfig);
    private final PieceAdapter pieceAdapter   = new PieceAdapter(playConfig);

    public FirstPage() {
        super("초기 설정");
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        // 플레이어 수 라디오버튼
        panel.add(Box.createHorizontalStrut(20));
        panel.add(new JLabel("사용자 수:"));
        ButtonGroup pg = new ButtonGroup();
        for (int i = 2; i <= MAX_PLAYER; i++) {
            JRadioButton btn = new JRadioButton(String.valueOf(i));
            btn.addActionListener(playerAdapter);
            pg.add(btn);
            panel.add(btn);
            if (i == playConfig.getPlayerCount()) btn.setSelected(true);
        }

        panel.add(Box.createHorizontalStrut(20));
        // 말 개수 라디오버튼
        panel.add(new JLabel("말 갯수:"));
        ButtonGroup pcg = new ButtonGroup();
        for (int i = 2; i <= MAX_PIECE; i++) {
            JRadioButton btn = new JRadioButton(String.valueOf(i));
            btn.addActionListener(pieceAdapter);
            pcg.add(btn);
            panel.add(btn);
            if (i == playConfig.getPieceCount()) btn.setSelected(true);
        }

        panel.add(Box.createHorizontalStrut(20));
        // 판 종류 라디오버튼
        panel.add(new JLabel("판 종류:"));
        ButtonGroup bg = new ButtonGroup();
        for (String label : BOARD_LABELS) {
            JRadioButton btn = new JRadioButton(label);
            btn.addActionListener(e -> playConfig.setBoardType(btn.getText()));
            bg.add(btn);
            panel.add(btn);
            if (label.equals("사각형")) btn.setSelected(true);
        }

        panel.add(Box.createHorizontalStrut(20));
        // 시작 버튼
        JButton start = new JButton("시작");
        start.addActionListener(e -> {
            // 플레이어 이름 자동 생성
            int pc = playConfig.getPlayerCount();
            List<String> names = new ArrayList<>();
            for (int i = 1; i <= pc; i++) names.add("Player" + i);
            // 새 게임 화면 열기
            new YutBoard(playConfig, names);
            dispose();
        });
        panel.add(start);

        getContentPane().add(panel);
        pack();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(FirstPage::new);
    }
}