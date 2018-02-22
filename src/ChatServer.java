import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatServer {
    public static void main(String[] args) throws IOException {
        final int port = 10000;
        ServerSocket server = new ServerSocket(port);
        System.out.println("Server bound to port " + port + ".");

        while (true) {
            Socket client = server.accept();
            System.out.println("Client " + client.getInetAddress().getHostAddress() + " connected.");
            new Thread(new ChatThread(client)).start();
            System.out.println("Chat thread initiated for " + client.getInetAddress().getHostAddress() + ".");
        }
    }
}
