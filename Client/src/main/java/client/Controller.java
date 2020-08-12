package client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML public TextArea textArea;
    @FXML public TextField textField;
    @FXML public TextField loginField;
    @FXML public PasswordField passwordField;
    @FXML public HBox authPanel;
    @FXML public HBox msgPanel;
    @FXML public ListView<String> listOfUsers;
    @FXML public Button register;
    @FXML public MenuItem btnDisconnect;
    @FXML public MenuItem btnChangeNick;
    @FXML public MenuItem btnClearHistory;
    @FXML public TextFlow textFlow;

    private final int PORT = 8189;
    private final String IP_ADDRESS = "127.0.0.1";
    private final String CHAT_TITLE_EMPTY = "Chat july 2020";
    private final int MAX_MSG=100;
    private final static String AUTH_OK="/authok";
    private final static String AUTH="/auth";
    private final static String END="/end";
    private final static String SEND_EXACT_USERS="/w";
    private final static String WHO_LOGGED_IN="/newclient";
    private final static String REG="/reg";
    private final static String REG_RESULT ="/regresult";
    private final static String CLIENT_LIST ="/clientlist";
    private final static String CHANGE_NICK ="/changenick";
    private final static String CHANGE_NICK_RESULT ="/changenickresult" ;
    private final static String RESULT_OK="ok";
    private final static String RESULT_FAILED="failed";
    private final static DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm");

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private ObjectInputStream ois;
    private ObjectOutputStream oos;

    private Stage regStage;
    RegController regController;

    private Stage nickStage;
    NickController nickController;

    private String nick;
    private Stage stage;
    private boolean authenticated=false;

    private File history;

    private LinkedList<Message> msgList;
    private Gson gson;


    public void setAuthenticated(boolean auth) {
        authPanel.setVisible(!auth);
        authPanel.setManaged(!auth);
        msgPanel.setVisible(auth);
        msgPanel.setManaged(auth);
        listOfUsers.setVisible(auth);
        listOfUsers.setManaged(auth);

        if (!auth) {
            nick = "";
            btnDisconnect.setDisable(true);
            btnChangeNick.setDisable(true);
            btnClearHistory.setDisable(true);
        } else {
            setTitle(nick);
            MultipleSelectionModel<String> langsSelectionModel = listOfUsers.getSelectionModel();
            langsSelectionModel.setSelectionMode(SelectionMode.MULTIPLE);
            listOfUsers.setOnMouseClicked(event -> {
                String str = event.getTarget().toString();
                if (str.contains("null")) {
                    listOfUsers.getSelectionModel().clearSelection();
                };
            });
            if (!new File("Client/src/main/resources/histories").exists()) {
                new File("Client/src/main/resources/histories").mkdirs();
            }
            history=new File(String.format("Client/src/main/resources/histories/history_%s.txt", nick));
            try {
                if (!history.exists()) {
                    history.createNewFile();
                    if (!authenticated) {
                        msgList = new LinkedList<>();
                    } else {
//                        textArea.clear();
                        clearTextFlow();
                    }
                } else {
                    ois = new ObjectInputStream(new FileInputStream(history));
                    msgList = (LinkedList<Message>) ois.readObject();
                    ois.close();
                    for (Message message : msgList) {
                        if (!message.isSystem()) {
//                            textArea.appendText(String.format("%s, [%s]->%s: %s\n",DATE_FORMAT.format(message.getDate()), message.getSender(),
//                                    message.getRecievers().length==0?"[everyone]": Arrays.toString(message.getRecievers()),message.getText()));
                            addMsgToTextFlow(message);
                        } else {
                            addMsgToTextFlow(message);
//                            textArea.appendText(message.getText()+"\n");
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        authenticated=auth;
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
        GsonBuilder builder = new GsonBuilder();
        gson = builder.create();
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
                        Message msg = gson.fromJson(in.readUTF(), Message.class);
                        System.out.println("От сервера пришло сообщение " + gson.toJson(msg));
                        if (msg.isSystem()) {
                            String[] token = msg.getSystemCommand().split("\\s");
                            if (token[0].equals(AUTH_OK)) {
                                nick = token[1];
//                                textArea.clear();
                                clearTextFlow();
                                setAuthenticated(true);
//                                textArea.appendText(msg.getText()+"\n");
                                addMsgToTextFlow(msg);
                                listOfUsers.setVisible(true);
                                listOfUsers.setManaged(true);
                                addMsgToHistory(msg);
                                break;
                            } else if (token[0].equals(END)) {
                                System.out.println("Связь с сервером была прервана");
                                throw new RuntimeException();
                            } else if (token[0].equals(REG_RESULT)) {
                                if (token[1].equals(RESULT_OK)) {
                                    regController.regMessage("Регистрация прошла успешно!");
                                } else {
                                    regController.regMessage("Регистрация не пройдена! \nПользователь с данным ником \nи/или логином уже существует");
                                }
                            } else {
                                setAuthenticated(false);
//                                textArea.appendText(msg.getText() + "\n");
                                addMsgToTextFlow(new Message(msg.getText() + "\n"));
                            }
                        }
                        addMsgToHistory(msg);
                    }

                    //цикл работы
                    btnDisconnect.setDisable(false);
                    btnChangeNick.setDisable(false);
                    btnClearHistory.setDisable(false);
                    while (true) {
                        Message msg=gson.fromJson(in.readUTF(),Message.class);
                        System.out.println("От сервера пришло сообщение " + gson.toJson(msg));
                        addMsgToHistory(msg);
                        if (msg.isSystem()) {
                            String[] token = msg.getSystemCommand().split("\\s");
                            if (token[0].equals(END)) {
                                setAuthenticated(false);
                                break;
                            } else if (token[0].equals(WHO_LOGGED_IN)) {
                                if (!token[1].equals(nick)) {
//                                    textArea.appendText(msg.getText()+"\n");
                                    addMsgToTextFlow(msg);
                                }
                            } else if (token[0].equals(CLIENT_LIST)) {
                                Platform.runLater(() -> {
                                    listOfUsers.getItems().clear();
                                    for (int i = 1; i < token.length; i++) {
                                        listOfUsers.getItems().add(token[i]);
                                    }
                                });
                            } else if (token[0].equals(CHANGE_NICK_RESULT)) {
                                if (token[1].equals(RESULT_OK)) {
                                    nick=token[2];
                                    nickController.regMessage("Ник успешно изменен!");
                                    setAuthenticated(true);
//                                    textArea.appendText(msg.getText()+"\n");
                                    addMsgToTextFlow(msg);
                                } else {
                                    nickController.regMessage("Ник не изменен. Возможно \nвы неверно ввели пароль");
                                }
                            }
                        } else {
//                            textArea.appendText(String.format("%s, [%s]->%s: %s\n",DATE_FORMAT.format(msg.getDate()), msg.getSender(),
//                                    msg.getRecievers().length==0?"[everyone]": Arrays.toString(msg.getRecievers()),msg.getText()));
                            addMsgToTextFlow(msg);
                        }
                    }
                } catch (IOException | RuntimeException e) {
                    e.printStackTrace();
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

    private void clearTextFlow() {
        Platform.runLater(()->{
            textFlow.getChildren().clear();
        });
    }

    private void addMsgToTextFlow(Message msg) {
        Text date = new Text(DATE_FORMAT.format(msg.getDate()));
        date.setStyle("-fx-font-weight: bold");
        date.setUnderline(true);
        Text text = new Text(msg.getText()+"\n");
        if (msg.isSystem()) {
            Platform.runLater(()->{
                textFlow.getChildren().addAll(date, text);
            });
        } else {
            Text preText = new Text(String.format(" [%s]->%s: ",msg.getSender(),
                    msg.getRecievers().length==0?"[everyone]": Arrays.toString(msg.getRecievers())));
            preText.setFill(Color.GREEN);
            preText.setStyle("-fx-font-weight: bold");
            Platform.runLater(()->{
                textFlow.getChildren().addAll(date, preText, text);
            });
        }
    }

    private void addMsgToHistory(Message msg) throws IOException {
        if (!msg.isSystem()
                || msg.getSystemCommand().startsWith(AUTH_OK)
                || (msg.getSystemCommand().startsWith(WHO_LOGGED_IN) && !msg.getSystemCommand().endsWith(nick))) {
            msgList.addLast(msg);
            if (msgList.size()>MAX_MSG) {
                msgList.removeFirst();
            }
            oos = new ObjectOutputStream(new FileOutputStream(history));
            oos.writeObject(msgList);
            oos.close();
        }
    }


    public void sendMsg(ActionEvent actionEvent) {
        try {
            if (!textField.getText().equals("")) {
                System.out.println(listOfUsers.getSelectionModel().getSelectedItems());
                Message msg = new Message(nick, listOfUsers.getSelectionModel().getSelectedItems().toArray(new String[0]), textField.getText());
                String msgAsJSON = new Gson().toJson(msg);
                out.writeUTF(msgAsJSON);
                textField.requestFocus();
                textField.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void tryToAuth(ActionEvent actionEvent) {
        if (loginField.getText().equals("") && passwordField.getText().equals("")) {
//            textArea.appendText("Введите логин и пароль\n");
            addMsgToTextFlow(new Message("Введите логин и пароль\n"));
            return;
        }
        if (socket == null || socket.isClosed()) {
            connect();
        }
        try {
            Message msg = new Message(String.format("%s %s %s",AUTH, loginField.getText().trim(), passwordField.getText().trim()));
            String msgAsJSON = new Gson().toJson(msg);
            System.out.println("Серверру направлено: " + msgAsJSON);
            out.writeUTF(msgAsJSON);
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
            stage.setScene(new Scene(root, 310, 240));
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
            stage.setScene(new Scene(root, 300, 150));
            stage.initModality(Modality.APPLICATION_MODAL);
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
            Message msg = new Message(String.format("%s %s %s %s", REG, login, password, nick));
            String msgAsJSON = new Gson().toJson(msg);
            System.out.println("Серверру направлено: " + msgAsJSON);
            out.writeUTF(msgAsJSON);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void clickClientList(MouseEvent mouseEvent) {

    }


    public void disconnect(ActionEvent actionEvent) {
        setAuthenticated(false);
        btnDisconnect.setDisable(true);
        btnChangeNick.setDisable(true);
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
            Message msg = new Message(String.format("%s %s %s",CHANGE_NICK, nick, password));
            String msgAsJSON = new Gson().toJson(msg);
            System.out.println("Серверру направлено: " + msgAsJSON);
            out.writeUTF(msgAsJSON);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearHistory(ActionEvent actionEvent) throws IOException {
        msgList.clear();
//        textArea.clear();
        clearTextFlow();
        oos = new ObjectOutputStream(new FileOutputStream(history));
        oos.writeObject(msgList);
        oos.close();
    }
}
