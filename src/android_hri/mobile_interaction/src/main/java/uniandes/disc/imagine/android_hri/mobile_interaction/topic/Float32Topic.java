package uniandes.disc.imagine.android_hri.mobile_interaction.topic;

import org.ros.concurrent.CancellableLoop;
import org.ros.message.MessageListener;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import std_msgs.Float32;

/**
 * Created by dhrodriguezg on 7/29/15.
 */
public class Float32Topic extends AbstractTopic {

    private static final String TAG = "Float32Topic";
    private Publisher<Float32> publisher = null;
    private Subscriber<Float32> subscriber = null;
    private float publisher_float =0;
    private float subcriber_float =0;

    protected void setupPublisher(ConnectedNode connectedNode){
        publisher = connectedNode.newPublisher(publisherTopic, Float32._TYPE);
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
        Float32 float32 = publisher.newMessage();
        float32.setData(publisher_float);
        publisher.publish(float32);
        hasPublishedMsg=true;
    }

    protected void setupSubscriber(ConnectedNode connectedNode) {
        subscriber = connectedNode.newSubscriber(subscriberTopic, Float32._TYPE);
        subscriber.addMessageListener(new MessageListener<Float32>() {
            @Override
            public void onNewMessage(Float32 float32) {
                subcriber_float =float32.getData();
                hasReceivedMsg=true;
            }
        });
    }

    public float getPublisher_float() {
        return publisher_float;
    }

    public void setPublisher_float(float publish_float) {
        this.publisher_float = publish_float;
    }

    public float getSubcriber_float() {
        return subcriber_float;
    }

    public void setSubcriber_float(float subcribe_float) {
        this.subcriber_float = subcribe_float;
    }

}
