package uniandes.disc.imagine.android_hri.mobile_interaction.topic;

import org.jboss.netty.buffer.ChannelBuffer;
import org.ros.concurrent.CancellableLoop;
import org.ros.message.MessageListener;
import org.ros.message.Time;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import sensor_msgs.CompressedImage;

/**
 * Created by dhrodriguezg on 7/29/15.
 */


public class CompressedImageTopic extends AbstractTopic {

    private static final String TAG = "CompressedImageTopic";

    private ChannelBuffer publisher_image = null;
    private ChannelBuffer subcriber_image = null;
    protected Publisher<CompressedImage> publisher = null;
    protected Subscriber<CompressedImage> subscriber = null;

    protected void setupPublisher(ConnectedNode connectedNode){
        publisher = connectedNode.newPublisher(publisherTopic, CompressedImage._TYPE);
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
        CompressedImage compressedImage = publisher.newMessage();
        compressedImage.getHeader().setStamp(Time.fromMillis(System.currentTimeMillis()));
        compressedImage.getHeader().setFrameId("AndroidCamera");
        compressedImage.setFormat("jpeg");
        compressedImage.setData(publisher_image);
        publisher.publish(compressedImage);
        hasPublishedMsg=true;
    }

    protected void setupSubscriber(ConnectedNode connectedNode) {
        subscriber = connectedNode.newSubscriber(subscriberTopic, CompressedImage._TYPE);
        subscriber.addMessageListener(new MessageListener<CompressedImage>() {
            @Override
            public void onNewMessage(CompressedImage compressedImage) {
                subcriber_image = compressedImage.getData();
                hasReceivedMsg = true;
            }
        });
    }

    public ChannelBuffer getPublisher_image() {
        return publisher_image;
    }

    public void setPublisher_image(ChannelBuffer publisher_image) {
        this.publisher_image = publisher_image;
    }

    public ChannelBuffer getSubcriber_image() {
        return subcriber_image;
    }

    public void setSubcriber_image(ChannelBuffer subcriber_image) {
        this.subcriber_image = subcriber_image;
    }

}
