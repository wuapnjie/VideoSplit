package com.xiaopo.flying.videosplit.utils;

import android.media.MediaCodec;
import android.os.Build;
import java.nio.ByteBuffer;

/**
 * Refer: https://github.com/ypresto/android-transcoder
 * A Wrapper to MediaCodec that facilitates the use of API-dependent get{Input/Output}Buffer methods,
 * in order to prevent: http://stackoverflow.com/q/30646885
 */
public class MediaCodecBufferCompatWrapper {

  private final MediaCodec mediaCodec;
  private final ByteBuffer[] inputBuffers;
  private final ByteBuffer[] outputBuffers;

  public MediaCodecBufferCompatWrapper(MediaCodec mediaCodec) {
    this.mediaCodec = mediaCodec;

    if (Build.VERSION.SDK_INT < 21) {
      inputBuffers = mediaCodec.getInputBuffers();
      outputBuffers = mediaCodec.getOutputBuffers();
    } else {
      inputBuffers = outputBuffers = null;
    }
  }

  public ByteBuffer getInputBuffer(final int index) {
    if (Build.VERSION.SDK_INT >= 21) {
      return mediaCodec.getInputBuffer(index);
    }
    return inputBuffers[index];
  }

  public ByteBuffer getOutputBuffer(final int index) {
    if (Build.VERSION.SDK_INT >= 21) {
      return mediaCodec.getOutputBuffer(index);
    }
    return outputBuffers[index];
  }
}
