package controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.snakeProto.SnakeProto;

public class HostGameScreenController {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private TextField widthTextField;

    @FXML
    private TextField heightTextField;

    @FXML
    private Button startButton;

    @FXML
    void startButtonPressed(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/fxml/game.fxml"));
        Parent root = loader.load();

        GameScreenController controller = loader.getController();
        controller.setStage(stage);

        controller.setConfig(SnakeProto.GameConfig.newBuilder()
                .setWidth(Integer.parseInt(widthTextField.getText()))
                .setHeight(Integer.parseInt(heightTextField.getText()))
                .setStateDelayMs(100)
                .build()
        );

        controller.setName(name);

        stage.setScene(new Scene(root));

        controller.hostGame();
    }

    Stage stage;
    String name;
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setName(String name) {
        this.name = name;
    }

    @FXML
    void initialize() {

    }
}
