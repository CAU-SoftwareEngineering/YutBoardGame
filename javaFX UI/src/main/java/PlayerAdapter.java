import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JRadioButton;

/**
 * FirstPage에서 “플레이어 수” 라디오 버튼 클릭을
 * PlayConfig에 반영합니다.
 */
public class PlayerAdapter implements ActionListener {
    private final PlayConfig config;

    public PlayerAdapter(PlayConfig config) {
        this.config = config;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JRadioButton btn = (JRadioButton) e.getSource();
        int playerCount = Integer.parseInt(btn.getText());
        config.setPlayerCount(playerCount);
        // System.out.println("Player count set to " + playerCount);
    }
}