package uniandes.disc.imagine.android_hri.mobile_interaction.topic;

import org.ros.concurrent.CancellableLoop;
import org.ros.message.MessageListener;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import geometry_msgs.Point;

/**
 * Created by dhrodriguezg on 7/29/15.
 */
public class PointTopic extends AbstractTopic {

    private static final String TAG = "PointTopic";
    private Publisher<Point> publisher = null;
    private Subscriber<Point> subscriber = null;
    private float[] publisher_point = new float[]{0, 0, 0};
    private float[] subcriber_point = new float[]{0, 0, 0};

    protected void setupPublisher(ConnectedNode connectedNode){
        publisher = connectedNode.newPublisher(publisherTopic, Point._TYPE);
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
                if(counter<0)
                    counter=0;
                Thread.sleep(publishFreq);
            }
        };
        connectedNode.executeCancellableLoop(aLoop);
    }

    protected void publish(){
        Point point = publisher.newMessage();
        point.setX(publisher_point[0]);
        point.setY(publisher_point[1]);
        point.setZ(publisher_point[2]);
        publisher.publish(point);
        hasPublishedMsg=true;
    }

    protected void setupSubscriber(ConnectedNode connectedNode) {
        subscriber = connectedNode.newSubscriber(subscriberTopic, Point._TYPE);
        subscriber.addMessageListener(new MessageListener<Point>() {
            @Override
            public void onNewMessage(Point point) {
                subcriber_point[0]=(float)point.getX();
                subcriber_point[1]=(float)point.getY();
                subcriber_point[2]=(float)point.getZ();
                hasReceivedMsg=true;
            }
        });
    }

    public float[] getPublisher_point() {
        return publisher_point;
    }

    public void setPublisher_point(float[] publisher_point) {
        this.publisher_point = publisher_point;
    }

    public float[] getSubcriber_point() {
        return subcriber_point;
    }

    public void setSubcriber_point(float[] subcriber_point) {
        this.subcriber_point = subcriber_point;
    }

}
