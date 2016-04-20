/*
 * This is a customized version of org.ros.android.view.camera.RosCameraPreviewView
 * The changes include:
 * -It allows the user to choose the camera resolution (it no longer depends on the Layout size)
 * -It doesn't need a listener to convert the stream.
 */

/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package uniandes.disc.imagine.android_hri.mobile_interaction.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.base.Preconditions;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.ros.exception.RosRuntimeException;
import org.ros.internal.message.MessageBuffers;

import java.io.IOException;
import java.util.List;

/**
 * Displays preview frames from the camera.
 *
 * @author damonkohler@google.com (Damon Kohler)
 */
public class CustomCameraView extends ViewGroup {

    private final static double ASPECT_TOLERANCE = 0.1;

    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private Size previewSize;
    private byte[] previewBuffer;
    private int cameraId;
    private SurfaceView surfaceView;
    private byte[] rawImageBuffer;
    private Size rawImageSize;
    private YuvImage yuvImage;
    private Rect rect;
    private ChannelBufferOutputStream stream;
    private ChannelBuffer image;
    private boolean imageChanged;
    private List<Size> supportedSizes;

    private int width;
    private int height;


    private BufferingPreviewCallback bufferingPreviewCallback;

    public CustomCameraView(Context context) {
        super(context);
        init(context);
    }

    public CustomCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CustomCameraView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }


    private final class BufferingPreviewCallback implements PreviewCallback {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            Preconditions.checkArgument(camera == CustomCameraView.this.camera);
            Preconditions.checkArgument(data == previewBuffer);
            onNewRawImage(data, previewSize);
            camera.addCallbackBuffer(previewBuffer);
        }
    }

    private void init(Context context) {
        surfaceView = new SurfaceView(context);
        setBackgroundColor(Color.BLACK);
        addView(surfaceView);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolderCallback());
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        bufferingPreviewCallback = new BufferingPreviewCallback();
        stream = new ChannelBufferOutputStream(MessageBuffers.dynamicBuffer());
    }

    public void releaseCamera() {
        if (camera == null) {
            return;
        }
        camera.setPreviewCallbackWithBuffer(null);
        camera.stopPreview();
        camera.release();
        camera = null;
    }

    public void onNewRawImage(byte[] data, Size size) {
        Preconditions.checkNotNull(data);
        Preconditions.checkNotNull(size);
        if (data != rawImageBuffer || !size.equals(rawImageSize)) {
            rawImageBuffer = data;
            rawImageSize = size;
            yuvImage = new YuvImage(rawImageBuffer, ImageFormat.NV21, size.width, size.height, null);
            rect = new Rect(0, 0, size.width, size.height);
        }
        Preconditions.checkState(yuvImage.compressToJpeg(rect, 20, stream));
        setImage(stream.buffer().copy());
        imageChanged = true;
        stream.buffer().clear();
    }

    public void setPreviewSize(Size previewSize) {
        this.previewSize = previewSize;
    }

    public Camera getCamera() {
        return camera;
    }

    public Size getPreviewSize() {
        return previewSize;
    }

    public void updateCamera(){
        releaseCamera();
        selectCamera(cameraId);
    }

    public List<Size> selectCamera(int id){
        cameraId=id;
        int numberOfCameras = Camera.getNumberOfCameras();

        if (numberOfCameras > 1)
            cameraId = cameraId % numberOfCameras;
        releaseCamera();
        setCamera(Camera.open(cameraId));
        return supportedSizes;
    }

    public void setCamera(Camera camera) {
        Preconditions.checkNotNull(camera);
        this.camera = camera;
        setupCameraParameters();
        setupBufferingPreviewCallback();
        camera.startPreview();
        try {
            // This may have no effect if the SurfaceHolder is not yet created.
            camera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            throw new RosRuntimeException(e);
        }
    }

    private void setupCameraParameters() {
        Camera.Parameters parameters = camera.getParameters();
        supportedSizes = parameters.getSupportedPreviewSizes();
        previewSize = getOptimalPreviewSize(supportedSizes, width, height);
        parameters.setPreviewSize(previewSize.width, previewSize.height);
        parameters.setPreviewFormat(ImageFormat.NV21);
        camera.setParameters(parameters);
    }

    private Size getOptimalPreviewSize(List<Size> sizes, int width, int height) {
        Preconditions.checkNotNull(sizes);
        double targetRatio = (double) width / height;
        double minimumDifference = Double.MAX_VALUE;
        Size optimalSize = null;

        // Try to find a size that matches the aspect ratio and size.
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                continue;
            }
            if (Math.abs(size.height - height) < minimumDifference) {
                optimalSize = size;
                minimumDifference = Math.abs(size.height - height);
            }
        }

        // Cannot find one that matches the aspect ratio, ignore the requirement.
        if (optimalSize == null) {
            minimumDifference = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - height) < minimumDifference) {
                    optimalSize = size;
                    minimumDifference = Math.abs(size.height - height);
                }
            }
        }

        Preconditions.checkNotNull(optimalSize);
        return optimalSize;
    }

    private void setupBufferingPreviewCallback() {
        int format = camera.getParameters().getPreviewFormat();
        int bits_per_pixel = ImageFormat.getBitsPerPixel(format);
        previewBuffer = new byte[previewSize.height * previewSize.width * bits_per_pixel / 8];
        camera.addCallbackBuffer(previewBuffer);
        camera.setPreviewCallbackWithBuffer(bufferingPreviewCallback);
    }

    public void setResolution(int width, int height) {
        this.width=width;
        this.height=height;

        float screenRatio=(float)getWidth()/(float)getHeight();
        float resolutionRatio=(float)width/(float)height;
        surfaceView.setScaleX(resolutionRatio/screenRatio);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);
            final int width = r - l;
            final int height = b - t;

            int previewWidth = width;
            int previewHeight = height;
            if (previewSize != null) {
                previewWidth = previewSize.width;
                previewHeight = previewSize.height;
            }

            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0, (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2, width, (height + scaledChildHeight) / 2);
            }
        }
    }

    public ChannelBuffer getImage() {
        return image;
    }

    public void setImage(ChannelBuffer image) {
        this.image = image;
    }

    public boolean hasImageChanged() {
        return imageChanged;
    }

    public void setImageChanged(boolean imageChanged) {
        this.imageChanged = imageChanged;
    }

    private final class SurfaceHolderCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                if (camera != null) {
                    camera.setPreviewDisplay(holder);
                }
            } catch (IOException e) {
                throw new RosRuntimeException(e);
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            releaseCamera();
        }
    }

}
