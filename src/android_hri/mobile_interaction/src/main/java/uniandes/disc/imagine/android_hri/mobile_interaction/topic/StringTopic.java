package uniandes.disc.imagine.android_hri.mobile_interaction.topic;

import org.ros.concurrent.CancellableLoop;
import org.ros.message.MessageListener;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

/**
 * Created by dhrodriguezg on 7/29/15.
 */
public class StringTopic extends AbstractTopic {

    private static final String TAG = "StringTopic";
    private Publisher<std_msgs.String> publisher = null;
    private Subscriber<std_msgs.String> subscriber = null;
    private String publisher_string;
    private String subcriber_string;

    protected void setupPublisher(ConnectedNode connectedNode){
        publisher = connectedNode.newPublisher(publisherTopic, std_msgs.String._TYPE);
        final CancellableLoop aLoop = new CancellableLoop() {
            @Override protected void loop() throws InterruptedException {
                if(alwaysPublish){
                    publish();
                }else{
                    if(counter>0)
                        publish();
                }
                counter--;
                if(counter<0)
                    counter=0;
                Thread.sleep(publishFreq);
            }
        };
        connectedNode.executeCancellableLoop(aLoop);
    }

    protected void publish(){
        if(publisher_string.isEmpty())
            return;
        std_msgs.String string = publisher.newMessage();
        string.setData(publisher_string);
        publisher.publish(string);
        hasPublishedMsg=true;
        publisher_string ="";
    }

    protected void setupSubscriber(ConnectedNode connectedNode) {
        subscriber = connectedNode.newSubscriber(subscriberTopic, std_msgs.String._TYPE);
        subscriber.addMessageListener(new MessageListener<std_msgs.String>() {
            @Override
            public void onNewMessage(std_msgs.String string) {
                subcriber_string = string.getData();
                hasReceivedMsg=true;
            }
        });
    }

    public String getSubcriber_string() {
        return subcriber_string;
    }

    public void setSubcriber_string(String subcriber_string) {
        this.subcriber_string = subcriber_string;
    }

    public void appendSubcribe_string(String subcribe_string) {
        this.subcriber_string += subcribe_string;
    }

    public String getPublisher_string() {
        return publisher_string;
    }

    public void setPublisher_string(String publisher_string) {
        this.publisher_string = publisher_string;
    }

}
