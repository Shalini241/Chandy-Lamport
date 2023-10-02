import java.io.Serializable;

public class ChannelState implements Serializable {
    private Node sourceNode;
    private Node destinationNode;

    private int[] channelClock;

    public ChannelState() {
        channelClock = new int[NodeWrapper.getTotalNodes()];
    }

    public ChannelState(Node sourceNode, Node destinationNode, int[] channelClock) {
        this.channelClock = new int[NodeWrapper.getTotalNodes()];
        setSourceNode(sourceNode);
        setDestinationNode(destinationNode);
        setChannelClock(channelClock);
    }

    public Node getSourceNode() {
        return sourceNode;
    }

    public void setSourceNode(Node sourceNode) {
        this.sourceNode = sourceNode;
    }

    public int[] getChannelClock() {
        return channelClock;
    }

    public void setChannelClock(int[] channelClock) {
        System.arraycopy(channelClock, 0, this.channelClock, 0, channelClock.length);
    }

    public Node getDestinationNode() {
        return destinationNode;
    }

    public void setDestinationNode(Node destinationNode) {
        this.destinationNode = destinationNode;
    }
}
