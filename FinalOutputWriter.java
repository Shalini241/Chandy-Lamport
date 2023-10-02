import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class FinalOutputWriter {

    private ArrayList<GlobalState> globalStates;

    public FinalOutputWriter(ArrayList<GlobalState> globalStates) {
        this.globalStates = globalStates;
    }
    public void writeFinalOutput() {
        synchronized (this.globalStates) {
            try {
                for (GlobalState globalState : this.globalStates) {
                    for (LocalState localState : globalState.getLocalStates()) {
                        String fileName =  "LocalState-" + localState.getNodeId() + ".out";
                        File file = new File(fileName);
                        FileWriter fileWriter;
                        boolean fileExists = false;
                        if (file.exists()) {
                            fileWriter = new FileWriter(file, true);
                            fileExists = true;
                        } else {
                            fileWriter = new FileWriter(file);
                        }
                        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                        if (fileExists)
                            bufferedWriter.write("\n");

                        for (Integer vectorValue : localState.getVectorClock())
                            bufferedWriter.write(vectorValue + " ");

                        // Always close files.
                        bufferedWriter.flush();
                        bufferedWriter.close();
                        fileWriter.close();
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

}
