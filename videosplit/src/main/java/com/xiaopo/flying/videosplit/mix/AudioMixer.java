package com.xiaopo.flying.videosplit.mix;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import com.xiaopo.flying.videosplit.mix.transcode.AudioTranscoder;
import com.xiaopo.flying.videosplit.utils.MediaUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;

/**
 * @author wupanjie
 */
class AudioMixer {
    private static final String TAG = "AudioMixer";
    private static final int MAX_SAMPLE_SIZE = 512 * 1024;
    private static final boolean VERBOSE = false;

    private static HashSet<String> muxerSupportedAudioType = new HashSet<>();

    static {
        muxerSupportedAudioType.add("audio/mp4a-latm");
        muxerSupportedAudioType.add("audio/3gpp");
        muxerSupportedAudioType.add("audio/amr-wb");
    }

    private final MediaMuxer muxer;
    private final String audioPath;
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

    private AudioTranscoder transcoder;

    AudioMixer(MediaMuxer muxer, String audioPath) {
        this.muxer = muxer;
        this.audioPath = audioPath;
        this.extractor = new MediaExtractor();
        this.dstBuffer = ByteBuffer.allocate(bufferSize);
        this.bufferInfo = new MediaCodec.BufferInfo();

        init();
    }

    private void init() {
        try {
            extractor.setDataSource(audioPath);
            int audioTrackIndex = -1;
            int trackCount = extractor.getTrackCount();
            String mimeType = "";
            for (int i = 0; i < trackCount; i++) {
                format = extractor.getTrackFormat(i);
                mimeType = format.getString(MediaFormat.KEY_MIME);
                if (mimeType.startsWith("audio/")) {
                    audioTrackIndex = i;
                    break;
                }
            }
            extractor.selectTrack(audioTrackIndex);
            durationUs = MediaUtil.determineDuration(audioPath);
            if (muxerSupportedAudioType.contains(mimeType)) {
                writeToMuxerTrackIndex = muxer.addTrack(format);
            } else {
                transcoder = new AudioTranscoder(muxer, extractor, audioTrackIndex);
            }
            if (VERBOSE) {
                Log.d(TAG, "Audio Info:\n" +
                    "mimeType:" + mimeType + "\n" +
                    "duration:" + durationUs / 1000000 + "s\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    boolean needTranscode() {
        return transcoder != null;
    }

    boolean mix(final long maxDuration) {
        if (mixEnded) {
            return false;
        }

        if (transcoder != null) {
            if (transcoder.isFinished()) {
                mixEnded = true;
                return false;
            }
            transcoder.mix(maxDuration);
            writtenPresentationTimeUs = transcoder.getWrittenPresentationTimeUs();
            return true;
        }

        bufferInfo.offset = offset;
        bufferInfo.size = extractor.readSampleData(dstBuffer, offset);
        bufferInfo.presentationTimeUs = extractor.getSampleTime();

        if (bufferInfo.size < 0 || bufferInfo.presentationTimeUs >= maxDuration) {
            bufferInfo.size = 0;
            mixEnded = true;
            return false;
        }

        boolean isKeyFrame = (extractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0;
        bufferInfo.flags = isKeyFrame ? MediaCodec.BUFFER_FLAG_SYNC_FRAME : 0;
        muxer.writeSampleData(writeToMuxerTrackIndex, dstBuffer, bufferInfo);
        extractor.advance();
        writtenPresentationTimeUs = bufferInfo.presentationTimeUs;
        frameCount++;

        if (VERBOSE) {
            Log.d(TAG, "Frame (" + frameCount + ") " +
                " AudioPresentationTimeUs:" + bufferInfo.presentationTimeUs +
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
