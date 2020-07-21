package client;


import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    public TextArea textArea;
    @FXML
    public TextField textField;
    @FXML
    public TextField loginField;
    @FXML
    public PasswordField passwordField;
    @FXML
    public HBox authPanel;
    @FXML
    public HBox msgPanel;
    @FXML
    public ListView<String> listOfUsers;
    @FXML
    public Button register;
    @FXML
    public MenuItem btnDisconnect;

    private final int PORT = 8189;
    private final String IP_ADDRESS = "localhost";
    private final String CHAT_TITLE_EMPTY = "Chat july 2020";
    private final static String AUTH_OK="/authok ";
    private final static String AUTH="/auth ";
    private final static String END="/end";
    private final static String SEND_EXACT_USERS="/w ";
    private final static String WHO_LOGGED_IN="/new client ";
    private final static String REG="/reg ";
    private final static String REG_RESULT ="/regresult ";
    private final static String CLIENT_LIST ="/clientlist ";
    private final static String CHANGE_NICK ="/changenick ";


    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private Stage regStage;
    RegController regController;

    private Stage nickStage;
    NickController nickController;

    private String nick;

    private Stage stage;

    public void setAuthenticated(boolean authenticated) {
        authPanel.setVisible(!authenticated);
        authPanel.setManaged(!authenticated);
        msgPanel.setVisible(authenticated);
        msgPanel.setManaged(authenticated);
        listOfUsers.setVisible(authenticated);
        listOfUsers.setManaged(authenticated);

        if (!authenticated) {
            nick = "";
        }
        setTitle(nick);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            stage = (Stage) textField.getScene().getWindow();
            stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent event) {
                    System.out.println("bye");
                    if (socket != null && !socket.isClosed()) {
                        try {
                            out.writeUTF(END);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        });

        setAuthenticated(false);
        regStage = createRegStage();
        nickStage = createNickChangeStage();
    }

    private void connect() {
        try {
            socket = new Socket(IP_ADDRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {

                    //цикл аутентификации
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith(AUTH_OK)) {
                            nick = str.split("\\s")[1];
                            setAuthenticated(true);
                            listOfUsers.setVisible(true);
                            listOfUsers.setManaged(true);
                            textArea.clear();
                            break;
                        } else if (str.equals(END)) {
                            throw new RuntimeException();
                        } else if (str.startsWith(REG_RESULT)) {
                            String result = str.split("\\s")[1];
                            if (result.equals("ok")) {
                                regController.regMessage("Регистрация прошла успешно!");
                            } else {
                                regController.regMessage("Регистрация не пройдена! \nПользователь с данным ником \nи/или логином уже существует");
                            }
                        } else {
                            setAuthenticated(false);
                            textArea.appendText(str + "\n");
                        }
                    }

                    //цикл работы
                    btnDisconnect.setDisable(false);
                    while (true) {

                        String str = in.readUTF();
                        if (str.equals(END)) {
                            setAuthenticated(false);
                            break;
                        } else if (str.startsWith(WHO_LOGGED_IN)) {
                            String[] token = str.split("\\s");
                            textArea.appendText("Пользователь " + token[2] + " вошел в чат\n");
                        } else if (str.startsWith(CLIENT_LIST)) {
                            String[] token = str.split("\\s");
                            Platform.runLater(() -> {
                                listOfUsers.getItems().clear();
                                for (int i = 1; i < token.length; i++) {
                                    listOfUsers.getItems().add(token[i]);
                                }
                            });
                        } else {
                            textArea.appendText(str + "\n");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (RuntimeException e) {
                    System.out.println("Сервер отключил соединение");
                } finally {
                    try {
                        in.close();
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void sendMsg(ActionEvent actionEvent) {
        try {
            out.writeUTF(textField.getText());
            textField.requestFocus();
            textField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToAuth(ActionEvent actionEvent) {
        if (loginField.getText().equals("") && passwordField.getText().equals("")) {
            textArea.appendText("Введите логин и пароль\n");
            return;
        }

        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            out.writeUTF(AUTH + loginField.getText().trim() + " " + passwordField.getText().trim());
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setTitle(String nick) {
        Platform.runLater(() -> {
            stage.setTitle(CHAT_TITLE_EMPTY + " : " + nick);
        });
    }

    public void close() {
        Stage stage = (Stage) textField.getScene().getWindow();
        stage.close();
    }

    public void openAbout(ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText(null);
        alert.setContentText("This is the program helping to connect each other. Version 1.0");
        alert.showAndWait();
    }

    public void showRegWindow(ActionEvent actionEvent) {
        regStage.show();
    }

    private Stage createRegStage() {
        Stage stage = new Stage();
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/Register.fxml"));
            Parent root = fxmlLoader.load();
            stage.setTitle("Registration Window");
            stage.setScene(new Scene(root, 300, 240));
            stage.initModality(Modality.APPLICATION_MODAL);
            regController = fxmlLoader.getController();
            regController.setController(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stage;
    }

    private Stage createNickChangeStage() {
        Stage stage = new Stage();
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/NicknameChange.fxml"));
            Parent root = fxmlLoader.load();
            stage.setTitle("Nickname Change Window");
            stage.setScene(new Scene(root, 300, 240));
//            stage.initModality(Modality.APPLICATION_MODAL);
            nickController = fxmlLoader.getController();
            nickController.setController(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stage;
    }

    protected void tryToReg(String login, String password, String nick) {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        try {
            out.writeUTF(String.format(REG + "%s %s %s", login, password, nick));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clickClientList(MouseEvent mouseEvent) {
        if (textField.getText().startsWith(SEND_EXACT_USERS)) {
            textField.appendText(" " + listOfUsers.getSelectionModel().getSelectedItem());
        } else {
            textField.setText(SEND_EXACT_USERS + listOfUsers.getSelectionModel().getSelectedItem());
        }
    }

    public void disconnect(ActionEvent actionEvent) {
        setAuthenticated(false);
        btnDisconnect.setDisable(true);
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showNickWindow(ActionEvent actionEvent) {
        nickStage.show();
    }

    public void tryToChangeNick(String nick, String password) {
        try {
            out.writeUTF(String.format(CHANGE_NICK + "%s %s", nick, password));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
