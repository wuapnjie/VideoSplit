package com.xiaopo.flying.demo.split;

import android.Manifest;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.xiaopo.flying.demo.R;
import com.xiaopo.flying.demo.layout.OneLayout;
import com.xiaopo.flying.demo.layout.ThreeLayout;
import com.xiaopo.flying.demo.layout.TwoLayout;
import com.xiaopo.flying.demo.utils.FileUtil;
import com.xiaopo.flying.glkit.filter.NoFilter;
import com.xiaopo.flying.puzzlekit.PuzzleLayout;
import com.xiaopo.flying.videosplit.SpiltVideoPlayer;
import com.xiaopo.flying.videosplit.SplitShaderProgram;
import com.xiaopo.flying.videosplit.mix.AVMixingTask;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.PermissionNo;
import com.yanzhenjie.permission.PermissionYes;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SplitVideoActivity extends AppCompatActivity implements
    TextureView.SurfaceTextureListener,
    SpiltVideoPlayer.OnRendererReadyListener,
    AVMixingTask.AVMixListener {
    private static final String TAG = "SplitVideoActivity";
    private TextureView textureView;
    private SurfaceTexture surfaceTexture;
    private SpiltVideoPlayer renderer;
    private Button btnRecord;
    private Button btnStop;
    private Button btnCombine;

    private String taeyeonVideoPathPrefix = "/storage/emulated/0/QuickSave_By_ProtonApps/videos/";
    private String videoPath = "/storage/emulated/0/Movies/VideoSplit/record.mp4";
    private String aacAudioPath = "/storage/emulated/0/Pictures/VideoSplit/hhhhhh.aac";
    private String mp3AudioPath = "/storage/emulated/0/netease/cloudmusic/Music/LastSurprise.mp3";

    private ExecutorService mixExecutor = Executors.newSingleThreadExecutor();
    private Handler mainHandler;

    private ArrayList<String> videoPaths;
    private ArrayList<PuzzleLayout> puzzleLayouts = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_split_video);

        videoPaths = getIntent().getStringArrayListExtra("paths");
        mainHandler = new Handler(Looper.getMainLooper());
        init();
        initView();

        AndPermission.with(this)
            .permission(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .requestCode(300)
            .callback(this)
            .start();
    }

    private void init() {
        puzzleLayouts.add(new OneLayout());
        puzzleLayouts.add(new TwoLayout());
        puzzleLayouts.add(new ThreeLayout());
    }

    @PermissionYes(300)
    private void getPermissionYes(List<String> grantedPermissions) {
        if (surfaceTexture == null) {
            textureView.setSurfaceTextureListener(this);
        }
    }

    @PermissionNo(300)
    private void getPermissionNo(List<String> deniedPermissions) {

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        renderer.getRenderHandler().sendShutdown();
    }

    private void play() {
        renderer.play();
    }

    private void initView() {
        textureView = findViewById(R.id.texture_view);
        btnRecord = findViewById(R.id.btn_record);
        btnStop = findViewById(R.id.btn_stop);
        btnCombine = findViewById(R.id.btn_combine);

        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file = FileUtil.getNewFile(SplitVideoActivity.this, "VideoSplit", "record.mp4");
                videoPath = file.getAbsolutePath();
                Log.d(TAG, "start recording : file is " + file.getAbsolutePath());
                renderer.startRecording(file);
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                renderer.stopRecording();
            }
        });

        btnCombine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file = FileUtil.getNewFile(SplitVideoActivity.this, "VideoSplit", "combineMP4.mp4");
                Log.d(TAG, "combine: file is " + file.getAbsolutePath());
                mixExecutor.execute(new AVMixingTask(file, videoPath, mp3AudioPath, SplitVideoActivity.this));
            }
        });
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        this.surfaceTexture = surface;
        SplitShaderProgram shaderProgram = new SplitShaderProgram();

        for (String path : videoPaths) {
            shaderProgram.addPiece(path, NoFilter.class);
        }

        PuzzleLayout puzzleLayout = puzzleLayouts.get(videoPaths.size() - 1);
        puzzleLayout.setOuterBounds(new RectF(0, 0, width, height));
        puzzleLayout.layout();
        puzzleLayout.setPadding(12);
        shaderProgram.setPuzzleLayout(puzzleLayout);
        shaderProgram.setBackgroundColor(Color.parseColor("#532421"));

        renderer = new SpiltVideoPlayer(surfaceTexture, width, height, shaderProgram);
        renderer.setViewport(width, height);
        renderer.setOnRendererReadyListener(this);
        renderer.start();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void onRendererReady() {
        play();
    }

    @Override
    public void onRendererFinished() {

    }

    @Override
    public void onMixStarted() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: Mix Start");
                Toast.makeText(SplitVideoActivity.this, "Mix Start", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMixProgress(double progress) {

    }

    @Override
    public void onMixEnded() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: Mix End");
                Toast.makeText(SplitVideoActivity.this, "Mix End", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
