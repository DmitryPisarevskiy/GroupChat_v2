package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class Server {
    private List<ClientHandler> clients;
    private AdvancedAuthService authService;
    final static String AUTH_OK="/authok ";
    final static String AUTH="/auth ";
    final static String END="/end";
    final static String SEND_EXACT_USERS="/w ";
    final static String WHO_LOGGED_IN="/new client ";
    final static String REG="/reg ";
    final static String REG_RESULT ="/regresult ";
    final static String CLIENT_LIST ="/clientlist ";
    final static String CHANGE_NICK ="/changenick ";
    static final String CHANGE_NICK_RESULT ="/changenickresult " ;

    public AuthService getAuthService() {
        return authService;
    }

    public Server() {
        clients = new Vector<>();

        ServerSocket server = null;
        Socket socket;

        final int PORT = 8189;

        try {
            server = new ServerSocket(PORT);
            System.out.println("Сервер запущен!");
            authService = new AdvancedAuthService();
            System.out.println("База данных подключена!");

            while (true) {
                socket = server.accept();
                System.out.println("Клиент подключился");
                System.out.println("socket.getRemoteSocketAddress(): "+socket.getRemoteSocketAddress());
                System.out.println("socket.getLocalSocketAddress() "+socket.getLocalSocketAddress());
                new ClientHandler(this, socket);
            }
        } catch (IOException | SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            authService.disconnectDB();
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void broadcastMsg(String sender, ArrayList<String> receivers, String msg){
        for (ClientHandler client : clients) {
            if (receivers.contains(client.getNick()) || sender.equals(client.getNick()))  {
                client.sendMsg(sender + "->" +receivers + ": " +msg);
            }
        }
    }

    void broadcastMsg(String sender, String msg){
        for (ClientHandler client : clients) {
            client.sendMsg(sender + "->everyone: " + msg);
        }
    }

    public void subscribe(ClientHandler clientHandler){
        clients.add(clientHandler);
        broadcastClientList();
    }

    public void unsubscribe(ClientHandler clientHandler){
        if (clientHandler.clientIsAuth) {
            broadcastMsg(clientHandler.getNick()+" вышел из чата");
        }
        clients.remove(clientHandler);
        broadcastClientList();
    }

    public List<ClientHandler> getClients() {
        return clients;
    }

    public void broadcastMsg(String s) {
        for (ClientHandler client : clients) {
            client.sendMsg(s);
        }
    }

    public void broadcastClientList() {
        StringBuilder sb=new StringBuilder("");
        sb.append(CLIENT_LIST);
        for (ClientHandler client : clients) {
            sb.append(client.getNick()).append(" ");
        }
        String str=sb.toString();
        broadcastMsg(str);
    }

    public Boolean nickIsOnLine(String nick) {
        for (ClientHandler client : clients) {
            if (client.getNick().equals(nick))  {
                return true;
            }
        }
        return false;
    }
}
