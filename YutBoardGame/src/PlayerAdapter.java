import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PlayerAdapter implements ActionListener {
    private PlayConfig config;

    public PlayerAdapter(PlayConfig config) {
        // 초기 설정 객체 주입
        this.config = config;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // 선택된 라디오 버튼에서 텍스트 추출 → 플레이어 수로 설정
    }
}