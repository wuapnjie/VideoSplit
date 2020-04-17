package com.xiaopo.flying.demo.select;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.xiaopo.flying.demo.R;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import me.drakeet.multitype.ItemViewBinder;

/**
 * @author wupanjie
 */

public class VideoAttachmentBinder extends ItemViewBinder<VideoAttachment, VideoAttachmentBinder.ViewHolder> {

    public static final int SELECT_MODE_MULTI = 0;
    public static final int SELECT_MODE_SINGLE = 1;

    private final Set<Integer> selectedPositions;
    private OnSelectedListener<VideoAttachment> onVideoAttachmentSelectedListener;
    private OnSelectedMaxCountListener onVideoAttachmentMaxCountListener;
    private int selectedPosition;
    private int maxCount;
    private final int width;
    private final int height;

    private RecyclerView recyclerView;
    private List<VideoAttachment> photos;

    private int selectMode;

    public VideoAttachmentBinder(Set<Integer> selectedPositions, int maxCount, int width, int height) {
        this.selectedPositions = selectedPositions;
        this.maxCount = maxCount;
        this.width = width;
        this.height = height;
        selectMode = SELECT_MODE_MULTI;
    }

    public VideoAttachmentBinder(RecyclerView recyclerView, List<VideoAttachment> photos, int width, int height) {
        this(Collections.emptySet(), 1, width, height);
        this.recyclerView = recyclerView;
        this.photos = photos;
        selectMode = SELECT_MODE_SINGLE;
    }

    public void setSelectMode(int selectMode) {
        this.selectMode = selectMode;
    }

    @NonNull
    @Override
    protected VideoAttachmentBinder.ViewHolder onCreateViewHolder(@NonNull LayoutInflater inflater,
                                                                  @NonNull ViewGroup parent) {
        View itemView = inflater.inflate(R.layout.item_photo, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @NonNull VideoAttachment item) {

        ViewGroup.LayoutParams layoutParams = holder.photoContainer.getLayoutParams();
        layoutParams.width = width;
        layoutParams.height = height;
        holder.photoContainer.setLayoutParams(layoutParams);

        Glide.with(holder.itemView.getContext())
            .load(item.getPath())
            .into(holder.ivVideoAttachment);

        holder.itemView.setOnClickListener(getClickListener(holder, item));

        if (item.isSelected()) {
            holder.shadow.setVisibility(View.VISIBLE);
        } else {
            holder.shadow.setVisibility(View.GONE);
        }
    }

    private View.OnClickListener getClickListener(@NonNull ViewHolder holder, @NonNull VideoAttachment item) {
        if (selectMode == SELECT_MODE_MULTI) {
            return view -> {
                if (item.isSelected()) {
                    holder.shadow.setVisibility(View.GONE);
                    item.setSelected(false);
                    selectedPositions.remove(holder.getAdapterPosition());

                    if (onVideoAttachmentSelectedListener != null) {
                        onVideoAttachmentSelectedListener.onSelected(item, holder.getAdapterPosition());
                    }
                } else {
                    if (selectedPositions.size() >= maxCount) {
                        if (onVideoAttachmentMaxCountListener != null) {
                            onVideoAttachmentMaxCountListener.onSelectedMaxCount();
                        }
                    } else {
                        holder.shadow.setVisibility(View.VISIBLE);
                        item.setSelected(true);
                        selectedPositions.add(holder.getAdapterPosition());

                        if (onVideoAttachmentSelectedListener != null) {
                            onVideoAttachmentSelectedListener.onSelected(item, holder.getAdapterPosition());
                        }
                    }
                }
            };
        } else if (selectMode == SELECT_MODE_SINGLE) {
            return view -> {
                photos.get(selectedPosition).setSelected(false);

                ViewHolder viewHolder =
                    (ViewHolder) recyclerView.findViewHolderForLayoutPosition(selectedPosition);

                if (viewHolder != null) {
                    viewHolder.shadow.setVisibility(View.GONE);
                } else {
                    getAdapter().notifyItemChanged(selectedPosition);
                }

                holder.shadow.setVisibility(View.VISIBLE);
                item.setSelected(true);
                selectedPosition = getPosition(holder);

                if (onVideoAttachmentSelectedListener != null) {
                    onVideoAttachmentSelectedListener.onSelected(item, holder.getAdapterPosition());
                }
            };
        }

        return null;
    }

    @Override
    protected void onBindViewHolder(
        @NonNull ViewHolder holder, @NonNull VideoAttachment item, @NonNull List<Object> payloads) {
        super.onBindViewHolder(holder, item, payloads);
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, item);
        } else {
            if (item.isSelected()) {
                holder.shadow.setVisibility(View.VISIBLE);
            } else {
                holder.shadow.setVisibility(View.GONE);
            }
        }
    }

    public void setOnVideoAttachmentSelectedListener(OnSelectedListener<VideoAttachment> onVideoAttachmentSelectedListener) {
        this.onVideoAttachmentSelectedListener = onVideoAttachmentSelectedListener;
    }

    public void setOnVideoAttachmentMaxCountListener(OnSelectedMaxCountListener onVideoAttachmentMaxCountListener) {
        this.onVideoAttachmentMaxCountListener = onVideoAttachmentMaxCountListener;
    }

    public void setSelectedPosition(int selectPosition) {
        this.selectedPosition = selectPosition;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivVideoAttachment;
        View shadow;
        FrameLayout photoContainer;

        ViewHolder(View itemView) {
            super(itemView);
            ivVideoAttachment = itemView.findViewById(R.id.iv_photo);
            shadow = itemView.findViewById(R.id.shadow);
            photoContainer = itemView.findViewById(R.id.photo_container);
        }
    }
}
