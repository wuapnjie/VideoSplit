package com.xiaopo.flying.demo.select;

/**
 * @author wupanjie
 */
public class VideoAttachment {
    private int id;
    private String path;
    private int width;
    private int height;
    private long duration;
    private long dataAdded;
    private long dataModified;

    private boolean selected;

    public VideoAttachment(int id, String path, int width, int height, long duration, long dataAdded, long dataModified) {
        this.id = id;
        this.path = path;
        this.width = width;
        this.height = height;
        this.duration = duration;
        this.dataAdded = dataAdded;
        this.dataModified = dataModified;
    }

    public int getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public long getDuration() {
        return duration;
    }

    public long getDataAdded() {
        return dataAdded;
    }

    public long getDataModified() {
        return dataModified;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

}
