package com.example.apadmin.cameraphoto;

/**
 * Created by apadmin on 18-2-7.
 */

public class Yuv420POperate {
    static {
        System.loadLibrary("yuv420pUtil");
    }

    //nv21格式转为yuv420P格式
    public static final native void NV21toYUV420P(byte[] nv21bytes, byte[] i420bytes,
                                                  int width, int height);
    //nv21格式转为nv12格式
    public static final native void NV21ToNV12(byte[] nv21, byte[] nv12, int width, int height);

    //YUV420P图像顺时针旋转90度
    public static final native void YUV420PClockRot90(byte[] dest, byte[] src, int w, int h);

}
