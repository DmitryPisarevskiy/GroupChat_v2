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
    final static String AUTH_OK="/authok";
    final static String AUTH="/auth";
    final static String END="/end";
    final static String SEND_EXACT_USERS="/w";
    final static String WHO_LOGGED_IN="/newclient";
    final static String REG="/reg";
    final static String REG_RESULT ="/regresult";
    final static String CLIENT_LIST ="/clientlist";
    final static String CHANGE_NICK ="/changenick";
    final static String CHANGE_NICK_RESULT ="/changenickresult" ;
    final static String RESULT_OK="ok";
    final static String RESULT_FAILED="failed";

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
            try {
                assert authService != null;
                authService.disconnectDB();
                assert server != null;
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

//    void broadcastMsg(String sender, ArrayList<String> receivers, Message msg){
//        for (ClientHandler client : clients) {
//            if (receivers.contains(client.getNick()) || sender.equals(client.getNick()))  {
//                client.sendSystemMsg(sender + "->" +receivers + ": " +msg);
//            }
//        }
//    }

    void broadcastMsg(Message msg){
        if (msg.isSystem()) {
            for (ClientHandler client : clients) {
                client.sendSystemMsg(msg.getSystemCommand());
            }
        } else {
            if (!msg.getRecievers().equals("")) {
                String[] token=msg.getRecievers().split("\\s");
                int i = 0;
                while (i < token.length) {
                    for (ClientHandler clientHandler : clients) {
                        if (clientHandler.getNick().equals(token[i])) {
                            clientHandler.sendMsg(msg);
                        }
                    }
                    i++;
                }
            } else {
                for (ClientHandler clientHandler : clients) {
                    clientHandler.sendMsg(msg);
                }
            }
        }
    }

    public void subscribe(ClientHandler clientHandler){
        clients.add(clientHandler);
        broadcastClientList();
    }

    public void unsubscribe(ClientHandler clientHandler){
        if (clientHandler.clientIsAuth) {
            broadcastSystemMsg(clientHandler.getNick()+" вышел из чата");
        }
        clients.remove(clientHandler);
        broadcastClientList();
    }

    public List<ClientHandler> getClients() {
        return clients;
    }

    public void broadcastSystemMsg(String s) {
        for (ClientHandler client : clients) {
            client.sendSystemMsg(s);
        }
    }

    public void broadcastClientList() {
        StringBuilder sb=new StringBuilder("");
        sb.append(CLIENT_LIST+" ");
        for (ClientHandler client : clients) {
            sb.append(client.getNick()).append(" ");
        }
        String str=sb.toString();
        broadcastSystemMsg(str);
    }

    public boolean nickIsOnLine(String nick) {
        for (ClientHandler client : clients) {
            if (client.getNick().equals(nick))  {
                return true;
            }
        }
        return false;
    }
}
