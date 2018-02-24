import java.io.Serializable;

class ClientMessage implements Serializable {
    private String senderName;
    private String text;

    ClientMessage(String senderName, String text) {
        this.senderName = senderName;
        this.text = text;
    }

    String getSenderName() {
        return this.senderName;
    }

    String getText() {
        return this.text;
    }
}
