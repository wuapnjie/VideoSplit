package com.xiaopo.flying.videosplit.mix;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import com.xiaopo.flying.videosplit.utils.MediaUtil;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author wupanjie
 */
class VideoMixer {
  private static final String TAG = "VideoMixer";
  private static final int MAX_SAMPLE_SIZE = 256 * 1024;
  private static final boolean VERBOSE = false;
  private final MediaMuxer muxer;
  private final String videoPath;
  private final MediaExtractor extractor;
  private MediaFormat format;
  private long durationUs;
  private int writeToMuxerTrackIndex = -1;

  private int bufferSize = MAX_SAMPLE_SIZE;
  private int frameCount;
  private int offset = 100;
  private ByteBuffer dstBuffer;
  private MediaCodec.BufferInfo bufferInfo;
  private long writtenPresentationTimeUs;
  private boolean mixEnded;

  VideoMixer(MediaMuxer muxer, String videoPath) {
    this.muxer = muxer;
    this.videoPath = videoPath;
    this.extractor = new MediaExtractor();
    this.dstBuffer = ByteBuffer.allocate(bufferSize);
    this.bufferInfo = new MediaCodec.BufferInfo();

    init();
  }

  private void init() {
    try {
      extractor.setDataSource(videoPath);
      int videoTrackIndex = -1;
      int trackCount = extractor.getTrackCount();
      for (int i = 0; i < trackCount; i++) {
        format = extractor.getTrackFormat(i);
        String mimeType = format.getString(MediaFormat.KEY_MIME);
        if (mimeType.startsWith("video/")) {
          videoTrackIndex = i;
          break;
        }
      }
      extractor.selectTrack(videoTrackIndex);
      durationUs = MediaUtil.determineDuration(videoPath);
      writeToMuxerTrackIndex = muxer.addTrack(format);
      if (VERBOSE) {
        Log.d(TAG, "Video Info:\n" +
            "mimeType:" + format.getString(MediaFormat.KEY_MIME) + "\n" +
            "duration:" + durationUs / 1000000 + "s\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  boolean mix(final long maxDuration) {
    if (mixEnded) {
      return false;
    }
    bufferInfo.offset = offset;
    bufferInfo.size = extractor.readSampleData(dstBuffer, offset);
    bufferInfo.presentationTimeUs = extractor.getSampleTime();

    if (bufferInfo.size < 0 || bufferInfo.presentationTimeUs >= maxDuration) {
      bufferInfo.size = 0;
      mixEnded = true;
      return false;
    }
    bufferInfo.flags = extractor.getSampleFlags();
    muxer.writeSampleData(writeToMuxerTrackIndex, dstBuffer, bufferInfo);
    extractor.advance();
    writtenPresentationTimeUs = bufferInfo.presentationTimeUs;
    frameCount++;

    if (VERBOSE) {
      Log.d(TAG, "Frame (" + frameCount + ") " +
          " VideoPresentationTimeUs:" + bufferInfo.presentationTimeUs +
          " Flags:" + bufferInfo.flags +
          " Size(KB) " + bufferInfo.size / 1024);
    }

    return true;
  }

  boolean isMixEnded() {
    return mixEnded;
  }


  long getDurationUs() {
    return durationUs;
  }

  long getWrittenPresentationTimeUs() {
    return writtenPresentationTimeUs;
  }
}
