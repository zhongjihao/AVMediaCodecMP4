package com.example.apadmin.cameraphoto;

/**
 * Created by zhongjihao on 18-1-31.
 */

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.support.annotation.IntDef;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Vector;

public class MediaMuxerRunnable extends Thread{
    public static final int TRACK_VIDEO = 0;
    public static final int TRACK_AUDIO = 1;
    public static boolean DEBUG = true;
    private static MediaMuxerRunnable mediaMuxerThread;
    private final Object lock = new Object();
    private MediaMuxer mediaMuxer = null;
    //缓冲传输过来的数据
    private Vector<MuxerData> muxerDatas;
    private volatile boolean isExit = false;
    private int videoTrackIndex = -1;
    private int audioTrackIndex = -1;
    private volatile boolean isVideoAdd = false;
    private volatile boolean isAudioAdd = false;
    private AudioRunnable audioThread;
    private VideoRunnable videoThread;
    private volatile boolean isMediaMuxerStart = false;
    private MediaFormat videoMediaFormat = null;
    private MediaFormat audioMediaFormat = null;

    private MediaMuxerRunnable() {
    }

    public static void startMuxer() {
        if (mediaMuxerThread == null) {
            synchronized (MediaMuxerRunnable.class) {
                if (mediaMuxerThread == null) {
                    mediaMuxerThread = new MediaMuxerRunnable();
                    mediaMuxerThread.start();
                }
            }
        }
    }

    public static void stopMuxer() {
        if (mediaMuxerThread != null) {
            mediaMuxerThread.exit();
            try {
                if (DEBUG) Log.e("MediaMuxerRunnable", "====zhongjihao===等待MediaMuxer混合器线程结束开始");
                mediaMuxerThread.join();
                if (DEBUG) Log.e("MediaMuxerRunnable", "====zhongjihao===等待MediaMuxer混合器线程结束完成");
            } catch (InterruptedException e) {

            }
            if(mediaMuxerThread.muxerDatas != null){
                mediaMuxerThread.muxerDatas.clear();
                mediaMuxerThread.muxerDatas = null;
            }
            mediaMuxerThread = null;
        }
    }

    public static void addVideoFrameData(byte[] data) {
        if (mediaMuxerThread != null) {
            mediaMuxerThread.addVideoData(data);
        }
    }

    private void initMuxer() {
        muxerDatas = new Vector<MuxerData>();

        audioThread = new AudioRunnable(new WeakReference<MediaMuxerRunnable>(this));
        videoThread = new VideoRunnable(CameraWrapper.IMAGE_WIDTH, CameraWrapper.IMAGE_HEIGHT, new WeakReference<MediaMuxerRunnable>(this));

        audioThread.start();
        videoThread.start();

        restartMediaMuxer();
    }

    private void addVideoData(byte[] data) {
        if (videoThread != null) {
            videoThread.add(data);
        }
    }

