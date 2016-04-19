package uniandes.disc.imagine.android_hri.mobile_interaction.topic;

import org.ros.concurrent.CancellableLoop;
import org.ros.message.MessageListener;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import sensor_msgs.Joy;

/**
 * Created by dhrodriguezg on 7/29/15.
 */
public class JoyTopic extends AbstractTopic {

    private static final String TAG = "JoyTopic";
    private Publisher<Joy> publisher = null;
    private Subscriber<Joy> subscriber = null;
    private float[] publisher_axes =new float[]{0, 0, 0, 0, 0, 0};
    private int[] publisher_button =new int[]{0,0,0,0,0,0,0,0,0,0,1};
    private float[] subcriber_axes =new float[]{0, 0, 0, 0, 0, 0};
    private int[] subcriber_button =new int[]{0,0,0,0,0,0,0,0,0,0,0};

    protected void setupPublisher(ConnectedNode connectedNode){
        publisher = connectedNode.newPublisher(publisherTopic, Joy._TYPE);
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
        Joy joy = publisher.newMessage();
        joy.setAxes(publisher_axes);
        joy.setButtons(publisher_button);
        publisher.publish(joy);
        hasPublishedMsg=true;
    }

    protected void setupSubscriber(ConnectedNode connectedNode) {
        subscriber = connectedNode.newSubscriber(subscriberTopic, Joy._TYPE);
        subscriber.addMessageListener(new MessageListener<Joy>() {
            @Override
            public void onNewMessage(Joy joy) {
                subcriber_axes =joy.getAxes();
                subcriber_button =joy.getButtons();
                hasReceivedMsg=true;
            }
        });
    }

    public float[] getPublisher_axes() {
        return publisher_axes;
    }

    public void setPublisher_axes(float[] publisher_axes) {
        this.publisher_axes = publisher_axes;
    }

    public int[] getPublisher_button() {
        return publisher_button;
    }

    public void setPublisher_button(int[] publisher_button) {
        this.publisher_button = publisher_button;
    }

    public float[] getSubcriber_axes() {
        return subcriber_axes;
    }

    public void setSubcriber_axes(float[] subcriber_axes) {
        this.subcriber_axes = subcriber_axes;
    }

    public int[] getSubcriber_button() {
        return subcriber_button;
    }

    public void setSubcriber_button(int[] subcriber_button) {
        this.subcriber_button = subcriber_button;
    }

}
