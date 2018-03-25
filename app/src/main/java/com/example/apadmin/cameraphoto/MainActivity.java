package com.example.apadmin.cameraphoto;

import android.os.Environment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, VideoGather.CameraOperateCallback{
    private final static String TAG = "MainActivity";
    private Button btnStart;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private SurfacePreview mSurfacePreview;
    private boolean isStarted;
    private AVmediaMuxer mediaMuxer;
    private int width;
    private int height;
    private int frameRate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置全屏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        isStarted = false;
        btnStart = (Button) findViewById(R.id.btn_start);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_view);
        mSurfaceView.setKeepScreenOn(true);
        // 获得SurfaceView的SurfaceHolder
        mSurfaceHolder = mSurfaceView.getHolder();
        // 设置surface不需要自己的维护缓存区
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        // 为srfaceHolder添加一个回调监听器
        mSurfacePreview = new SurfacePreview(this);
        mSurfaceHolder.addCallback(mSurfacePreview);
        btnStart.setOnClickListener(this);

        String filePath = Environment
                .getExternalStorageDirectory()
                + "/"+"zhongjihao/out.mp4";
        Log.d(TAG, "===zhongjihao====创建混合器,保存至:" + filePath);
        mediaMuxer = AVmediaMuxer.newInstance();
        mediaMuxer.initMediaMuxer(filePath);
    }

    private void codecToggle() {
        if (isStarted) {
            isStarted = false;
            //停止编码 先要停止编码，然后停止采集
            mediaMuxer.stopEncoder();
            //停止音频采集
            mediaMuxer.stopAudioGather();
            //释放编码器
            mediaMuxer.release();
            mediaMuxer = null;
        } else {
            isStarted = true;
            if(mediaMuxer == null){
                String filePath = Environment
                        .getExternalStorageDirectory()
                        + "/"+"zhongjihao/out.mp4";
                Log.d(TAG, "===zhongjihao====创建混合器,保存至:" + filePath);
                mediaMuxer = AVmediaMuxer.newInstance();
                mediaMuxer.initMediaMuxer(filePath);
            }
            //采集音频
            mediaMuxer.startAudioGather();
            //初始化音频编码器
            mediaMuxer.initAudioEncoder();
            //初始化视频编码器
            mediaMuxer.initVideoEncoder(width,height,frameRate);
            //启动编码
            mediaMuxer.startEncoder();
        }
        btnStart.setText(isStarted ? "停止" : "开始");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isStarted) {
            isStarted = false;
            //停止编码 先要停止编码，然后停止采集
            mediaMuxer.stopEncoder();
            //停止音频采集
            mediaMuxer.stopAudioGather();
            //释放编码器
            mediaMuxer.release();
            mediaMuxer = null;
        }
        VideoGather.getInstance().doStopCamera();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            finish();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btn_start:
                codecToggle();
                break;
        }
    }

    @Override
    public void cameraHasOpened() {
        VideoGather.getInstance().doStartPreview(this, mSurfaceHolder);
    }

    @Override
    public void cameraHasPreview(int width,int height,int fps) {
        this.width = width;
        this.height = height;
        this.frameRate = fps;
    }
}
