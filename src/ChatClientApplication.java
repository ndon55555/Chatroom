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

public class ChatClientApplication extends Application {
    private final String HOST = "donraspberrypi.ddns.net";
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
        ClientChatConnection ccc = new ClientChatConnection(this.HOST, this.PORT, lblMessages, sp);

        // Makes sure that user doesn't pick a name longer than the max user name length.
        // Platform.runLater() allows any extra characters to be added to the end first
        // so that it definitely clears out all extra characters.
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
                ccc.sendMessage(new ClientMessage(tfUserName.getText(), tfUserInput.getText()));
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
        ccc.startReceivingMessages();
        Scene scene = new Scene(primaryPane);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Client Chat Application");
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