    private void restartMediaMuxer() {
        try {
            resetMediaMuxer();
            if (videoMediaFormat != null) {
                videoTrackIndex = mediaMuxer.addTrack(videoMediaFormat);
                isVideoAdd = true;
            }
            if (audioMediaFormat != null) {
                audioTrackIndex = mediaMuxer.addTrack(audioMediaFormat);
                isAudioAdd = true;
            }
            requestStart();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopMediaMuxer() {
        if (mediaMuxer != null) {
            try {
                mediaMuxer.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                mediaMuxer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            isAudioAdd = false;
            isVideoAdd = false;
            isMediaMuxerStart = false;
            mediaMuxer = null;
        }
    }

    private void resetMediaMuxer() throws Exception {
        stopMediaMuxer();
        String filePath = Environment
                .getExternalStorageDirectory()
                + "/"+"zhongjihao/out.mp4";
        mediaMuxer = new MediaMuxer(filePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        Log.d("MediaMuxerRunnable", "===zhongjihao====创建混合器,保存至:" + filePath);
    }

    public synchronized void setMediaFormat(@TrackIndex int index, MediaFormat mediaFormat) {
        if (mediaMuxer == null) {
            return;
        }
        if (DEBUG) Log.e("MediaMuxerRunnable", "====zhongjihao===setMediaFormat=======index: "+index);
        if (index == TRACK_VIDEO) {
            if (videoMediaFormat == null) {
                videoMediaFormat = mediaFormat;
                videoTrackIndex = mediaMuxer.addTrack(mediaFormat);
                isVideoAdd = true;
                if (DEBUG) Log.e("MediaMuxerRunnable", "====zhongjihao===setMediaFormat=======videoTrackIndex: "+videoTrackIndex);
            }
        } else {
            if (audioMediaFormat == null) {
                audioMediaFormat = mediaFormat;
                audioTrackIndex = mediaMuxer.addTrack(mediaFormat);
                isAudioAdd = true;
                if (DEBUG) Log.e("MediaMuxerRunnable", "====zhongjihao===setMediaFormat=======audioTrackIndex: "+audioTrackIndex);
            }
        }

        requestStart();
    }

    private void exit() {
        if (videoThread != null) {
            videoThread.exit();
            try {
                if (DEBUG) Log.e("MediaMuxerRunnable", "====zhongjihao===等待Video视频编码线程结束开始");
                videoThread.join();
                videoThread = null;
                if (DEBUG) Log.e("MediaMuxerRunnable", "====zhongjihao===等待Video视频编码线程结束完成");
            } catch (InterruptedException e) {

            }
        }
        if (audioThread != null) {
            audioThread.exit();
            try {
                if (DEBUG) Log.e("MediaMuxerRunnable", "====zhongjihao===等待Audio音频编码线程结束开始");
                audioThread.join();
                audioThread = null;
                if (DEBUG) Log.e("MediaMuxerRunnable", "====zhongjihao===等待Audio音频编码线程结束完成");
            } catch (InterruptedException e) {

            }
        }

        isExit = true;
        synchronized (lock) {
            lock.notify();
        }
    }

    public synchronized void addMuxerData(MuxerData data) {
        if (muxerDatas == null) {
            return;
        }
        muxerDatas.add(data);
        synchronized (lock) {
            lock.notify();
        }
    }

    @Override
    public void run() {
        initMuxer();
        while (!isExit) {
            if (isMediaMuxerStart) {
                //混合器开启后
                if (muxerDatas.isEmpty()) {
                    synchronized (lock) {
                        try {
                            if (DEBUG) Log.e("MediaMuxerRunnable", "====zhongjihao===等待混合数据...");
                            lock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                        MuxerData data = muxerDatas.remove(0);
                        int track;
                        if (data.trackIndex == TRACK_VIDEO) {
                            track = videoTrackIndex;
                        } else {
                            track = audioTrackIndex;
                        }
                        if (DEBUG) Log.e("MediaMuxerRunnable", "====zhongjihao====trackIndex: "+data.trackIndex+"   track: "+track+"    写入混合数据 " + data.bufferInfo.size);
                        try {
                            //添加数据
                            mediaMuxer.writeSampleData(track, data.byteBuf, data.bufferInfo);
                        } catch (Exception e) {
                            Log.e("MediaMuxerRunnable", "===zhongjihao====写入混合数据失败!" + e.toString());
                            restartMediaMuxer();
                        }
                }
            } else {
                //混合器未开启
                synchronized (lock) {
                    try {
                        if (DEBUG) Log.e("MediaMuxerRunnable", "====zhongjihao=====混合器等待开始...");
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        stopMediaMuxer();
        if (DEBUG) Log.e("MediaMuxerRunnable", "=====zhongjihao====混合器退出...");
    }

    private void requestStart() {
        synchronized (lock) {
            if (isMuxerStart()) {
                mediaMuxer.start();
                isMediaMuxerStart = true;
                if (DEBUG) Log.e("MediaMuxerRunnable", "===zhongjihao====requestStart 启动混合器 开始等待数据输入...");
                lock.notify();
            }
        }
    }

    private boolean isMuxerStart() {
        return isAudioAdd && isVideoAdd;
    }

    @IntDef({TRACK_VIDEO, TRACK_AUDIO})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TrackIndex {
    }

    /**
     * 封装需要传输的数据类型
     */
    public static class MuxerData {
        int trackIndex;
        ByteBuffer byteBuf;
        MediaCodec.BufferInfo bufferInfo;

        public MuxerData(@TrackIndex int trackIndex, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
            this.trackIndex = trackIndex;
            this.byteBuf = byteBuf;
            this.bufferInfo = bufferInfo;
        }
    }

}
