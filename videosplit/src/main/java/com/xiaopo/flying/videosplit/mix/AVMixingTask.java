package com.xiaopo.flying.videosplit.mix;

import android.media.MediaMuxer;

import java.io.File;
import java.io.IOException;

/**
 * @author wupanjie
 */
public class AVMixingTask implements Runnable {
    private static final String TAG = "AVMixingTask";
    private static final long SLEEP_TO_WAIT_TRACK_TRANSCODERS = 10;
    private static final long PROGRESS_INTERVAL_STEPS = 10;
    private final File output;
    private final String videoPath;
    private final String audioPath;
    private final AVMixListener listener;

    public AVMixingTask(File output, String videoPath, String audioPath, AVMixListener listener) {
        this.output = output;
        this.videoPath = videoPath;
        this.audioPath = audioPath;
        this.listener = listener;
    }

    @Override
    public void run() {
        if (listener != null) {
            listener.onMixStarted();
        }
        mixAV();
        if (listener != null) {
            listener.onMixEnded();
        }
    }

    private void mixAV() {
        try {
            MediaMuxer muxer = new MediaMuxer(output.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            VideoMixer videoMixer = new VideoMixer(muxer, videoPath);
            AudioMixer audioMixer = new AudioMixer(muxer, audioPath);

            final long mixDuration = Math.min(videoMixer.getDurationUs(), audioMixer.getDurationUs());
            // if audio need transcode, let transcoder to start muxer
            if (!audioMixer.needTranscode()) {
                muxer.start();
            }
            long loopCount = 0;
            while (!(videoMixer.isMixEnded() && audioMixer.isMixEnded())) {
                loopCount++;
                boolean mixed = audioMixer.mix(mixDuration) || videoMixer.mix(mixDuration);

                if (mixDuration > 0 && loopCount % PROGRESS_INTERVAL_STEPS == 0) {
                    double audioProgress = audioMixer.isMixEnded() ? 1.0 : Math.min(1.0, (double) audioMixer.getWrittenPresentationTimeUs() / mixDuration);
                    double videoProgress = videoMixer.isMixEnded() ? 1.0 : Math.min(1.0, (double) videoMixer.getWrittenPresentationTimeUs() / mixDuration);
                    double progress = (videoProgress + audioProgress) / 2.0;
                    if (listener != null) {
                        listener.onMixProgress(progress);
                    }
                }

                if (!mixed) {
                    Thread.sleep(SLEEP_TO_WAIT_TRACK_TRANSCODERS);
                }
            }
            muxer.stop();
            muxer.release();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public interface AVMixListener {

        void onMixStarted();

        void onMixProgress(double progress);

        void onMixEnded();

    }
}
