package com.xiaopo.flying.videosplit.utils;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;

/**
 * @author wupanjie
 */
public class MediaUtil {
  private static final String FORMAT_AUDIO_AAC =  "audio/mp4a-latm";

  private MediaUtil() {
    //no instance
  }

  public static long determineDuration(final String path){
    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
    retriever.setDataSource(path);
    long durationUs;
    try {
      durationUs = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) * 1000;
    } catch (NumberFormatException e) {
      durationUs = -1;
    }

    return durationUs;
  }

  public static MediaFormat determineAACFormat(MediaFormat inputFormat){
    final MediaFormat format = MediaFormat.createAudioFormat(
        FORMAT_AUDIO_AAC,
        inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
        inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
    format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
    format.setInteger(MediaFormat.KEY_BIT_RATE, 128 * 1000);
    return format;
  }

}
