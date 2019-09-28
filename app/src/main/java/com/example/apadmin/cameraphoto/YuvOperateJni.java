package com.example.apadmin.cameraphoto;

/**
 * Created by zhongjihao on 18-2-7.
 */

public class YuvOperateJni {
    static {
        System.loadLibrary("yuvengine");
    }

    public native static long startYuvEngine();
    public native static void Yv12ToI420(long cPtr,byte[] pYv12, byte[] pI420, int width, int height);
    public native static void I420ToYv12(long cPtr,byte[] pI420, byte[] pYv12, int width, int height);
    public native static void Nv21ToI420(long cPtr,byte[] pNv21,byte[] pI420,int width,int height);
    public native static void I420ToNv21(long cPtr,byte[] pI420,byte[] pNv21,int width,int height);
    public native static void Nv21ToYV12(long cPtr,byte[] pNv21,byte[] pYv12,int width,int height);
    public native static void YV12ToNv21(long cPtr,byte[] pYv12,byte[] pNv21,int width,int height);
    public native static void Nv21ToNv12(long cPtr,byte[] pNv21,byte[] pNv12,int width,int height);
    public native static void Nv12ToNv21(long cPtr,byte[] pNv12,byte[] pNv21,int width,int height);
    public native static void cutCommonYuv(long cPtr,int yuvType, int startX,int startY,byte[] srcYuv, int srcW,int srcH,byte[] tarYuv,int cutW, int cutH);
    public native static void getSpecYuvBuffer(long cPtr,int yuvType,byte[] dstBuf, byte[] srcYuv, int srcW, int srcH,int dirty_Y,int dirty_UV);
    public native static void yuvAddWaterMark(long cPtr,int yuvType, int startX, int startY, byte[] waterMarkData,
                                              int waterMarkW, int waterMarkH,byte[] yuvData, int yuvW, int yuvH);

    public native static void Nv21ClockWiseRotate90(long cPtr,byte[] pNv21,int srcWidth,int srcHeight,byte[] outData,int[] outWidth,int[] outHeight);
    public native static void Nv12ClockWiseRotate90(long cPtr,byte[] pNv12,int srcWidth,int srcHeight,byte[] outData,int[] outWidth,int[] outHeight);
    public native static void Nv21ClockWiseRotate180(long cPtr,byte[] pNv21,int srcWidth,int srcHeight,byte[] outData,int[] outWidth,int[] outHeight);
    public native static void Nv21ClockWiseRotate270(long cPtr,byte[] pNv21,int srcWidth,int srcHeight,byte[] outData,int[] outWidth,int[] outHeight);

    //I420(YUV420P)图像顺时针旋转90度
    public static final native void I420ClockWiseRotate90(long cPtr,byte[] pI420, int srcWidth,int srcHeight,byte[] outData, int[] outWidth,int[] outHeight);
    //YV12图像顺时针旋转90度
    public static final native void Yv12ClockWiseRotate90(long cPtr,byte[] pYv12, int srcWidth,int srcHeight,byte[] outData, int[] outWidth,int[] outHeight);
    public native static void stopYuvEngine(long cPtr);

}
