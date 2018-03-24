package com.example.apadmin.cameraphoto;

/**
 * Created by zhongjihao on 18-2-1.
 */

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.hardware.Camera.Parameters.FOCUS_MODE_AUTO;
import static android.hardware.Camera.Parameters.PREVIEW_FPS_MAX_INDEX;
import static android.hardware.Camera.Parameters.PREVIEW_FPS_MIN_INDEX;

@SuppressLint("NewApi")
public class CameraWrapper {
    public static  int IMAGE_HEIGHT = 1080;
    public static  int IMAGE_WIDTH = 1920;
    public static  int FRAME_RATE = 30; // 30fps
    private static final String TAG = "CameraWrapper";
    private static final boolean DEBUG = true;    // TODO set false on release
    private static CameraWrapper mCameraWrapper;

    // 定义系统所用的照相机
    private Camera mCamera;
    //预览尺寸
    private Camera.Size previewSize;
    private Camera.Parameters mCameraParamters;
    private boolean mIsPreviewing = false;
    private volatile boolean isStopRecord = false;
    private CameraPreviewCallback mCameraPreviewCallback;

    private CameraWrapper() {
    }

    public interface CamOpenOverCallback {
        public void cameraHasOpened();
    }

    public static CameraWrapper getInstance() {
        if (mCameraWrapper == null) {
            synchronized (CameraWrapper.class) {
                if (mCameraWrapper == null) {
                    mCameraWrapper = new CameraWrapper();
                }
            }
        }
        return mCameraWrapper;
    }

    public void doOpenCamera(CamOpenOverCallback callback) {
        Log.d(TAG, "====zhongjihao====Camera open....");
        if(mCamera != null)
            return;
        if (mCamera == null) {
            Log.d(TAG, "No front-facing camera found; opening default");
            mCamera = Camera.open();    // opens first back-facing camera
        }
        if (mCamera == null) {
            throw new RuntimeException("Unable to open camera");
        }
        Log.d(TAG, "====zhongjihao=====Camera open over....");
        callback.cameraHasOpened();
    }

    public void doStartPreview(Activity activity,SurfaceHolder surfaceHolder) {
        if (mIsPreviewing) {
            return;
        }
        setCameraDisplayOrientation(activity, Camera.CameraInfo.CAMERA_FACING_BACK);
        setCameraParamter();
        try {
            // 通过SurfaceView显示取景画面
            mCamera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "=====zhongjihao===doStartPreview()");
        mCamera.startPreview();
        mIsPreviewing = true;
    }

    public void doStopCamera() {
        Log.d(TAG, "=====zhongjihao=======doStopCamera");
       // stopRecording();
        // 如果camera不为null，释放摄像头
        if (mCamera != null) {
            mCamera.setPreviewCallbackWithBuffer(null);
            mCameraPreviewCallback = null;
            if (mIsPreviewing)
                mCamera.stopPreview();
            mIsPreviewing = false;
            mCamera.release();
            mCamera = null;
        }
    }

    private void setCameraParamter() {
        if (!mIsPreviewing && mCamera != null) {
            mCameraParamters = mCamera.getParameters();
            mCameraParamters.setPreviewFormat(ImageFormat.NV21);
            mCameraParamters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCameraParamters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            mCameraParamters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
            // Set preview size.
            List<Camera.Size> supportedPreviewSizes = mCameraParamters.getSupportedPreviewSizes();
            Collections.sort(supportedPreviewSizes, new Comparator<Camera.Size>() {
                @Override
                public int compare(Camera.Size o1, Camera.Size o2) {
                    Integer left = o1.width;
                    Integer right = o2.width;
                    return left.compareTo(right);
                }
            });

            for (Camera.Size size : supportedPreviewSizes) {
                if (size.width >= 240 && size.width <= 720) {
                    previewSize = size;
                    Log.d(TAG, "====zhongjihao=====select preview size width=" + size.width + ",height=" + size.height);
                    break;
                }
            }
            IMAGE_WIDTH = previewSize.width;
            IMAGE_HEIGHT = previewSize.height;
            mCameraParamters.setPreviewSize(previewSize.width, previewSize.height);
            mCameraParamters.setFocusMode(FOCUS_MODE_AUTO);

            //set fps range.
            int defminFps = 5;
            int defmaxFps = 30;
            List<int[]> supportedPreviewFpsRange = mCameraParamters.getSupportedPreviewFpsRange();
            for (int[] fps : supportedPreviewFpsRange) {
                if (fps[PREVIEW_FPS_MAX_INDEX] <= defmaxFps && fps[PREVIEW_FPS_MIN_INDEX] >= defminFps) {
                    defminFps = fps[PREVIEW_FPS_MIN_INDEX];
                    defmaxFps = fps[PREVIEW_FPS_MAX_INDEX];
                    //设置相机预览帧率
                    mCameraParamters.setPreviewFpsRange(defminFps,defmaxFps);
                    FRAME_RATE = defmaxFps;
                    Log.d(TAG, "=====zhongjihao=====setParameters====find fps:" + Arrays.toString(fps));
                    break;
                }
            }
            mCameraPreviewCallback = new CameraPreviewCallback();
            mCamera.addCallbackBuffer(new byte[calculateLength(ImageFormat.NV21)]);
            mCamera.setPreviewCallbackWithBuffer(mCameraPreviewCallback);
//            List<String> focusModes = parameters.getSupportedFocusModes();
//            if (focusModes.contains("continuous-video")) {
//                parameters
//                        .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
//            }
            Log.d(TAG, "=====zhongjihao=====setParameters====IMAGE_WIDTH:" + IMAGE_WIDTH+"   IMAGE_HEIGHT: "+IMAGE_HEIGHT+"  FRAME_RATE: "+FRAME_RATE);
            mCamera.setParameters(mCameraParamters);
        }
    }

    private int calculateLength(int format) {
        return previewSize.width * previewSize.height
                * ImageFormat.getBitsPerPixel(format) / 8;
    }


    public void setCameraDisplayOrientation(Activity activity,int cameraId) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result = 0;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        Log.d(TAG, "=====zhongjihao=====setCameraDisplayOrientation=====result:" + result+"  rotation: "+rotation+"  degrees: "+degrees+"  orientation: "+info.orientation);
        mCamera.setDisplayOrientation(result);
    }

    public void startRecording() {
        isStopRecord = false;
        MediaMuxerRunnable.startMuxer();
    }

    public void stopRecording() {
        isStopRecord = true;
        MediaMuxerRunnable.stopMuxer();
    }

    class CameraPreviewCallback implements Camera.PreviewCallback {

        private CameraPreviewCallback() {

        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            //通过回调,拿到的data数据是原始数据
            //丢给VideoRunnable线程,使用MediaCodec进行h264编码操作
            if(data != null){
                if(!isStopRecord)
                    MediaMuxerRunnable.addVideoFrameData(data);
                camera.addCallbackBuffer(data);
            }
            else {
                camera.addCallbackBuffer(new byte[calculateLength(ImageFormat.NV21)]);
            }
        }
    }

}
