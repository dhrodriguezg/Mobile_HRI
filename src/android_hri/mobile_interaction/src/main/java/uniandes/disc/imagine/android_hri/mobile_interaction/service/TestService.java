package uniandes.disc.imagine.android_hri.mobile_interaction.service;

import org.ros.exception.RemoteException;
import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseBuilder;
import org.ros.node.service.ServiceResponseListener;
import org.ros.node.service.ServiceServer;

import rosjava_test_msgs.AddTwoInts;
import rosjava_test_msgs.AddTwoIntsRequest;
import rosjava_test_msgs.AddTwoIntsResponse;

/**
 * Created by dhrodriguezg on 7/29/15.
 */

public class TestService extends AbstractService {

    private static final String TAG = "TestService";

    protected ServiceClient<AddTwoIntsRequest, AddTwoIntsResponse> serviceClient = null;
    protected ServiceServer<AddTwoIntsRequest, AddTwoIntsResponse> serviceServer = null;
    protected AddTwoIntsRequest request = null;
    private long sum;
    private long a;
    private long b;

    protected void setupClient(ConnectedNode connectedNode) {
        //The service must have been created before setting the client up.
        try {
            Thread.sleep(10); //If the service was created in Android, give ROS Android a few ms to finish creating it.
            serviceClient = connectedNode.newServiceClient(clientTopic, AddTwoInts._TYPE);
        } catch (ServiceNotFoundException e) {
            serviceClient=null;
            e.printStackTrace();
        } catch (InterruptedException e) {
            serviceClient=null;
            e.printStackTrace();
        }
    }

    public void callService() {
        if (serviceClient==null)
            return;
        request = serviceClient.newMessage();
        request.setA(a);
        request.setB(b);
        serviceClient.call(request, new ServiceResponseListener<AddTwoIntsResponse>() {
            @Override
            public void onSuccess(AddTwoIntsResponse response) {
                sum=response.getSum();
                hasClientSentMsg = true;
            }
            @Override
            public void onFailure(RemoteException e) {
                hasClientSentMsg = false;
                throw new RosRuntimeException(e);
            }
        });
    }

    protected void setupServer(ConnectedNode connectedNode) {
        serviceServer = connectedNode.newServiceServer(serverTopic, AddTwoInts._TYPE, new ServiceResponseBuilder<AddTwoIntsRequest, AddTwoIntsResponse>() {
            @Override
            public void build(AddTwoIntsRequest request, AddTwoIntsResponse response) {
                response.setSum(request.getA() + request.getB());
                hasServerReceivedMsg = true;
            }
        });
    }

    public long getA() {
        return a;
    }

    public void setA(long a) {
        this.a = a;
    }

    public long getB() {
        return b;
    }

    public void setB(long b) {
        this.b = b;
    }

    public long getSum() {
        return sum;
    }

    public void setSum(long sum) {
        this.sum = sum;
    }
}
