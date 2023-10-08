import java.io.Serializable;

public class LocalState implements Serializable {
    private int[] vectorClock;
    private int nodeId;

    LocalState() {
        vectorClock = new int[NodeWrapper.getTotalNodes()];
    }

    public int[] getVectorClock() {
        return vectorClock;
    }

    public void setVectorClock(int[] vectorClock) {
        System.arraycopy(vectorClock, 0, this.vectorClock, 0, vectorClock.length);
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }
}
