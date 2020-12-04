package controller;

import java.io.IOException;
import java.net.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import model.client.SnakeClient;
import model.client.SnakeClientListener;
import model.server.SnakeServer;
import model.snakeProto.SnakeProto;

public class GameScreenController implements SnakeClientListener {
    public AnchorPane pane;
    public TextArea textArea;
    public Button exitButton;
    private Stage stage;

    private SnakeProto.GameConfig config;

    private String name;

    private SnakeClient client;
    private SnakeServer server = null;

    private Thread clientThread;
    private Thread serverThread = null;

    boolean watching = false;
    @FXML
    void initialize() {
    }

    void setKeys(){
        stage.getScene().setOnKeyPressed(keyEvent ->{
            try {
                pane.requestFocus();
                switch (keyEvent.getCode()){
                    case UP:
                        client.changeDirection(SnakeProto.Direction.UP);
                        break;
                    case LEFT:
                        client.changeDirection(SnakeProto.Direction.LEFT);
                        break;
                    case DOWN:
                        client.changeDirection(SnakeProto.Direction.DOWN);
                        break;
                    case RIGHT:
                        client.changeDirection(SnakeProto.Direction.RIGHT);
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    void hostGame() throws SocketException, UnknownHostException {
        DatagramSocket serverSocket = new DatagramSocket();
        server = new SnakeServer(serverSocket, config);

        DatagramSocket clientSocket = new DatagramSocket();
        client = new SnakeClient(clientSocket, new InetSocketAddress(InetAddress.getLocalHost(), serverSocket.getLocalPort()), this, name);


        serverThread = new Thread(server);
        clientThread = new  Thread(client);

        serverThread.start();
        clientThread.start();

        setKeys();
    }

    void connect(InetSocketAddress serverAddress) throws SocketException {
        DatagramSocket clientSocket = new DatagramSocket();
        client = new SnakeClient(clientSocket, serverAddress, this, name);

        clientThread = new  Thread(client);
        clientThread.start();

        setKeys();

    }

    private String stateToString(SnakeProto.GameState state){
        StringBuilder builder = new StringBuilder();
        builder.append("   Player         Score     Role   ").append('\n');
        for (SnakeProto.GamePlayer player: state.getPlayers().getPlayersList()){
            builder.append(String.format("%15s%10d%10s\n", player.getName(), player.getScore(), player.getRole().toString()));
        }

        builder.append('\n');
        builder.append("Size: ").append(state.getConfig().getWidth()).append("x").append(state.getConfig().getHeight()).append('\n')
        .append("Food = ").append(state.getConfig().getFoodStatic()).append(" + players_number*").append(state.getConfig().getFoodPerPlayer());

        return builder.toString();
    }

    private SnakeProto.GameState.Coord round(SnakeProto.GameState.Coord coord){
        return SnakeProto.GameState.Coord.newBuilder()
                .setX((config.getWidth() + coord.getX()) % config.getWidth())
                .setY((config.getHeight() + coord.getY()) % config.getHeight())
                .build();
    }

    private java.util.List<SnakeProto.GameState.Coord> snakeBodyCords(SnakeProto.GameState.Snake snake){
        java.util.List<SnakeProto.GameState.Coord> snakeOffsetPoints = snake.getPointsList();
        List<SnakeProto.GameState.Coord> snakeBodyPoints = new LinkedList<>();
        snakeBodyPoints.add(snakeOffsetPoints.get(0)); //add head
        for (int i = 1; i < snake.getPointsCount(); i++){
            SnakeProto.GameState.Coord lastBodyPoint = snakeBodyPoints.get(snakeBodyPoints.size() - 1);

            int xOffset = snakeOffsetPoints.get(i).getX();
            int yOffset = snakeOffsetPoints.get(i).getY();

            if (xOffset == 0){
                if (yOffset < 0)
                    for (int j = 0; j  < -yOffset; j++){
                        lastBodyPoint = round(lastBodyPoint.toBuilder().setY((lastBodyPoint.getY() - 1) % config.getHeight()).build());
                        snakeBodyPoints.add(lastBodyPoint);
                    }
                else
                    for (int j = 0; j  < yOffset; j++){
                        lastBodyPoint = round(lastBodyPoint.toBuilder().setY((lastBodyPoint.getY() + 1) % config.getHeight()).build());
                        snakeBodyPoints.add(lastBodyPoint);
                    }
            }
            else if (yOffset == 0){
                if (xOffset < 0)
                    for (int j = 0; j  < -xOffset; j++){
                        lastBodyPoint = round(lastBodyPoint.toBuilder().setX((lastBodyPoint.getX() - 1) % config.getWidth()).build());
                        snakeBodyPoints.add(lastBodyPoint);
                    }
                else
                    for (int j = 0; j  < xOffset; j++){
                        lastBodyPoint = round(lastBodyPoint.toBuilder().setX((lastBodyPoint.getX() + 1) % config.getWidth()).build());
                        snakeBodyPoints.add(lastBodyPoint);
                    }
            }
        }
        return  snakeBodyPoints;
    }

    private void drawRectangle(SnakeProto.GameState.Coord coord, Paint paint){
        double offsetX = pane.getWidth() / config.getWidth();
        double offsetY = pane.getHeight() / config.getHeight();

        Rectangle rectangle = new Rectangle();
        rectangle.setFill(paint);
        rectangle.setX(coord.getX() * offsetX + 1);
        rectangle.setY(coord.getY() * offsetY + 1);
        rectangle.setWidth(offsetX - 3);
        rectangle.setHeight(offsetY - 3);

        Platform.runLater(() -> pane.getChildren().add(rectangle));
    }

    private void drawSnake(SnakeProto.GameState.Snake snake){
        List<SnakeProto.GameState.Coord> snakeCords = snakeBodyCords(snake);
        drawRectangle(snakeCords.get(0), Color.BLUE);
        for (int i = 1; i < snakeCords.size(); i++){
            drawRectangle(snakeCords.get(i), Color.SKYBLUE);
        }
    }

    private void clear(){
        pane.getChildren().clear();
    }

    private void setStateText(SnakeProto.GameState gameState){
        textArea.setText(stateToString(gameState));
        pane.requestFocus();
    }

    @Override
    public synchronized void updateState(SnakeProto.GameState gameState) {
        Platform.runLater(this::clear);
        for (SnakeProto.GameState.Coord foodCord: gameState.getFoodsList()){
            drawRectangle(foodCord, Color.RED);
        }

        for (SnakeProto.GameState.Snake snake: gameState.getSnakesList()){
            drawSnake(snake);
        }
        Platform.runLater(() -> setStateText(gameState));
    }

    @Override
    public void gotException(Exception exception) {
        exception.printStackTrace();
    }

    private void goToStartScreen() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/start.fxml"));
        try {
            Parent root = loader.load();
            StartScreenController controller = loader.getController();
            controller.setStage(stage);
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void showGameOverAlert() throws IOException {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Game over");
            alert.show();
        });
    }

    @Override
    public void gameOver() {
        if (watching)
            return;

        watching = true;

        if (clientThread.isAlive())
            clientThread.interrupt();

        if (serverThread != null && serverThread.isAlive())
            serverThread.interrupt();

        //send alert to user
        try {
            showGameOverAlert();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setConfig(SnakeProto.GameConfig config) {
        this.config = config;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void exitPressed(ActionEvent actionEvent) {
        if (clientThread.isAlive())
            clientThread.interrupt();

        if (serverThread != null && serverThread.isAlive())
            serverThread.interrupt();
        Platform.runLater(this::goToStartScreen);
    }
}
