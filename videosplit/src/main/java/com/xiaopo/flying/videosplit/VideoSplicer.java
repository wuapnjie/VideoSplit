package com.xiaopo.flying.videosplit;

import android.content.Context;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.support.annotation.ColorInt;
import com.xiaopo.flying.glkit.filter.ShaderFilter;
import com.xiaopo.flying.puzzlekit.PuzzleLayout;
import com.xiaopo.flying.videosplit.mix.AVMixingTask;
import java.io.File;

/**
 * @author wupanjie
 */
public class VideoSplicer {
  private static final String TAG = "VideoSplicer";

  private Context context;
  private PuzzleLayout puzzleLayout;
  private int padding;
  private int width;
  private int height;
  private SpiltVideoCreator creator;
  private SplitShaderProgram shaderProgram;
  private File outputFile;
  private String audioPath;
  private File tempFile;
  private OnProgressListener onProgressListener;

  private VideoSplicer(Context context) {
    this.context = context;
    this.shaderProgram = new SplitShaderProgram();
  }

  public static VideoSplicer newInstance(Context context) {
    return new VideoSplicer(context);
  }

  public VideoSplicer puzzleLayout(PuzzleLayout puzzleLayout) {
    this.puzzleLayout = puzzleLayout;
    return this;
  }

  public VideoSplicer width(int width) {
    this.width = width;
    return this;
  }

  public VideoSplicer height(int height) {
    this.height = height;
    return this;
  }

  public VideoSplicer padding(int padding) {
    this.padding = padding;
    return this;
  }

  public VideoSplicer addVideo(String path, Class<? extends ShaderFilter> filter) {
    shaderProgram.addPiece(path, filter);
    return this;
  }

  public VideoSplicer addVideo(String path, ShaderFilter filter) {
    shaderProgram.addPiece(path, filter);
    return this;
  }

  public VideoSplicer backgroundColor(@ColorInt int color) {
    shaderProgram.setBackgroundColor(color);
    return this;
  }

  public VideoSplicer outputFile(File outputFile) {
    this.outputFile = outputFile;
    return this;
  }

  public VideoSplicer audioPath(String audioPath) {
    this.audioPath = audioPath;
    return this;
  }

  public VideoSplicer progressListener(OnProgressListener onProgressListener) {
    this.onProgressListener = onProgressListener;
    return this;
  }

  public void create() {
    puzzleLayout.setOuterBounds(new RectF(0, 0, width, height));
    puzzleLayout.layout();
    puzzleLayout.setPadding(padding);
    shaderProgram.setPuzzleLayout(puzzleLayout);
    final String tempPath = context.getCacheDir().getPath() + File.separator + "Temp" + System.currentTimeMillis() + ".mp4";
    tempFile = new File(tempPath);
    creator = new SpiltVideoCreator(tempFile, width, height, shaderProgram);
    creator.setViewport(width, height);
    creator.setOnRendererReadyListener(new SpiltVideoCreator.OnRendererListener() {
      @Override
      public void onRendererReady() {
        creator.create();

        if (onProgressListener != null) {
          onProgressListener.onStart();
        }
      }

      @Override
      public void onRendererFinished() {

      }
    });
    creator.setOnProcessProgressListener(new SpiltVideoCreator.OnProcessProgressListener() {
      @Override
      public void onProcessStarted() {

      }

      @Override
      public void onProcessProgressChanged(double progress) {
        if (onProgressListener != null) {
          onProgressListener.onProgress(progress / 2.0);
        }
      }

      @Override
      public void onProcessEnded() {
        mixAV();
      }
    });
    creator.start();
  }

  private void mixAV() {
    AsyncTask.SERIAL_EXECUTOR
        .execute(new AVMixingTask(outputFile, tempFile.getAbsolutePath(), audioPath, new AVMixingTask.AVMixListener() {
          @Override
          public void onMixStarted() {

          }

          @Override
          public void onMixProgress(double progress) {
            if (onProgressListener != null) {
              onProgressListener.onProgress(0.5 + progress / 2.0);
            }
          }

          @Override
          public void onMixEnded() {
            boolean success = tempFile.delete();
            if (onProgressListener != null) {
              onProgressListener.onEnd();
            }
          }
        }));
  }

  public interface OnProgressListener {
    void onStart();

    void onProgress(double progress);

    void onEnd();
  }
}
