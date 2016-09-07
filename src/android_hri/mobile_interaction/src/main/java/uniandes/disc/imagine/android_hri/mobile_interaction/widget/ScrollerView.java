package uniandes.disc.imagine.android_hri.mobile_interaction.widget;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Vector;

import uniandes.disc.imagine.android_hri.mobile_interaction.R;


/**
 * Created by dhrodriguezg on 10/15/15.
 */
public class ScrollerView extends RelativeLayout {

    private Context context;
    private RelativeLayout mainLayout;
    private CustomScrollView scrollView;
    private LinearLayout viewContainer;
    private Vector<TextView> vectorText;
    private TextView activeView;
    private boolean updateView;
    private int BACKGROUND = Color.LTGRAY;
    private int BORDER = Color.argb(191,0,0,0);
    private boolean percentage;

    private ImageView top;
    private ImageView bottom;
    private ImageView selection;

    private float topValue = 0;
    private float bottomValue = 0;
    private int maxVisibleItems = 5;//odd number
    private int maxTotalItems = 10;
    private float fontSize = 14;
    private int initialPosition;

    public ScrollerView(Context context) {
        super(context);
        initScroller(context);
    }

    public ScrollerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initScroller(context);
    }

    public ScrollerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initScroller(context);
    }

    private void initScroller(Context context) {

        /** Init Layouts**/
        this.context=context;
        updateView = true;
        percentage = false;
        mainLayout = this;
        scrollView = new CustomScrollView(context);
        viewContainer = new LinearLayout(context);
        viewContainer.setOrientation(LinearLayout.VERTICAL);
        top = new ImageView(context);
        bottom = new ImageView(context);
        selection = new ImageView(context);

        mainLayout.addView(scrollView);
        scrollView.addView(viewContainer);
        mainLayout.addView(selection);
        mainLayout.addView(bottom);
        mainLayout.addView(top);

        LayoutParams scrollParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        scrollView.setLayoutParams(scrollParams);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setHorizontalScrollBarEnabled(false);

        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        viewContainer.setLayoutParams(containerParams);
        viewContainer.setBackgroundColor(BORDER);
        viewContainer.setPadding(10, 0, 10, 0);

        LayoutParams selectionParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        selectionParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        selection.setLayoutParams(selectionParams);
        selection.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.scroller_selection));
        selection.setAlpha(0.7f);
        selection.setScaleType(ImageView.ScaleType.FIT_XY);

        LayoutParams bottomParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        bottomParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        bottomParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        bottom.setLayoutParams(bottomParams);
        bottom.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.scroller_bottom));
        bottom.setScaleType(ImageView.ScaleType.FIT_XY);

        LayoutParams topParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        topParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        topParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        top.setLayoutParams(topParams);
        top.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.scroller_top));
        top.setScaleType(ImageView.ScaleType.FIT_XY);

        populateContainer();
    }

    private void populateContainer(){
        vectorText= new Vector<>();
        viewContainer.removeAllViews();
        for(int n=0;n< maxTotalItems + maxVisibleItems -1;n++){
            TextView tv = new TextView(context);
            tv.setBackgroundColor(BACKGROUND);
            tv.setGravity(Gravity.CENTER);
            tv.setText("" + n);
            vectorText.add(tv);
            viewContainer.addView(tv);
        }
        activeView=vectorText.elementAt(maxVisibleItems /2);
    }

    private void resizeContainer(){
        populateContainer();
        int height = scrollView.getHeight()/ maxVisibleItems;
        top.getLayoutParams().height=height*3/2;
        bottom.getLayoutParams().height=height*3/2;
        selection.getLayoutParams().height=height;

        for(int i = 0; i < vectorText.size(); i++){
            TextView tv = vectorText.elementAt(i);
            tv.setHeight(height);

            float value;
            float bottomValue;
            float topValue;
            if(percentage){
                if(this.topValue > this.bottomValue){
                    topValue = 1.f;
                    bottomValue = 0.f;
                }else{
                    topValue = 0.f;
                    bottomValue = 1.f;
                }
            }else{
                bottomValue = this.bottomValue;
                topValue = this.topValue;
            }
            value = (bottomValue - topValue) * (float) (i - maxVisibleItems / 2) / (float) (maxTotalItems -1) + topValue;
            if(i-1 < maxVisibleItems /2)
                value= topValue;
            if(i > vectorText.size()- maxVisibleItems /2-1)
                value = bottomValue;

            if(percentage)
                tv.setText(String.format("%.1f", 100.f*value)+"%");
            else
                tv.setText(String.format("%.2f", value));
            tv.setTextSize(fontSize);
        }
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.scrollTo(0, (int) vectorText.elementAt(initialPosition - 1).getY());
            }
        });
        updateView = false;
    }

    public void updateView(){
        if(updateView)
            resizeContainer();
        int index = Math.round((float)scrollView.getScrollY()/((float)activeView.getHeight()));
        activeView.setTypeface(null, Typeface.NORMAL);
        activeView=vectorText.elementAt(index + maxVisibleItems /2);
        activeView.setTextSize(fontSize);
        activeView.setTypeface(null, Typeface.BOLD);

        float cy = scrollView.getScrollY()+activeView.getHeight()*maxVisibleItems/2;
        for(int n=1; n < 1+maxVisibleItems/2; n++){
            TextView belowText = vectorText.elementAt(index+maxVisibleItems/2 + n );
            TextView aboveText = vectorText.elementAt(index+maxVisibleItems/2 - n );
            belowText.setTextSize(fontSize-Math.abs(belowText.getY()-cy)/(float)belowText.getHeight());
            aboveText.setTextSize(fontSize-0.5f*Math.abs(aboveText.getY()-cy)/(float)aboveText.getHeight());
        }
    }

    public float getValue(){
        
        float max=Math.max(topValue,bottomValue);
        float min = Math.min(topValue,bottomValue);
        float normalizedScroll=(float)scrollView.getScrollY()/(float)(viewContainer.getBottom()-scrollView.getHeight());
        float selection=(bottomValue - topValue)*normalizedScroll + topValue;
        //float selection=Float.parseFloat(activeView.getText().toString());
        if(selection>max)
            selection=max;
        if(selection<min)
            selection=min;
        return selection;
    }

    public void showPercentage(){
        percentage = true;
    }

    public void showVales(){
        percentage = false;
    }

    public void beginAtTop(){
        initialPosition=1;
    }

    public void beginAtBottom(){
        initialPosition=maxTotalItems;
    }

    public void beginAtMiddle(){
        initialPosition=maxTotalItems/2+1;
    }

    public void beginAtItem(int index){
        initialPosition=index;
    }

    public float getTopValue() {
        return topValue;
    }

    public void setTopValue(float topValue) {
        updateView = true;
        this.topValue = topValue;
    }

    public float getBottomValue() {
        return bottomValue;
    }

    public void setBottomValue(float bottomValue) {
        updateView = true;
        this.bottomValue = bottomValue;
    }

    public int getMaxVisibleItems() {
        return maxVisibleItems;
    }

    public void setMaxVisibleItems(int maxVisibleItems) {
        updateView = true;
        this.maxVisibleItems = maxVisibleItems%2 == 0 ? maxVisibleItems-1 : maxVisibleItems;
    }

    public int getMaxTotalItems() {
        return maxTotalItems;
    }

    public void setMaxTotalItems(int maxTotalItems) {
        updateView = true;
        this.maxTotalItems = maxTotalItems;
    }

    public float getFontSize() {
        return fontSize;
    }

    public void setFontSize(float fontSize) {
        updateView = true;
        this.fontSize = fontSize;
    }


    public class CustomScrollView extends ScrollView{

        public CustomScrollView(Context context) {
            super(context);
        }

        public CustomScrollView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public CustomScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        public void fling(int velocity){
        }

        @Override
        public boolean onTouchEvent(MotionEvent motionEvent) {
            super.onTouchEvent(motionEvent);

            switch (motionEvent.getActionMasked()) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_CANCEL:
                    scrollTo(0, (int) vectorText.elementAt(initialPosition - 1).getY());
                    break;
            }
            return true;
        }
    }

}
