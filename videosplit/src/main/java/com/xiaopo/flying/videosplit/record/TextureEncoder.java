package com.xiaopo.flying.videosplit.record;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * Refer: grafika project
 */
public class TextureEncoder implements Runnable {
    private static final String TAG = "TextureEncoder";
    private static final boolean VERBOSE = false;

    private static final int MSG_STOP_RECORDING = 1;
    private static final int MSG_FRAME_AVAILABLE = 2;

    // ----- accessed exclusively by encoder thread -----
    private TextureRecorder textureRecorder;

    // ----- accessed by multiple threads -----
    private volatile EncoderHandler handler;

    private final Object readyFence = new Object();      // guards ready/running
    private boolean ready;
    private boolean running;


    /**
     * Tells the video recorder to start recording.  (Call from non-encoder thread.)
     * <p>
     * Creates a new thread, which will own the provided TextureRecorder.  When the
     * thread exits, the TextureRecorder will be released.
     * <p>
     * Returns after the recorder thread has started and is ready to accept Messages.
     */
    public TextureEncoder(TextureRecorder textureRecorder) {
        Log.d(TAG, "Encoder: startRecording()");

        this.textureRecorder = textureRecorder;

        synchronized (readyFence) {
            if (running) {
                Log.w(TAG, "Encoder thread already running");
                return;
            }
            running = true;
            new Thread(this, "TextureEncoder").start();
            while (!ready) {
                try {
                    readyFence.wait();
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
        }
    }

    public void stopRecording() {
        handler.sendMessage(handler.obtainMessage(MSG_STOP_RECORDING));
        // We don't know when these will actually finish (or even start).  We don't want to
        // delay the UI thread though, so we return immediately.
    }

    public boolean isRecording() {
        synchronized (readyFence) {
            return running;
        }
    }

    public void notifyFrameAvailableSoon() {
        synchronized (readyFence) {
            if (!ready) {
                return;
            }
        }

        handler.sendEmptyMessage(MSG_FRAME_AVAILABLE);
    }

    @Override
    public void run() {
        // Establish a Looper for this thread, and define a Handler for it.
        Looper.prepare();
        synchronized (readyFence) {
            handler = new EncoderHandler(this);
            ready = true;
            readyFence.notify();
        }
        Looper.loop();

        Log.d(TAG, "Encoder thread exiting");
        synchronized (readyFence) {
            ready = running = false;
            handler = null;
        }
    }


    /**
     * Handles encoder state change requests.  The handler is created on the encoder thread.
     */
    private static class EncoderHandler extends Handler {
        private WeakReference<TextureEncoder> weakEncoder;

        public EncoderHandler(TextureEncoder encoder) {
            weakEncoder = new WeakReference<>(encoder);
        }

        @Override  // runs on encoder thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;

            TextureEncoder encoder = weakEncoder.get();
            if (encoder == null) {
                Log.w(TAG, "EncoderHandler.handleMessage: encoder is null");
                return;
            }

            switch (what) {
                case MSG_STOP_RECORDING:
                    encoder.handleStopRecording();
                    Looper.myLooper().quit();
                    break;
                case MSG_FRAME_AVAILABLE:
                    encoder.handleFrameAvailable();
                    break;
                default:
                    throw new RuntimeException("Unhandled msg what=" + what);
            }
        }
    }

    private void handleFrameAvailable() {
        if (VERBOSE) Log.d(TAG, "handleFrameAvailable");
        textureRecorder.drainEncoder(false);
    }

    private void handleStopRecording() {
        Log.d(TAG, "handleStopRecording");
        textureRecorder.drainEncoder(true);
        textureRecorder.release();
    }
}
