package com.example.apadmin.cameraphoto;

/**
 * Created by zhongjihao on 18-1-31.
 */

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Vector;

public class VideoRunnable extends Thread{
    private static final String TAG = "VideoRunnable";
    private static final boolean VERBOSE = true; // lots of logging
    // parameters for the encoder
    private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video
    private static final int IFRAME_INTERVAL = 5; // 10 between
    // I-frames
    private static final int TIMEOUT_USEC = 10000;
    private static int BIT_RATE;
    public static boolean DEBUG = true;
    private final Object lock = new Object();
    private byte[] mFrameData;
    private byte[] mFrameTmpData;
    private Vector<byte[]> frameBytes;
    private int mWidth;
    private int mHeight;
    private MediaCodec mMediaCodec = null;
    private MediaCodec.BufferInfo mBufferInfo = null;
    private int mColorFormat;
    private long mStartTime = 0;
    private volatile boolean isExit = false;
    private WeakReference<MediaMuxerRunnable> mediaMuxerRunnable;
    private MediaFormat mediaFormat = null;
    private MediaCodecInfo codecInfo = null;
    private volatile boolean isStartCodec = false;
    /**
     * previous presentationTimeUs for writing
     */
    private long prevOutputPTSUs = 0;

    public VideoRunnable(int mWidth, int mHeight, WeakReference<MediaMuxerRunnable> mediaMuxerRunnable) {
        this.mWidth = mWidth;
        this.mHeight = mHeight;
        this.mediaMuxerRunnable = mediaMuxerRunnable;
        frameBytes = new Vector<byte[]>();
        frameBytes.clear();
        BIT_RATE = (CameraWrapper.IMAGE_HEIGHT * CameraWrapper.IMAGE_WIDTH * 3 ) * 8 * CameraWrapper.FRAME_RATE;
        prepare();
    }

