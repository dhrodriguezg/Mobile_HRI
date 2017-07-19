package uniandes.disc.imagine.android_hri.mobile_interaction.utils;

import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;

import java.util.HashSet;
import java.util.Set;

import uniandes.disc.imagine.android_hri.mobile_interaction.service.AbstractService;
import uniandes.disc.imagine.android_hri.mobile_interaction.topic.AbstractTopic;

/**
 * Created by dhrodriguezg on 7/29/15.
 */
public class AndroidNode implements NodeMain {

    private static final String TAG = "AndroidNode";

    private Set<AbstractService> serviceSet = null;
    private Set<AbstractTopic> topicSet = null;
    private Set<NodeMain> nodeSet = null;
    private String nodeName = null;

    public AndroidNode(String nodeName){
        this.nodeName=nodeName;
        serviceSet = new HashSet<>();
        topicSet = new HashSet<>();
        nodeSet = new HashSet<>();
    }

    public void addService(AbstractService service){
        serviceSet.add(service);
    }

    public void addServices(AbstractService... services){
        for(AbstractService service : services)
            serviceSet.add(service);
    }

    public void addTopic(AbstractTopic topic){
        topicSet.add(topic);
    }

    public void addTopics(AbstractTopic... topics){
        for(AbstractTopic topic : topics)
            topicSet.add(topic);
    }

    public void addNodeMain(NodeMain nodeMain){
        nodeSet.add(nodeMain);
    }

    public void addNodeMains(NodeMain... nodeMains){
        for(NodeMain nodeMain : nodeMains)
            nodeSet.add(nodeMain);
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(nodeName);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        for(AbstractService service: serviceSet)
            service.onStart(connectedNode);
        for(AbstractTopic topic: topicSet)
            topic.onStart(connectedNode);
        for(NodeMain nodeMain: nodeSet)
            nodeMain.onStart(connectedNode);
    }

    @Override
    public void onShutdown(Node node) {
        for(NodeMain nodeMain: nodeSet)
            nodeMain.onShutdown(node);
    }

    @Override
    public void onShutdownComplete(Node node) {
        for(NodeMain nodeMain: nodeSet)
            nodeMain.onShutdownComplete(node);
    }

    @Override
    public void onError(Node node, Throwable throwable) {
        for(NodeMain nodeMain: nodeSet)
            nodeMain.onError(node, throwable);
    }

    public String getName(){
        return nodeName;
    }
}
