import java.io.Serializable;

public class TerminateMessage extends Message implements Serializable {
    public TerminateMessage() {
        super();
    }

    public TerminateMessage(Node sourceNode) {
        super("", sourceNode);
    }
}
