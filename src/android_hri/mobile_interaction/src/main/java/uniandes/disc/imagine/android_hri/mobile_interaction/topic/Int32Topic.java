package uniandes.disc.imagine.android_hri.mobile_interaction.topic;

import org.ros.concurrent.CancellableLoop;
import org.ros.message.MessageListener;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import std_msgs.Int32;

/**
 * Created by dhrodriguezg on 7/29/15.
 */
public class Int32Topic extends AbstractTopic {

    private static final String TAG = "Int32Topic";
    private Publisher<Int32> publisher = null;
    private Subscriber<Int32> subscriber = null;
    private int publisher_int =0;
    private int subcriber_int =0;

    protected void setupPublisher(ConnectedNode connectedNode){
        publisher = connectedNode.newPublisher(publisherTopic, Int32._TYPE);
        final CancellableLoop aLoop = new CancellableLoop() {
            @Override protected void loop() throws InterruptedException {
                if(alwaysPublish)
                    publish();
                else{
                    if(counter>0){
                        publish();
                    }
                    counter--;
                }
                Thread.sleep(publishFreq);
            }
        };
        connectedNode.executeCancellableLoop(aLoop);
    }

    protected void publish(){
        Int32 int32 = publisher.newMessage();
        int32.setData(publisher_int);
        publisher.publish(int32);
        hasPublishedMsg=true;
    }

    protected void setupSubscriber(ConnectedNode connectedNode) {
        subscriber = connectedNode.newSubscriber(subscriberTopic, Int32._TYPE);
        subscriber.addMessageListener(new MessageListener<Int32>() {
            @Override
            public void onNewMessage(Int32 int32) {
                subcriber_int =int32.getData();
                hasReceivedMsg=true;
            }
        });
    }

    public int getPublisher_int() {
        return publisher_int;
    }

    public void setPublisher_int(int publisher_int) {
        this.publisher_int = publisher_int;
    }

    public int getSubcriber_int() {
        return subcriber_int;
    }

    public void setSubcriber_int(int subcriber_int) {
        this.subcriber_int = subcriber_int;
    }

}
