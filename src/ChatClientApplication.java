import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClientApplication extends Application {
    private final String HOST = "donraspberrypi.ddns.net";
    private final int PORT = 10000;

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Create UI elements
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
        HBox userControls = new HBox(10);
        userControls.getChildren().addAll(tfUserInput, btnSend);
        VBox primaryPane = new VBox(10);
        primaryPane.getChildren().addAll(sp, userControls);
        primaryPane.setPadding(new Insets(5, 5, 5, 5));
        primaryPane.setPrefHeight(400);

        // Set property bindings
        lblMessages.prefWidthProperty().bind(sp.widthProperty().subtract(20));
        sp.prefHeightProperty().bind(primaryPane.heightProperty().subtract(userControls.heightProperty()));

        // Establish back and forth connections with server
        ChatConnection cc = new ChatConnection(this.HOST, this.PORT, lblMessages, sp);

        // Handle when user clicks button
        btnSend.setOnAction(event -> {
            String textInput = tfUserInput.getText();

            if (!textInput.isEmpty()) {
                cc.sendMessage(tfUserInput.getText());
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

        void sendMessage(String msg) {
            this.serverWriter.println(msg);
        }

        void startReceivingMessages() {
            Thread readThread = new Thread(this.serverReader);
            readThread.setDaemon(true);
            readThread.start();
        }
    }

    private static class ServerWriter {
        PrintWriter out;

        ServerWriter(OutputStream outputStream) {
            this.out = new PrintWriter(outputStream, true);
        }

        void println(String msg) {
            out.println(msg);
        }
    }

    private static class ServerReader implements Runnable {
        Scanner in;
        Label toBeWrittenOn;
        ScrollPane toBeAutoScrolled;

        ServerReader(InputStream inputStream, Label toBeWrittenOn, ScrollPane toBeAutoScrolled) {
            this.in = new Scanner(inputStream);
            this.toBeWrittenOn = toBeWrittenOn;
            this.toBeAutoScrolled = toBeAutoScrolled;
        }

        @Override
        public void run() {
            while (in.hasNextLine()) {
                String newMsg = in.nextLine();

                // To change a JavaFX UI element from outside of the main thread, put the changes in Platform.runLater()
                Platform.runLater(() -> {
                    String prevMsg = this.toBeWrittenOn.getText();

                    if (!prevMsg.isEmpty()) {
                        this.toBeWrittenOn.setText(prevMsg + "\n" + newMsg);
                    } else {
                        this.toBeWrittenOn.setText(newMsg);
                    }

                    // Following line lets the ScrollPane know that its content and therefore its own properties have
                    // been updated. Can sort of think of this as a flush. This allows setVvalue to actually put it
                    // in the intended spot.
                    this.toBeAutoScrolled.layout();
                    // Pull the scrollbar down every time a message is received.
                    this.toBeAutoScrolled.setVvalue(1.0);
                });

                System.out.println(newMsg);
            }
        }
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
