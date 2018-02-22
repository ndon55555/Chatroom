import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ChatThread implements Runnable {
    private static Map<Socket, Scanner> clientScanners = Collections.synchronizedMap(new HashMap<>());
    private static Map<Socket, PrintWriter> clientWriters = Collections.synchronizedMap(new HashMap<>());
    private Socket client;
    private String header;

    // constructor
    public ChatThread(Socket client) {
        this.client = client;
        this.header = client.getInetAddress().getHostName();
        this.setupReaderAndWriter();
    }

    private void setupReaderAndWriter() {
        try {
            System.out.println("Setting up client reader and writer.");
            ChatThread.clientScanners.put(this.client, new Scanner(this.client.getInputStream()));
            ChatThread.clientWriters.put(this.client, new PrintWriter(this.client.getOutputStream(), true));
        } catch (Exception e) {
            System.out.println("Something went wrong with setting up "
                    + header
                    + "'s connections.");
            this.removeScannerAndWriter();
            this.closeSocket();
        }
    }

    @Override
    public void run() {
        try (Scanner in = new Scanner(this.client.getInputStream())) {
            while (in.hasNextLine()) {
                String msg = in.nextLine();
                System.out.println(this.header + ": " + msg);
                this.sendToAllClients(msg);
            }

            System.out.println(this.header + " has stopped sending data.");
        } catch (Exception e) {
            this.removeScannerAndWriter();
            System.out.println(this.header + "'s chat session abruptly ended.");
            e.printStackTrace();
        }

        this.closeSocket();
    }

    private void sendToAllClients(String msg) {
        for (Socket otherClient : ChatThread.clientWriters.keySet()) {
            PrintWriter out = ChatThread.clientWriters.get(otherClient);
            out.println(this.header + ": " + msg);
        }
    }

    private void removeScannerAndWriter() {
        ChatThread.clientScanners.remove(this.client);
        ChatThread.clientWriters.remove(this.client);
    }

    private void closeSocket() {
        try {
            System.out.println("Attempting to close "
                    + this.header
                    + "'s socket...");
            this.client.close();
            System.out.println(this.header + "'s socket closed.");
        } catch (IOException ex) {
            System.out.println("Unable to close "
                    + this.header
                    + "'s socket.");
        }
    }
}
