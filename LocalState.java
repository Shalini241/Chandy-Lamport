import java.io.Serializable;

public class LocalState implements Serializable {
    private int[] vectorClock;
    private boolean activeStatus;
    private int nodeId;

    LocalState() {
        vectorClock = new int[NodeWrapper.getTotalNodes()];
        activeStatus = false;
    }

    public int[] getVectorClock() {
        return vectorClock;
    }

    public void setVectorClock(int[] vectorClock) {
        System.arraycopy(vectorClock, 0, this.vectorClock, 0, vectorClock.length);
    }

    public boolean isActiveStatus() {
        return activeStatus;
    }

    public void setActiveStatus(boolean activeStatus) {
        this.activeStatus = activeStatus;
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }
}
