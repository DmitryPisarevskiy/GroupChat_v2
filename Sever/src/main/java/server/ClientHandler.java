package server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;

public class ClientHandler {
    Server server;
    Socket socket = null;
    DataInputStream in;
    DataOutputStream out;

    private String nick;
    private String login;
    protected boolean clientIsAuth;
    private final int SOCKET_TIME_OUT = 5000;
    private Gson gson;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            clientIsAuth = false;
            GsonBuilder builder = new GsonBuilder();
            gson = builder.create();


            Server.executorService.execute(new Thread(() -> {
                try {
                    //цикл аутентификации
                    try {
                        socket.setSoTimeout(SOCKET_TIME_OUT);
                        Message msg;
                        while (true) {
                            msg = gson.fromJson(in.readUTF(),Message.class);
                            Server.LOGGER.log(Level.INFO,"Серверу пришло сообщение: " + gson.toJson(msg));
//                            System.out.println("Серверу пришло сообщение: " + gson.toJson(msg));
                            if (msg.isSystem()) {
                                String[] token = msg.getSystemCommand().split("\\s");
                                if (token[0].equals(Server.REG)) {
                                    boolean b = server.getAuthService().registration(token[1], token[2], token[3]);
                                    if (b) {
                                        Server.LOGGER.log(Level.INFO,"Прошла регистрация нового пользователя " + token[3]);
//                                        System.out.println("Прошла регистрация нового пользователя " + token[3] + "\n");
                                        sendSystemMsg(String.format("%s %s",Server.REG_RESULT, Server.RESULT_OK));
                                    } else {
                                        Server.LOGGER.log(Level.INFO,"Была неудачная попытка регистрации");
//                                        System.out.println("Была неудачная попытка регистрации");
                                        sendSystemMsg(String.format("%s %s",Server.REG_RESULT, Server.RESULT_FAILED));
                                    }
                                } else if (token[0].equals(Server.AUTH)) {
                                    if (token.length < 3) {
                                        continue;
                                    }
                                    String newNick = server
                                            .getAuthService()
                                            .getNicknameByLoginAndPassword(token[1], token[2]);
                                    boolean nickIsOnLine = server.nickIsOnLine(newNick);
                                    if (newNick == null) {
                                        sendSystemMsg("Неверный логин / пароль");
                                    } else if (!nickIsOnLine) {
                                        sendSystemMsg(String.format("%s %s", Server.AUTH_OK, newNick));
                                        nick = newNick;
                                        login = token[1];
                                        server.subscribe(this);
                                        Server.LOGGER.log(Level.INFO, String.format("Клиент %s подключился", nick));
//                                        System.out.printf("Клиент %s подключился \n", nick);
                                        server.broadcastSystemMsg(String.format("%s %s", Server.WHO_LOGGED_IN, nick));
                                        clientIsAuth = true;
                                        socket.setSoTimeout(0);
                                        break;
                                    } else {
                                        sendSystemMsg("Пользователь с данным логином уже зашел в чат");
                                    }
                                }
                            }
                        }
                    } catch (SocketTimeoutException e) {
                        sendSystemMsg(Server.END);
                    }

                    //цикл работы
                    while (clientIsAuth) {
                        Message msg=gson.fromJson(in.readUTF(),Message.class);
                        Server.LOGGER.log(Level.INFO, "Серверу пришло сообщение: " + gson.toJson(msg));
//                        System.out.println("Серверу пришло сообщение: " + gson.toJson(msg));
                        if (msg.isSystem()) {
                            String[] token = msg.getSystemCommand().split("\\s");
                            if (token[0].equals(Server.END)) {
                                sendSystemMsg(Server.END);
                                break;
                            } else if (token[0].equals(Server.CHANGE_NICK)) {
                                boolean b = server.getAuthService().changeNick(login, token[2], token[1]);
                                if (b) {
                                    sendSystemMsg(String.format("%s %s %s",Server.CHANGE_NICK_RESULT, Server.RESULT_OK, token[1]));
                                    nick=token[1];
                                    server.broadcastClientList();
                                } else {
                                    sendSystemMsg(String.format("%s %s",Server.CHANGE_NICK_RESULT, Server.RESULT_FAILED));
                                }
                            }
                        } else {
                            server.broadcastMsg(msg);
                        }
                    }
                } catch (IOException | SQLException e) {
                    e.printStackTrace();
                } finally {
                    Server.LOGGER.log(Level.WARNING,"Клиент отключился");
//                    System.out.println("Клиент отключился");
                    server.unsubscribe(this);
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
            }));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void sendSystemMsg(String str) {
        try {
            Message msg = new Message(str);
            String msgAsJSON = new Gson().toJson(msg);
            out.writeUTF(msgAsJSON);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void sendMsg(Message msg) {
        try {
            String msgAsJSON = new Gson().toJson(msg);
            out.writeUTF(msgAsJSON);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNick() {
        return nick;
    }
}
