import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;

public class ChatClientApplication extends Application {
    private final String HOST = "localhost";
    private final int PORT = 10000;
    private final int MAX_USER_NAME_LENGTH = 20;

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Create UI elements
        Label lblPromptName = new Label("Enter your name:");
        TextField tfUserName = new TextField();
        Label lblMessages = new Label();
        lblMessages.setWrapText(true);
        TextField tfUserInput = new TextField();
        System.out.println(tfUserInput.getPromptText());
        Button btnSend = new Button("Send Message");
        ScrollPane sp = new ScrollPane();
        sp.setContent(lblMessages);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        // Arrange UI elements
        HBox userNameStuff = new HBox(10);
        HBox userControls = new HBox(10);
        VBox primaryPane = new VBox(10);
        userNameStuff.getChildren().addAll(lblPromptName, tfUserName);
        userControls.getChildren().addAll(tfUserInput, btnSend);
        primaryPane.getChildren().addAll(userNameStuff, sp, userControls);
        primaryPane.setPadding(new Insets(5, 5, 5, 5));
        primaryPane.setPrefHeight(400);

        // Set property bindings
        lblMessages.prefWidthProperty().bind(sp.widthProperty().subtract(20));
        sp.prefHeightProperty().bind(primaryPane.heightProperty().subtract(userControls.heightProperty()));

        // Establish back and forth connections with server
        ChatConnection cc = new ChatConnection(this.HOST, this.PORT, lblMessages, sp);

        // Makes sure that user doesn't pick a name longer than the max user name length.
        // Platform.runLater() allows any extra characters to be added first so that it
        // definitely clears out all extra characters.
        tfUserName.setOnKeyTyped(event ->
            Platform.runLater(() -> {
                String userName = tfUserName.getText();

                if (userName.length() > MAX_USER_NAME_LENGTH) {
                    tfUserName.setText(userName.substring(0, MAX_USER_NAME_LENGTH));
                    tfUserName.end(); // moves caret to the end of the text
                }
            }));

        // Handle when user clicks button
        btnSend.setOnAction(event -> {
            String textInput = tfUserInput.getText();

            if (!textInput.isEmpty()) {
                cc.sendMessage(new ClientMessage(tfUserName.getText(), tfUserInput.getText()));
                tfUserInput.clear();
            }
        });

        // Handle for when user presses enter when in the Textfield
        tfUserInput.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                btnSend.fire();
            }
        });

        // Prepare the Application for use
        cc.startReceivingMessages();
        Scene scene = new Scene(primaryPane);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Client Chat Application");
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private static class ChatConnection {
        Label toBeWrittenOn;
        ScrollPane toBeAutoScrolled;
        Socket server;
        ServerWriter serverWriter;
        ServerReader serverReader;

        ChatConnection(String host, int port, Label toBeWrittenOn, ScrollPane toBeAutoScrolled) throws IOException {
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

            while (isReceivingMessages) {
                try {
                    ClientMessage msg = (ClientMessage) in.readObject();
                    String textToAdd = msg.getSenderName() + ":\n" + msg.getText() + "\n";

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
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
