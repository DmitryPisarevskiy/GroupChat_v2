package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class NickController implements Initializable {
    @FXML
    public TextField tfNewNickname;
    @FXML
    public PasswordField pfPassword;
    @FXML
    public TextArea taMsg;
    @FXML
    public Button btnChange;
    @FXML
    public Button btnCancel;

    private Stage stage;
    private Controller controller;

    public void tryToChangeNick(ActionEvent actionEvent) {
        taMsg.clear();
        if (tfNewNickname.getText().equals("") || pfPassword.getText().equals("")) {
            taMsg.appendText("Введите новый ник и пароль\n");
            return;
        }
        controller.tryToChangeNick(tfNewNickname.getText(),pfPassword.getText());
    }

    public void cancel(ActionEvent actionEvent) {
        stage.close();
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            stage = (Stage) taMsg.getScene().getWindow();
        });
    }
}
