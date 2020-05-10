package com.xiaopo.flying.videosplit;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.xiaopo.flying.glkit.gl.EglCore;
import com.xiaopo.flying.glkit.gl.ShaderProgram;
import com.xiaopo.flying.glkit.gl.WindowSurface;
import com.xiaopo.flying.videosplit.record.TextureEncoder;
import com.xiaopo.flying.videosplit.record.TextureRecorder;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * @author wupanjie
 */
public class SpiltVideoPlayer extends Thread implements SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = SpiltVideoPlayer.class.getSimpleName();
    private static final String THREAD_NAME = "SpiltVideoPlayer";
    private static final int BIT_RATE = 4000000;

    private final Object lock = new Object();
    private int surfaceWidth;
    private int surfaceHeight;

    private SurfaceTexture previewSurfaceTexture;

    private EglCore eglCore;
    private WindowSurface previewWindowSurface;

    private RenderHandler handler;
    private OnRendererReadyListener onRendererReadyListener;

    private SplitShaderProgram shaderProgram;

    private WindowSurface inputWindowSurface;
    private TextureEncoder videoEncoder;

    public SpiltVideoPlayer(SurfaceTexture texture, int width, int height, SplitShaderProgram shaderProgram) {
        this.setName(THREAD_NAME);
        this.previewSurfaceTexture = texture;
        this.surfaceWidth = width;
        this.surfaceHeight = height;
        this.shaderProgram = shaderProgram;
    }

    private void initialize() {
        setViewport(surfaceWidth, surfaceHeight);
    }

    private void initGL() {
        eglCore = new EglCore(null, EglCore.FLAG_RECORDABLE | EglCore.FLAG_TRY_GLES3);

        //create preview surface
        previewWindowSurface = new WindowSurface(eglCore, previewSurfaceTexture);
        previewWindowSurface.makeCurrent();

        initGLComponents();
    }

    private void initGLComponents() {
        shaderProgram.prepare();
        shaderProgram.setOnFrameAvailableListener(this);

        onSetupComplete();
    }

    private void deinitGL() {
        shaderProgram.release();
        previewWindowSurface.release();
        eglCore.release();
    }

    private void onSetupComplete() {
        onRendererReadyListener.onRendererReady();
    }

    @Override
    public synchronized void start() {
        initialize();

        if (onRendererReadyListener == null) {
            throw new RuntimeException("OnRenderReadyListener is not set! Set listener prior to calling start()");
        }

        super.start();
    }

    @Override
    public void run() {
        Looper.prepare();

        handler = new RenderHandler(this);

        initGL();

        Looper.loop();

        deinitGL();

        onRendererReadyListener.onRendererFinished();
    }

    public void shutdown() {
        deinitGL();
        onRendererReadyListener.onRendererFinished();
        Looper.myLooper().quit();
    }

    public void play() {
        shaderProgram.play();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture previewSurfaceTexture) {
        handler.sendEmptyMessage(RenderHandler.MSG_RENDER);
    }

    public void startRecording(File outputFile) {
        synchronized (lock) {
            TextureRecorder muxer;
            try {
                muxer = new TextureRecorder(surfaceWidth, surfaceHeight,
                    BIT_RATE, outputFile);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
            inputWindowSurface = new WindowSurface(eglCore, muxer.getInputSurface(), true);
            videoEncoder = new TextureEncoder(muxer);
        }
    }

    public void stopRecording() {
        synchronized (lock) {
            if (videoEncoder != null) {
                Log.d(TAG, "stopping recorder, mVideoEncoder=" + videoEncoder);
                videoEncoder.stopRecording();
                videoEncoder = null;
            }
            if (inputWindowSurface != null) {
                inputWindowSurface.release();
                inputWindowSurface = null;
            }
        }

    }

    private void draw() {
        shaderProgram.execute();
    }

    private void render() {
        boolean swapResult;

        synchronized (lock) {
            shaderProgram.updatePreviewTexture();

            if (eglCore.getGlVersion() >= 3) {
                draw();

                if (videoEncoder != null && inputWindowSurface != null && videoEncoder.isRecording()) {
                    videoEncoder.notifyFrameAvailableSoon();
                    inputWindowSurface.makeCurrentReadFrom(previewWindowSurface);
                    // Clear the pixels we're not going to overwrite with the blit.  Once again,
                    // this is excessive -- we don't need to clear the entire screen.
                    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                    ShaderProgram.checkError("before glBlitFramebuffer");
                    GLES30.glBlitFramebuffer(
                        0, 0, previewWindowSurface.getWidth(), previewWindowSurface.getHeight(),
                        0, 0, inputWindowSurface.getWidth(), inputWindowSurface.getHeight(),
                        GLES30.GL_COLOR_BUFFER_BIT, GLES30.GL_NEAREST);
                    int err;
                    if ((err = GLES30.glGetError()) != GLES30.GL_NO_ERROR) {
                        Log.w(TAG, "ERROR: glBlitFramebuffer failed: 0x" +
                            Integer.toHexString(err));
                    }
                    inputWindowSurface.setPresentationTime(previewSurfaceTexture.getTimestamp());
                    inputWindowSurface.swapBuffers();
                }

                //swap main buff
                previewWindowSurface.makeCurrent();
                swapResult = previewWindowSurface.swapBuffers();
            } else {
                //gl v2
                draw();

                if (videoEncoder != null && inputWindowSurface != null && videoEncoder.isRecording()) {
                    // Draw for recording, swap.
                    videoEncoder.notifyFrameAvailableSoon();
                    inputWindowSurface.makeCurrent();
                    GLES20.glClearColor(0f, 0f, 0f, 1f);
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                    GLES20.glViewport(0, 0, inputWindowSurface.getWidth(), inputWindowSurface.getHeight());
                    GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
                    GLES20.glScissor(0, 0, inputWindowSurface.getWidth(), inputWindowSurface.getHeight());
                    draw();
                    GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
                    inputWindowSurface.setPresentationTime(previewSurfaceTexture.getTimestamp());
                    inputWindowSurface.swapBuffers();

                    // Restore.
                    GLES20.glViewport(0, 0, previewWindowSurface.getWidth(), previewWindowSurface.getHeight());
                }

                // Restore previous values.
                GLES20.glViewport(0, 0, previewWindowSurface.getWidth(), previewWindowSurface.getHeight());

                previewWindowSurface.makeCurrent();
                swapResult = previewWindowSurface.swapBuffers();
            }

            if (!swapResult) {
                // This can happen if the Activity stops without waiting for us to halt.
                Log.e(TAG, "swapBuffers failed, killing renderer thread");
                shutdown();
            }
        }
    }

    public void setViewport(int viewportWidth, int viewportHeight) {
        shaderProgram.setViewport(viewportWidth, viewportHeight);
    }

    public RenderHandler getRenderHandler() {
        return handler;
    }

    public void setOnRendererReadyListener(OnRendererReadyListener listener) {
        this.onRendererReadyListener = listener;
    }

    public static class RenderHandler extends Handler {
        private static final String TAG = RenderHandler.class.getSimpleName();

        private static final int MSG_SHUTDOWN = 0;
        private static final int MSG_RENDER = 1;

        private WeakReference<SpiltVideoPlayer> weakRenderer;

        RenderHandler(SpiltVideoPlayer rt) {
            weakRenderer = new WeakReference<>(rt);
        }

        public void sendShutdown() {
            sendMessage(obtainMessage(RenderHandler.MSG_SHUTDOWN));
        }

        @Override
        public void handleMessage(Message msg) {
            SpiltVideoPlayer renderer = weakRenderer.get();
            if (renderer == null) {
                Log.w(TAG, "RenderHandler.handleMessage: weak ref is null");
                return;
            }

            int what = msg.what;
            switch (what) {
                case MSG_SHUTDOWN:
                    renderer.shutdown();
                    break;
                case MSG_RENDER:
                    renderer.render();
                    break;
                default:
                    throw new RuntimeException("unknown message " + what);
            }
        }
    }

    public interface OnRendererReadyListener {

        void onRendererReady();

        void onRendererFinished();
    }
}
