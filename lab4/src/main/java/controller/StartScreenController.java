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

public class StartScreenController {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private TextField nameTextField;

    @FXML
    private Button hostGameButton;

    @FXML
    private Button findGameButton;

    Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    void findGameButtonPressed(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/findGame.fxml"));
        Parent root = loader.load();

        FindGameScreenController controller = loader.getController();
        controller.setStage(stage);
        controller.setName(nameTextField.getText());
        stage.setScene(new Scene(root));
    }

    @FXML
    void hostGameButtonPressed(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/hostGame.fxml"));
        Parent root = loader.load();

        HostGameScreenController controller = loader.getController();
        controller.setStage(stage);
        controller.setName(nameTextField.getText());
        stage.setScene(new Scene(root));
    }

    @FXML
    void initialize() throws IOException {

    }
}
