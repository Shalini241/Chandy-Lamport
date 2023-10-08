public class ChandyLamportProtocol implements Runnable{

    private final Node node;

    ChandyLamportProtocol(Node node){
        this.node = node;
    }
    @Override
    public void run() {
        try {
            Thread.sleep(NodeWrapper.getSnapshotDelay());
            startProtocol();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void startProtocol(){
        // null will denote that the current node started the protocol
        node.processChandyLamportProtocol(null);
    }
}
