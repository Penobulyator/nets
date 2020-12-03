package model.MsgSender;

import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;

public interface MessageSenderListener {
    void connectionNotResponding(InetSocketAddress address);
}
