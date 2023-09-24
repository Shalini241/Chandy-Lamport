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

    private transient Server server;

    public int getNodeId() {
        return nodeId;
    }

    private final int nodeId;
    private List<Node> neighbors;
    private transient Boolean active;

    private transient int messagesSent;

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


    public String getHostName() {
        return hostName;
    }


    public Node(int id, String hostName, int port) throws IOException {
        this.hostName = hostName;
        this.port = port;
        this.nodeId = id;
        this.active = true;
        server = new Server();
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
            sendApplicationMessages();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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
                    synchronized (Node.this.active) {
                        Node.this.active = (Node.this.messagesSent < NodeWrapper.getMaxNumber());
                        logger.info("Receiving message "+ incomingMessage.getMessage()+" from "+ incomingMessage.getSourceNode().getNodeId() +" to "+ Node.this.nodeId);
                        if (Node.this.active)
                            sendApplicationMessages();
                    }
                } catch (EOFException ex) {

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

        }
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
        Integer messagePerActive;
        try {
            // get random message count each time node becomes active
            messagePerActive = getRandomMessageCount();
            for (int i = 0; i < messagePerActive; i++) {
                if (this.active && (messagesSent < NodeWrapper.getMaxNumber ())) {
                    sendMessage = new Message();
                    sendMessage.setMessage(""+messagesSent);
                    sendMessage.setSourceNode(this);
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

    private void send(Node destinationNode, Message sendMessage) {
        // since both chandy lamport protocol thread and message processing thread uses this resource, it must be synchronized
        synchronized (server) {
            logger.info("Sending message "+ sendMessage.getMessage()+" from "+this.nodeId+ " to "+ destinationNode.nodeId);
            server.send(destinationNode, sendMessage);
        }
    }

}
