import java.io.Serializable;

public class MarkerMessage extends Message implements Serializable {
    public MarkerMessage() {
        super();
    }

    public MarkerMessage(Node sourceNode) {
        super("", sourceNode);
    }
}
