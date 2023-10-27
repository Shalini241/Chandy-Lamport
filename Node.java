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
    private final transient Server server;
    private final int nodeId;
    private List<Node> neighbors;
    private transient Boolean active;
    private transient int messagesSent;
    private int[] vectorClock;
    private transient LocalState localState;
    private transient ArrayList<ChannelState> channelStates;
    private transient GlobalState globalState;
    private Node parent;
    public int getNodeId() {
        return nodeId;
    }
    private transient HashMap<Integer, Boolean> receivedMarkers;
    public List<Node> getNeighbors() {
        return neighbors;
    }
    public void setNeighbors(ArrayList<Node> neighbours) {
        this.neighbors = neighbours;
    }
    public boolean isActive() {
        return active;
    }
    public int getPort() {
        return port;
    }
    public String getHostName() {
        return hostName;
    }
    public void setParent(Node parent) {
        this.parent = parent;
    }
    public NodeColor getColor() {
        return color;
    }

    public Node(int id, String hostName, int port) throws IOException {
        this.hostName = hostName;
        this.port = port;
        this.nodeId = id;
        this.active = true;
        this.color = NodeColor.BLUE;
        vectorClock = new int[NodeWrapper.getTotalNodes()];
        server = new Server();
        channelStates = new ArrayList<>();
        globalState = new GlobalState();
        receivedMarkers = new HashMap<>();
        FileHandler fh = new FileHandler("myapp.log");
        logger.addHandler(fh);

        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);
    }
    public void initializeNode() {
        // Start Listening thread to listen from its neighbors
        new Thread(new ListenerThread()).start();
        try {
            Thread.sleep(20000);
            server.initializeNeighbors(neighbors);
            if(nodeId == 0){
                // sleep for 10 seconds before starting the protocol
                Thread.sleep(10000);
                sendApplicationMessages();
                // start the protocol
                new Thread(new ChandyLamportProtocol(this)).start();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void initializeReceivedMarkerMap() {
        this.receivedMarkers = new HashMap<>();
        for (Node neighbourNode : this.neighbors) {
            this.receivedMarkers.put(neighbourNode.nodeId, false);
        }
    }

    class ListenerThread implements Runnable {
        private ServerSocket serverSocket;
        public ListenerThread() {}
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(getPort());
                while (true) {
                    // keep trying until all the socket connections get accepted,
                    // accept the connection and start processing messages
                    Socket socket = serverSocket.accept();
                    new Thread(new messageProcessor(socket)).start();
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

    class messageProcessor implements Runnable {
        ObjectInputStream inputStream;
        Socket socket;

        public messageProcessor(Socket socket) {
            this.socket = socket;
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
                    // Synchronize the block wherever any parameter is getting updates
                    // since the block is being accessed by multiple message processing threads
                    if(incomingMessage instanceof ApplicationMessage){
                        ApplicationMessage applicationMessage = (ApplicationMessage) incomingMessage;
                        // set the active status as per the logic
                        synchronized (Node.this.active) {
                            Node.this.active = (Node.this.messagesSent < NodeWrapper.getMaxNumber());
                        }
                        logger.info("Received Application message");

                        //update vector clock
                        synchronized (Node.this.vectorClock) {
                            for (int i = 0; i < Node.this.vectorClock.length; i++) {
                                Node.this.vectorClock[i] = Math.max(Node.this.vectorClock[i],
                                        applicationMessage.messageClock[i]);
                            }
                            Node.this.vectorClock[Node.this.getNodeId()]++;
                        }
                        // if the process is already red and hasn't received markers from the another node,
                        // then start recording the channel states
                        if(Node.this.color == NodeColor.RED &&
                                !Node.this.receivedMarkers.get(incomingMessage.getSourceNode().getNodeId())){
                            synchronized (Node.this.channelStates) {
                                ChannelState newChannelState = new ChannelState(incomingMessage.getSourceNode(),
                                        Node.this, applicationMessage.messageClock);
                                Node.this.channelStates.add(newChannelState);
                            }

                        }

                        // if the node is still active then send application messages
                        if (Node.this.active)
                            sendApplicationMessages();
                    } else if(incomingMessage instanceof MarkerMessage){
                        logger.info("Received marker message");
                        processChandyLamportProtocol((MarkerMessage) incomingMessage);
                    } else if(incomingMessage instanceof SnapshotMessage){
                        SnapshotMessage snapshotMessage = (SnapshotMessage) incomingMessage;
                        logger.info("Received snapshot message");

                        // if it is not the node that initiated the protocol, then pass the snapshot to its parent,
                        // otherwise update its global states and check if the global state is consistent
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
                        logger.info("Received termination message");
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
        // if default node has received local state from all nodes then do either of the following
        //  1. if the current node is passive and channel are empty then terminate the protocol
        //  2. Otherwise, restart the chandy lamport protocol
        if (this.globalState.getLocalStates().size() == NodeWrapper.getTotalNodes()) {
            NodeWrapper.getGlobalStates().add(this.globalState);
            if (isApplicationPassive() && this.globalState.getChannelStates().isEmpty()) {
                sendTerminateMessage();
            } else {
                restartChandyLamport();
            }
        }
    }

    private boolean isApplicationPassive() {
        // is all nodes passive
        for (LocalState nodeLocalState : this.globalState.getLocalStates()) {
            if (nodeLocalState.isActiveStatus())
                return false;
        }
        return true;
    }

    private void restartChandyLamport() {
        logger.info("Restarting Chandy Lamport Protocol");
        color = NodeColor.BLUE;
        localState = new LocalState();
        channelStates = new ArrayList<>();
        globalState = new GlobalState();
        initializeReceivedMarkerMap();
        new Thread(new ChandyLamportProtocol(this)).start();
    }

    private void sendTerminateMessage() {
        // send terminate message to all neighbours
        for (Node neighbour : this.neighbors) {
            if (neighbour.nodeId != 0) {
                TerminateMessage finishMessage = new TerminateMessage(this);
                send(neighbour, finishMessage);
            }
        }
        // if current node is the one that initiated the protocol then check
        // if snapshot is consistent then write it in a file, else restart the protocol
        if (this.nodeId == 0) {
            writeFinalOutput();
            if(checkSnapShotConsistency()){
                logger.info("Protocol is finished");
            } else{
                logger.info("Snapshot is not consistent");
            }
        }
    }

    private void writeFinalOutput() {
        FinalOutputWriter finalOutputWriter = new FinalOutputWriter(NodeWrapper.getGlobalStates());
        finalOutputWriter.writeFinalOutput();
    }

    void processChandyLamportProtocol(MarkerMessage markerMessage){
        synchronized (this) {
            // if the node color is blue then:
            // record the local state and pass the marker message to its neighbor
            LocalState recordedState = null;
            if(this.getColor() == NodeColor.BLUE){

                if(localState ==null){
                    localState = new LocalState();
                    this.color = NodeColor.RED;
                    this.localState.setVectorClock(this.vectorClock);
                    this.localState.setNodeId(this.nodeId);
                    this.localState.setActiveStatus(this.active);
                }

                sendApplicationMessages();

                // this condition will only be true for the nodes that didn't initiate the protocol
                // and will be executed by the last node in the topology.
                // After this process will start sending snapshots to its parent as response to the marker messages
                if (markerMessage != null) {
                    receivedMarkers.put(markerMessage.getSourceNode().getNodeId(), true);
                    // check if the node has received markers from all its neighbor
                    if (isAllMarkerMessageReceived()) {
                        this.color = NodeColor.BLUE;

                        SnapshotMessage snapShotMessage = new SnapshotMessage(this.localState, new ArrayList<>(), this);
                        // send the snapshot to its parent

                        send(this.parent, snapShotMessage);
                        // reset all chandy lamport parameters for another snapshot to be taken if needed
                        resetNodes();
                    }
                }

                for (Node neighbour : this.neighbors) {
                    Message marker = new MarkerMessage(this);
                    send(neighbour, marker);
                }
            } else {
                receivedMarkers.put(markerMessage.getSourceNode().getNodeId(), true);
                sendApplicationMessages();
                
                if (isAllMarkerMessageReceived() && this.color != NodeColor.BLUE) {

                    this.color = NodeColor.BLUE;
                    if (this.getNodeId() != 0) {

                        SnapshotMessage snapShotMessage = new SnapshotMessage(this.localState, this.channelStates, this);
                        send(this.parent, snapShotMessage);
                        resetNodes();
                    } else {
                        // if the node that started the protocol received marker from all noes then save the global states
                        this.globalState.getLocalStates().add(this.localState);
                        for (ChannelState localChannelState : this.channelStates)
                            this.globalState.getChannelStates().add(localChannelState);
                        saveSnapshot();
                    }
                }
            }
        }
    }

    private boolean checkSnapShotConsistency() {
        boolean anyConsistentSnapshotFound = false;
        for (int snapshotNum = 0; snapshotNum < NodeWrapper.getGlobalStates().size(); snapshotNum++) {
            GlobalState globalState = NodeWrapper.getGlobalStates().get(snapshotNum);
            boolean isSnapshotConsistent = true;
            int ithProcess = 0;
            while (ithProcess < NodeWrapper.getTotalNodes()) {
                int iVal = globalState.getLocalStateByNodeId(ithProcess).getVectorClock()[ithProcess];
                for (int i = 0; i < globalState.getLocalStates().size(); i++) {
                    if (i != ithProcess) {
                        int jVal = globalState.getLocalStates().get(i).getVectorClock()[ithProcess];
                        if (jVal > iVal) {
                            isSnapshotConsistent = false;
                            logger.info("Global Snapshot number " + (snapshotNum + 1) + " is not consistent");
                            break;
                        }
                    }
                }
                ithProcess++;
            }
            if (isSnapshotConsistent) {
                anyConsistentSnapshotFound = true;
                logger.info("Global Snapshot number " + (snapshotNum + 1) + " is consistent");
            }
        }
        return anyConsistentSnapshotFound;
    }
    private boolean isAllMarkerMessageReceived() {
        for (Integer nodeId : receivedMarkers.keySet()) {
            if (!receivedMarkers.get(nodeId))
                return false;
        }
        return true;
    }

    private void resetNodes() {
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
        int messagePerActive;
        try {
            messagePerActive = getRandomMessageCount();
            for (int i = 0; i < messagePerActive; i++) {
                synchronized (this.vectorClock){
                    if (this.active && (messagesSent < NodeWrapper.getMaxNumber ())) {
                        this.vectorClock[this.getNodeId()]++;
                        sendMessage = new ApplicationMessage(this.vectorClock, this);
                        send(neighbors.get(new Random().nextInt(neighbors.size())), sendMessage);
                        messagesSent++;
                    } else
                        break;
                }
                Thread.sleep(NodeWrapper.getMinSendDelay());
            }
            synchronized (this.active) {
                active = false;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void sendApplicationMessagesToParent() {
        Message sendMessage;
        int messagePerActive;
        try {
            messagePerActive = getRandomMessageCount();
            for (int i = 0; i < messagePerActive; i++) {
                if (this.active && (messagesSent < NodeWrapper.getMaxNumber ())) {
                    this.vectorClock[this.getNodeId()]++;
                    sendMessage = new ApplicationMessage(this.vectorClock, this);
                    send(this.parent, sendMessage);
                    messagesSent++;
                } else
                    break;
                Thread.sleep(NodeWrapper.getMinSendDelay());
            }
            synchronized (this.active) {
                active = false;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void send(Node destinationNode, Message sendMessage) {
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

    private LocalState cloneLocalState(){
        LocalState state = new LocalState();
        state.setActiveStatus(this.isActive());
        state.setNodeId(this.nodeId);
        state.setVectorClock(this.vectorClock);
        return state;
    }
}
enum NodeColor{
    RED, BLUE;
}
