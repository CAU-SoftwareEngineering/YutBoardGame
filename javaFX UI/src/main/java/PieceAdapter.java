import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JRadioButton;

/**
 * FirstPage에서 “말 개수” 라디오 버튼 클릭을
 * PlayConfig에 반영합니다.
 */
public class PieceAdapter implements ActionListener {
    private final PlayConfig config;

    public PieceAdapter(PlayConfig config) {
        this.config = config;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JRadioButton btn = (JRadioButton) e.getSource();
        int pieceCount = Integer.parseInt(btn.getText());
        config.setPieceCount(pieceCount);
        // System.out.println("Piece count set to " + pieceCount);
    }
}