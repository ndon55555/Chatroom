import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class SendMessageClient {
    public static void main(String[] args) throws IOException {
        final String host = "localhost";//"donraspberrypi.ddns.net";
        final int port = 10000;
        Socket server = new Socket(host, port);
        System.out.println("Connected to " + host + " at port " + port + ".");

        new Thread(new ClientWriter(server.getOutputStream())).start();
        System.out.println("Client writer started.");
    }

    private static class ClientWriter implements Runnable {
        OutputStream outputStream;

        ClientWriter(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public void run() {
            Scanner stdIn = new Scanner(System.in);
            PrintWriter out = new PrintWriter(this.outputStream, true);

            while (stdIn.hasNextLine()) {
                out.println(stdIn.nextLine());
            }
        }
    }
}
