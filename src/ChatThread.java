import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ChatThread implements Runnable {
    private static Map<Socket, ObjectOutputStream> clientWriters = Collections.synchronizedMap(new HashMap<>());
    private Socket client;
    private String clientAddress;

    // constructor
    ChatThread(Socket client) {
        this.client = client;
        this.clientAddress = client.getInetAddress().getHostAddress();
        this.setupReaderAndWriter();
    }

    private void setupReaderAndWriter() {
        try {
            System.out.println("Setting up client " + clientAddress + "'s writer.");
            ChatThread.clientWriters.put(this.client, new ObjectOutputStream(this.client.getOutputStream()));
        } catch (Exception e) {
            System.out.println("Something went wrong with setting up "
                    + clientAddress
                    + "'s writer.");
            this.closeSocket();
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try (ObjectInputStream in = new ObjectInputStream(this.client.getInputStream())) {
            boolean isReceiving = true;

            while (isReceiving) {
                ClientMessage clientMsg = (ClientMessage) in.readObject();
                System.out.println(this.clientAddress + ": " + clientMsg.getText());
                this.sendToAllClients(clientMsg);
            }
        } catch (Exception e) {
            ChatThread.clientWriters.remove(this.client);
            System.out.println(this.clientAddress + "'s chat session abruptly ended.");
        }

        this.closeSocket();
    }

    private void sendToAllClients(ClientMessage msg) {
        for (Socket otherClient : ChatThread.clientWriters.keySet()) {
            ObjectOutputStream out = ChatThread.clientWriters.get(otherClient);

            try {
                out.writeObject(msg);
                out.flush();
            } catch (Exception e) {
                System.out.println("Couldn't send message to " + otherClient.getInetAddress().getHostAddress() + ".");
            }
        }
    }

    private void closeSocket() {
        try {
            System.out.println("Attempting to close "
                    + this.clientAddress
                    + "'s socket...");
            this.client.close();
            System.out.println(this.clientAddress + "'s socket closed.");
        } catch (IOException ex) {
            System.out.println("Unable to close "
                    + this.clientAddress
                    + "'s socket.");
        }
    }
}
