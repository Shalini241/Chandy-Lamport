import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;

public class Server{

    private HashMap<Integer, Socket> socketMap;
    private HashMap<Integer, ObjectOutputStream> outputStreamMap;

    public Server() {
        socketMap = new HashMap<>();
        outputStreamMap = new HashMap<>();
    }

    void initializeNeighbors(List<Node> neighbourNodes) {
        if (neighbourNodes != null) {
            boolean connectionEstablished = false;
            while(!connectionEstablished){
                try {
                    for (Node neighbour : neighbourNodes) {
                        Socket clientSocket = new Socket(neighbour.getHostName(), neighbour.getPort());
                        socketMap.put(neighbour.getNodeId(), clientSocket);
                        outputStreamMap.put(neighbour.getNodeId(), new ObjectOutputStream(clientSocket.getOutputStream()));
                    }
                    connectionEstablished = true;
                } catch (Exception ex) {

                }
            }

        }
    }

    public void send(Node destinationNode, Message sendMessage) {
        try {
            if (outputStreamMap.size() == 0) {
                initializeNeighbors(sendMessage.getSourceNode().getNeighbors());
            }
            ObjectOutputStream output = outputStreamMap.get(destinationNode.getNodeId());
            output.writeObject(sendMessage);
            output.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void haltAllNodes() {
        try {
            for (Integer nodeId : outputStreamMap.keySet()) {
                outputStreamMap.get(nodeId).close();
            }
            for (Integer nodeId : socketMap.keySet()) {
                socketMap.get(nodeId).close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
}
