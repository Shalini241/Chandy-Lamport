import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;

public class Server{

    private HashMap<Integer, Socket> socketMap;
    private HashMap<Integer, ObjectOutputStream> outputMap;

    public Server() {
        socketMap = new HashMap<>();
        outputMap = new HashMap<>();
    }

    void initializeNeighbors(List<Node> neighbourNodes) {
        if (neighbourNodes != null) {
            boolean connectionEstablished = false;
            while(!connectionEstablished){
                try {
                    for (Node neighbour : neighbourNodes) {
                        Socket clientSocket = new Socket(neighbour.getHostName(), neighbour.getPort());
                        socketMap.put(neighbour.getNodeId(), clientSocket);
                        outputMap.put(neighbour.getNodeId(), new ObjectOutputStream(clientSocket.getOutputStream()));
                    }
                    connectionEstablished = true;
                } catch (Exception ex) {

                }
            }

        }
    }

    public void send(Node destinationNode, Message sendMessage) {
        try {
            if (outputMap.size() == 0) {
                initializeNeighbors(sendMessage.getSourceNode().getNeighbors());
            }
            ObjectOutputStream output = outputMap.get(destinationNode.getNodeId());
            output.writeObject(sendMessage);
            output.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
}
