package com.xiaopo.flying.videosplit.record;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.view.Surface;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Refer: grafika project
 */
public class TextureRecorder {
  private static final String TAG = "TextureRecorder";
  private static final boolean VERBOSE = false;

  // TODO: these ought to be configurable as well
  private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
  private static final int FRAME_RATE = 30;               // 30fps
  private static final int IFRAME_INTERVAL = 5;           // 5 seconds between I-frames

  private Surface inputSurface;
  private MediaMuxer muxer;
  private MediaCodec encoder;
  private MediaCodec.BufferInfo bufferInfo;
  private int videoTrackIndex;
  private boolean muxerStarted;

  /**
   * Configures encoder and muxer state, and prepares the input Surface.
   */
  public TextureRecorder(int width, int height, int bitRate, File outputFile)
      throws IOException {
    bufferInfo = new MediaCodec.BufferInfo();

    MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);

    // Set some properties.  Failing to specify some of these can cause the MediaCodec
    // configure() call to throw an unhelpful exception.
    format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
    format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
    format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
    format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
    if (VERBOSE) Log.d(TAG, "format: " + format);

    // Create a MediaCodec encoder, and configure it with our format.  Get a Surface
    // we can use for input and wrap it with a class that handles the EGL work.
    encoder = MediaCodec.createEncoderByType(MIME_TYPE);
    encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    inputSurface = encoder.createInputSurface();
    encoder.start();

    // Create a MediaMuxer.  We can't add the video track and start() the muxer here,
    // because our MediaFormat doesn't have the Magic Goodies.  These can only be
    // obtained from the encoder after it has started processing data.
    //
    // We're not actually interested in multiplexing audio.  We just want to convert
    // the raw H.264 elementary stream we get from MediaCodec into a .mp4 file.
    muxer = new MediaMuxer(outputFile.getPath(),
        MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

    videoTrackIndex = -1;
    muxerStarted = false;
  }

  /**
   * Returns the encoder's input surface.
   */
  public Surface getInputSurface() {
    return inputSurface;
  }

  /**
   * Releases encoder resources.
   */
  public void release() {
    if (VERBOSE) Log.d(TAG, "releasing encoder objects");
    if (encoder != null) {
      encoder.stop();
      encoder.release();
      encoder = null;
    }
    if (muxer != null) {
      muxer.stop();
      muxer.release();
      muxer = null;
    }
  }

  /**
   * Extracts all pending data from the encoder and forwards it to the muxer.
   * <p>
   * If endOfStream is not set, this returns when there is no more data to drain.  If it
   * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
   * Calling this with endOfStream set should be done once, right before stopping the muxer.
   * <p>
   * We're just using the muxer to get a .mp4 file (instead of a raw H.264 stream).  We're
   * not recording audio.
   */
  public void drainEncoder(boolean endOfStream) {
    final int TIMEOUT_USEC = 10000;
    if (VERBOSE) Log.d(TAG, "drainEncoder(" + endOfStream + ")");

    if (endOfStream) {
      if (VERBOSE) Log.d(TAG, "sending EOS to encoder");
      encoder.signalEndOfInputStream();
    }

    ByteBuffer[] encoderOutputBuffers = encoder.getOutputBuffers();
    while (true) {
      int encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
      if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
        // no output available yet
        if (!endOfStream) {
          break;      // out of while
        } else {
          if (VERBOSE) Log.d(TAG, "no output available, spinning to await EOS");
        }
      } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
        // not expected for an encoder
        encoderOutputBuffers = encoder.getOutputBuffers();
      } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
        // should happen before receiving buffers, and should only happen once
        if (muxerStarted) {
          throw new RuntimeException("format changed twice");
        }
        MediaFormat newFormat = encoder.getOutputFormat();
        Log.d(TAG, "encoder output format changed: " + newFormat);

        // now that we have the Magic Goodies, start the muxer
        videoTrackIndex = muxer.addTrack(newFormat);
        muxer.start();
        muxerStarted = true;
      } else if (encoderStatus < 0) {
        Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
            encoderStatus);
        // let's ignore it
      } else {
        ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
        if (encodedData == null) {
          throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
              " was null");
        }

        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
          // The codec config data was pulled out and fed to the muxer when we got
          // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
          if (VERBOSE) Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
          bufferInfo.size = 0;
        }

        if (bufferInfo.size != 0) {
          if (!muxerStarted) {
            throw new RuntimeException("muxer hasn't started");
          }

          // adjust the ByteBuffer values to match BufferInfo (not needed?)
          encodedData.position(bufferInfo.offset);
          encodedData.limit(bufferInfo.offset + bufferInfo.size);

          muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo);
          if (VERBOSE) {
            Log.d(TAG, "sent " + bufferInfo.size + " bytes to muxer, ts=" +
                bufferInfo.presentationTimeUs);
          }
        }
        encoder.releaseOutputBuffer(encoderStatus, false);

        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
          if (!endOfStream) {
            Log.w(TAG, "reached end of stream unexpectedly");
          } else {
            if (VERBOSE) Log.d(TAG, "end of stream reached");
          }
          break;      // out of while
        }
      }
    }
  }
}
