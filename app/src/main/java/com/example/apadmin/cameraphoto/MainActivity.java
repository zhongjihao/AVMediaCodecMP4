package com.example.apadmin.cameraphoto;

/**
 * Created by zhongjihao on 18-1-31.
 */

import android.Manifest;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, CameraWrapper.CamOpenOverCallback {
    private static final String TAG = "MainActivity";
    private Button btnStart;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private SurfacePreview mSurfacePreview;

    private boolean isStarted;

    // 要申请的权限
    private String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO};

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

        // 版本判断。当手机系统大于 23 时，才有必要去判断权限是否获取
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 检查该权限是否已经获取
            for (int i = 0; i < permissions.length; i++) {
                int result = ContextCompat.checkSelfPermission(this, permissions[i]);
                // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
                if (result != PackageManager.PERMISSION_GRANTED) {
                    // 如果没有授予该权限，就去提示用户请求
                    ActivityCompat.requestPermissions(this,
                            new String[]{permissions[i]}, i);
                }
            }

        }
    }

    private void codecToggle() {
        if (isStarted) {
            isStarted = false;
            CameraWrapper.getInstance().stopRecording();
        } else {
            isStarted = true;
            CameraWrapper.getInstance().startRecording();
        }
        btnStart.setText(isStarted ? "停止" : "开始");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CameraWrapper.getInstance().doStopCamera();
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
        CameraWrapper.getInstance().doStartPreview(this, mSurfaceHolder);
    }
}