package com.xiaopo.flying.demo;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.xiaopo.flying.demo.danmaku.DanmakuActivity;
import com.xiaopo.flying.demo.filter.IF1977Filter;
import com.xiaopo.flying.demo.layout.OneLayout;
import com.xiaopo.flying.demo.layout.ThreeLayout;
import com.xiaopo.flying.demo.layout.TwoLayout;
import com.xiaopo.flying.demo.select.GridItemDecoration;
import com.xiaopo.flying.demo.select.MediaManager;
import com.xiaopo.flying.demo.select.VideoAttachment;
import com.xiaopo.flying.demo.select.VideoAttachmentBinder;
import com.xiaopo.flying.demo.split.SplitVideoActivity;
import com.xiaopo.flying.demo.utils.DipPixelKit;
import com.xiaopo.flying.demo.utils.FileUtil;
import com.xiaopo.flying.puzzlekit.PuzzleLayout;
import com.xiaopo.flying.videosplit.VideoSplicer;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.PermissionNo;
import com.yanzhenjie.permission.PermissionYes;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import me.drakeet.multitype.MultiTypeAdapter;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "VideoSpilt";

    private String mp3AudioPath = "/storage/emulated/0/netease/cloudmusic/Music/LastSurprise.mp3";

    private RecyclerView videoList;
    private MultiTypeAdapter videoAdapter;
    private Set<Integer> selectedPositions = new LinkedHashSet<>();
    private ArrayList<VideoAttachment> videoAttachments = new ArrayList<>();
    private int processProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        AndPermission.with(this)
            .permission(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .requestCode(300)
            .callback(this)
            .start();
    }

    private void initView() {
        videoList = findViewById(R.id.video_list);
        videoList.setLayoutManager(new GridLayoutManager(this, 4));
        final int space = DipPixelKit.dip2px(MainActivity.this, 2);
        GridItemDecoration itemDecoration
            = new GridItemDecoration(4, space, false);
        videoList.addItemDecoration(itemDecoration);

        videoAdapter = new MultiTypeAdapter();
        final int screenWidth = DipPixelKit.getDeviceWidth(this);
        final int availableLength = screenWidth - 3 * DipPixelKit.dip2px(this, 2);
        VideoAttachmentBinder videoAttachmentBinder =
            new VideoAttachmentBinder(selectedPositions, 3, availableLength / 4, availableLength / 4);
        videoAdapter.register(VideoAttachment.class, videoAttachmentBinder);

        videoList.setAdapter(videoAdapter);

        findViewById(R.id.fab_action).setOnClickListener(this::toSplitVideo);
    }

    private void startMix(View view) {
        if (selectedPositions.isEmpty()) {
            Toast.makeText(MainActivity.this, "Select Video", Toast.LENGTH_SHORT).show();
            return;
        }

        final int width = 1080;
        final int height = 1080;

        ArrayList<PuzzleLayout> puzzleLayouts = new ArrayList<>();
        puzzleLayouts.add(new OneLayout());
        puzzleLayouts.add(new TwoLayout());
        puzzleLayouts.add(new ThreeLayout());

        ArrayList<String> videoPaths = new ArrayList<>();
        for (Integer selectedPosition : selectedPositions) {
            videoPaths.add(videoAttachments.get(selectedPosition).getPath());
        }

        ProgressDialog progressDialog = new ProgressDialog(this, R.style.Progress);
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.show();

        final Runnable changeProgress = new Runnable() {
            @Override
            public void run() {
                progressDialog.setProgress(processProgress);
            }
        };

        VideoSplicer splicer = VideoSplicer.newInstance(this)
            .height(height)
            .width(width)
            .padding(12)
            .audioPath(mp3AudioPath)
            .backgroundColor(Color.rgb(254, 248, 201))
            .outputFile(FileUtil.getNewFile(this, "VideoSplit", "fromVideoSplicer.mp4"))
            .puzzleLayout(puzzleLayouts.get(videoPaths.size() - 1))
            .progressListener(new VideoSplicer.OnProgressListener() {
                @Override
                public void onStart() {

                }

                @Override
                public void onProgress(double progress) {
                    processProgress = (int) (progress * 100);
                    runOnUiThread(changeProgress);
                    Log.d(TAG, "onProgress: progress -> " + progress);
                }

                @Override
                public void onEnd() {
                    progressDialog.dismiss();
                }
            });

        for (String path : videoPaths) {
            splicer.addVideo(path, new IF1977Filter(this));
        }

        splicer.create();

    }

    private void toSplitVideo(View view) {
        ArrayList<String> videoPaths = new ArrayList<>();
        for (Integer selectedPosition : selectedPositions) {
            videoPaths.add(videoAttachments.get(selectedPosition).getPath());
        }

        Intent intent = new Intent(this, SplitVideoActivity.class);
        intent.putStringArrayListExtra("paths", videoPaths);
        startActivity(intent);
    }

    private void toDanmkau(View view) {
        Intent intent = new Intent(this, DanmakuActivity.class);
        startActivity(intent);
    }

    @PermissionYes(300)
    private void getPermissionYes(List<String> grantedPermissions) {
        loadVideos();
    }

    private void loadVideos() {
        MediaManager mediaManager = new MediaManager(this);
        videoAttachments.clear();
        videoAttachments.addAll(mediaManager.getAllVideos());
        videoAdapter.setItems(videoAttachments);
        videoAdapter.notifyDataSetChanged();
    }

    @PermissionNo(300)
    private void getPermissionNo(List<String> deniedPermissions) {
        Toast.makeText(this, "必须要权限", Toast.LENGTH_SHORT).show();
    }

}
