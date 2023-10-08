import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class NodeWrapper {
    private static int totalNodes;
    private static int minPerActive;
    private static int maxPerActive;
    private static int minSendDelay;
    private static int snapshotDelay;
    private static int maxNumber;
    private static HashMap<Integer, Node> nodeMap;
    private static ArrayList<GlobalState> globalStates;
    private static String configName;
    public static ArrayList<GlobalState> getGlobalStates() {
        if(globalStates==null){
            globalStates = new ArrayList<>();
        }
        return globalStates;
    }
    public static int getMinPerActive() {
        return minPerActive;
    }
    public static String getConfigName() {
        return configName;
    }

    public static int getMaxPerActive() {
        return maxPerActive;
    }

    public static int getMinSendDelay() {
        return minSendDelay;
    }

    public static int getSnapshotDelay() {
        return snapshotDelay;
    }

    public static int getMaxNumber() {
        return maxNumber;
    }

    public static int getTotalNodes() {
        return totalNodes;
    }

    private static List<List<Integer>> parseConfigFile(String configFilePath) {

        List<List<Integer>> neighboursList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(configFilePath))) {
            String line = reader.readLine();
            String[] parameters = line.split(" ");
            System.out.println(Arrays.asList(parameters));
            totalNodes = Integer.parseInt(parameters[0]);
            minPerActive = Integer.parseInt(parameters[1]);
            maxPerActive = Integer.parseInt(parameters[2]);
            minSendDelay = Integer.parseInt(parameters[3]);
            snapshotDelay = Integer.parseInt(parameters[4]);
            maxNumber = Integer.parseInt(parameters[5]);
            if(nodeMap == null){
                nodeMap = new HashMap<>();
            }
            for(int node = 0; node < totalNodes; node++){
                line = reader.readLine();
                String[] nodeDetails = line.split("\\s+");
                int id = Integer.parseInt(nodeDetails[0]);
                Node newNode = new Node(id,nodeDetails[1],Integer.parseInt(nodeDetails[2]));
                nodeMap.put(id, newNode);
            }
            for(int node = 0; node < totalNodes; node++){
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

    private static void buildSpanningTree(List<List<Integer>> neighbourList) {
        if (nodeMap != null && !nodeMap.isEmpty()) {
            boolean[] visited = new boolean[nodeMap.size()];
            Arrays.fill(visited, false);
            Queue<Integer> queue = new LinkedList<>();
            queue.add(0);
            visited[0] = true;
            while (!queue.isEmpty()) {
                int nodeId = queue.poll();
                Node parent = nodeMap.get(nodeId);
                List<Integer> neighbours = neighbourList.get(nodeId);
                if (neighbours != null && !neighbours.isEmpty()) {
                    for (int id : neighbours) {
                        if (!visited[id]) {
                            Node neighbour = nodeMap.get(id);
                            neighbour.setParent(parent);
                            visited[id] = true;
                            queue.add(id);
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        configName = "configuration";
        List<List<Integer>> neighborList = parseConfigFile(configName+".text");
        buildSpanningTree(neighborList);
        int currentNodeId = Integer.parseInt(args[0]);
        Node currentNode = nodeMap.get(currentNodeId);
        ArrayList<Node> neighbourNodes = new ArrayList<>();
        List<Integer> neighbours = neighborList.get(currentNodeId);
        for (Integer i : neighbours) {
            Node neighbour = nodeMap.get(i);
            neighbourNodes.add(neighbour);
        }
        currentNode.setNeighbors(neighbourNodes);
        currentNode.initializeReceivedMarkerMap();
        currentNode.initializeNode();
    }
}
