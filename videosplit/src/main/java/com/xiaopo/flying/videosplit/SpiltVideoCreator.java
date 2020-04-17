package com.xiaopo.flying.videosplit;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.math.MathUtils;
import android.util.Log;
import com.xiaopo.flying.glkit.gl.EglCore;
import com.xiaopo.flying.glkit.gl.WindowSurface;
import com.xiaopo.flying.videosplit.record.TextureEncoder;
import com.xiaopo.flying.videosplit.record.TextureRecorder;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * @author wupanjie
 */
class SpiltVideoCreator extends Thread implements SurfaceTexture.OnFrameAvailableListener {
  private static final String TAG = SpiltVideoCreator.class.getSimpleName();
  private static final String THREAD_NAME = "SpiltVideoCreator";
  private static final long PROGRESS_INTERVAL_STEPS = 10;
  private static final int BIT_RATE = 4000000;

  private final Object lock = new Object();
  private int surfaceWidth;
  private int surfaceHeight;
  private File outputFile;

  private EglCore eglCore;

  private RenderHandler handler;
  private OnRendererListener onRendererReadyListener;

  private SplitShaderProgram shaderProgram;

  private WindowSurface inputWindowSurface;
  private TextureEncoder videoEncoder;

  private long outputVideoDuration;
  private long startTime;

  private OnProcessProgressListener onProcessProgressListener;

  public SpiltVideoCreator(File outputFile, int width, int height, SplitShaderProgram shaderProgram) {
    this.setName(THREAD_NAME);
    this.outputFile = outputFile;
    this.surfaceWidth = width;
    this.surfaceHeight = height;
    this.shaderProgram = shaderProgram;
  }

  private void initialize() {
    setViewport(surfaceWidth, surfaceHeight);
  }

  private void initGL() {
    eglCore = new EglCore(null, EglCore.FLAG_RECORDABLE | EglCore.FLAG_TRY_GLES3);
    //init encoder
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

    inputWindowSurface.makeCurrent();

    initGLComponents();
  }

  private void initGLComponents() {
    shaderProgram.prepare();
    shaderProgram.setOnFrameAvailableListener(this);

    outputVideoDuration = shaderProgram.getLongestVideoPieceDuration();

    onSetupComplete();
  }


  private void deinitGL() {
    if (videoEncoder != null) {
      videoEncoder.stopRecording();
      videoEncoder = null;
    }
    if (inputWindowSurface != null) {
      inputWindowSurface.release();
      inputWindowSurface = null;
    }
    shaderProgram.release();
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

  private void shutdown() {
    Looper.myLooper().quit();
  }

  public void create() {
    startTime = System.currentTimeMillis();
    if (onProcessProgressListener != null) {
      onProcessProgressListener.onProcessStarted();
    }
    shaderProgram.play();
  }

  @Override
  public void onFrameAvailable(SurfaceTexture previewSurfaceTexture) {
    handler.sendEmptyMessage(RenderHandler.MSG_RENDER);
  }

  public void stopMixing() {
    synchronized (lock) {
      if (videoEncoder != null) {
        videoEncoder.stopRecording();
        videoEncoder = null;
      }
      if (inputWindowSurface != null) {
        inputWindowSurface.release();
        inputWindowSurface = null;
      }

      if (onProcessProgressListener != null) {
        // 等待视频写入完成
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        onProcessProgressListener.onProcessEnded();
      }
    }

    handler.sendShutdown();
  }

  private void draw() {
    shaderProgram.run();
  }

  private boolean started = true;
  private long loopCount;

  private void render() {
    synchronized (lock) {

      shaderProgram.updatePreviewTexture();

      if (videoEncoder != null && inputWindowSurface != null && videoEncoder.isRecording()) {
        loopCount++;
        videoEncoder.notifyFrameAvailableSoon();
        inputWindowSurface.makeCurrent();
        draw();
        inputWindowSurface.setPresentationTime(shaderProgram.getPresentationTimeUs() * 1000);
        inputWindowSurface.swapBuffers();

        if (started) {
          startTime = shaderProgram.getPresentationTimeUs();
          started = false;
        } else {
          final long usageTime = shaderProgram.getPresentationTimeUs() - startTime;
          if (shaderProgram.isFinished() || usageTime >= outputVideoDuration * 1000) {
            stopMixing();
            return;
          }

          if (!shaderProgram.isFinished() && onProcessProgressListener != null && loopCount % PROGRESS_INTERVAL_STEPS == 0) {
            double progress = MathUtils.clamp((double) usageTime / (outputVideoDuration * 1000), 0.0, 1.0);
            onProcessProgressListener.onProcessProgressChanged(progress);
          }
        }
      }

    }
  }

  public void setViewport(int viewportWidth, int viewportHeight) {
    shaderProgram.setViewport(viewportWidth, viewportHeight);
  }

  public RenderHandler getRenderHandler() {
    return handler;
  }

  public void setOnRendererReadyListener(OnRendererListener listener) {
    this.onRendererReadyListener = listener;
  }

  public static class RenderHandler extends Handler {
    private static final String TAG = RenderHandler.class.getSimpleName();

    private static final int MSG_SHUTDOWN = 0;
    private static final int MSG_RENDER = 1;

    private WeakReference<SpiltVideoCreator> weakRenderer;

    RenderHandler(SpiltVideoCreator rt) {
      weakRenderer = new WeakReference<>(rt);
    }

    public void sendShutdown() {
      sendMessage(obtainMessage(RenderHandler.MSG_SHUTDOWN));
    }

    @Override
    public void handleMessage(Message msg) {
      SpiltVideoCreator renderer = weakRenderer.get();
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

  void setOnProcessProgressListener(OnProcessProgressListener onProcessProgressListener) {
    this.onProcessProgressListener = onProcessProgressListener;
  }

  interface OnRendererListener {

    void onRendererReady();

    void onRendererFinished();
  }

  interface OnProcessProgressListener {

    void onProcessStarted();

    void onProcessProgressChanged(double progress);

    void onProcessEnded();

  }
}
