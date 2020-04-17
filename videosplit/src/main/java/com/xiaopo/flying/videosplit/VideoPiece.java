package com.xiaopo.flying.videosplit;

import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

import com.xiaopo.flying.puzzlekit.Area;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import static com.xiaopo.flying.glkit.gl.ShaderProgram.MATRIX_SIZE;

/**
 * @author wupanjie
 */
class VideoPiece extends Thread {
    private static final String TAG = "VideoPiece";
    private static final long TIMEOUT_US = 10000;
    private static final boolean VERBOSE = false;

    private final String path;
    private final float[] positionMatrix = new float[MATRIX_SIZE];
    private final float[] textureMatrix = new float[MATRIX_SIZE];

    private SurfaceTexture outputTexture;
    private Surface surface;
    private int textureId;
    private RectF displayArea = new RectF();
    private RectF textureArea = new RectF(0, 0, 1, 1);

    private int videoWidth;
    private int videoHeight;
    private long videoDuration;

    private long presentationTime;
    private boolean isMediaEOS;
    private CyclicBarrier barrier;

    VideoPiece(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    void configOutput(int textureId) {
        this.textureId = textureId;
        outputTexture = new SurfaceTexture(textureId);
        surface = new Surface(outputTexture);
        parseVideoInfo();

        if (VERBOSE) {
            Log.d(TAG, "configOutput: video path is " + path + ",video duration is " + videoDuration);
        }
    }

    public long getPresentationTimeUs() {
        return presentationTime;
    }

    SurfaceTexture getOutputTexture() {
        return outputTexture;
    }

    public long getVideoDuration() {
        return videoDuration;
    }

    void release() {
        outputTexture.release();
        outputTexture = null;
        interrupt();
    }

    void play() {
        start();
    }

    void setDisplayArea(Area area) {
        final RectF rect = area.getAreaRect();
        setDisplayArea(rect.left, rect.top, rect.right, rect.bottom);
    }

    private void setDisplayArea(float left, float top, float right, float bottom) {
        displayArea.set(left, top, right, bottom);

        float scale;

        float displayWidth = displayArea.width();
        float displayHeight = displayArea.height();

        if (videoWidth * displayHeight > displayWidth * videoHeight) {
            scale = (displayHeight) / videoHeight;
        } else {
            scale = (displayWidth) / videoWidth;
        }
        float scaleWidth = videoWidth * scale;
        float scaleHeight = videoHeight * scale;

        float offsetW = (scaleWidth - displayWidth) / 2;
        float offsetH = (scaleHeight - displayHeight) / 2;

        textureArea.set(offsetW, offsetH, scaleWidth - offsetW, scaleHeight - offsetH);
        normalize(textureArea, scaleWidth, scaleHeight);
    }

    private void normalize(RectF textureArea, float scaleWidth, float scaleHeight) {
        textureArea.left = textureArea.left / scaleWidth;
        textureArea.top = textureArea.top / scaleHeight;
        textureArea.right = textureArea.right / scaleWidth;
        textureArea.bottom = textureArea.bottom / scaleHeight;
    }

    void setTexture(final int textureHandle, final int textureMatrixHandle) {
        outputTexture.getTransformMatrix(textureMatrix);

        textureMatrix[0] = textureArea.width();
        textureMatrix[5] = textureArea.height();
        textureMatrix[12] = textureArea.left;
        textureMatrix[13] = textureArea.top;

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glUniform1i(textureHandle, 0);

        GLES20.glUniformMatrix4fv(textureMatrixHandle, 1, false, textureMatrix, 0);
    }

    void setMatrix(int matrixHandle, float[] viewMatrix, float[] projectionMatrix) {
        final float x = displayArea.left;
        final float y = displayArea.top;
        final float width = displayArea.width();
        final float height = displayArea.height();
        Matrix.translateM(positionMatrix, 0, viewMatrix, 0, x, y, 0f);
        Matrix.scaleM(positionMatrix, 0, width, height, 1f);
        Matrix.multiplyMM(positionMatrix, 0, projectionMatrix, 0, positionMatrix, 0);
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, positionMatrix, 0);
    }

    public boolean isFinished() {
        return isMediaEOS;
    }

