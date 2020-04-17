package com.xiaopo.flying.videosplit.mix.transcode;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import com.xiaopo.flying.videosplit.utils.MediaCodecBufferCompatWrapper;
import com.xiaopo.flying.videosplit.utils.MediaUtil;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Refer: https://github.com/ypresto/android-transcoder
 * <p>
 * little change
 * <p>
 * Transcoding mp3 format to aac format
 *
 * @author wupanjie
 */
public class AudioTranscoder {
    private static final String TAG = "AudioTranscoder";
    private static final boolean VERBOSE = false;

    private static final int DRAIN_STATE_NONE = 0;
    private static final int DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY = 1;
    private static final int DRAIN_STATE_CONSUMED = 2;

    private final MediaMuxer muxer;
    private final MediaExtractor extractor;

    private final int inputAudioTrackIndex;

    /**
     * mp3 audio/mpeg
     */
    private final MediaFormat inputAudioFormat;

    /**
     * aac audio/mp4a-latm
     */
    private MediaFormat outputAudioFormat;

    private MediaCodec decoder;
    private MediaCodec encoder;
    private MediaFormat actualOutputFormat;

    private MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

    private MediaCodecBufferCompatWrapper decoderBuffers;
    private MediaCodecBufferCompatWrapper encoderBuffers;

    private boolean isTranscodeEnd;

    private boolean isExtractorEOS;
    private boolean isDecoderEOS;
    private boolean isEncoderEOS;
    private boolean encoderStarted;
    private boolean decoderStarted;

    private AudioChannel audioChannel;
    private long writtenPresentationTimeUs;
    private int writeToMuxerTrackIndex = -1;
    private int frameCount;

    public AudioTranscoder(MediaMuxer muxer, MediaExtractor extractor, int inputAudioTrackIndex) {
        this.muxer = muxer;
        this.extractor = extractor;
        this.inputAudioTrackIndex = inputAudioTrackIndex;
        this.inputAudioFormat = extractor.getTrackFormat(inputAudioTrackIndex);

        init();
    }

    private void init() {
        outputAudioFormat = MediaUtil.determineAACFormat(inputAudioFormat);
        extractor.selectTrack(inputAudioTrackIndex);

        // configure encoder
        try {
            encoder = MediaCodec.createEncoderByType(outputAudioFormat.getString(MediaFormat.KEY_MIME));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        encoder.configure(outputAudioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        encoder.start();
        encoderStarted = true;
        encoderBuffers = new MediaCodecBufferCompatWrapper(encoder);

        // configure decoder
        try {
            decoder = MediaCodec.createDecoderByType(inputAudioFormat.getString(MediaFormat.KEY_MIME));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        decoder.configure(inputAudioFormat, null, null, 0);
        decoder.start();
        decoderStarted = true;
        decoderBuffers = new MediaCodecBufferCompatWrapper(decoder);

        audioChannel = new AudioChannel(decoder, encoder, outputAudioFormat);
    }

    public boolean mix(final long maxDuration) {
        if (writtenPresentationTimeUs >= maxDuration) {
            isTranscodeEnd = true;
            return false;
        }
        boolean busy = false;

        int status;
        while (drainEncoder(0) != DRAIN_STATE_NONE) {
            busy = true;
        }
        do {
            status = drainDecoder(0);
            if (status != DRAIN_STATE_NONE) {
                busy = true;
            }
            // NOTE: not repeating to keep from deadlock when encoder is full.
        } while (status == DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY);

        while (audioChannel.feedEncoder(0)) {
            busy = true;
        }
        while (drainExtractor(0) != DRAIN_STATE_NONE) {
            busy = true;
        }

        return busy;
    }

    private int drainExtractor(long timeoutUs) {
        if (isExtractorEOS) {
            return DRAIN_STATE_NONE;
        }
        int trackIndex = extractor.getSampleTrackIndex();
        if (trackIndex >= 0 && trackIndex != inputAudioTrackIndex) {
            return DRAIN_STATE_NONE;
        }

        final int result = decoder.dequeueInputBuffer(timeoutUs);
        if (result < 0) {
            return DRAIN_STATE_NONE;
        }
        if (trackIndex < 0) {
            isExtractorEOS = true;
            decoder.queueInputBuffer(result, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            return DRAIN_STATE_NONE;
        }

        final int sampleSize = extractor.readSampleData(decoderBuffers.getInputBuffer(result), 0);
        final boolean isKeyFrame = (extractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0;
        decoder.queueInputBuffer(result, 0, sampleSize, extractor.getSampleTime(), isKeyFrame ? MediaCodec.BUFFER_FLAG_SYNC_FRAME : 0);
        extractor.advance();
        return DRAIN_STATE_CONSUMED;
    }

    private int drainDecoder(long timeoutUs) {
        if (isDecoderEOS) {
            return DRAIN_STATE_NONE;
        }

        int result = decoder.dequeueOutputBuffer(bufferInfo, timeoutUs);
        switch (result) {
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                return DRAIN_STATE_NONE;
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                audioChannel.setActualDecodedFormat(decoder.getOutputFormat());
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
            default:
                // no-ops
        }

        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            isDecoderEOS = true;
            audioChannel.drainDecoderBufferAndQueue(AudioChannel.BUFFER_INDEX_END_OF_STREAM, 0);
        } else if (bufferInfo.size > 0) {
            audioChannel.drainDecoderBufferAndQueue(result, bufferInfo.presentationTimeUs);
        }

        return DRAIN_STATE_CONSUMED;
    }

    private int drainEncoder(long timeoutUs) {
        if (isEncoderEOS) {
            return DRAIN_STATE_NONE;
        }

        int result = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs);
        switch (result) {
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                return DRAIN_STATE_NONE;
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                if (actualOutputFormat != null) {
                    throw new RuntimeException("Audio output format changed twice.");
                }
                actualOutputFormat = encoder.getOutputFormat();

                writeToMuxerTrackIndex = muxer.addTrack(actualOutputFormat);
                muxer.start();
                return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                encoderBuffers = new MediaCodecBufferCompatWrapper(encoder);
                return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
            default:
                // no-ops
        }

        if (actualOutputFormat == null) {
            throw new RuntimeException("Could not determine actual output format.");
        }

        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            isEncoderEOS = true;
            isTranscodeEnd = true;
            bufferInfo.set(0, 0, 0, bufferInfo.flags);
        }
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            // SPS or PPS, which should be passed by MediaFormat.
            encoder.releaseOutputBuffer(result, false);
            return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
        }
        ByteBuffer byteBuf = encoderBuffers.getOutputBuffer(result);
        muxer.writeSampleData(writeToMuxerTrackIndex, byteBuf, bufferInfo);
        frameCount++;

        writtenPresentationTimeUs = bufferInfo.presentationTimeUs;
        encoder.releaseOutputBuffer(result, false);

        if (VERBOSE) {
            Log.d(TAG, "Frame (" + frameCount + ") " +
                " AudioPresentationTimeUs:" + bufferInfo.presentationTimeUs +
                " Flags:" + bufferInfo.flags +
                " Size(KB) " + bufferInfo.size / 1024);
        }
        return DRAIN_STATE_CONSUMED;
    }

    public void release() {
        if (decoder != null) {
            if (decoderStarted) {
                decoder.stop();
            }
            decoder.release();
            decoder = null;
        }
        if (encoder != null) {
            if (encoderStarted) {
                encoder.stop();
            }
            encoder.release();
            encoder = null;
        }
    }

    public boolean isFinished() {
        return isTranscodeEnd;
    }

    public long getWrittenPresentationTimeUs() {
        return writtenPresentationTimeUs;
    }
}
