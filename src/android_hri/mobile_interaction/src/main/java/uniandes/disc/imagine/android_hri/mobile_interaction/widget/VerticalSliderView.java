package uniandes.disc.imagine.android_hri.mobile_interaction.widget;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import uniandes.disc.imagine.android_hri.mobile_interaction.R;

/**
 * Created by dhrodriguezg on 10/15/15.
 */
public class VerticalSliderView extends RelativeLayout{

    private static final int INVALID_POINTER_ID = -1;
    private RelativeLayout mainLayout;
    private ImageView sliderB;
    private ImageView sliderP;
    private int ptrID;
    private boolean detectingGesture;
    private float value;

    public VerticalSliderView(Context context) {
        super(context);
        initSlider(context);
    }

    public VerticalSliderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initSlider(context);
    }

    public VerticalSliderView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initSlider(context);
    }

    private void initSlider(Context context) {

        /** Init Layouts**/
        ptrID = INVALID_POINTER_ID;
        sliderB = new ImageView(context);
        sliderB.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.slider_b));
        sliderB.setScaleType(ImageView.ScaleType.FIT_XY);
        sliderP = new ImageView(context);
        sliderP.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.slider_p));
        sliderP.setScaleType(ImageView.ScaleType.FIT_XY);
        mainLayout = this;
        mainLayout.addView(sliderB);
        mainLayout.addView(sliderP);

        //sliderHandler = new TouchArea(this, sliderTouch);
        //sliderHandler.enableScroll();
    }


    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        int barWidth=(int)(mainLayout.getWidth()*0.4f);
        int barHeight=(int)(mainLayout.getHeight()*0.9f);
        LayoutParams sliderBParams = new LayoutParams(barWidth, barHeight);
        sliderBParams.addRule(RelativeLayout.CENTER_VERTICAL);
        sliderBParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        sliderB.setLayoutParams(sliderBParams);

        LayoutParams sliderPParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        sliderPParams.addRule(RelativeLayout.CENTER_VERTICAL);
        sliderPParams.width=barWidth;
        sliderPParams.height=barHeight/10;
        sliderP.setLayoutParams(sliderPParams);
        sliderP.setX(mainLayout.getWidth()/2-barWidth/2);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                detectingGesture = true;
                ptrID = event.getPointerId(event.getActionIndex());
                break;
            case MotionEvent.ACTION_MOVE:
                if (ptrID == INVALID_POINTER_ID)
                    return true;
                float y = event.getY(event.findPointerIndex(ptrID));
                if(y<mainLayout.getHeight()*0.05f)
                    y=mainLayout.getHeight()*0.05f;
                if(y>mainLayout.getHeight()*0.95f)
                    y=mainLayout.getHeight()*0.95f;
                sliderP.setY(y - sliderP.getHeight()/2);
                value=2.f*(-(y-(float)mainLayout.getHeight()*0.05f)/((float)mainLayout.getHeight()*0.9f))+1f;
                break;
            case MotionEvent.ACTION_UP:
                sliderP.setY(mainLayout.getHeight()/2-sliderP.getHeight()/2);
                ptrID = INVALID_POINTER_ID;
                detectingGesture = false;
                value=0;
                break;
        }
        return true;
    }

    public boolean isDetectingGesture() {
        return detectingGesture;
    }

    public void setDetectingGesture(boolean detectingGesture) {
        this.detectingGesture = detectingGesture;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }
}
