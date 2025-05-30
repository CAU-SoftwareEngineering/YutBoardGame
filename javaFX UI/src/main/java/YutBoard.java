import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// YutBoardFX: JavaFX 기반의 윷놀이 게임판 UI를 구현하는 클래스
public class YutBoard implements GameView {
    private static final String IMG_ROOT = "/img/"; // 이미지 리소스의 루트 경로

    // 게임 설정, 컨트롤러, 경로 설정 등 핵심 로직 관련 객체
    private final PlayConfig config;
    private final GameController controller;
    private final PathConfig pathConfigInstance;

    // JavaFX UI 요소
    private Stage primaryStage; // 메인 윈도우
    private BorderPane rootPane; // 전체 레이아웃을 위한 루트 페인
    private Pane boardPane; // 윷판의 말과 칸이 그려지는 중앙 페인
    private VBox pieceActionPanelFX; // '새 말 꺼내기' 등 말 관련 액션 버튼이 위치할 패널
    private Label statusLabelFX; // 게임 상태 메시지를 표시할 라벨
    private VBox infoPanelFX; // 플레이어 정보를 표시할 패널

    // UI 상태 및 데이터
    private List<Point2D>[] pathPointsScreen; // 윷판 위 각 경로 지점의 화면 좌표
    private Map<String, Button> boardButtonsMap = new HashMap<>(); // 윷판의 각 칸에 해당하는 클릭 가능한 버튼들

    private final int buttonSize = 30; // 윷판 위 칸(버튼)의 기본 크기
    private boolean canMove = false; // 현재 말을 움직일 수 있는 상태인지 여부

    // 윷 던지기 관련 UI 요소
    private Button rndBtnFX; // 랜덤 윷 던지기 버튼
    private Button specBtnFX; // 지정 윷 던지기 버튼
    private ComboBox<Yut.Result> yutComboBoxFX; // 지정 윷 결과를 선택하는 콤보박스

    /**
     * YutBoardFX 생성자. 게임 설정, 플레이어 이름, 주 Stage를 받아 UI를 초기화하고 게임 로직을 연결합니다.
     * @param config 게임 설정 객체
     * @param playerNames 플레이어 이름 리스트
     * @param stage 주 Stage 객체
     */
    public YutBoard(PlayConfig config, List<String> playerNames, Stage stage) {
        this.primaryStage = stage;
        this.config = config;
        this.pathConfigInstance = new PathConfig(config.getBoardType()); //
        this.controller = new GameController(config, playerNames, this); //

        initUI(); // JavaFX UI 요소들 초기화 및 레이아웃 설정
        // UI가 완전히 그려진 후 보드 기하학 정보 계산 및 게임 시작
        Platform.runLater(() -> {
            initBoardGeometry(); // 윷판 경로의 화면 좌표 계산
            initBoardUIElements(); // 윷판의 각 칸(버튼) UI 요소 생성
            controller.startGame(); // 게임 로직 시작 및 초기 상태 업데이트 요청
        });
    }

    /**
     * JavaFX UI 요소들을 초기화하고 전체 레이아웃을 설정합니다.
     */
    private void initUI() {
        rootPane = new BorderPane(); // 기본 레이아웃으로 BorderPane 사용

        // --- 상단: 윷 던지기 패널 ---
        HBox topPanel = new HBox(10); //
        topPanel.setPadding(new Insets(10));
        topPanel.setAlignment(Pos.CENTER);
        rndBtnFX = new Button("랜덤 윷 던지기"); //
        yutComboBoxFX = new ComboBox<>(); //
        yutComboBoxFX.getItems().setAll(Yut.Result.values()); //
        yutComboBoxFX.setValue(Yut.Result.도); // 기본값 설정
        specBtnFX = new Button("지정 윷 던지기"); //

        rndBtnFX.setOnAction(e -> controller.onThrowRandom()); // 랜덤 윷 던지기 이벤트 연결
        specBtnFX.setOnAction(e -> controller.onThrowSpecified(yutComboBoxFX.getValue())); // 지정 윷 던지기 이벤트 연결

        topPanel.getChildren().addAll(rndBtnFX, yutComboBoxFX, specBtnFX);
        rootPane.setTop(topPanel);

        // --- 중앙: 윷판 패널 ---
        boardPane = new Pane(); //
        // 배경 이미지 설정
        try (InputStream bgInput = getClass().getResourceAsStream(IMG_ROOT + "background.png")) { //
            if (bgInput != null) {
                Image bgImage = new Image(bgInput);
                BackgroundImage backgroundImage = new BackgroundImage(bgImage,
                        BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                        BackgroundPosition.CENTER,
                        new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true));
                boardPane.setBackground(new Background(backgroundImage));
            } else {
                System.err.println("배경 이미지 로드 실패: " + IMG_ROOT + "background.png"); //
                boardPane.setStyle("-fx-background-color: #D2B48C;"); // 대체 배경색
            }
        } catch (Exception e) {
            System.err.println("배경 이미지 로드 중 오류: " + e.getMessage());
            boardPane.setStyle("-fx-background-color: #D2B48C;");
        }
        boardPane.setPrefSize(600, 600); // 초기 윷판 크기 설정
        // 윷판(Pane)의 크기가 변경될 때마다 내부 요소들을 다시 그리도록 리스너 추가
        boardPane.widthProperty().addListener((obs, oldVal, newVal) -> refreshBoardLayout());
        boardPane.heightProperty().addListener((obs, oldVal, newVal) -> refreshBoardLayout());
        rootPane.setCenter(boardPane);

