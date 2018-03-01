import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    public static void main(String[] args) throws IOException {
        final int port = 10000;
        ServerSocket server = new ServerSocket(port);
        System.out.println("Server bound to port " + port + ".");
        ExecutorService executor = Executors.newCachedThreadPool();
        boolean isListeningForClients = true;

        while (isListeningForClients) {
            Socket client = server.accept();
            System.out.println("Client " + client.getInetAddress().getHostAddress() + " connected.");

            try {
                executor.execute(new ChatThread(client));
                System.out.println("Chat thread initiated for " + client.getInetAddress().getHostAddress() + ".");
            } catch (Exception e) {
                System.out.println("Could not start chat session for " + client.getInetAddress().getHostAddress() + ".");
            }
        }
    }
}
