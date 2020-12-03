import controller.StartScreenController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/start.fxml"));
        Parent root = loader.load();

        StartScreenController controller = loader.getController();
        controller.setStage(primaryStage);

        primaryStage.setTitle("Snake");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }
}