        // --- 우측: 플레이어 정보 패널 ---
        infoPanelFX = new VBox(10); //
        infoPanelFX.setPadding(new Insets(10)); //
        infoPanelFX.setPrefWidth(220); //
        infoPanelFX.setStyle("-fx-border-color: lightgray; -fx-border-width: 1;");
        ScrollPane infoScrollPane = new ScrollPane(infoPanelFX); // 정보가 많아지면 스크롤 가능하도록
        infoScrollPane.setFitToWidth(true); // 스크롤팬 너비를 infoPanelFX 너비에 맞춤
        rootPane.setRight(infoScrollPane);

        // --- 하단: 액션 버튼 및 상태 메시지 패널 ---
        VBox bottomOuterPanel = new VBox(5); // 전체 하단 영역
        bottomOuterPanel.setPadding(new Insets(10));
        bottomOuterPanel.setAlignment(Pos.CENTER);

        pieceActionPanelFX = new VBox(5); // 새 말 꺼내기 버튼 등이 위치할 패널
        pieceActionPanelFX.setAlignment(Pos.CENTER);

        statusLabelFX = new Label("게임을 시작해주세요."); // 게임 상태 메시지 라벨
        statusLabelFX.setFont(Font.font("SansSerif", FontWeight.BOLD, 14)); //

        bottomOuterPanel.getChildren().addAll(pieceActionPanelFX, statusLabelFX);
        rootPane.setBottom(bottomOuterPanel);

        enableYutButtons(false); // 초기에는 윷 던지기 버튼 비활성화

