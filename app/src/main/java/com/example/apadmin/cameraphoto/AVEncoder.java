package com.example.apadmin.cameraphoto;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by zhongjihao on 17/03/18.
 *
 * 音视频编码，对视频进行AVC编码、对音频进行AAC编码
 */

public class AVEncoder {
    private static final String TAG = "AVEncoder";
    public static boolean DEBUG = true;
    //////////////////VIDEO////////////////////////////
    // parameters for the encoder
    private static final String VIDEO_MIME_TYPE = "video/avc"; // H.264 Advanced Video
    // I-frames
    private static final int IFRAME_INTERVAL = 5; // 10 between
    //预览格式转换后的数据
    private byte[] yuvBuffer;
    //旋转后的数据和分辨率
    private byte[] rotateYuvBuffer;
    private int[] outWidth = new int[1];
    private int[] outHeight = new int[1];

    //Camera预览分辨率和帧率
    private int mWidth;
    private int mHeight;
    private int mFps;
    private MediaCodec vEncoder;
    private MediaFormat videoFormat;
    private int mColorFormat = 0;
    private MediaCodec.BufferInfo vBufferInfo;
    private ArrayList<Integer> supportColorFormatList;
    private Thread videoEncoderThread;
    private volatile boolean videoEncoderLoop = false;
    private volatile boolean vEncoderEnd = false;
    private LinkedBlockingQueue<byte[]> videoQueue;

    ///////////////////AUDIO/////////////////////////////////
    // parameters for the encoder
    private static final String AUDIO_MIME_TYPE = "audio/mp4a-latm";
    private MediaCodec aEncoder;                // API >= 16(Android4.1.2)
    private MediaCodec.BufferInfo aBufferInfo;        // API >= 16(Android4.1.2)
    private MediaCodecInfo audioCodecInfo;
    private MediaFormat audioFormat;
    private Thread audioEncoderThread;
    private volatile boolean audioEncoderLoop = false;
    private volatile boolean aEncoderEnd = false;
    private LinkedBlockingQueue<byte[]> audioQueue;

    /*
    * 直播流的时间戳不论音频还是视频，在整体时间线上应当呈现递增趋势。如果时间戳计算方法是按照音视频分开计算，那么音频时戳和视频时戳可能并不是在一条时间线上，
    * 这就有可能出现音频时戳在某一个时间点比对应的视频时戳小， 在某一个时间点又跳变到比对应的视频时戳大，导致播放端无法对齐。
    * 目前采用的时间戳以发送视频SPS帧为基础，不区分音频流还是视频流，统一使用即将发送RTMP包的系统时间作为该包的时间戳。
    */
    private long presentationTimeUs;
    private final int TIMEOUT_USEC = 10000;
    private Callback mCallback;

    public static AVEncoder newInstance() {
        return new AVEncoder();
    }

    private AVEncoder() {

    }

    /**
     * 设置回调
     *
     * @param callback 回调
     */
    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    public interface Callback {
        void outputVideoFrame(final int trackIndex,final ByteBuffer outBuf,final MediaCodec.BufferInfo bufferInfo);
        void outputAudioFrame(final int trackIndex,final ByteBuffer outBuf,final MediaCodec.BufferInfo bufferInfo);
        void outMediaFormat(final int trackIndex,MediaFormat mediaFormat);
    }

