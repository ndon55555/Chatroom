import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.io.*;
import java.net.Socket;

class ClientChatConnection {
    private VBox chatBubbles;
    private ScrollPane toBeAutoScrolled;
    private Socket server;
    private ServerWriter serverWriter;
    private ServerReader serverReader;

    ClientChatConnection(String host, int port, VBox chatBubbles, ScrollPane toBeAutoScrolled) throws IOException {
        this.chatBubbles = chatBubbles;
        this.toBeAutoScrolled = toBeAutoScrolled;
        this.connectToServer(host, port);
        this.setupReaderAndWriter();
    }

    private void connectToServer(String host, int port) throws IOException {
        this.server = new Socket(host, port);
        System.out.println("Successfully connected to " + host + " at port " + port + ".");
    }

    private void setupReaderAndWriter() throws IOException {
        this.serverWriter = new ServerWriter(server.getOutputStream());
        this.serverReader = new ServerReader(server.getInputStream(), this.chatBubbles, this.toBeAutoScrolled);
    }

    void sendMessage(ClientMessage msg) {
        this.serverWriter.sendMessage(msg);
    }

    void startReceivingMessages() {
        Thread readThread = new Thread(this.serverReader);
        readThread.setDaemon(true);
        readThread.start();
    }

    private static class ServerWriter {
        ObjectOutputStream out;

        ServerWriter(OutputStream outputStream) throws IOException {
            this.out = new ObjectOutputStream(outputStream);
        }

        void sendMessage(ClientMessage msg) {
            try {
                out.writeObject(msg);
            } catch (Exception e) {
                System.out.println("Problem sending message.");
                e.printStackTrace();
            }
        }
    }

    private static class ServerReader implements Runnable {
        ObjectInputStream in;
        VBox chatBubbles;
        ScrollPane toBeAutoScrolled;

        ServerReader(InputStream inputStream, VBox chatBubbles, ScrollPane toBeAutoScrolled) throws IOException {
            this.in = new ObjectInputStream(inputStream);
            this.chatBubbles = chatBubbles;
            this.toBeAutoScrolled = toBeAutoScrolled;
        }

        @Override
        public void run() {
            boolean isReceivingMessages = true;
            ChatBubble lastChatBubble = null;

            while (isReceivingMessages) {
                try {
                    ClientMessage msg = (ClientMessage) in.readObject();

                    if (lastChatBubble == null || !lastChatBubble.hasSenderName(msg.getSenderName())) {
                        ChatBubble bubbleToAdd = new ChatBubble(msg);
                        Platform.runLater(() -> this.chatBubbles.getChildren().add(bubbleToAdd));
                        lastChatBubble = bubbleToAdd;
                    } else {
                        lastChatBubble.addText(msg.getText());
                    }

                    this.updateScrollBar();
                } catch (Exception e) {
                    System.out.println("Problem reading message.");
                    isReceivingMessages = false;
                    e.printStackTrace();
                }
            }
        }

        private void updateScrollBar() {
            // To change a JavaFX UI element from outside of the main thread, put the changes in Platform.runLater()
            Platform.runLater(() -> {
                // Following line lets the ScrollPane know that its content and therefore its own properties have
                // been updated. Can sort of think of this as a flush. This allows setVvalue to actually put it
                // in the intended spot.
                this.toBeAutoScrolled.layout();
                // Pull the scrollbar down every time a message is received.
                this.toBeAutoScrolled.setVvalue(1.0);
            });
        }
    }
}
