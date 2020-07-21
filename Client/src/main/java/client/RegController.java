package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class RegController implements Initializable {
    @FXML
    public TextField tfLogin;
    @FXML
    public PasswordField pfPassword;
    @FXML
    public PasswordField pfRepeatPassword;
    @FXML
    public TextField tfNickname;
    @FXML
    public Button btnRegister;
    @FXML
    public Button btnCancel;
    @FXML
    public TextArea taMsg;

    private Stage stage;
    private Controller controller;

    public void setController(Controller controller) {
        this.controller = controller;
    }


    public void tryToRegister(ActionEvent actionEvent) {
        taMsg.clear();
        if (tfLogin.getText().equals("") || tfNickname.getText().equals("") || pfPassword.getText().equals("") || pfRepeatPassword.getText().equals("")) {
            taMsg.appendText("Заполните все поля\n");
        } else if (pfPassword.getText().equals(pfRepeatPassword.getText())) {
            controller.tryToReg(tfLogin.getText().trim()
                    ,pfPassword.getText().trim()
                    ,tfNickname.getText().trim());
        } else {
            taMsg.appendText("Пароли в полях Password и \nRepeat password не совпадают\n");
        }
    }

    public void cancel(ActionEvent actionEvent) {
        stage.close();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            stage = (Stage) btnCancel.getScene().getWindow();
        });
    }

    public void regMessage(String message) {
        taMsg.clear();
        taMsg.appendText(message+"\n");
    }
}
