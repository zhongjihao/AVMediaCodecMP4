package com.example.apadmin.cameraphoto;

/**
 * Created by zhongjihao on 18-2-7.
 */

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.DisplayMetrics;
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
public class VideoGather {
    private static final String TAG = "VideoGather";
    private int preWidth;
    private int preHeight;
    private int frameRate;
    private static VideoGather mCameraWrapper;

    // 定义系统所用的照相机
    private Camera mCamera;
    //预览尺寸
    private Camera.Size previewSize;
    private Camera.Parameters mCameraParamters;
    private boolean mIsPreviewing = false;
    private CameraPreviewCallback mCameraPreviewCallback;

    private Callback mCallback;
    private CameraOperateCallback cameraCb;
    private Context mContext;

    private VideoGather() {
    }

    public interface CameraOperateCallback {
        public void cameraHasOpened();
        public void cameraHasPreview(int width,int height,int fps);
    }

    public interface Callback {
        public void videoData(byte[] data);
    }

    public static VideoGather getInstance() {
        if (mCameraWrapper == null) {
            synchronized (VideoGather.class) {
                if (mCameraWrapper == null) {
                    mCameraWrapper = new VideoGather();
                }
            }
        }
        return mCameraWrapper;
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void doOpenCamera(CameraOperateCallback callback) {
        Log.d(TAG, "====zhongjihao====Camera open....");
        cameraCb = callback;
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
        cameraCb.cameraHasOpened();
    }

    public void doStartPreview(Activity activity,SurfaceHolder surfaceHolder) {
        if (mIsPreviewing) {
            return;
        }
        mContext = activity;
        setCameraDisplayOrientation(activity, Camera.CameraInfo.CAMERA_FACING_BACK);
        setCameraParamter();
        try {
            // 通过SurfaceView显示取景画面
            mCamera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
        mIsPreviewing = true;
        Log.d(TAG, "=====zhongjihao===Camera Preview Started...");
        cameraCb.cameraHasPreview(preWidth,preHeight,frameRate);
    }

    public void doStopCamera() {
        Log.d(TAG, "=====zhongjihao=======doStopCamera");
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
        mContext = null;
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

            DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
            Log.d(TAG, "====zhongjihao=====Screen width=" + dm.widthPixels + ", height=" + dm.heightPixels);
            for (Camera.Size size : supportedPreviewSizes) {
                if (size.width >= dm.heightPixels && size.height >= dm.widthPixels) {
                    if ((1.0f * size.width / size.height) == (1.0f * dm.heightPixels / dm.widthPixels)) {
                        previewSize = size;
                        Log.d(TAG, "====zhongjihao=====select preview size width=" + size.width + ",height=" + size.height);
                        break;
                    }
                }
            }
            preWidth = previewSize.width;
            preHeight = previewSize.height;
            mCameraParamters.setPreviewSize(previewSize.width, previewSize.height);
            mCameraParamters.setFocusMode(FOCUS_MODE_AUTO);

            //set fps range.
            int defminFps = 0;
            int defmaxFps = 0;
            List<int[]> supportedPreviewFpsRange = mCameraParamters.getSupportedPreviewFpsRange();
            for (int[] fps : supportedPreviewFpsRange) {
                Log.d(TAG, "=====zhongjihao=====setParameters====find fps:" + Arrays.toString(fps));
                if (defminFps <= fps[PREVIEW_FPS_MIN_INDEX] && defmaxFps <= fps[PREVIEW_FPS_MAX_INDEX]) {
                    defminFps = fps[PREVIEW_FPS_MIN_INDEX];
                    defmaxFps = fps[PREVIEW_FPS_MAX_INDEX];
                }
            }
            //设置相机预览帧率
            Log.d(TAG, "=====zhongjihao=====setParameters====defminFps:" + defminFps+"    defmaxFps: "+defmaxFps);
            mCameraParamters.setPreviewFpsRange(defminFps,defmaxFps);
            frameRate = defmaxFps / 1000;
            mCameraPreviewCallback = new CameraPreviewCallback();
            mCamera.addCallbackBuffer(new byte[calculateLength(ImageFormat.NV21)]);
            mCamera.setPreviewCallbackWithBuffer(mCameraPreviewCallback);
//            List<String> focusModes = parameters.getSupportedFocusModes();
//            if (focusModes.contains("continuous-video")) {
//                parameters
//                        .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
//            }
            Log.d(TAG, "=====zhongjihao=====setParameters====preWidth:" + preWidth+"   preHeight: "+preHeight+"  frameRate: "+frameRate);
            mCamera.setParameters(mCameraParamters);
        }
    }

    private int calculateLength(int format) {
        return previewSize.width * previewSize.height
                * ImageFormat.getBitsPerPixel(format) / 8;
    }

    private void setCameraDisplayOrientation(Activity activity,int cameraId) {
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

    class CameraPreviewCallback implements Camera.PreviewCallback {
        private CameraPreviewCallback() {

        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            //通过回调,拿到的data数据是原始数据
            //丢给VideoRunnable线程,使用MediaCodec进行h264编码操作
            if(data != null){
                if(mCallback != null)
                    mCallback.videoData(data);
                camera.addCallbackBuffer(data);
            }
            else {
                camera.addCallbackBuffer(new byte[calculateLength(ImageFormat.NV21)]);
            }
        }
    }

}