        // --- Scene 및 Stage 설정 ---
        Scene scene = new Scene(rootPane, 820, 720); // (너비, 높이) 초기 창 크기
        primaryStage.setTitle("윷놀이 게임 (JavaFX)");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> System.exit(0)); // 창 닫기 버튼 클릭 시 프로그램 종료
        primaryStage.show(); // 창 보여주기
    }

    /**
     * 윷판(boardPane)의 크기가 변경될 때 호출되어 내부 UI 요소들의 레이아웃을 갱신합니다.
     */
    private void refreshBoardLayout() {
        // Pane의 너비나 높이가 유효한 값일 때만 실행 (초기화 중 0일 수 있음)
        if (boardPane.getWidth() > 0 && boardPane.getHeight() > 0) {
            initBoardGeometry(); // 변경된 크기에 맞춰 경로 좌표 재계산
            initBoardUIElements(); // 재계산된 좌표를 바탕으로 칸(버튼) UI 재생성
            // 컨트롤러와 게임 상태가 유효하면 현재 상태로 보드 다시 그리기
            if (controller != null && controller.getState() != null) {
                updateBoard(controller.getState()); //
            }
        }
    }

    /**
     * 윷판의 종류(config.getBoardType())와 현재 윷판(boardPane)의 크기를 기반으로
     * 각 경로 지점의 화면 좌표(pathPointsScreen)를 계산합니다.
     * Swing 구현의 initBoardGeometry 로직을 JavaFX Point2D 기준으로 수정.
     */
    @SuppressWarnings("unchecked")
    private void initBoardGeometry() {
        PlayConfig.BoardType boardType = config.getBoardType(); //
        int sides;

        double w = boardPane.getWidth();
        double h = boardPane.getHeight();
        // Pane이 그려지기 전이라 크기가 0일 경우, prefSize를 사용하거나 반환하여 오류 방지
        if (w == 0 || h == 0) { //
            if (boardPane.getPrefWidth() > 0 && boardPane.getPrefHeight() > 0) {
                w = boardPane.getPrefWidth();
                h = boardPane.getPrefHeight();
            } else {
                System.err.println("Board panel size is zero, cannot init geometry."); //
                return;
            }
        }

        double centerX = w / 2.0; //
        double centerY = h / 2.0; //
        Point2D centerPoint = new Point2D(centerX, centerY); //

        List<Point2D> outerPath = new ArrayList<>();
        int numberOfShortcuts = this.pathConfigInstance.getBranchCount() - 1; //
        this.pathPointsScreen = new List[1 + numberOfShortcuts]; //

        // 사각형 윷판 경로 계산
        if (boardType == PlayConfig.BoardType.SQUARE) { //
            sides = 4; //
            double mainRadius = Math.min(w, h) * 0.4; //

            Point2D v0 = new Point2D(centerX + mainRadius, centerY + mainRadius); //
            Point2D v1 = new Point2D(centerX + mainRadius, centerY - mainRadius); //
            Point2D v2 = new Point2D(centerX - mainRadius, centerY - mainRadius); //
            Point2D v3 = new Point2D(centerX - mainRadius, centerY + mainRadius); //

            outerPath.add(v0); //
            for (int i = 1; i <= 4; i++) outerPath.add(interpolate(v0, v1, i / 5.0)); //
            outerPath.add(v1); //
            for (int i = 1; i <= 4; i++) outerPath.add(interpolate(v1, v2, i / 5.0)); //
            outerPath.add(v2); //
            for (int i = 1; i <= 4; i++) outerPath.add(interpolate(v2, v3, i / 5.0)); //
            outerPath.add(v3); //
            for (int i = 1; i <= 4; i++) outerPath.add(interpolate(v3, v0, i / 5.0)); //
            this.pathPointsScreen[0] = outerPath; //

            List<Point2D> shortcutPath_5_to_15 = new ArrayList<>(); //
            shortcutPath_5_to_15.add(interpolate(v1, centerPoint, 0.33)); //
            shortcutPath_5_to_15.add(interpolate(v1, centerPoint, 0.66)); //
            shortcutPath_5_to_15.add(centerPoint); //
            shortcutPath_5_to_15.add(interpolate(centerPoint, v3, 0.33)); //
            shortcutPath_5_to_15.add(interpolate(centerPoint, v3, 0.66)); //
            this.pathPointsScreen[1] = shortcutPath_5_to_15; //

            List<Point2D> shortcutPath_10_to_0 = new ArrayList<>(); //
            shortcutPath_10_to_0.add(interpolate(v2, centerPoint, 0.33)); //
            shortcutPath_10_to_0.add(interpolate(v2, centerPoint, 0.66)); //
            shortcutPath_10_to_0.add(centerPoint); //
            shortcutPath_10_to_0.add(interpolate(centerPoint, v0, 0.33)); //
            shortcutPath_10_to_0.add(interpolate(centerPoint, v0, 0.66)); //
            this.pathPointsScreen[2] = shortcutPath_10_to_0; //

            // 오각형 또는 육각형 윷판 경로 계산
        } else { //
            sides = (boardType == PlayConfig.BoardType.PENTAGON) ? 5 : 6; //
            double radius = Math.min(w, h) * 0.35; //
            double angleOffsetRadians = (sides == 5) ? -Math.PI / 2 : ( (sides == 6) ? 0 : -Math.PI/4 ); //

            Point2D[] vertices = new Point2D[sides]; //
            for (int i = 0; i < sides; i++) {
                double angle = 2 * Math.PI * i / sides + angleOffsetRadians; //
                double x = centerX + radius * Math.cos(angle); //
                double y = centerY + radius * Math.sin(angle); //
                vertices[i] = new Point2D(x, y); //
            }

            int startVertexIndex = switch (sides) { //
                case 4 -> 1;
                case 5 -> 2;
                case 6 -> 1;
                default -> 0;
            };

            for (int i = 0; i < sides; i++) {
                int currentVertexIdx = (startVertexIndex - i + sides) % sides; //
                int nextVertexIdx = (startVertexIndex - i - 1 + sides) % sides; //
                Point2D pA = vertices[currentVertexIdx]; //
                Point2D pB = vertices[nextVertexIdx]; //
                for (int k = 0; k < 5; k++) { // 각 변을 5개의 점으로 나눔
                    outerPath.add(interpolate(pA, pB, k / 5.0)); //
                }
            }
            this.pathPointsScreen[0] = outerPath; //

            // 지름길 경로 계산
            for (int scIndex = 0; scIndex < numberOfShortcuts; scIndex++) { //
                int branchOuterStep = this.pathConfigInstance.getBranchPoint(scIndex + 1); // 지름길 분기점
                if (branchOuterStep >= outerPath.size()) { //
                    System.err.println("Error in initBoardGeometry: branchOuterStep " + branchOuterStep + " is out of bounds for outerPath size " + outerPath.size()); //
                    continue;
                }
                Point2D shortcutStartPoint = outerPath.get(branchOuterStep); //

                int exitOuterStepForThisSC = this.pathConfigInstance.getExitOffset(scIndex + 1); // 지름길 탈출 후 외곽 경로 복귀 지점
                Point2D actualShortcutEndTarget; // 지름길의 시각적 목표 지점

                if (exitOuterStepForThisSC > outerPath.size()) { //
                    System.err.println("Critical Error in initBoardGeometry: exitOuterStepForThisSC " + exitOuterStepForThisSC + " is strictly greater than outerPath size " + outerPath.size() + ". Skipping this shortcut."); //
                    continue;
                } else if (exitOuterStepForThisSC == outerPath.size()) { // 완주 지점을 의미
                    if (outerPath.isEmpty()) { //
                        System.err.println("Error: outerPath is empty when trying to handle exitOuterStepForThisSC == outerPath.size(). Skipping shortcut."); //
                        continue;
                    }
                    actualShortcutEndTarget = outerPath.get(0); // 출발/도착점을 시각적 목표로
                } else {
                    actualShortcutEndTarget = outerPath.get(exitOuterStepForThisSC); //
                }

                List<Point2D> shortcutPath = new ArrayList<>(); //
                shortcutPath.add(interpolate(shortcutStartPoint, centerPoint, 0.33)); //
                shortcutPath.add(interpolate(shortcutStartPoint, centerPoint, 0.66)); //
                shortcutPath.add(centerPoint); // 중앙점
                shortcutPath.add(interpolate(centerPoint, actualShortcutEndTarget, 0.33)); //
                shortcutPath.add(interpolate(centerPoint, actualShortcutEndTarget, 0.66)); //

                if (this.pathPointsScreen.length > scIndex + 1) { // 배열 범위 확인
                    this.pathPointsScreen[scIndex + 1] = shortcutPath; //
                } else {
                    System.err.println("Error in initBoardGeometry: scIndex+1 " + (scIndex+1) + " is out of bounds for pathPointsScreen length " + this.pathPointsScreen.length); //
                }
            }
        }
    }

    /**
     * 계산된 경로 좌표(pathPointsScreen)를 기반으로 윷판의 각 칸을 나타내는 UI 요소(버튼)들을 생성하고 배치합니다.
     * 이전에 생성된 버튼들은 제거 후 다시 만듭니다.
     * Swing 구현의 initBoardUI와 유사.
     */
    private void initBoardUIElements() {
        if (pathPointsScreen == null) { //
            System.err.println("initBoardUIElements: pathPointsScreen이 null입니다. initBoardGeometry()를 먼저 호출해야 합니다."); //
            return;
        }

        boardPane.getChildren().removeIf(node -> node.getUserData() instanceof String && ((String)node.getUserData()).startsWith("board_btn_"));
        boardButtonsMap.clear();

        for (int pathIdx = 0; pathIdx < pathPointsScreen.length; pathIdx++) {
            List<Point2D> currentPathCoords = pathPointsScreen[pathIdx];
            if (currentPathCoords == null) { //
                System.err.println("initBoardUIElements: pathPointsScreen[" + pathIdx + "] is null."); //
                continue;
            }
            for (int stepIdx = 0; stepIdx < currentPathCoords.size(); stepIdx++) {
                Point2D p = currentPathCoords.get(stepIdx); //
                String iconName;

                boolean isOuterPath = (pathIdx == 0); //
                boolean isStartFinishPoint = isOuterPath && stepIdx == 0; //
                boolean isVertex = isOuterPath && (stepIdx % 5 == 0) && !isStartFinishPoint; //
                boolean isCenterNode = !isOuterPath && (stepIdx == this.pathConfigInstance.getMergeStep()); //

                boolean isSpecialSpotForButton = isStartFinishPoint || isVertex || isCenterNode; // 칸 버튼 크기 결정을 위한 플래그

                if (isStartFinishPoint) iconName = "startcircle.jpg"; //
                else if (isVertex || isCenterNode) iconName = "bigcircle.jpg"; //
                else iconName = "circle.jpg"; //

                Image iconImage = loadImage(IMG_ROOT + iconName); //
                // createBoardButtonFX 호출 시 isSpecialSpotForButton 전달
                Button btn = createBoardButton(iconImage, p.getX(), p.getY(), isSpecialSpotForButton);
                btn.setUserData("board_btn_" + pathIdx + "_" + stepIdx);

                final int fPathIdx = pathIdx; //
                final int fStepIdx = stepIdx; //
                btn.setOnAction(e -> { //
                    // ... (기존 이벤트 핸들러 로직 동일) ...
                    if (!canMove) { //
                        statusLabelFX.setText("먼저 윷을 던져주세요!"); //
                        return;
                    }
                    GameState currentState = controller.getState(); //
                    if (currentState.getLastThrow() != null && currentState.getLastThrow().size() > 1) { //
                        List<Yut.Result> choices = new ArrayList<>(currentState.getLastThrow());
                        ChoiceDialog<Yut.Result> dialog = new ChoiceDialog<>(choices.get(0), choices);
                        dialog.setTitle("이동할 윷 선택");
                        dialog.setHeaderText(null);
                        dialog.setContentText("어떤 윷으로 이동하시겠습니까?");

                        Optional<Yut.Result> result = dialog.showAndWait();
                        if (result.isPresent()) {
                            int choiceIndex = choices.indexOf(result.get());
                            currentState.setSelect(choiceIndex); //
                        } else {
                            return;
                        }
                    } else if (currentState.getLastThrow() != null && currentState.getLastThrow().size() == 1){ //
                        currentState.setSelect(0); //
                    }
                    controller.onSelectPiece(fPathIdx, fStepIdx); //
                });
                boardPane.getChildren().add(btn); //
                boardButtonsMap.put(fPathIdx + "-" + fStepIdx, btn);
            }
        }
    }

    /**
     * GameView 인터페이스 메소드 구현. 게임 상태(GameState)가 변경될 때마다 호출되어
     * 화면(윷판, 플레이어 정보, 상태 메시지 등)을 최신 상태로 업데이트합니다.
     * Swing 구현의 updateBoard와 유사.
     * @param state 최신 게임 상태 객체
     */
    @Override
    public void updateBoard(GameState state) {
        Platform.runLater(() -> {
            if (pathPointsScreen == null || boardPane.getWidth() == 0) {
                if (boardPane.getWidth() > 0 && boardPane.getHeight() > 0 && pathPointsScreen == null) {
                    initBoardGeometry();
                    initBoardUIElements();
                } else {
                    System.err.println("updateBoard: UI not ready or pathPointsScreen is null. Update deferred.");
                    return;
                }
            }

            boardPane.getChildren().removeIf(node -> node instanceof ImageView && "piece".equals(node.getUserData()));

            pieceActionPanelFX.getChildren().clear();
            Button newPieceBtn = new Button("새 말 꺼내기");
            newPieceBtn.setOnAction(e -> {
                GameState currentState = controller.getState();
                if (currentState.getLastThrow() != null && currentState.getLastThrow().size() > 1) {
                    List<Yut.Result> choices = new ArrayList<>(currentState.getLastThrow());
                    ChoiceDialog<Yut.Result> dialog = new ChoiceDialog<>(choices.get(0), choices);
                    dialog.setTitle("새 말 이동 윷 선택");
                    dialog.setHeaderText(null);
                    dialog.setContentText("어떤 윷으로 새 말을 이동하시겠습니까?");
                    Optional<Yut.Result> result = dialog.showAndWait();
                    if (result.isPresent()) {
                        int choiceIndex = choices.indexOf(result.get());
                        currentState.setSelect(choiceIndex);
                    } else {
                        return;
                    }
                } else if (currentState.getLastThrow() != null && currentState.getLastThrow().size() == 1){
                    currentState.setSelect(0);
                }
                controller.deployNewPiece();
            });
            pieceActionPanelFX.getChildren().add(newPieceBtn);

            boardButtonsMap.forEach((key, button) -> {
                String[] parts = key.split("-");
                int pIdx = Integer.parseInt(parts[0]);
                int sIdx = Integer.parseInt(parts[1]);
                String baseIconName;
                boolean isOuterPath = (pIdx == 0);
                boolean isStartFinishPoint = isOuterPath && sIdx == 0;
                boolean isVertex = isOuterPath && (sIdx % 5 == 0) && !isStartFinishPoint;
                boolean isCenterNode = !isOuterPath && (sIdx == this.pathConfigInstance.getMergeStep());
                if (isStartFinishPoint) baseIconName = "startcircle.jpg";
                else if (isVertex || isCenterNode) baseIconName = "bigcircle.jpg";
                else baseIconName = "circle.jpg";
                ImageView iv = (ImageView) button.getGraphic();
                if (iv != null) {
                    iv.setImage(loadImage(IMG_ROOT + baseIconName));
                } else {
                    button.setGraphic(new ImageView(loadImage(IMG_ROOT + baseIconName)));
                }
            });

            Map<String, Map<Player, Integer>> pieceStackCounts = new HashMap<>();
            for (Player player : state.getPlayers()) {
                for (Piece piece : player.getPieces()) {
                    if (piece.isFinished() || piece.getPathIndex() < 0 || piece.getStepIndex() < 0) continue;
                    String locationKey = piece.getPathIndex() + "-" + piece.getStepIndex();
                    pieceStackCounts.computeIfAbsent(locationKey, k -> new HashMap<>())
                            .merge(player, 1, Integer::sum);
                }
            }

            List<Node> pieceImageViews = new ArrayList<>();
            for (Player player : state.getPlayers()) {
                for (Piece piece : player.getPieces()) {
                    if (piece.isFinished() || piece.getPathIndex() < 0 || piece.getStepIndex() < 0) continue;
                    if (piece.getPathIndex() >= pathPointsScreen.length || pathPointsScreen[piece.getPathIndex()] == null ||
                            piece.getStepIndex() >= pathPointsScreen[piece.getPathIndex()].size()) {
                        System.err.printf("잘못된 말 위치 참조 (화면): Player %d, Piece %d, Path %d, Step %d\n",
                                player.getId(), piece.getId(), piece.getPathIndex(), piece.getStepIndex());
                        continue;
                    }
                    Point2D piecePos = pathPointsScreen[piece.getPathIndex()].get(piece.getStepIndex());
                    String locationKey = piece.getPathIndex() + "-" + piece.getStepIndex();
                    int displayStack = pieceStackCounts.getOrDefault(locationKey, new HashMap<>())
                            .getOrDefault(player, 1);
                    displayStack = Math.min(displayStack, 5);
                    if (displayStack <= 0) displayStack = 1;

                    String iconFileName;
                    boolean isOuterPath = (piece.getPathIndex() == 0);
                    boolean isStartFinishPoint = isOuterPath && (piece.getStepIndex() == 0);
                    boolean isVertex = isOuterPath && (piece.getStepIndex() % 5 == 0) && !isStartFinishPoint;
                    boolean isCenterNode = !isOuterPath && (piece.getStepIndex() == this.pathConfigInstance.getMergeStep());

                    if (isVertex || isCenterNode) {
                        iconFileName = "big" + player.getColor() + displayStack + ".jpg";
                    } else {
                        iconFileName = player.getColor() + displayStack + ".jpg";
                    }

                    ImageView pieceView = new ImageView(loadImage(IMG_ROOT + iconFileName));
                    Image actualImage = pieceView.getImage();
                    pieceView.setPreserveRatio(true);

                    if (actualImage != null && actualImage.getWidth() > 0 && actualImage.getHeight() > 0) {
                        pieceView.setLayoutX(piecePos.getX() - actualImage.getWidth() / 2.0);
                        pieceView.setLayoutY(piecePos.getY() - actualImage.getHeight() / 2.0);
                    } else {
                        pieceView.setLayoutX(piecePos.getX() - buttonSize / 2.0);
                        pieceView.setLayoutY(piecePos.getY() - buttonSize / 2.0);
                    }

                    pieceView.setUserData("piece");
                    pieceView.setMouseTransparent(true);
                    pieceImageViews.add(pieceView);
                }
            }
            boardPane.getChildren().addAll(pieceImageViews);

            // --- 게임 상태 메시지 생성 및 표시 ---
            String statusText;
            // 게임 종료 시
            if (state.isGameOver() && state.getWinner() != null) { //
                statusText = "Player " + state.getWinner().getId() + " (" + state.getWinner().getColor() + ")" + " 승리! 게임 종료."; //
                enableYutButtons(false); //
                newPieceBtn.setDisable(true); //
                canMove = false; //

                pieceActionPanelFX.getChildren().clear(); //
                Button restartButton = new Button("게임 재시작"); //
                restartButton.setOnAction(event -> controller.restartGame()); //
                Button exitButton = new Button("게임 종료"); //
                exitButton.setOnAction(event -> System.exit(0)); //
                pieceActionPanelFX.getChildren().addAll(restartButton, exitButton); //
                // 윷 던질 차례일 때
            } else if (state.getPhase() == GameState.phase.THROW) { //
                String baseMessage = "Player " + state.getCurrentPlayer().getId() + ": 윷을 던지세요. (남은 횟수: " + state.getThrowCount() + ")"; //
                String prefix = ""; // 추가 안내 메시지

                Yut.Result lastThrownResult = state.getLastThrow().isEmpty() ? null : state.getLastThrow().get(state.getLastThrow().size() - 1);

                if (state.getLastTurnEvent() == GameState.TurnEvent.YUT_OR_MO_THROWN && lastThrownResult != null) {
                    prefix = "'" + lastThrownResult.toString() + "'이(가) 나와 한 번 더 던지세요!\n";
                } else if (state.getLastTurnEvent() == GameState.TurnEvent.CAPTURE_OCCURRED) {
                    prefix = "상대 말을 잡았습니다! 한 번 더 던지세요.\n";
                }

                statusText = prefix + baseMessage;
                enableYutButtons(true); //
                newPieceBtn.setDisable(true); //
                canMove = false; //
                // 말 이동할 차례일 때
            } else { // GameState.phase.MOVE
                String yutResultsStr = state.getLastThrow().stream() //
                        .map(Yut.Result::toString).collect(Collectors.joining(", ")); //
                statusText = "Player " + state.getCurrentPlayer().getId() + ": 말을 선택하세요. (결과: " + yutResultsStr + ")"; //
                enableYutButtons(false); //
                newPieceBtn.setDisable(!(state.getLastThrow() != null && !state.getLastThrow().isEmpty())); //
                canMove = true; //
            }

            statusLabelFX.setText(statusText); // 최종적으로 생성된 상태 메시지를 라벨에 설정

            updateInfoPanel(state); //
        });
    }

    /**
     * 우측 플레이어 정보 패널(infoPanelFX)을 현재 게임 상태(state)에 따라 업데이트합니다.
     * Swing 구현의 updateInfoPanel과 유사.
     * @param state 현재 게임 상태 객체
     */
    private void updateInfoPanel(GameState state) {
        infoPanelFX.getChildren().clear(); //

        Label titleLabel = new Label("플레이어 정보"); //
        titleLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 16)); //
        infoPanelFX.getChildren().add(titleLabel); //

        for (Player player : state.getPlayers()) { //
            VBox playerBox = new VBox(5); //
            playerBox.setPadding(new Insets(8));
            String playerTitle = "Player " + player.getId() + " (" + player.getColor() + ")"; //
            if (player == state.getCurrentPlayer() && !(state.isGameOver() && state.getWinner() != null) ) { //
                playerBox.setStyle("-fx-border-color: blue; -fx-border-width: 2; -fx-padding: 6;"); //
            } else {
                playerBox.setStyle("-fx-border-color: lightgray; -fx-border-width: 1; -fx-padding: 7;");
            }
            Label playerTitleLabel = new Label(playerTitle);
            playerTitleLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 13));
            playerBox.getChildren().add(playerTitleLabel);

            ImageView playerIconView = new ImageView(loadImage(IMG_ROOT + player.getColor() + ".jpg")); //
            playerIconView.setFitWidth(20); //
            playerIconView.setFitHeight(20); //
            playerIconView.setPreserveRatio(true);
            playerBox.getChildren().add(playerIconView); //

            long piecesNotYetOnBoard = player.getPieces().stream() //
                    .filter(p -> p.getPathIndex() == -1 && !p.isFinished()) //
                    .count(); //
            Label remainingPiecesLabel = new Label("대기 중인 말: " + piecesNotYetOnBoard); //
            playerBox.getChildren().add(remainingPiecesLabel); //

            long score = player.getPieces().stream().filter(Piece::isFinished).count(); //
            Label scoreLabel = new Label("완주한 말: " + score + " / " + config.getPieceCount()); //
            playerBox.getChildren().add(scoreLabel); //

            infoPanelFX.getChildren().add(playerBox); //
        }
    }

    /**
     * GameView 인터페이스 메소드. 윷 던지기 결과를 받아 화면을 업데이트합니다.
     * @param result 윷 던지기 결과
     */
    @Override
    public void showThrowResult(Yut.Result result) { //
        Platform.runLater(() -> { //
            System.out.println("윷 던짐 결과 (View): " + result); //
            updateBoard(controller.getState()); //
        });
    }

    /**
     * GameView 인터페이스 메소드. 승리자를 받아 화면에 알림을 표시하고 UI를 최종 상태로 업데이트합니다.
     * @param winner 승리한 플레이어 객체
     */
    @Override
    public void showWinner(Player winner) { //
        Platform.runLater(() -> { //
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("게임 종료");
            alert.setHeaderText(null);
            alert.setContentText("Player " + winner.getId() + " (" + winner.getColor() + ") 님 승리!"); //
            alert.showAndWait();

            if (controller != null && controller.getState() != null) {
                updateBoard(controller.getState()); //
            }
        });
    }

    /**
     * GameView 인터페이스 메소드. 현재 게임 화면(Stage)을 닫습니다.
     */
    @Override
    public void closeGameView() { //
        Platform.runLater(() -> {
            if (primaryStage != null) {
                primaryStage.close(); //
            }
        });
    }

    /**
     * GameView 인터페이스 메소드. 초기 설정 화면(FirstPage)을 다시 표시합니다.
     */
    @Override
    public void showInitialSetup() { //
        Platform.runLater(() -> {
            FirstPage firstPage = new FirstPage(); //
            try {
                firstPage.start(new Stage());
            } catch (Exception e) {
                System.err.println("FirstPage를 다시 시작하는 중 오류 발생: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * 지정된 경로의 이미지 파일을 로드하여 JavaFX Image 객체로 반환합니다.
     * Swing 구현의 loadIcon과 유사.
     * @param path 클래스패스 기준 이미지 파일 경로
     * @return 로드된 Image 객체 또는 플레이스홀더 Image 객체
     */
    private Image loadImage(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                System.err.println("이미지 파일 누락 또는 경로 문제: " + path); //
                return createPlaceholderImageFX(Color.LIGHTGRAY, this.buttonSize, this.buttonSize); //
            }
            return new Image(is);
        } catch (Exception e) {
            System.err.println("이미지 로드 중 예외 발생: " + path + " (" + e.getMessage() + ")");
            return createPlaceholderImageFX(Color.LIGHTGRAY, this.buttonSize, this.buttonSize);
        }
    }

    /**
     * 이미지 로드 실패 시 표시할 플레이스홀더 이미지를 생성합니다.
     * Swing 구현의 createPlaceholderIcon과 유사.
     * @param color 플레이스홀더 색상
     * @param width 이미지 너비
     * @param height 이미지 높이
     * @return 생성된 플레이스홀더 Image 객체
     */
    private Image createPlaceholderImageFX(Color color, int width, int height) {
        WritableImage img = new WritableImage(width, height);
        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(width, height);
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(color); //
        gc.fillOval(0, 0, width, height); //
        gc.setStroke(Color.DARKGRAY); //
        gc.strokeOval(0, 0, width - 1, height - 1); //
        canvas.snapshot(null, img);
        return img;
    }

    /**
     * 윷판의 각 칸을 나타내는 JavaFX Button을 생성합니다.
     * Swing 구현의 createBoardButton과 유사.
     * @param icon 칸에 표시할 기본 이미지
     * @param centerX 버튼의 중심 X 좌표
     * @param centerY 버튼의 중심 Y 좌표
     * @return 생성된 JavaFX Button 객체
     */
    private Button createBoardButton(Image icon, double centerX, double centerY, boolean isSpecialSpot) { // isSpecialSpot 파라미터 추가
        Button btn = new Button(); //
        ImageView imageView = new ImageView(icon);

        double spotImageSize;
        if (isSpecialSpot) {
            // 꼭짓점 또는 출발점의 칸 이미지는 더 크게 (예: 50x50, 원본 이미지 크기 사용 또는 지정)
            // bigcircle.jpg, startcircle.jpg의 원본 크기가 50x50이라고 가정
            spotImageSize = 50;
        } else {
            // 일반 칸 이미지는 기존 buttonSize 사용
            spotImageSize = buttonSize; //
        }

        imageView.setFitWidth(spotImageSize);
        imageView.setFitHeight(spotImageSize);
        imageView.setPreserveRatio(true);
        btn.setGraphic(imageView);

        // 버튼 자체의 크기도 이미지 크기에 맞춤
        btn.setMinSize(spotImageSize, spotImageSize);
        btn.setPrefSize(spotImageSize, spotImageSize);
        btn.setMaxSize(spotImageSize, spotImageSize);

        // 버튼의 레이아웃 위치는 (centerX, centerY)가 버튼의 중심이 되도록 설정
        btn.setLayoutX(centerX - spotImageSize / 2.0); //
        btn.setLayoutY(centerY - spotImageSize / 2.0); //

        btn.setStyle("-fx-background-color: transparent; -fx-padding: 0;"); //
        return btn;
    }

    /**
     * 두 Point2D 객체 사이를 주어진 비율(t)로 내분하는 점의 좌표를 반환합니다.
     * Swing 구현의 interpolate와 동일 로직.
     * @param p1 첫 번째 점
     * @param p2 두 번째 점
     * @param t 내분 비율 (0.0 ~ 1.0)
     * @return 내분점의 Point2D 객체
     */
    private Point2D interpolate(Point2D p1, Point2D p2, double t) {
        return new Point2D(p1.getX() * (1 - t) + p2.getX() * t, p1.getY() * (1 - t) + p2.getY() * t); //
    }

    /**
     * 윷 던지기 관련 버튼의 활성화/비활성화 상태를 설정합니다.
     * Swing 구현의 enableYutButtons와 동일 로직.
     * @param enable true이면 활성화, false이면 비활성화
     */
    private void enableYutButtons(boolean enable) {
        rndBtnFX.setDisable(!enable); //
        specBtnFX.setDisable(!enable); //
        yutComboBoxFX.setDisable(!enable); //
    }
}