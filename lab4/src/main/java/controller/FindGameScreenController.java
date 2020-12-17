package controller;

import java.io.IOException;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import model.snakeNode.announcementMessageReceiver.AnnouncementMessageReceiver;
import model.snakeNode.announcementMessageReceiver.AnnouncementMessageReceiverListener;
import model.net.NetConfig;
import model.snakeProto.SnakeProto;

public class FindGameScreenController implements AnnouncementMessageReceiverListener {
    public VBox vbox;

    Stage stage;

    String name;

    Map<InetSocketAddress, SnakeProto.GameMessage.AnnouncementMsg> announcementMsgMap = new ConcurrentHashMap<>();

    MulticastSocket socket;

    AnnouncementMessageReceiver announcementMessageReceiver;
    Thread announcementMessageReceiverThread;
    @FXML
    void initialize() throws IOException {
        socket = new MulticastSocket(NetConfig.ANNOUNCEMENT_MSG_PORT);
        socket.joinGroup(InetAddress.getByName(NetConfig.ANNOUNCEMENT_MSG_ADDRESS));

        announcementMessageReceiver = new AnnouncementMessageReceiver(socket, this);
        announcementMessageReceiverThread = new Thread(announcementMessageReceiver);
        announcementMessageReceiverThread.start();
    }

    private void connect(InetSocketAddress server, SnakeProto.GameConfig config) throws IOException {
        socket.close();
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/fxml/game.fxml"));
        Parent root = loader.load();

        GameScreenController controller = loader.getController();
        controller.setStage(stage);

        controller.setConfig(config);

        controller.setName(name);

        stage.setScene(new Scene(root));

        controller.connect(server);
    }

    private String getConfigString(InetSocketAddress address, SnakeProto.GameMessage.AnnouncementMsg announcementMsg){
        SnakeProto.GameConfig config = announcementMsg.getConfig();
        StringBuilder builder = new StringBuilder();
        builder.append(address.toString()).append('\n')
                .append("Width: ").append(config.getWidth()).append('\n')
                .append("Height:").append(config.getHeight()).append('\n')
                .append("Food = ").append(config.getFoodStatic()).append(" + player_count*").append(config.getFoodPerPlayer()).append('\n');
                //.append("State delay: ").append(config.getStateDelayMs()).append('\n')
                //.append("Dead food prob: ").append(config.getDeadFoodProb()).append('\n')
                //.append("Ping delay: ").append(config.getPingDelayMs()).append('\n')
                //.append("Node timeout: ").append(config.getNodeTimeoutMs()).append('\n');
        builder.append("Players:\n");
        for (SnakeProto.GamePlayer player: announcementMsg.getPlayers().getPlayersList()){
            builder.append(player.getName()).append(": ").append(player.getIpAddress()).append(":").append(player.getPort());
        }

        return builder.toString();
    }

    private void printAnnouncements(){
        Platform.runLater(() -> vbox.getChildren().clear());

        for (Map.Entry<InetSocketAddress, SnakeProto.GameMessage.AnnouncementMsg> entry: announcementMsgMap.entrySet()){

            Button button = new Button();
            button.setTextAlignment(TextAlignment.CENTER);
            button.setPrefWidth(500);
            button.setText(getConfigString(entry.getKey(), entry.getValue()));

            button.setOnAction(event -> {
                try {
                    System.out.println("Connecting to " + entry.getKey().toString());
                    connect(entry.getKey(), entry.getValue().getConfig());
                } catch (IOException e) {
                    return;
                }
            });

            Platform.runLater(() -> vbox.getChildren().add(button));
        }
    }

    @Override
    public void gotAnnouncementMessage(InetSocketAddress serverAddress, SnakeProto.GameMessage.AnnouncementMsg announcementMsg) {
        announcementMsgMap.put(serverAddress, announcementMsg);
        printAnnouncements();
    }

    @Override
    public void announcementTimeout(InetSocketAddress serverAddress) {
        announcementMsgMap.remove(serverAddress);
        printAnnouncements();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setName(String name) {
        this.name = name;
    }
}
