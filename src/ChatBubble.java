import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class ChatBubble extends VBox {
    private static boolean isLightBlue = true;
    private ClientMessage msg;
    private Label lblText;

    ChatBubble(ClientMessage msg) {
        super(1.0);
        Label lblSender = this.createSenderLabel(msg.getSenderName());
        Label lblText = this.createTextLabel(msg.getText());
        this.msg = msg;
        this.lblText = lblText;
        this.getChildren().addAll(lblSender, lblText);
        this.setMinHeight(53.0); // THIS IS A HACK!!! Allows scrolling down in ClientChatConnection to work :(
    }

    private Label createSenderLabel(String senderName) {
        Label lblSender = new Label(senderName + ":\n");
        lblSender.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, new CornerRadii(0.0), new Insets(0.0))));
        lblSender.setWrapText(true);
        lblSender.prefWidthProperty().bind(this.widthProperty());
        lblSender.setStyle("-fx-font-weight: bold");

        return lblSender;
    }

    private Label createTextLabel(String text) {
        Label lblText = new Label(text);
        Color bgColor = (ChatBubble.isLightBlue) ? Color.LIGHTBLUE : Color.LIGHTGREY;
        ChatBubble.isLightBlue = !ChatBubble.isLightBlue;
        lblText.setBackground(new Background(new BackgroundFill(bgColor, new CornerRadii(7.5), new Insets(0.0))));
        lblText.setPadding(new Insets(5));
        lblText.setWrapText(true);
        lblText.prefWidthProperty().bind(this.widthProperty());

        return lblText;
    }

    void addText(String newText) {
        Platform.runLater(() -> this.lblText.setText(this.lblText.getText() + "\n" + newText));
    }

    String getSenderName() {
        return this.msg.getSenderName();
    }

    boolean hasSenderName(String name) {
        return this.getSenderName().equals(name);
    }
}
