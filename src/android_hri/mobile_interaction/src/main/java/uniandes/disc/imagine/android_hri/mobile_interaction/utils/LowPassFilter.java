package uniandes.disc.imagine.android_hri.mobile_interaction.utils;

/**
 * Created by dhrodriguezg on 10/9/15.
 */
import com.leapmotion.leap.Vector;

public class LowPassFilter {

    ChevyshovFilter axis_x = null;
    ChevyshovFilter axis_y = null;
    ChevyshovFilter axis_z = null;

    public LowPassFilter(){
        axis_x = new ChevyshovFilter();
        axis_y = new ChevyshovFilter();
        axis_z = new ChevyshovFilter();
    }

    //Fastest filter, only 5 frames of delay but not very smooth
    public float applyFilter1(float currentValue){
        axis_x.applyFilter1(currentValue);
        return axis_x.getOutput();
    }

    //Slowest filter, 10 frames of delay but smoother
    public float applyFilter2(float currentValue){
        axis_x.applyFilter2(currentValue);
        return axis_x.getOutput();
    }

    //Fastest filter, only 5 frames of delay but not very smooth
    public void applyFilter1(Vector currentVector){
        axis_x.applyFilter1(currentVector.getX());
        axis_y.applyFilter1(currentVector.getY());
        axis_z.applyFilter1(currentVector.getZ());
        currentVector.setX(axis_x.getOutput());
        currentVector.setY(axis_y.getOutput());
        currentVector.setZ(axis_z.getOutput());
    }

    //Slowest filter, 10 frames of delay but smoother
    public void applyFilter2(Vector currentVector){
        axis_x.applyFilter2(currentVector.getX());
        axis_y.applyFilter2(currentVector.getY());
        axis_z.applyFilter2(currentVector.getZ());
        currentVector.setX(axis_x.getOutput());
        currentVector.setY(axis_y.getOutput());
        currentVector.setZ(axis_z.getOutput());
    }


    public class ChevyshovFilter {
        private float[] input;
        private float[] output;

        public ChevyshovFilter(){
            input = new float[3];
            output = new float[3];
        }

        public void applyFilter1(float value){
            input[2]=input[1];
            input[1]=input[0];
            input[0]=value;

            output[2]=output[1];
            output[1]=output[0];
            applyFilter1Order();
        }

        public void applyFilter2(float value){
            input[2]=input[1];
            input[1]=input[0];
            input[0]=value;

            output[2]=output[1];
            output[1]=output[0];
            applyFilter2Order();
        }

        public float getOutput(){
            return output[0];
        }

        /*
        * filter Order 1, FC = 2Hz
        * for n=2:length(input)
        *     output(n)=0.1862*input(n) + 0.1862*input(n-1) + 0.6276*output(n-1);
        * end
        */
        private void applyFilter1Order(){
            output[0]=0.1862f*input[0] + 0.1862f*input[1] + 0.6276f*output[1];
        }

        /*
        * filter Order 2, FC = 4Hz
        * for n=3:length(input)
        *     output(n)=0.01355*input(n) + 0.0271*input(n-1) + 0.01355*input(n-2) + 1.6645*output(n-1) -0.7193*output(n-2);
        * end
        */
        private void applyFilter2Order(){
            output[0]=0.01355f*input[0] + 0.0271f*input[1] + 0.01355f*input[2] + 1.6645f*output[1] -0.7193f*output[2];
        }

    }
}
