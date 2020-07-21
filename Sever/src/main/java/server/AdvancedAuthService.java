package server;

import java.sql.*;

public class AdvancedAuthService implements AuthService {
    private static Connection connection;
    private static Statement stmt;

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
    public void changeNick(String login, String password, String nickname) throws SQLException {
//        ResultSet rs = null;
//        rs = stmt.executeQuery(String.format("SELECT nickname FROM users WHERE login=\"%s\" AND password=\"%s\";",login, nickname));
//        if (rs.next()) {
//            System.out.println("Регистрация нового пользователя не прошла");
//            rs.close();
//            return false;
//        }
//        stmt.executeUpdate(String.format("INSERT INTO users (login, password, nickname) VALUES (\"%s\",\"%s\", \"%s\")", login, password, nickname));
//        stmt.executeUpdate(String.format("UPDATE users SET nickname = \"%s\" WHERE score = 100;"));
//        System.out.println("Добавлен новый пользователь");
//        rs.close();
//        return true;
    }

    public void connectDB() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:AuthDataBase.db");
        stmt = connection.createStatement();
        System.out.println("База данных подключена");
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