    public void initAudioEncoder(int sampleRate, int pcmFormat,int chanelCount){
        if (aEncoder != null) {
            return;
        }
        aBufferInfo = new MediaCodec.BufferInfo();
        audioQueue = new LinkedBlockingQueue<>();
        audioCodecInfo = selectCodec(AUDIO_MIME_TYPE);
        if (audioCodecInfo == null) {
            if (DEBUG) Log.e(TAG, "=====zhongjihao====Unable to find an appropriate codec for " + AUDIO_MIME_TYPE);
            return;
        }
        Log.d(TAG, "===zhongjihao===selected codec: " + audioCodecInfo.getName());
        audioFormat = MediaFormat.createAudioFormat(AUDIO_MIME_TYPE, sampleRate, chanelCount);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_STEREO);//CHANNEL_IN_STEREO 立体声
        int bitRate = sampleRate * pcmFormat * chanelCount;
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, chanelCount);
        audioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);
        Log.d(TAG, "====zhongjihao=========format: " + audioFormat.toString());

        try {
            aEncoder = MediaCodec.createEncoderByType(AUDIO_MIME_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("===zhongjihao===初始化音频编码器失败", e);
        }
        Log.d(TAG, String.format("=====zhongjihao=====编码器:%s创建完成", aEncoder.getName()));
       // aEncoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    public void initVideoEncoder(int width, int height,int fps) {
        if (vEncoder != null) {
            return;
        }
        this.mWidth = width;
        this.mHeight = height;
        mFps = fps;
        videoQueue = new LinkedBlockingQueue<>();
        supportColorFormatList = new ArrayList<>();
        rotateYuvBuffer = new byte[this.mWidth * this.mHeight * 3 / 2];
        yuvBuffer = new byte[this.mWidth * this.mHeight * 3 / 2];
//        yuvBuffer = new byte[getYuvBuffer(mWidth, mHeight)];
//        rotateYuvBuffer = new byte[getYuvBuffer(mWidth, mHeight)];
        Log.d(TAG, "===zhongjihao===initVideoEncoder====width: "+mWidth+"  height: "+mHeight);
        vBufferInfo = new MediaCodec.BufferInfo();
        //选择系统用于编码H264的编码器信息
        MediaCodecInfo vCodecInfo = selectCodec(VIDEO_MIME_TYPE);
        if (vCodecInfo == null) {
            Log.e(TAG, "====zhongjihao=====Unable to find an appropriate codec for " + VIDEO_MIME_TYPE);
            return;
        }

        Log.d(TAG, "======zhongjihao====found video codec: " + vCodecInfo.getName());
        //根据MIME格式,选择颜色格式
        selectColorFormat(vCodecInfo, VIDEO_MIME_TYPE);

        for (int i = 0; i < supportColorFormatList.size(); i++) {
            if (isRecognizedFormat(supportColorFormatList.get(i))) {
                mColorFormat = supportColorFormatList.get(i);
                break;
            }
        }

        if(mColorFormat == 0){
            Log.e(TAG,
                    "==zhongjihao====couldn't find a good color format for " + vCodecInfo.getName()
                            + " / " + VIDEO_MIME_TYPE);
            return;
        }

        Log.d(TAG, "=====zhongjihao====found colorFormat: " + mColorFormat);
        //根据MIME创建MediaFormat
        // sensor出来的是逆时针旋转90度的数据，hal层没有做旋转导致APP显示和编码需要自己做顺时针旋转90,这样看到的图像才是正常的
        videoFormat = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE,
                this.mHeight, this.mWidth);
        int bitrate = (mWidth * mHeight * 3 / 2) * 8 * fps;
        //设置比特率,将编码比特率值设为bitrate
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        //设置帧率,将编码帧率设为Camera实际帧率mFps
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
        //设置颜色格式
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, mColorFormat);
        //设置关键帧的时间
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

        Log.d(TAG, "=====zhongjihao=====video==format: " + videoFormat.toString());

        try {
            //创建一个MediaCodec
            vEncoder = MediaCodec.createByCodecName(vCodecInfo.getName());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("===zhongjihao===初始化视频编码器失败", e);
        }
        Log.d(TAG, String.format("=====zhongjihao=====编码器:%s创建完成", vEncoder.getName()));
    }

    private void selectColorFormat(MediaCodecInfo codecInfo,
                                        String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo
                .getCapabilitiesForType(mimeType);
        supportColorFormatList.clear();
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            Log.d(TAG,
                    "==zhongjihao====selectColorFormat=====color format: " + colorFormat);
            supportColorFormatList.add(colorFormat);
        }
    }

    private boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            // these are the formats we know how to handle for this test
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar://对应Camera预览格式I420(YV21/YUV420P)
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar: //对应Camera预览格式NV12
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar://对应Camera预览格式NV21
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:{////对应Camera预览格式YV12
                return true;
            }
            default:
                return false;
        }
    }

    private MediaCodecInfo selectCodec(String mimeType) {
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
     * 开始
     */
    public void start() {
        startAudioEncode();
        startVideoEncode();
    }

    /**
     * 停止
     */
    public void stop() {
        stopAudioEncode();
        stopVideoEncode();
    }

    private void startVideoEncode(){
        if (vEncoder == null) {
            throw new RuntimeException("====zhongjihao=====请初始化视频编码器=====");
        }

        if (videoEncoderLoop) {
            throw new RuntimeException("====zhongjihao====视频编码必须先停止===");
        }

        videoEncoderThread = new Thread() {
            @Override
            public void run() {
                Log.d(TAG, "===zhongjihao=====Video 编码线程 启动...");
                presentationTimeUs = System.currentTimeMillis() * 1000;
                vEncoderEnd = false;
                vEncoder.configure(videoFormat, null, null,
                        MediaCodec.CONFIGURE_FLAG_ENCODE);
                vEncoder.start();
                while (videoEncoderLoop && !Thread.interrupted()) {
                    try {
                        byte[] data = videoQueue.take(); //待编码的数据
                        if (DEBUG) Log.d(TAG, "======zhongjihao====要编码的Video数据大小:" + data.length);
                        encodeVideoData(data);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "===zhongjihao==========编码(Video)数据 失败");
                        e.printStackTrace();
                        break;
                    }
                }

                if (vEncoder != null) {
                    //停止视频编码器
                    vEncoder.stop();
                    //释放视频编码器
                    vEncoder.release();
                    vEncoder = null;
                }
                videoQueue.clear();
                Log.d(TAG, "=====zhongjihao======Video 编码线程 退出...");
            }
        };
        videoEncoderLoop = true;
        videoEncoderThread.start();
    }

    private void stopVideoEncode() {
        Log.d(TAG, "======zhongjihao======stop video 编码...");
        vEncoderEnd = true;
    }

    private void startAudioEncode() {
        if (aEncoder == null) {
            throw new RuntimeException("====zhongjihao=====请初始化音频编码器=====");
        }

        if (audioEncoderLoop) {
            throw new RuntimeException("====zhongjihao====音频编码线程必须先停止===");
        }
        audioEncoderThread = new Thread() {
            @Override
            public void run() {
                Log.d(TAG, "===zhongjihao=====Audio 编码线程 启动...");
                presentationTimeUs = System.currentTimeMillis() * 1000;
                aEncoderEnd = false;
                aEncoder.configure(audioFormat, null, null,
                        MediaCodec.CONFIGURE_FLAG_ENCODE);
                aEncoder.start();
                while (audioEncoderLoop && !Thread.interrupted()) {
                    try {
                        byte[] data = audioQueue.take();
                        if (DEBUG) Log.d(TAG, "======zhongjihao====要编码的Audio数据大小:" + data.length);
                        encodeAudioData(data);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }

                if (aEncoder != null) {
                    //停止音频编码器
                    aEncoder.stop();
                    //释放音频编码器
                    aEncoder.release();
                    aEncoder = null;
                }
                audioQueue.clear();
                Log.d(TAG, "=====zhongjihao======Audio 编码线程 退出...");
            }
        };
        audioEncoderLoop = true;
        audioEncoderThread.start();
    }

    private void stopAudioEncode() {
        Log.d(TAG, "======zhongjihao======stop Audio 编码...");
        aEncoderEnd = true;
    }

    /**
     * 添加视频数据
     *
     * @param data
     */
    public void putVideoData(byte[] data) {
        try {
            if(videoQueue != null)
                videoQueue.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 添加音频数据
     *
     * @param data
     */
    public void putAudioData(byte[] data) {
        try {
            if(audioQueue != null)
                audioQueue.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

//    private int getYuvBuffer(int width, int height) {
//        int yStride = (int) Math.ceil(width / 16.0) * 16;
//        int uvStride = (int) Math.ceil( (yStride / 2) / 16.0) * 16;
//        int ySize = yStride * height;
//        int uvSize = uvStride * height / 2;
//        return ySize + uvSize * 2;
//    }

    private void encodeVideoData(byte[] input) {
        //input为Camera预览格式NV21数据
        if (mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
            //nv21格式转为nv12格式
            YuvEngineWrap.newInstance().Nv21ToNv12(input, yuvBuffer, mWidth, mHeight);
            YuvEngineWrap.newInstance().Nv12ClockWiseRotate90(yuvBuffer, mWidth, mHeight, rotateYuvBuffer, outWidth, outHeight);
        } else if (mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) {
            //用于NV21格式转换为I420(YUV420P)格式
            YuvEngineWrap.newInstance().Nv21ToI420(input, yuvBuffer, mWidth, mHeight);
            YuvEngineWrap.newInstance().I420ClockWiseRotate90(yuvBuffer, mWidth, mHeight, rotateYuvBuffer, outWidth, outHeight);
        } else if (mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar) {
            System.arraycopy(input, 0, yuvBuffer, 0, mWidth * mHeight * 3 / 2);
            YuvEngineWrap.newInstance().Nv21ClockWiseRotate90(yuvBuffer, mWidth, mHeight, rotateYuvBuffer, outWidth, outHeight);
        }else if (mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar) {
            //用于NV21格式转换为YV12格式
            YuvEngineWrap.newInstance().Nv21ToYv12(input, yuvBuffer, mWidth, mHeight);
            YuvEngineWrap.newInstance().Yv12ClockWiseRotate90(yuvBuffer, mWidth, mHeight, rotateYuvBuffer, outWidth, outHeight);
        }

        try {
            //拿到输入缓冲区,用于传送数据进行编码
            ByteBuffer[] inputBuffers = vEncoder.getInputBuffers();
            //得到当前有效的输入缓冲区的索引
            int inputBufferIndex = vEncoder.dequeueInputBuffer(TIMEOUT_USEC);
            Log.d(TAG, "==1====zhongjihao=====Video===inputBufferIndex: " + inputBufferIndex+"  yuvLen: "+rotateYuvBuffer.length);
            if (inputBufferIndex >= 0) { //输入缓冲区有效
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                Log.d(TAG, "===2===zhongjihao=====Video===inputBufferIndex: " + inputBufferIndex+"  capacity: "+inputBuffer.capacity());
                //往输入缓冲区写入数据
                inputBuffer.put(rotateYuvBuffer);
                Log.d(TAG, "===3===zhongjihao=====Video===inputBufferIndex: " + inputBufferIndex+"  capacity: "+inputBuffer.capacity()+"  limit: "+inputBuffer.limit());

                //计算pts，这个值是一定要设置的
               // long pts = new Date().getTime() * 1000 - presentationTimeUs;
                long pts = System.currentTimeMillis() * 1000 -  presentationTimeUs;
                if (vEncoderEnd) {
                    //结束时，发送结束标志，在编码完成后结束
                    Log.d(TAG, "=====zhongjihao===send Video Encoder BUFFER_FLAG_END_OF_STREAM====");
                    vEncoder.queueInputBuffer(inputBufferIndex, 0, rotateYuvBuffer.length,
                            pts, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                } else {
                    //将缓冲区入队
                    Log.d(TAG, "=====zhongjihao===Video====inputBufferIndex: "+inputBufferIndex+"  pts: "+pts);
                    vEncoder.queueInputBuffer(inputBufferIndex, 0, rotateYuvBuffer.length,
                            pts, 0);
                }
            }

            //拿到输出缓冲区,用于取到编码后的数据
            ByteBuffer[] outputBuffers = vEncoder.getOutputBuffers();
            //拿到输出缓冲区的索引
            int outputBufferIndex = vEncoder.dequeueOutputBuffer(vBufferInfo, TIMEOUT_USEC);
            Log.d(TAG, "=====zhongjihao===Video====outputBufferIndex: "+outputBufferIndex);
            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuffers = vEncoder.getOutputBuffers();
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                Log.d(TAG, "=====zhongjihao====Video===INFO_OUTPUT_FORMAT_CHANGED===");
                MediaFormat newFormat = vEncoder.getOutputFormat();
                if (null != mCallback && !vEncoderEnd) {
                    Log.d(TAG,"======zhongjihao======添加视轨 INFO_OUTPUT_FORMAT_CHANGED " + newFormat.toString());
                    mCallback.outMediaFormat(AVmediaMuxer.TRACK_VIDEO, newFormat);
                }
            }
            while (outputBufferIndex >= 0) {
                //数据已经编码成H264格式
                //outputBuffer保存的就是H264数据
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                if (outputBuffer == null) {
                    throw new RuntimeException("encoderOutputBuffer " + outputBufferIndex +
                            " was null");
                }

                if ((vBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    Log.d(TAG, "======zhongjihao====Video====ignoring BUFFER_FLAG_CODEC_CONFIG");
                    vBufferInfo.size = 0;
                }

                if (vBufferInfo.size != 0) {
//                    byte[] outData = new byte[vBufferInfo.size];
//                    outputBuffer.get(outData);

                    if (null != mCallback && !vEncoderEnd) {
                        mCallback.outputVideoFrame(AVmediaMuxer.TRACK_VIDEO,outputBuffer, vBufferInfo);
                    }
                }
                //释放资源
                vEncoder.releaseOutputBuffer(outputBufferIndex, false);
                //拿到输出缓冲区的索引
                outputBufferIndex = vEncoder.dequeueOutputBuffer(vBufferInfo, 0);
                //编码结束的标志
                if ((vBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "=====zhongjihao=====Recv Video Encoder===BUFFER_FLAG_END_OF_STREAM=====" );
                    videoEncoderLoop = false;
                    videoEncoderThread.interrupt();
                    return;
                }
            }
        } catch (Exception t) {
            Log.e(TAG, "====zhongjihao=====encodeVideoData=====error: " + t.toString());
        }
    }

    private void encodeAudioData(byte[] input){
        try {
            //拿到输入缓冲区,用于传送数据进行编码
            ByteBuffer[] inputBuffers = aEncoder.getInputBuffers();
            //得到当前有效的输入缓冲区的索引
            int inputBufferIndex = aEncoder.dequeueInputBuffer(TIMEOUT_USEC);
            if (inputBufferIndex >= 0) { //输入缓冲区有效
                if (DEBUG) Log.d(TAG, "======zhongjihao====Audio===inputBufferIndex: " + inputBufferIndex);
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                //往输入缓冲区写入数据
                inputBuffer.put(input);

                //计算pts，这个值是一定要设置的
                long pts = System.currentTimeMillis() * 1000 - presentationTimeUs;
                if (aEncoderEnd) {
                    //结束时，发送结束标志，在编码完成后结束
                    Log.d(TAG, "=====zhongjihao===send Audio Encoder BUFFER_FLAG_END_OF_STREAM====");
                    aEncoder.queueInputBuffer(inputBufferIndex, 0, input.length,
                            pts, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                } else {
                    //将缓冲区入队
                    aEncoder.queueInputBuffer(inputBufferIndex, 0, input.length,
                            pts, 0);
                }
            }

            //拿到输出缓冲区,用于取到编码后的数据
            ByteBuffer[] outputBuffers = aEncoder.getOutputBuffers();
            //拿到输出缓冲区的索引
            int outputBufferIndex = aEncoder.dequeueOutputBuffer(aBufferInfo, TIMEOUT_USEC);
            Log.d(TAG, "=====zhongjihao====Audio======outputBufferIndex: "+outputBufferIndex);
            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){
                outputBuffers = aEncoder.getOutputBuffers();
            }else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                Log.d(TAG, "=====zhongjihao======Audio===INFO_OUTPUT_FORMAT_CHANGED===");
                //加入音轨的时刻,一定要等编码器设置编码格式完成后，再将它加入到混合器中，
                // 编码器编码格式设置完成的标志是dequeueOutputBuffer得到返回值为MediaCodec.INFO_OUTPUT_FORMAT_CHANGED
                final MediaFormat newformat = aEncoder.getOutputFormat(); // API >= 16
                if (null != mCallback && !aEncoderEnd) {
                    Log.d(TAG,"======zhongjihao======添加音轨 INFO_OUTPUT_FORMAT_CHANGED " + newformat.toString());
                    mCallback.outMediaFormat(AVmediaMuxer.TRACK_AUDIO, newformat);
                }
            }
            while (outputBufferIndex >= 0) {
                //数据已经编码成AAC格式
                //outputBuffer保存的就是AAC数据
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                if (outputBuffer == null) {
                    throw new RuntimeException("encoderOutputBuffer " + outputBufferIndex +
                            " was null");
                }

                if ((aBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // You shoud set output format to muxer here when you target Android4.3 or less
                    // but MediaCodec#getOutputFormat can not call here(because INFO_OUTPUT_FORMAT_CHANGED don't come yet)
                    // therefor we should expand and prepare output format from buffer data.
                    // This sample is for API>=18(>=Android 4.3), just ignore this flag here
                    Log.d(TAG, "======zhongjihao====Audio====drain:BUFFER_FLAG_CODEC_CONFIG===");
                    aBufferInfo.size = 0;
                }

                if (aBufferInfo.size != 0) {
                    // byte[] outData = new byte[mBufferInfo.size];
                    // outputBuffer.get(outData);
                    if (null != mCallback && !aEncoderEnd) {
                        mCallback.outputAudioFrame(AVmediaMuxer.TRACK_AUDIO,outputBuffer, aBufferInfo);
                    }
                }
                //释放资源
                aEncoder.releaseOutputBuffer(outputBufferIndex, false);
                //拿到输出缓冲区的索引
                outputBufferIndex = aEncoder.dequeueOutputBuffer(aBufferInfo, 0);
                //编码结束的标志
                if ((aBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.e(TAG, "=====zhongjihao=====Recv Audio Encoder===BUFFER_FLAG_END_OF_STREAM=====");
                    audioEncoderLoop = false;
                    audioEncoderThread.interrupt();
                    return;
                }
            }
        } catch (Exception t) {
            Log.e(TAG, "=====zhongjihao=====encodeAudioData=====error: " + t.toString());
        }
    }
}
