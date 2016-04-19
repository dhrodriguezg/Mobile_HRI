package uniandes.disc.imagine.android_hri.mobile_interaction.service;

import org.ros.node.ConnectedNode;

/**
 * Created by dhrodriguezg on 9/28/15.
 */
public abstract class AbstractService {

    private static final String TAG = "AbstractService";
    protected String serverTopic;
    protected String clientTopic;

    protected boolean hasServerReceivedMsg = false;
    protected boolean hasClientSentMsg = false;
    protected boolean isServer;
    protected boolean isClient;

    public void serverOf(String topic) {
        isServer = true;
        serverTopic = topic;
    }

    public void clientOf(String topic) {
        isClient = true;
        clientTopic = topic;
    }

    public void onStart(ConnectedNode connectedNode) {
        if(isServer)
            setupServer(connectedNode);
        if(isClient)
            setupClient(connectedNode);
    }

    public boolean hasReceivedMsg() {
        return hasServerReceivedMsg;
    }

    public void setHasServerReceivedMsg(boolean hasServerReceivedMsg) {
        this.hasServerReceivedMsg = hasServerReceivedMsg;
    }

    public boolean hasPublishedMsg() {
        return hasClientSentMsg;
    }

    public void setHasClientSentMsg(boolean hasClientSentMsg) {
        this.hasClientSentMsg = hasClientSentMsg;
    }

    protected abstract void setupClient(final ConnectedNode connectedNode);
    public abstract void callService();
    protected abstract void setupServer(final ConnectedNode connectedNode);

}
