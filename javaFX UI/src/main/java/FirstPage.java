import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class FirstPage extends Application {
    private static final int MAX_PLAYER = 4;
    private static final int MIN_PLAYER = 2;
    private static final int MAX_PIECE = 5;
    private static final int MIN_PIECE = 2;
    private static final String[] BOARD_LABELS = {"사각형", "오각형", "육각형"};

    private final PlayConfig playConfig = new PlayConfig(); //

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("윷놀이 초기 설정 (JavaFX)");

        VBox root = new VBox(20);
        root.setPadding(new Insets(25, 25, 25, 25));
        root.setAlignment(Pos.CENTER);

        // --- 플레이어 수 선택 ---
        HBox playerCountBox = new HBox(10);
        playerCountBox.setAlignment(Pos.CENTER_LEFT);
        Label playerLabel = new Label("사용자 수:");
        ToggleGroup playerToggleGroup = new ToggleGroup();
        for (int i = MIN_PLAYER; i <= MAX_PLAYER; i++) {
            RadioButton btn = new RadioButton(String.valueOf(i));
            btn.setToggleGroup(playerToggleGroup);
            btn.setUserData(i);
            if (i == playConfig.getPlayerCount()) { //
                btn.setSelected(true);
            }
            playerCountBox.getChildren().add(btn);
        }
        playerToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                playConfig.setPlayerCount((Integer) newValue.getUserData()); //
            }
        });

        // --- 말 개수 선택 ---
        HBox pieceCountBox = new HBox(10);
        pieceCountBox.setAlignment(Pos.CENTER_LEFT);
        Label pieceLabel = new Label("말 개수:");
        ToggleGroup pieceToggleGroup = new ToggleGroup();
        for (int i = MIN_PIECE; i <= MAX_PIECE; i++) {
            RadioButton btn = new RadioButton(String.valueOf(i));
            btn.setToggleGroup(pieceToggleGroup);
            btn.setUserData(i);
            if (i == playConfig.getPieceCount()) { //
                btn.setSelected(true);
            }
            pieceCountBox.getChildren().add(btn);
        }
        pieceToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                playConfig.setPieceCount((Integer) newValue.getUserData()); //
            }
        });

        // --- 판 종류 선택 ---
        HBox boardTypeBox = new HBox(10);
        boardTypeBox.setAlignment(Pos.CENTER_LEFT);
        Label boardLabel = new Label("판 종류:");
        ToggleGroup boardToggleGroup = new ToggleGroup();
        for (String label : BOARD_LABELS) { //
            RadioButton btn = new RadioButton(label);
            btn.setToggleGroup(boardToggleGroup);
            btn.setUserData(label);
            // playConfig의 BoardType enum 값과 라디오 버튼의 텍스트를 비교하여 초기 선택
            if (playConfig.getBoardType() == PlayConfig.BoardType.SQUARE && label.equals("사각형")) { //
                btn.setSelected(true);
            } else if (playConfig.getBoardType() == PlayConfig.BoardType.PENTAGON && label.equals("오각형")) {
                btn.setSelected(true);
            } else if (playConfig.getBoardType() == PlayConfig.BoardType.HEXAGON && label.equals("육각형")) {
                btn.setSelected(true);
            }
            boardTypeBox.getChildren().add(btn);
        }
        // 만약 위에서 기본값이 설정되지 않았다면 (예: PlayConfig의 기본값이 "사각형"이 아닌 경우), "사각형"을 명시적으로 선택
        if (boardToggleGroup.getSelectedToggle() == null) {
            for (javafx.scene.control.Toggle toggle : boardToggleGroup.getToggles()) {
                if (((RadioButton) toggle).getText().equals("사각형")) {
                    toggle.setSelected(true);
                    playConfig.setBoardType("사각형"); // PlayConfig의 상태도 일치시킴
                    break;
                }
            }
        }
        boardToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                playConfig.setBoardType((String) newValue.getUserData()); //
            }
        });


        // --- 시작 버튼 ---
        Button startButton = new Button("시작");
        startButton.setOnAction(e -> {
            // 플레이어 이름 자동 생성
            int pc = playConfig.getPlayerCount(); //
            List<String> names = new ArrayList<>();
            for (int i = 1; i <= pc; i++) { //
                names.add("Player" + i); //
            }

            // 새로운 Stage (게임 창) 생성
            Stage gameStage = new Stage();

            // YutBoardFX 인스턴스 생성 (JavaFX 버전의 윷놀이 판)
            // YutBoardFX 생성자에 Stage를 전달하도록 수정되었습니다.
            YutBoard yutBoard = new YutBoard(playConfig, names, gameStage);
            // YutBoardFX 내부에서 Scene을 생성하고 Stage에 설정하여 보여줄 것입니다.

            // 현재 FirstPage 창 닫기
            primaryStage.close();
        });

        root.getChildren().addAll(
                new HBox(10, playerLabel, playerCountBox),
                new HBox(10, pieceLabel, pieceCountBox),
                new HBox(10, boardLabel, boardTypeBox),
                startButton
        );

        Scene scene = new Scene(root, 450, 300);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}