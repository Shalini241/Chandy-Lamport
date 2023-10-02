import java.io.Serializable;

public class ApplicationMessage extends Message implements Serializable {

   int[] messageClock;

    public ApplicationMessage() {
        this.messageClock = new int[NodeWrapper.getTotalNodes()];
    }

    public ApplicationMessage(int[] messageClock, Node sourceNode) {
        this.messageClock = new int[NodeWrapper.getTotalNodes()];
        System.arraycopy(messageClock, 0, this.messageClock, 0, messageClock.length);
        this.setSourceNode(sourceNode);
    }

}
