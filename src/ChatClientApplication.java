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
        Label lblMessages = new Label();
        lblMessages.setWrapText(true);
        TextField tfUserInput = new TextField();
        Button btnSend = new Button("Send Message");
        ScrollPane sp = new ScrollPane();
        sp.setContent(lblMessages);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        lblMessages.prefWidthProperty().bind(sp.widthProperty().subtract(20));

        HBox userControls = new HBox(10);
        userControls.getChildren().addAll(tfUserInput, btnSend);
        VBox primaryPane = new VBox(10);
        primaryPane.getChildren().addAll(sp, userControls);
        primaryPane.setPadding(new Insets(5, 5, 5, 5));
        primaryPane.setPrefHeight(400);
        sp.prefHeightProperty().bind(primaryPane.heightProperty().subtract(userControls.heightProperty()));

        Scene scene = new Scene(primaryPane);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Client Chat Application");
        primaryStage.setResizable(false);

        ChatConnection cc = new ChatConnection(this.HOST, this.PORT, lblMessages);

        btnSend.setOnAction(event -> {
            String textInput = tfUserInput.getText();

            if (!textInput.isEmpty()) {
                cc.sendMessage(tfUserInput.getText());
                tfUserInput.clear();
            }

            // Any time user sends a message, automatically scroll their scrollbar down
            sp.setVvalue(1.0);
        });

        tfUserInput.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                btnSend.fire();
            }
        });

        cc.startReceivingMessages();

        primaryStage.show();
    }

    private static class ChatConnection {
        Label toBeWrittenOn;
        Socket server;
        ServerWriter serverWriter;
        ServerReader serverReader;

        ChatConnection(String host, int port, Label toBeWrittenOn) throws IOException {
            this.toBeWrittenOn = toBeWrittenOn;
            this.connectToServer(host, port);
            this.setupReaderAndWriter();
        }

        private void connectToServer(String host, int port) throws IOException {
            this.server = new Socket(host, port);
            System.out.println("Successfully connected to " + host + " at port " + port);
        }

        private void setupReaderAndWriter() throws IOException {
            this.serverWriter = new ServerWriter(server.getOutputStream());
            this.serverReader = new ServerReader(server.getInputStream(), this.toBeWrittenOn);
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

        ServerReader(InputStream inputStream, Label toBeWrittenOn) {
            this.in = new Scanner(inputStream);
            this.toBeWrittenOn = toBeWrittenOn;
        }

        @Override
        public void run() {
            while (in.hasNextLine()) {
                String newMsg = in.nextLine();

                Platform.runLater(() -> {
                    String prevMsg = toBeWrittenOn.getText();

                    if (!prevMsg.isEmpty()) {
                        toBeWrittenOn.setText(prevMsg + "\n" + newMsg);
                    } else {
                        toBeWrittenOn.setText(newMsg);
                    }
                });

                System.out.println(newMsg);
            }
        }
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
