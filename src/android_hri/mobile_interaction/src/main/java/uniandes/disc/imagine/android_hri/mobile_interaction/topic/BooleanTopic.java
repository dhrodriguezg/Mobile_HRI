package uniandes.disc.imagine.android_hri.mobile_interaction.topic;

import org.ros.concurrent.CancellableLoop;
import org.ros.message.MessageListener;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import std_msgs.Bool;

/**
 * Created by dhrodriguezg on 7/29/15.
 */


public class BooleanTopic extends AbstractTopic {

    private static final String TAG = "BooleanTopic";
    private boolean publisher_bool;
    private boolean subcriber_bool;
    protected Publisher<Bool> publisher = null;
    protected Subscriber<Bool> subscriber = null;

    protected void setupPublisher(ConnectedNode connectedNode){
        publisher = connectedNode.newPublisher(publisherTopic, Bool._TYPE);
        final CancellableLoop aLoop = new CancellableLoop() {
            @Override protected void loop() throws InterruptedException {
                if(alwaysPublish){
                    publish();
                    if(counter==0 && maxPublishing!=0){
                        publisher_bool =false;
                    }
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
        Bool bool = publisher.newMessage();
        bool.setData(publisher_bool);
        publisher.publish(bool);
        hasPublishedMsg=true;
    }

    protected void setupSubscriber(ConnectedNode connectedNode) {
        subscriber = connectedNode.newSubscriber(subscriberTopic, Bool._TYPE);
        subscriber.addMessageListener(new MessageListener<Bool>() {
            @Override
            public void onNewMessage(Bool bool) {
                subcriber_bool = bool.getData();
                hasReceivedMsg = true;
            }
        });
    }

    public boolean isPublisher_bool() {
        return publisher_bool;
    }

    public void setPublisher_bool(boolean publisher_bool) {
        this.publisher_bool = publisher_bool;
    }

    public boolean isSubcriber_bool() {
        return subcriber_bool;
    }

    public void setSubcriber_bool(boolean subcriber_bool) {
        this.subcriber_bool = subcriber_bool;
    }

}
