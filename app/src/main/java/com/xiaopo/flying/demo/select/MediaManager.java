package com.xiaopo.flying.demo.select;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author wupanjie
 */
public class MediaManager {
    private final String TAG = MediaManager.class.getSimpleName();
    private ContentResolver contentResolver;

    public MediaManager(Context context) {
        contentResolver = context.getContentResolver();
    }

    public List<VideoAttachment> getAllVideos() {
        List<VideoAttachment> videos = new ArrayList<>();

        String[] thumbColumns = {MediaStore.Video.Thumbnails.DATA,
            MediaStore.Video.Thumbnails.VIDEO_ID};

        String projects[] = new String[]{
            MediaStore.Video.Media._ID, MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATA, MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED, MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        };

        Cursor cursor =
            contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projects, null, null,
                MediaStore.Video.Media.DATE_MODIFIED);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor
                    .getColumnIndex(MediaStore.Video.Media._ID));
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
                Long dataAdded = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED));
                Long dataModified =
                    cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED));
                Long duration =
                    cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DURATION));

                int width = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media.WIDTH));
                int height = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT));

                Cursor thumbCursor = contentResolver.query(
                    MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI,
                    thumbColumns, null, null, null);
                String thumbnailPath = "";
                if (thumbCursor != null && thumbCursor.moveToFirst()) {
                    thumbnailPath = thumbCursor.getString(thumbCursor
                        .getColumnIndex(MediaStore.Video.Thumbnails.DATA));
                }
                if (thumbCursor != null) {
                    thumbCursor.close();
                }

                VideoAttachment attachment = new VideoAttachment(
                    id,
                    path,
                    width,
                    height,
                    duration,
                    dataAdded,
                    dataModified
                );

                videos.add(attachment);
            } while (cursor.moveToNext());
            cursor.close();
        }

        Collections.sort(videos, (lhs, rhs) -> {
            long l = lhs.getDataModified();
            long r = rhs.getDataModified();
            return Long.compare(r, l);
        });

        return videos;
    }
}
