import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Node implements Serializable {

    private static final Logger logger = Logger.getLogger(Node.class.getName());

    private final String hostName;

    private final int port;

    private NodeColor color;

    private transient Server server;

    public int getNodeId() {
        return nodeId;
    }

    private final int nodeId;
    private List<Node> neighbors;
    private transient Boolean active;

    private transient int messagesSent;

    private int[] vectorClock;

    private transient LocalState localState;

    private transient ArrayList<ChannelState> channelStates;
    private transient GlobalState globalState;

    private Node parent;

    private transient HashMap<Integer, Boolean> receivedMarkers;

    public HashMap<Integer, Boolean> getReceivedMarkers() {
        return receivedMarkers;
    }

    public void setReceivedMarkers(HashMap<Integer, Boolean> receivedMarkers) {
        this.receivedMarkers = receivedMarkers;
    }

    public ArrayList<ChannelState> getChannelStates() {
        return channelStates;
    }

    public void setChannelStates(ArrayList<ChannelState> channelStates) {
        this.channelStates = channelStates;
    }

    public GlobalState getGlobalState() {
        return globalState;
    }

    public void setGlobalState(GlobalState globalState) {
        this.globalState = globalState;
    }

    public List<Node> getNeighbors() {
        return neighbors;
    }
    public void setNeighbors(ArrayList<Node> neighbours) {
        this.neighbors = neighbours;
    }

    public boolean isActive() {
        return active;
    }

    public int getMessagesSent() {
        return messagesSent;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setMessagesSent(int messagesSent) {
        this.messagesSent = messagesSent;
    }
    public int getPort() {
        return port;
    }

    public LocalState getLocalState() {
        return localState;
    }

    public void setLocalState(LocalState localState) {
        this.localState = localState;
    }


    public String getHostName() {
        return hostName;
    }

    public Node getParent() {
        return parent;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public NodeColor getColor() {
        return color;
    }

    public void setColor(NodeColor color) {
        this.color = color;
    }
    public int[] getVectorClock() {
        return vectorClock;
    }

    public void setVectorClock(int[] vectorClock) {
        this.vectorClock = vectorClock;
    }


    public Node(int id, String hostName, int port) throws IOException {
        this.hostName = hostName;
        this.port = port;
        this.nodeId = id;
        this.active = true;
        this.color = NodeColor.BLUE;
        vectorClock = new int[NodeWrapper.getTotalNodes()];
        server = new Server();
        localState = new LocalState();
        channelStates = new ArrayList<>();
        globalState = new GlobalState();
        receivedMarkers = new HashMap<>();
        FileHandler fh = new FileHandler("myapp.log");
        logger.addHandler(fh);

        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);
    }

    public void initializeNode() {
        // Start Listening thread
        new Thread(new ListenerThread()).start();
        // Put the main thread in sleep for few seconds
        try {
            Thread.sleep(20000);
            server.initializeNeighbors(neighbors);
            //Start Chandy lamport protocol
            if(nodeId == 0){
                // sleep for 10 seconds before starting the protocol
                Thread.sleep(10000);
                sendApplicationMessages();
                new Thread(new ChandyLamportProtocol(this)).start();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void initializeReceivedMarkerMap() {
        // set or reset logmap to false
        this.receivedMarkers = new HashMap<>();
        for (Node neighbourNode : this.neighbors) {
            this.receivedMarkers.put(neighbourNode.nodeId, false);
        }
    }

    class ListenerThread implements Runnable {
        //private Socket socket;
        private ServerSocket serverSocket;

        public ListenerThread() {

        }

        @Override
        public void run() {
            try {
//                System.out.print("Started Listening from " + nodeId);
                serverSocket = new ServerSocket(getPort());
                // creating new message procesisng thread for each socket getting openned
                while (true) {
                    Socket socket = serverSocket.accept();
                    new Thread(new MessageProcessingThread(socket)).start();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class MessageProcessingThread implements Runnable {
        ObjectInputStream inputStream;
        Socket socket;

        public MessageProcessingThread(Socket socket) {
            this.socket = socket;
//            System.out.println(socket.toString());
            try {
                inputStream = new ObjectInputStream(socket.getInputStream());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Message incomingMessage = (Message) inputStream.readObject();
                    if(incomingMessage instanceof ApplicationMessage){
                        ApplicationMessage applicationMessage = (ApplicationMessage) incomingMessage;
                        synchronized (Node.this.active) {
                            Node.this.active = (Node.this.messagesSent < NodeWrapper.getMaxNumber());
                        }
                        System.out.println("Received Application message");
                        synchronized (Node.this.vectorClock) {
                            for (int i = 0; i < Node.this.vectorClock.length; i++) {
                                Node.this.vectorClock[i] = Math.max(Node.this.vectorClock[i],
                                        applicationMessage.messageClock[i]);
                            }
                            Node.this.vectorClock[Node.this.getNodeId()]++;
                        }
                        if(Node.this.color == NodeColor.RED &&
                                !Node.this.receivedMarkers.get(incomingMessage.getSourceNode().getNodeId())){
                            synchronized (Node.this.channelStates) {
                                if (Node.this.channelStates == null)
                                    Node.this.channelStates = new ArrayList<>();

                                ChannelState newChannelState = new ChannelState(incomingMessage.getSourceNode(),
                                        Node.this, applicationMessage.messageClock);
                                Node.this.channelStates.add(newChannelState);
                            }

                        }
                        if (Node.this.active)
                            sendApplicationMessages();
                    } else if(incomingMessage instanceof MarkerMessage){
                        System.out.println("Received marker message");
                        processChandyLamportProtocol((MarkerMessage) incomingMessage);
                    } else if(incomingMessage instanceof SnapshotMessage){
                        SnapshotMessage snapshotMessage = (SnapshotMessage) incomingMessage;
                        System.out.println("Received snapshot message");
                        if (Node.this.getNodeId() != 0) {
                            send(Node.this.parent, snapshotMessage);
                        } else {
                            synchronized (Node.this.globalState) {
                                Node.this.globalState.getLocalStates().add(snapshotMessage.getLocalState());
                                for (ChannelState messageChannelState : snapshotMessage.getChannelStates()) {
                                    Node.this.globalState.getChannelStates().add(messageChannelState);
                                }
                            }
                            saveSnapshot();
                        }
                    } else if (incomingMessage instanceof TerminateMessage) {
                        System.out.println("Received termination message");
                        sendTerminateMessage();
                    }
                } catch (EOFException ex) {

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

        }
    }

    private void saveSnapshot() {
        // if default node has received all local state do the following
        //  a. add the current global state to NodeRunner.GlobalState
        //  b. start taking next snapshot or exit based on application status
        if (this.globalState.getLocalStates().size() == NodeWrapper.getTotalNodes()) {
            NodeWrapper.getGlobalStates().add(this.globalState);
            if (!Node.this.isActive() && this.globalState.getChannelStates().isEmpty()) {
                sendTerminateMessage();
            } else {
                restartChandyLamport();
            }
        }
    }

    private void restartChandyLamport() {
        System.out.println("Restarting Chandy Lamport Protocol");
        color = NodeColor.BLUE;
        localState = new LocalState();
        channelStates = new ArrayList<>();
        globalState = new GlobalState();
        initializeReceivedMarkerMap();
        new Thread(new ChandyLamportProtocol(this)).start();
    }

    private void sendTerminateMessage() {
        // send finish message to all neighbours
        for (Node neighbour : this.neighbors) {
            if (neighbour.nodeId != 0) {
                TerminateMessage finishMessage = new TerminateMessage(this);
                send(neighbour, finishMessage);
            }
        }
        // if current node is default node then write the snapshot to a file
        if (this.nodeId == 0) {
            if(checkSnapShotConsistency()){
                System.out.println("Protocol is finished");
                writeFinalOutput();
                server.haltAllNodes();
                System.exit(0);
            } else {
                restartChandyLamport();
            }
        }
    }

    private void writeFinalOutput() {
        FinalOutputWriter finalOutputWriter = new FinalOutputWriter(NodeWrapper.getGlobalStates());
        finalOutputWriter.writeFinalOutput();
    }

    void processChandyLamportProtocol(MarkerMessage markerMessage){
        synchronized (this) {
            if(this.getColor() == NodeColor.BLUE){
                this.color = NodeColor.RED;
                this.localState.setVectorClock(this.vectorClock);
                this.localState.setActiveStatus(this.active);
                this.localState.setNodeId(this.nodeId);

                for (Node neighbour : this.neighbors) {
                    Message marker = new MarkerMessage(this);
                    send(neighbour, marker);
                }
                if (markerMessage != null) {
                    receivedMarkers.put(markerMessage.getSourceNode().getNodeId(), true);
                    // check if this nodes expects marker message from only one node ( if this node has only one neighbour in the topology
                    if (isAllMarkerMessageReceived()) {
                        this.color = NodeColor.BLUE;
                        SnapshotMessage snapShotMessage = new SnapshotMessage(this.localState, new ArrayList<>(), this);
                        // send the snapshot to its parent
                        send(this.parent, snapShotMessage);
                        // reset all chandy lamport parameters for another snapshot to be taken if needed
                        resetOtherNodes();
                    }
                }
            } else {
                // set the logMap of node from where marker message received as true
                receivedMarkers.put(markerMessage.getSourceNode().getNodeId(), true);
                // this.logStatus != Color.BLUE is checked to ensure logic works multi threading access at almost same time
                if (isAllMarkerMessageReceived()) {
                    this.color = NodeColor.BLUE;
                    // if current node is not default node (node 0), then forward the snapshot to its parent. Else add it to global state of default node.
                    if (this.getNodeId() != 0) {
                        SnapshotMessage snapShotMessage = new SnapshotMessage(this.localState, this.channelStates, this);
                        send(this.parent, snapShotMessage);
                        resetOtherNodes();
                    } else {
                        this.globalState.getLocalStates().add(localState);
                        for (ChannelState localChannelState : this.channelStates)
                            this.globalState.getChannelStates().add(localChannelState);
                        // print the output
                        saveSnapshot();
                    }
                }
            }
        }
    }

    private boolean checkSnapShotConsistency() {
        boolean anyConsistentSnapshotFound = false;
        for (int snapshotIndex = 0; snapshotIndex < NodeWrapper.getGlobalStates().size(); snapshotIndex++) {
            GlobalState globalState = NodeWrapper.getGlobalStates().get(snapshotIndex);
            boolean isSnapshotConsistent = true;
            int ithProcess = 0;
            while (ithProcess < NodeWrapper.getTotalNodes()) {
                int ithVectorValue = globalState.getLocalStateByNodeId(ithProcess).getVectorClock()[ithProcess];
                for (int i = 0; i < globalState.getLocalStates().size(); i++) {
                    if (i != ithProcess) {
                        int jthVectorValue = globalState.getLocalStates().get(i).getVectorClock()[ithProcess];
                        if (jthVectorValue > ithVectorValue) {
                            isSnapshotConsistent = false;
                            System.out.println("Global Snapshot number " + (snapshotIndex + 1) + " is not consistent");
                            System.out.println("Process in Node " + ithProcess + " is invalid");
                            break;
                        }
                    }
                }
                ithProcess++;
            }
            if (isSnapshotConsistent) {
                anyConsistentSnapshotFound = true;
                System.out.println("Global Snapshot number " + (snapshotIndex + 1) + " is consistent");
            }
        }
        return anyConsistentSnapshotFound;
    }

    private boolean isAllMarkerMessageReceived() {
        // Check using logmap if all marker messages are received
        for (Integer nodeId : receivedMarkers.keySet()) {
            if (!receivedMarkers.get(nodeId))
                return false;
        }
        return true;
    }

    private void resetOtherNodes() {
        this.localState = new LocalState();
        this.channelStates = new ArrayList<>();
        initializeReceivedMarkerMap();
    }

    private int getRandomMessageCount() {
        Random randomGenerator = new Random();
        int difference = NodeWrapper.getMaxPerActive() - NodeWrapper.getMinPerActive() + 1;
        int randomNumber = randomGenerator.nextInt(difference);
        return NodeWrapper.getMinPerActive() + randomNumber;
    }
    private void sendApplicationMessages() {
        Message sendMessage;
        StringBuilder message;
        int messagePerActive;
        try {
            // get random message count each time node becomes active
            messagePerActive = getRandomMessageCount();
            for (int i = 0; i < messagePerActive; i++) {
                if (this.active && (messagesSent < NodeWrapper.getMaxNumber ())) {
                    this.vectorClock[this.getNodeId()]++;
                    sendMessage = new ApplicationMessage(this.vectorClock, this);
                    send(neighbors.get(new Random().nextInt(neighbors.size())), sendMessage);
                    messagesSent++;
                } else
                    break;
                Thread.sleep(NodeWrapper.getMinSendDelay());
            }
//            System.out.println(this);
            // set node as passive after it sends all messages
            synchronized (this.active) {
                active = false;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void send(Node destinationNode, Message sendMessage) {
        // since both chandy lamport protocol thread and message processing thread uses this resource,
        // it must be synchronized
        synchronized (server) {
            if (sendMessage instanceof ApplicationMessage){
                logger.info("Sending Application message from " + this.nodeId + " to "
                        + destinationNode.nodeId);
            }
            else if (sendMessage instanceof MarkerMessage){
                logger.info("Sending Marker message from " + this.nodeId + " to "
                        + destinationNode.nodeId);
            }
            if (sendMessage instanceof SnapshotMessage){
                logger.info("Sending Snapshot message from " + this.nodeId + " to "
                        + destinationNode.nodeId);
            }
            if (sendMessage instanceof TerminateMessage){
                logger.info("Sending Terminate message from " + this.nodeId + " to "
                        + destinationNode.nodeId);
            }
            server.send(destinationNode, sendMessage);
        }
    }

}
enum NodeColor{
    RED, BLUE;
}
