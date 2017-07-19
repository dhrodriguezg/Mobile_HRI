package uniandes.disc.imagine.android_hri.mobile_interaction.topic;

import org.ros.concurrent.CancellableLoop;
import org.ros.message.MessageListener;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import geometry_msgs.Twist;

/**
 * Created by dhrodriguezg on 7/29/15.
 */
public class TwistTopic extends AbstractTopic {

    private static final String TAG = "TwistTopic";
    private Publisher<Twist> publisher = null;
    private Subscriber<Twist> subscriber = null;
    private float[] publisher_angular =new float[]{0, 0, 0,};
    private float[] publisher_linear =new float[]{0, 0, 0,};
    private float[] subcriber_angular =new float[]{0, 0, 0,};
    private float[] subcriber_linear =new float[]{0, 0, 0,};

    protected void setupPublisher(ConnectedNode connectedNode){
        publisher = connectedNode.newPublisher(publisherTopic, Twist._TYPE);
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
        Twist twist = publisher.newMessage();
        twist.getLinear().setX(publisher_linear[0]);
        twist.getLinear().setY(publisher_linear[1]);
        twist.getLinear().setZ(publisher_linear[2]);
        twist.getAngular().setX(publisher_angular[0]);
        twist.getAngular().setY(publisher_angular[1]);
        twist.getAngular().setZ(publisher_angular[2]);
        publisher.publish(twist);
        hasPublishedMsg=true;
    }

    protected void setupSubscriber(ConnectedNode connectedNode) {
        subscriber = connectedNode.newSubscriber(subscriberTopic, Twist._TYPE);

        subscriber.addMessageListener(new MessageListener<Twist>() {
            @Override
            public void onNewMessage(Twist twist) {
                subcriber_angular[0] = (float) twist.getAngular().getX();
                subcriber_angular[1] = (float) twist.getAngular().getY();
                subcriber_angular[2] = (float) twist.getAngular().getZ();
                subcriber_linear[0] = (float) twist.getLinear().getX();
                subcriber_linear[1] = (float) twist.getLinear().getY();
                subcriber_linear[2] = (float) twist.getLinear().getZ();
                hasReceivedMsg=true;
            }
        });
    }

    public float[] getPublisher_angular() {
        return publisher_angular;
    }

    public void setPublisher_angular(float[] publisher_angular) {
        this.publisher_angular = publisher_angular;
    }

    public float[] getPublisher_linear() {
        return publisher_linear;
    }

    public void setPublisher_linear(float[] publisher_linear) {
        this.publisher_linear = publisher_linear;
    }

    public float[] getSubcriber_angular() {
        return subcriber_angular;
    }

    public void setSubcriber_angular(float[] subcriber_angular) {
        this.subcriber_angular = subcriber_angular;
    }

    public float[] getSubcriber_linear() {
        return subcriber_linear;
    }

    public void setSubcriber_linear(float[] subcriber_linear) {
        this.subcriber_linear = subcriber_linear;
    }

}
