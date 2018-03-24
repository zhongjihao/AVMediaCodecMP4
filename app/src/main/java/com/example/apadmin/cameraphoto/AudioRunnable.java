package com.example.apadmin.cameraphoto;

/**
 * Created by zhongjihao on 18-1-31.
 */

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

public class AudioRunnable extends Thread {
    public static final boolean DEBUG = true;
    public static final String TAG = "AudioRunnable";
    public static int SAMPLES_PER_FRAME = 1024;    // AAC, frameBytes/frame/channel
    protected static final int TIMEOUT_USEC = 10000;    // 10[msec]
    private static final String MIME_TYPE = "audio/mp4a-latm";
    private static final int SAMPLE_RATE = 44100;    // 44.1[KHz] is only setting guaranteed to be available on all devices.
    private static final int BIT_RATE = 64000;
    private MediaCodec mMediaCodec;                // API >= 16(Android4.1.2)
    private volatile boolean isExit = false;
    private WeakReference<MediaMuxerRunnable> mediaMuxerRunnable;
    private AudioRecord audioRecord;
    private MediaCodec.BufferInfo mBufferInfo;        // API >= 16(Android4.1.2)
    private MediaCodecInfo audioCodecInfo;
    private volatile boolean mStartCodecFlag = false;
    private int aChannelCount;
    private int aSampleRate;
    private ByteBuffer audioBuf = null;

    /**
     * previous presentationTimeUs for writing
     */
    private long prevOutputPTSUs = 0;
    private MediaFormat audioFormat = null;

    public AudioRunnable(WeakReference<MediaMuxerRunnable> mediaMuxerRunnable) {
        this.mediaMuxerRunnable = mediaMuxerRunnable;
        mBufferInfo = new MediaCodec.BufferInfo();
        prepare();
    }

