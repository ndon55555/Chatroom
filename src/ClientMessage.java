import java.io.Serializable;

class Message implements Serializable {
    private String senderName;
    private String text;

    Message(String senderName, String text) {
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
