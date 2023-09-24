import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class NodeWrapper {
    private static int minPerActive;
    private static int maxPerActive;
    private static int minSendDelay;

    private static int snapshotDelay;
    private static int maxNumber;

    private static HashMap<Integer, Node> nodeMap;

    public static int getMinPerActive() {
        return minPerActive;
    }

    public static int getMaxPerActive() {
        return maxPerActive;
    }

    public static int getMinSendDelay() {
        return minSendDelay;
    }

    public int getSnapshotDelay() {
        return snapshotDelay;
    }

    public static int getMaxNumber() {
        return maxNumber;
    }

    private static List<List<Integer>> parseConfigFile(String configFilePath) {

        List<List<Integer>> neighboursList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(configFilePath))) {
            String line = reader.readLine();
            String[] parameters = line.split(" ");
            int num_of_nodes = Integer.parseInt(parameters[0]);
            minPerActive = Integer.parseInt(parameters[1]);
            maxPerActive = Integer.parseInt(parameters[2]);
            minSendDelay = Integer.parseInt(parameters[3]);
            snapshotDelay = Integer.parseInt(parameters[4]);
            maxNumber = Integer.parseInt(parameters[5]);
            if(nodeMap == null){
                nodeMap = new HashMap<>();
            }
            for(int node = 0; node < num_of_nodes; node++){
                line = reader.readLine();
                String[] nodeDetails = line.split("\\s+");
                int id = Integer.parseInt(nodeDetails[0]);
                Node newNode = new Node(id,nodeDetails[1],Integer.parseInt(nodeDetails[2]));
                nodeMap.put(id, newNode);
            }
            for(int node = 0; node < num_of_nodes; node++){
                line = reader.readLine();
                List<Integer> neighbors = Arrays.stream(line.split("\\s+"))
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
                neighboursList.add(neighbors);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return neighboursList;
    }

    public static void main(String[] args) {
        int id = Integer.parseInt(args[0]);
        List<List<Integer>> neighborList = parseConfigFile("configuration.text");

        int currentNodeId = Integer.parseInt(args[0]);
        Node currentNode = nodeMap.get(currentNodeId);

        // Load its neighbours
        ArrayList<Node> neighbourNodes = new ArrayList<>();
        List<Integer> neighbours = neighborList.get(currentNodeId);

        for (Integer i : neighbours) {
            Node neighbour = nodeMap.get(i);
            neighbourNodes.add(neighbour);
        }
        currentNode.setNeighbors(neighbourNodes);

        // set up current node; close socket is pending
        currentNode.initializeNode();
    }
}