    private static final MediaCodecInfo selectAudioCodec(final String mimeType) {
        if (DEBUG) Log.d(TAG, "======zhongjihao====selectAudioCodec:");

        MediaCodecInfo result = null;
        // get the list of available codecs
        final int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {    // skipp decoder
                continue;
            }
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (DEBUG)
                    Log.d(TAG, "====zhongjihao====supportedType:" + codecInfo.getName() + ",MIME=" + types[j]);
                if (types[j].equalsIgnoreCase(mimeType)) {
                    if (result == null) {
                        result = codecInfo;
                        return result;
                    }
                }
            }
        }
        return result;
    }

    private void prepare() {
        audioCodecInfo = selectAudioCodec(MIME_TYPE);
        if (audioCodecInfo == null) {
            if (DEBUG) Log.e(TAG, "=====zhongjihao====Unable to find an appropriate codec for " + MIME_TYPE);
            return;
        }
        if (DEBUG)  Log.d(TAG, "===zhongjihao===selected codec: " + audioCodecInfo.getName());

        audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, 1);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO);//CHANNEL_IN_STEREO 立体声
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);
        audioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, SAMPLE_RATE);
        if (DEBUG)  Log.d(TAG, "====zhongjihao=========format: " + audioFormat.toString());
    }

    private void startMediaCodec() throws IOException {
        if (mMediaCodec != null) {
            return;
        }

        try {
            mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("===zhongjihao===初始化音频编码器失败", e);
        }
        Log.d(TAG, String.format("=====zhongjihao=====编码器:%s创建完成", mMediaCodec.getName()));

        prepareAudioRecord();
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, aChannelCount);
        audioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, aSampleRate);
        mMediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();
        if (DEBUG) Log.d(TAG, "===zhongjihao=====编码器 prepare finishing");

        mStartCodecFlag = true;
    }

    private void stopMediaCodec() {
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
        mStartCodecFlag = false;
        if (DEBUG) Log.e(TAG, "====zhongjihao======stop audio 录制 和 编码...");
    }

    private void prepareAudioRecord() {
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        //音频采样率，44100是目前的标准，但是某些设备仍然支持22050,16000,11025
        int[] sampleRates = {44100, 22050, 16000, 11025};
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        try {
            for (int sampleRate : sampleRates) {
                //编码制式PCM
                int audioForamt = AudioFormat.ENCODING_PCM_16BIT;
                // stereo 立体声,mono单声道
                int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_STEREO;

                final int min_buffer_size = 2 * AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioForamt);
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT, min_buffer_size);
                if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED){
                    audioRecord = null;
                    Log.e(TAG, "====zhongjihao===initialized the mic failed");
                    continue;
                }

                aSampleRate = sampleRate;
                aChannelCount = channelConfig == AudioFormat.CHANNEL_CONFIGURATION_STEREO ? 2 : 1;
                //ByteBuffer分配内存的最大值为4096
                SAMPLES_PER_FRAME =  Math.min(4096, min_buffer_size);
                audioBuf = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME);
                Log.d(TAG, "====zhongjihao===aSampleRate: "+aSampleRate+"   aChannelCount: "+aChannelCount+"   min_buffer_size: "+min_buffer_size);
                break;
            }
        } catch (final Exception e) {
            if (DEBUG) Log.e(TAG, "AudioThread#run", e);
        }

        if (audioRecord != null) {
            audioRecord.startRecording();
        }
    }

    public void exit() {
        isExit = true;
    }

    @Override
    public void run() {
        int readBytes;
        while (!isExit) {
            /*启动或者重启*/
            if (!mStartCodecFlag) {
                stopMediaCodec();
                try {
                    if (DEBUG) Log.e(TAG, "=====zhongjihao=======audio -- startMediaCodec...");
                    startMediaCodec();
                } catch (IOException e) {
                    e.printStackTrace();
                    mStartCodecFlag = false;
                }
            } else if (audioRecord != null) {
                audioBuf.clear();
                //读取音频数据到buf
                readBytes = audioRecord.read(audioBuf, SAMPLES_PER_FRAME);
                if (readBytes > 0) {
                    // set audio data to encoder
                    audioBuf.position(readBytes);
                    audioBuf.flip();
                    if(DEBUG) Log.e(TAG, "======zhongjihao========编码音频数据:" + readBytes);
                    try {
                        //开始编码
                        encode(audioBuf, readBytes, getPTSUs());
                    } catch (Exception e) {
                        if (DEBUG) Log.e(TAG, "=====zhongjihao========编码音频(Audio)数据 失败");
                        e.printStackTrace();
                    }
                }
            }
        }
        stopMediaCodec();
        if (DEBUG) Log.e(TAG, "=====zhongjihao======Audio 编码线程 退出...");
    }

    private void encode(final ByteBuffer buffer, final int length, final long presentationTimeUs) {
        if (isExit) return;
        final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        final int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
        if (DEBUG) Log.d(TAG, "=====zhongjihao====inputBufferIndex-->" + inputBufferIndex);
        /*向编码器输入数据*/
        if (inputBufferIndex >= 0) {
            final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            if (buffer != null) {
                inputBuffer.put(buffer);
            }

            if (!mStartCodecFlag) {
                // send EOS
                //结束时，发送结束标志，在编码完成后结束
                if (DEBUG) Log.d(TAG, "=====zhongjihao======send BUFFER_FLAG_END_OF_STREAM");
                mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length,
                        presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            } else {
                //将缓冲区入队
                if (DEBUG) Log.d(TAG, "=====zhongjihao====length-->" + length);
                mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length,
                        presentationTimeUs, 0);
            }
        } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            // wait for MediaCodec encoder is ready to encode
            // nothing to do here because MediaCodec#dequeueInputBuffer(TIMEOUT_USEC)
            // will wait for maximum TIMEOUT_USEC(10msec) on each call
        }

        /*获取解码后的数据*/
        final MediaMuxerRunnable muxer = mediaMuxerRunnable.get();
        if (muxer == null) {
            if (DEBUG) Log.w(TAG, "=====zhongjihao======MediaMuxerRunnable is unexpectedly null");
            return;
        }
        ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();
        int encoderStatus;

        do {
            encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (DEBUG) Log.d(TAG, "=====zhongjihao====outputBufferIndex-->" + encoderStatus);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                encoderOutputBuffers = mMediaCodec.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                //加入音轨的时刻,一定要等编码器设置编码格式完成后，再将它加入到混合器中，
                // 编码器编码格式设置完成的标志是dequeueOutputBuffer得到返回值为MediaCodec.INFO_OUTPUT_FORMAT_CHANGED
                final MediaFormat format = mMediaCodec.getOutputFormat(); // API >= 16
                if (muxer != null) {
                    if (DEBUG)
                        Log.e(TAG, "======zhongjihao======添加音轨 INFO_OUTPUT_FORMAT_CHANGED " + format.toString());
                    muxer.setMediaFormat(MediaMuxerRunnable.TRACK_AUDIO, format);
                }
            } else if (encoderStatus < 0) {
            } else {
                final ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // You shoud set output format to muxer here when you target Android4.3 or less
                    // but MediaCodec#getOutputFormat can not call here(because INFO_OUTPUT_FORMAT_CHANGED don't come yet)
                    // therefor we should expand and prepare output format from buffer data.
                    // This sample is for API>=18(>=Android 4.3), just ignore this flag here
                    if (DEBUG) if (DEBUG) Log.d(TAG, "======zhongjihao======drain:BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0 && muxer != null) {
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                    mBufferInfo.presentationTimeUs = getPTSUs();
                    if(DEBUG) Log.e(TAG, "=====zhongjihao=====Add Audio Data 添加音频数据 size: " + mBufferInfo.size+"   offset: "+ mBufferInfo.offset);
                    //当保证视轨和音轨都添加完成之后,才可以添加数据到混合器
                    muxer.addMuxerData(new MediaMuxerRunnable.MuxerData(
                            MediaMuxerRunnable.TRACK_AUDIO, encodedData, mBufferInfo));
                    prevOutputPTSUs = mBufferInfo.presentationTimeUs;
                }
                // return buffer to encoder
                mMediaCodec.releaseOutputBuffer(encoderStatus, false);

                //编码结束的标志
                if((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) !=0 ){
                    return;
                }
            }
        } while (encoderStatus >= 0);
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
//        if (result < prevOutputPTSUs)
//            result =  (prevOutputPTSUs - result) + result;
        if (result < prevOutputPTSUs)
            result =  (prevOutputPTSUs - result) + result;
        return result;
    }
}