    private static int selectColorFormat(MediaCodecInfo codecInfo,
                                         String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo
                .getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat)) {
                return colorFormat;
            }
        }
        if (DEBUG) Log.e(TAG,
                "=====zhongjihao=======couldn't find a good color format for " + codecInfo.getName()
                        + " / " + mimeType);
        return 0; // not reached
    }

    private static boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            // these are the formats we know how to handle for this test
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                return false;
        }
    }

    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    /**
     * Nv21:
     * YYYYYYYY
     * YYYYYYYY
     * YYYYYYYY
     * YYYYYYYY
     * VUVU
     * VUVU
     * VUVU
     * VUVU
     * <p>
     * I420:
     * YYYYYYYY
     * YYYYYYYY
     * YYYYYYYY
     * YYYYYYYY
     * UUUU
     * UUUU
     * VVVV
     * VVVV
     */
    //nv21格式转为yuv420P格式
    private  void NV21toI420SemiPlanar(byte[] nv21bytes, byte[] i420bytes,
                                             int width, int height) {
        final int iSize = width * height;
        System.arraycopy(nv21bytes, 0, i420bytes, 0, iSize);   // Y

        for (int iIndex = 0; iIndex < iSize / 2; iIndex += 2) {
            i420bytes[iSize + iIndex / 2 + iSize / 4] = nv21bytes[iSize + iIndex]; // V
            i420bytes[iSize + iIndex / 2] = nv21bytes[iSize + iIndex + 1]; // U
        }
    }

    //sensor出来的是逆时针旋转90度的数据，hal层没有做旋转导致APP显示和编码需要自己做顺时针旋转90,这样看到的图像才是正常的
    private void YUV420PClockRot90(byte[] dest, byte[] src, int w, int h) {
        int nPos = 0;
        //旋转Y
        int k = 0;
        for (int i = 0; i < w; i++) {
            for (int j = h - 1; j >= 0; j--) {
                dest[k++] = src[j * w + i];
            }
        }
        //旋转U
        nPos = w * h;
        for (int i = 0; i < w / 2; i++) {
            for (int j = h / 2 - 1; j >= 0; j--) {
                dest[k++] = src[nPos + j * w / 2 + i];
            }
        }

        //旋转V
        nPos = w * h * 5 / 4;
        for (int i = 0; i < w / 2; i++) {
            for (int j = h / 2 - 1; j >= 0; j--) {
                dest[k++] = src[nPos + j * w / 2 + i];
            }
        }
    }

    public void exit() {
        isExit = true;
        frameBytes.clear();
        frameBytes = null;
        synchronized (lock) {
            lock.notify();
        }
        if (DEBUG) Log.e(TAG, "=====zhongjihao=========Video 编码开始退出 isStart: "+isStartCodec);
    }

    public void add(byte[] data) {
        if (frameBytes != null) {
            frameBytes.add(data);
            synchronized (lock) {
                lock.notify();
            }
        }
    }

    private void prepare() {
        if (DEBUG) Log.d(TAG, "=======zhongjihao=====VideoEncoder()");
        mFrameData = new byte[this.mWidth * this.mHeight * 3 / 2];
        mFrameTmpData = new byte[this.mWidth * this.mHeight * 3 / 2];
        mBufferInfo = new MediaCodec.BufferInfo();
        //选择系统用于编码H264的编码器信息
        codecInfo = selectCodec(MIME_TYPE);
        if (codecInfo == null) {
            if (DEBUG) Log.e(TAG, "====zhongjihao=====Unable to find an appropriate codec for " + MIME_TYPE);
            return;
        }
        if (VERBOSE)
            if (DEBUG) Log.d(TAG, "======zhongjihao====found codec: " + codecInfo.getName());
        //根据MIME格式,选择颜色格式
        mColorFormat = selectColorFormat(codecInfo, MIME_TYPE);
        if (VERBOSE)
            if (DEBUG) Log.d(TAG, "=====zhongjihao====found colorFormat: " + mColorFormat);
        //根据MIME创建MediaFormat
        // sensor出来的是逆时针旋转90度的数据，hal层没有做旋转导致APP显示和编码需要自己做顺时针旋转90,这样看到的图像才是正常的
        mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE,
                this.mHeight, this.mWidth);
        //设置比特率
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        //设置帧率
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, CameraWrapper.FRAME_RATE);
        //设置颜色格式
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, mColorFormat);
        //设置关键帧的时间
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        if (VERBOSE)
            if (DEBUG) Log.d(TAG, "=====zhongjihao=======format: " + mediaFormat);
    }

    private void startMediaCodec() throws IOException {
        //创建一个MediaCodec
        mMediaCodec = MediaCodec.createByCodecName(codecInfo.getName());
        mMediaCodec.configure(mediaFormat, null, null,
                MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();

        isStartCodec = true;
    }

    private void stopMediaCodec() {
        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
        isStartCodec = false;
        if (DEBUG) Log.e(TAG, "======zhongjihao======stop video 编码...");
    }

    //这个参数input就是上面回调拿到的原始数据
    private void encodeFrame(byte[] input,final long presentationTimeUs) {
        if (DEBUG) Log.d(TAG, "=====zhongjihao=====encodeFrame()");
        if (isExit) return;
        //用于NV21格式转换为YUV420P格式
        NV21toI420SemiPlanar(input, mFrameTmpData, this.mWidth, this.mHeight);
        YUV420PClockRot90(mFrameData,mFrameTmpData,mWidth,mHeight);

        //拿到输入缓冲区,用于传送数据进行编码
        ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        //得到当前有效的输入缓冲区的索引
        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
        if (DEBUG) Log.d(TAG, "====zhongjihao=======inputBufferIndex-->" + inputBufferIndex);
        if (inputBufferIndex >= 0) { //输入缓冲区有效
            long endTime = System.nanoTime();
            long ptsUsec = (endTime - mStartTime) / 1000;
            if (DEBUG) Log.d(TAG, "======zhongjihao======resentationTime: " + ptsUsec);
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            //往输入缓冲区写入数据
            inputBuffer.put(mFrameData);

            if (!isStartCodec) {
                //结束时，发送结束标志，在编码完成后结束
                if (DEBUG) Log.d(TAG, "=====zhongjihao======send BUFFER_FLAG_END_OF_STREAM");
                mMediaCodec.queueInputBuffer(inputBufferIndex, 0, mFrameData.length,
                        presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            } else {
                //将缓冲区入队
                mMediaCodec.queueInputBuffer(inputBufferIndex, 0, mFrameData.length,
                        presentationTimeUs, 0);
//                mMediaCodec.queueInputBuffer(inputBufferIndex, 0,
//                        mFrameData.length, System.nanoTime() / 1000, 0);
           }
        } else {
            // either all in use, or we timed out during initial setup
            if (DEBUG) Log.d(TAG, "====zhongjihao=====input buffer not available");
        }

        MediaMuxerRunnable muxer = mediaMuxerRunnable.get();
        if (muxer == null) {
            if (DEBUG) Log.e(TAG, "=====zhongjihao======MediaMuxerRunnable is unexpectedly null");
            return;
        }

        //拿到输出缓冲区,用于取到编码后的数据
        ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
        int outputBufferIndex;
        do {
            //拿到输出缓冲区的索引
            outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo,TIMEOUT_USEC);
            if (DEBUG) Log.d(TAG, "=====zhongjihao====outputBufferIndex-->" + outputBufferIndex);
            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuffers = mMediaCodec.getOutputBuffers();
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = mMediaCodec.getOutputFormat();
                if (muxer != null) {
                    if (DEBUG)
                        Log.e(TAG, "======zhongjihao======添加视轨 INFO_OUTPUT_FORMAT_CHANGED " + newFormat.toString());
                    //如果要合成视频和音频,需要处理混合器的音轨和视轨的添加.因为只有添加音轨和视轨之后,写入数据才有效
                    muxer.setMediaFormat(MediaMuxerRunnable.TRACK_VIDEO, newFormat);
                }
            } else if (outputBufferIndex < 0) {
            } else {
                if (DEBUG) Log.d(TAG, "=====zhongjihao======perform encoding");
                //数据已经编码成H264格式
                //outputBuffer保存的就是H264数据
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                if (outputBuffer == null) {
                    throw new RuntimeException("encoderOutputBuffer " + outputBufferIndex +
                            " was null");
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    if (DEBUG) Log.d(TAG, "======zhongjihao=======ignoring BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }
                if (mBufferInfo.size != 0 && muxer != null) {
                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    outputBuffer.position(mBufferInfo.offset);
                    outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);
                    mBufferInfo.presentationTimeUs = getPTSUs();
                    if (DEBUG) Log.e(TAG, "=====zhongjihao======添加视轨 数据 size: " + mBufferInfo.size+"   offset: "+mBufferInfo.offset);
                    //这一步就是添加视频数据到混合器了,在调用添加数据之前,一定要确保视轨和音轨都添加到了混合器
                    muxer.addMuxerData(new MediaMuxerRunnable.MuxerData(
                                MediaMuxerRunnable.TRACK_VIDEO, outputBuffer, mBufferInfo));
                    prevOutputPTSUs = mBufferInfo.presentationTimeUs;
                    if (DEBUG) Log.d(TAG, "======zhongjihao=======sent " + mBufferInfo.size + " frameBytes to muxer");
                }
                //释放资源
                mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);

                //编码结束的标志
                if((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) !=0 ){
                    return;
                }
            }
        } while (outputBufferIndex >= 0);
    }

    @Override
    public void run() {
        while (!isExit) {
            if (!isStartCodec) {
                stopMediaCodec();
                try {
                    if (DEBUG) Log.e(TAG, "=====zhongjihao======video -- startMediaCodec...");
                    startMediaCodec();
                } catch (IOException e) {
                    isStartCodec = false;
                }
            } else if (!frameBytes.isEmpty()) {
                byte[] bytes = this.frameBytes.remove(0);
                if(DEBUG) Log.e(TAG, "======zhongjihao====编码视频数据:" + bytes.length);
                try {
                    encodeFrame(bytes,getPTSUs());
                } catch (Exception e) {
                    if (DEBUG) Log.e(TAG, "===zhongjihao==========编码视频(Video)数据 失败");
                    e.printStackTrace();
                }
            } else if (frameBytes.isEmpty()) {
                synchronized (lock) {
                    try {
                        if (DEBUG) Log.e(TAG, "===zhongjihao=======video -- 等待数据编码...");
                        lock.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
        stopMediaCodec();
        if (DEBUG) Log.e(TAG, "=====zhongjihao=========Video 编码线程 退出...");
    }

    /**
     * get next encoding presentationTimeUs
     *
     * @return
     */
    private long getPTSUs() {
        long result = System.nanoTime() / 1000L;
        // presentationTimeUs should be monotonic
        // otherwise muxer fail to write
        if (result < prevOutputPTSUs)
            result =  (prevOutputPTSUs - result) + result;
        return result;
    }
}
