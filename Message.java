import java.io.Serializable;

public class Message implements Serializable {
    private String message;
    private Node sourceNode;

    public Message() {

    }

    public Message(String message, Node sourceNode) {
        this.message = message;
        this.sourceNode = sourceNode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Node getSourceNode() {
        return sourceNode;
    }

    public void setSourceNode(Node sourceNode) {
        this.sourceNode = sourceNode;
    }

}

