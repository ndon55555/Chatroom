import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;

import java.io.*;
import java.net.Socket;

class ClientChatConnection {
    private Label toBeWrittenOn;
    private ScrollPane toBeAutoScrolled;
    private Socket server;
    private ServerWriter serverWriter;
    private ServerReader serverReader;

    ClientChatConnection(String host, int port, Label toBeWrittenOn, ScrollPane toBeAutoScrolled) throws IOException {
        this.toBeWrittenOn = toBeWrittenOn;
        this.toBeAutoScrolled = toBeAutoScrolled;
        this.connectToServer(host, port);
        this.setupReaderAndWriter();
    }

    private void connectToServer(String host, int port) throws IOException {
        this.server = new Socket(host, port);
        System.out.println("Successfully connected to " + host + " at port " + port);
    }

    private void setupReaderAndWriter() throws IOException {
        this.serverWriter = new ServerWriter(server.getOutputStream());
        this.serverReader = new ServerReader(server.getInputStream(), this.toBeWrittenOn, this.toBeAutoScrolled);
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
        Label toBeWrittenOn;
        ScrollPane toBeAutoScrolled;

        ServerReader(InputStream inputStream, Label toBeWrittenOn, ScrollPane toBeAutoScrolled) throws IOException {
            this.in = new ObjectInputStream(inputStream);
            this.toBeWrittenOn = toBeWrittenOn;
            this.toBeAutoScrolled = toBeAutoScrolled;
        }

        @Override
        public void run() {
            boolean isReceivingMessages = true;
            String nameOfPrevSender = null;

            while (isReceivingMessages) {
                try {
                    ClientMessage msg = (ClientMessage) in.readObject();
                    String textToAdd;

                    if (msg.getSenderName().equals(nameOfPrevSender)) {
                        textToAdd = msg.getText();
                    } else {
                        textToAdd = msg.getSenderName() + ":\n" + msg.getText();
                    }

                    nameOfPrevSender = msg.getSenderName();

                    // To change a JavaFX UI element from outside of the main thread, put the changes in Platform.runLater()
                    Platform.runLater(() -> {
                        String prevMsgTexts = this.toBeWrittenOn.getText();

                        if (!prevMsgTexts.isEmpty()) {
                            this.toBeWrittenOn.setText(prevMsgTexts + "\n" + textToAdd);
                        } else {
                            this.toBeWrittenOn.setText(textToAdd);
                        }

                        // Following line lets the ScrollPane know that its content and therefore its own properties have
                        // been updated. Can sort of think of this as a flush. This allows setVvalue to actually put it
                        // in the intended spot.
                        this.toBeAutoScrolled.layout();
                        // Pull the scrollbar down every time a message is received.
                        this.toBeAutoScrolled.setVvalue(1.0);
                    });

                    System.out.println(textToAdd);
                } catch (Exception e) {
                    System.out.println("Problem reading message.");
                    isReceivingMessages = false;
                    e.printStackTrace();
                }
            }
        }
    }
}
