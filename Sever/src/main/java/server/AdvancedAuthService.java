package server;

import java.sql.*;

public class AdvancedAuthService implements AuthService {
    private static Connection connection;
    private static Statement stmt;
    private int numOfRegisteredClients;

    public AdvancedAuthService() throws SQLException, ClassNotFoundException {
        connectDB();
    }


    @Override
    public String getNicknameByLoginAndPassword(String login, String password) throws SQLException {
        ResultSet rs = null;
            rs = stmt.executeQuery(String.format("SELECT nickname FROM users WHERE login=\"%s\" AND password=\"%s\";",login, password));
        if (rs.next()) {
            String nick=rs.getString(1);
            rs.close();
            return nick;
        }
        return null;
    }

    @Override
    public boolean registration(String login, String password, String nickname) throws SQLException {
        ResultSet rs = null;
        rs = stmt.executeQuery(String.format("SELECT password FROM users WHERE login=\"%s\" OR nickname=\"%s\";",login, nickname));
        if (rs.next()) {
            System.out.println("Регистрация нового пользователя не прошла");
            rs.close();
            return false;
        }
        stmt.executeUpdate(String.format("INSERT INTO users (login, password, nickname) VALUES (\"%s\",\"%s\", \"%s\")", login, password, nickname));
        System.out.println("Добавлен новый пользователь");
        rs.close();
        return true;
    }

    @Override
    public boolean changeNick(String login, String password, String nickname) throws SQLException {
        ResultSet rs = null;
        rs = stmt.executeQuery(String.format("SELECT nickname FROM users WHERE login=\"%s\" AND password=\"%s\";",login, password));
        if (rs.next()) {
            stmt.executeUpdate(String.format("UPDATE users SET nickname = \"%s\" WHERE login = \"%s\";",nickname,login));
            System.out.println("Изменение ника произведено");
            rs.close();
            return true;
        }
        return false;
    }

    public void connectDB() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:AuthDataBase.db");
        stmt = connection.createStatement();
        System.out.println("База данных подключена.");
        ResultSet rs = null;
        rs = stmt.executeQuery("SELECT nickname FROM users");
        numOfRegisteredClients=0;
        while (rs.next()) {
            numOfRegisteredClients++;
        }
        System.out.println("Зарегистрированных пользователей - " + numOfRegisteredClients + " человек");
        rs.close();
    }

    public int getNumOfRegisteredClients() {
        return numOfRegisteredClients;
    }

    public void disconnectDB() {
        try {
            connection.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        try {
            stmt.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}
