package com.example.apadmin.cameraphoto;

/**
 * Created by zhongjihao on 18-2-7.
 */
import android.view.SurfaceHolder;
import android.util.Log;

public class SurfacePreview  implements SurfaceHolder.Callback{
    private final static String TAG = "SurfacePreview";
    private VideoGather.CameraOperateCallback mCallback;
    private PermissionNotify listener;

    public interface PermissionNotify{
        boolean hasPermission();
    }

    public SurfacePreview(VideoGather.CameraOperateCallback cb,PermissionNotify listener){
        mCallback = cb;
        this.listener = listener;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0) {
        Log.d(TAG, "======zhongjihao=====surfaceDestroyed()====");
        VideoGather.getInstance().doStopCamera();
    }

    @Override
    public void surfaceCreated(SurfaceHolder arg0) {
        Log.d(TAG, "======zhongjihao=====surfaceCreated()====");
        if(listener != null){
            if(listener.hasPermission())
                // 打开摄像头
                VideoGather.getInstance().doOpenCamera(mCallback);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
        Log.d(TAG, "======zhongjihao=====surfaceChanged()====");
        if(listener != null){
            if(listener.hasPermission())
                // 打开摄像头
                VideoGather.getInstance().doOpenCamera(mCallback);
        }
    }

}