    public void setBarrier(CyclicBarrier barrier) {
        this.barrier = barrier;
    }

    @Override
    public void run() {
        MediaExtractor videoExtractor = new MediaExtractor();
        MediaCodec videoCodec = null;
        try {
            videoExtractor.setDataSource(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int videoTrackIndex;
        //获取视频所在轨道
        videoTrackIndex = getMediaTrackIndex(videoExtractor);
        if (videoTrackIndex >= 0) {
            MediaFormat mediaFormat = videoExtractor.getTrackFormat(videoTrackIndex);
            videoExtractor.selectTrack(videoTrackIndex);
            try {
                videoCodec = MediaCodec.createDecoderByType(mediaFormat.getString(MediaFormat.KEY_MIME));
                videoCodec.configure(mediaFormat, surface, null, 0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (videoCodec == null) {
            if (VERBOSE) {
                Log.d(TAG, "MediaCodec null");
            }
            return;
        }
        videoCodec.start();

        MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
        ByteBuffer[] inputBuffers = videoCodec.getInputBuffers();
        boolean isVideoEOS = false;

        long startMs = System.currentTimeMillis();
        // 等待同时开始
        try {
            if (barrier != null) {
                barrier.await();
            }
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
        while (!Thread.interrupted()) {
            //将资源传递到解码器
            try {
                if (!isVideoEOS) {

                    isVideoEOS = putBufferToCoder(videoExtractor, videoCodec, inputBuffers);
                    isMediaEOS = isVideoEOS;
                }
                int outputBufferIndex = videoCodec.dequeueOutputBuffer(videoBufferInfo, TIMEOUT_US);
                switch (outputBufferIndex) {
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        if (VERBOSE) {
                            Log.d(TAG, "format changed");
                        }
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        if (VERBOSE) {
                            Log.d(TAG, "超时 : " + path);
                        }
                        break;
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        if (VERBOSE) {
                            Log.d(TAG, "output buffers changed");
                        }
                        break;
                    default:
                        //延时操作
                        //如果缓冲区里的可展示时间>当前视频播放的进度，就休眠一下
                        sleepRender(videoBufferInfo, startMs);
                        //渲染
                        presentationTime = videoBufferInfo.presentationTimeUs;
                        videoCodec.releaseOutputBuffer(outputBufferIndex, true);
                        break;
                }
            } catch (Exception e) {
                // no-ops
            }
            if ((videoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                if (VERBOSE) {
                    Log.d(TAG, "buffer stream end");
                }
                break;
            }
        }//end while
        videoCodec.stop();
        videoCodec.release();
        videoExtractor.release();
    }

    private int getMediaTrackIndex(MediaExtractor videoExtractor) {
        int trackIndex = -1;
        for (int i = 0; i < videoExtractor.getTrackCount(); i++) {
            MediaFormat mediaFormat = videoExtractor.getTrackFormat(i);
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                trackIndex = i;
                break;
            }
        }
        return trackIndex;
    }

    private void sleepRender(MediaCodec.BufferInfo bufferInfo, long startMs) {
        while (bufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    private boolean putBufferToCoder(MediaExtractor extractor, MediaCodec decoder,
                                     ByteBuffer[] inputBuffers) {
        boolean isMediaEOS = false;
        int inputBufferIndex = decoder.dequeueInputBuffer(TIMEOUT_US);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            int sampleSize = extractor.readSampleData(inputBuffer, 0);
            if (sampleSize < 0) {
                decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                isMediaEOS = true;
                if (VERBOSE) {
                    Log.d(TAG, "media eos");
                }
            } else {
                decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                extractor.advance();
            }
        }
        return isMediaEOS;
    }

    private void parseVideoInfo() {
        MediaExtractor videoExtractor = new MediaExtractor();
        try {
            videoExtractor.setDataSource(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int videoTrackIndex;
        //获取视频所在轨道
        videoTrackIndex = getMediaTrackIndex(videoExtractor);
        if (videoTrackIndex >= 0) {
            MediaFormat mediaFormat = videoExtractor.getTrackFormat(videoTrackIndex);
            videoWidth = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
            videoHeight = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
            videoDuration = mediaFormat.getLong(MediaFormat.KEY_DURATION) / 1000;
        }
    }
}
