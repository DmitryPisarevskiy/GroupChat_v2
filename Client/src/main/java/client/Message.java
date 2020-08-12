package client;

import javafx.collections.ObservableList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

public class Message implements Serializable {
    private final static String AUTH_OK="/authok";
    private final static String AUTH="/auth";
    private final static String END="/end";
    private final static String SEND_EXACT_USERS="/w";
    private final static String WHO_LOGGED_IN="/newclient";
    private final static String REG="/reg";
    private final static String REG_RESULT ="/regresult";
    private final static String CLIENT_LIST ="/clientlist";
    private final static String CHANGE_NICK ="/changenick";
    private final static String CHANGE_NICK_RESULT ="/changenickresult";
    private final static String RESULT_OK="ok";
    private final static String RESULT_FAILED="failed";

    private Date date;

    public Date getDate() {
        return date;
    }

    public String getSystemCommand() {
        return systemCommand;
    }

    public String getSender() {
        return sender;
    }

    public boolean isSystem() {
        return isSystem;
    }

    private boolean isSystem;
    private String systemCommand;
    private String sender;
    private String[] recievers;
    private String text;


    public String[] getRecievers() {
        return recievers;
    }

    public Message(String systemCommand) {
        date = new Date();
        isSystem=true;
        this.systemCommand = systemCommand;
        String[] token = systemCommand.split("\\s");
        if (token[0].equals(END)) {
            text = "Связь с сервером была прервана";
        } else if (token[0].equals(WHO_LOGGED_IN)) {
            text= token[1] + " вошел в чат";
        } else if (token[0].equals(CLIENT_LIST)) {
            text="";
        } else if (token[0].equals(CHANGE_NICK_RESULT)) {
            if (token[1].equals(RESULT_OK)) {
                text="Ваш ник был успешно изменен на " + token[2];
            } else {
                text="";
            }
        } else if (token[0].equals(REG_RESULT)) {
            if (token[1].equals(RESULT_OK)) {
                text="Регистрация прошла успешно!";
            } else {
                text="Регистрация не пройдена! \nПользователь с данным ником \nи/или логином уже существует";
            }
        } else if (token[0].equals(AUTH_OK)) {
            text="Вы вошли под ником " + token[1];
        } else {
            text=systemCommand;
        }
    }

    public Message(String sender, String[] recievers, String str) {
        this.sender = sender;
        this.recievers = recievers;
        isSystem=false;
        systemCommand="";
        date=new Date();
        text=str;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}