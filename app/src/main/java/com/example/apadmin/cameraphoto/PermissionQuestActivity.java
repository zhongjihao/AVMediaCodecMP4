package com.example.apadmin.cameraphoto;

/**
 * Created by zhongjihao on 24/03/18.
 */

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

public class PermissionQuestActivity extends AppCompatActivity{
    // 要申请的权限
    private String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 版本判断。当手机系统大于 23 时，才有必要去判断权限是否获取
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 检查该权限是否已经获取
            boolean permission = false;
            for (int i = 0; i < permissions.length; i++) {
                int result = ContextCompat.checkSelfPermission(this, permissions[i]);
                // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
                if (result != PackageManager.PERMISSION_GRANTED) {
                    permission = false;
                    break;
                } else
                    permission = true;
            }
            if(!permission){
                // 如果没有授予权限，就去提示用户请求
                ActivityCompat.requestPermissions(this,
                        permissions, 100);
            }else{
                startActivity(new Intent(this,MainActivity.class));
                finish();
            }
        }else{
            startActivity(new Intent(this,MainActivity.class));
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100
                && permissions.length == 3
                && permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                && permissions[1].equals(Manifest.permission.CAMERA)
                && permissions[2].equals(Manifest.permission.RECORD_AUDIO)
                && grantResults[0] ==PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED
                && grantResults[2] ==PackageManager.PERMISSION_GRANTED
                ) {
              startActivity(new Intent(this,MainActivity.class));
        }else{
            Toast.makeText(PermissionQuestActivity.this,"请先开启相应的权限!",Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
