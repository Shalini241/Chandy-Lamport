import java.util.ArrayList;
import java.util.List;

public class GlobalState {
    List<LocalState> localStates;
    List<ChannelState> channelStates;

    GlobalState(){
        localStates = new ArrayList<>();
        channelStates = new ArrayList<>();
    }

    public List<LocalState> getLocalStates() {
        return localStates;
    }

    public void setLocalStates(List<LocalState> localStates) {
        this.localStates = localStates;
    }

    public List<ChannelState> getChannelStates() {
        return channelStates;
    }

    public void setChannelStates(List<ChannelState> channelStateList) {
        this.channelStates = channelStateList;
    }

    public LocalState getLocalStateByNodeId(int nodeId) {
        for (LocalState localState : this.localStates) {
            if (localState.getNodeId() == nodeId)
                return localState;
        }
        return null;
    }
}
