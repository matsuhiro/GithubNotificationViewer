package net.matsuhiro.github.notificationviewer;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;


import net.matsuhiro.github.notificationviewer.entity.NotificationEntity;

import java.util.List;

public class NotificationRecyclerViewAdapter extends RecyclerView.Adapter<NotificationRecyclerViewAdapter.ViewHolder> {

    private List<NotificationEntity> mValues;
    private final OnListInteractionListener mListener;
    private boolean mEnabledClick = true;
    private Interpolator mInterpolator = new LinearInterpolator();

    public NotificationRecyclerViewAdapter(List<NotificationEntity> items, OnListInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    public void setItemsAndNotify(List<NotificationEntity> items) {
        mValues = items;
        notifyDataSetChanged();
    }

    public void enableClick(boolean enable) {
        mEnabledClick = enable;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.notification_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);

        String title = "Title: " + mValues.get(position).subject.title;
        String name = "Repository name: " + mValues.get(position).repository.name;
        String lastUpdateAt = "Last update at: " + mValues.get(position).lastUpdateAt;

        holder.mTitleView.setText(title);
        holder.mNameView.setText(name);
        holder.mLastUpdateAtView.setText(lastUpdateAt);

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener && mEnabledClick) {
                    mListener.onListFragmentInteraction(holder.mItem);
                }
            }
        });
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(holder.mView, "scaleX", .5f, 1.f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(holder.mView, "scaleY", .5f, 1.f);
        Animator[] anims = new ObjectAnimator[] { scaleX, scaleY };
        for (Animator anim : anims) {
            anim.setDuration(300).start();
            anim.setInterpolator(mInterpolator);
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mTitleView;
        public final TextView mNameView;
        public final TextView mLastUpdateAtView;
        public NotificationEntity mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mNameView = (TextView) view.findViewById(R.id.name);
            mTitleView = (TextView) view.findViewById(R.id.title);
            mLastUpdateAtView = (TextView) view.findViewById(R.id.last_update_at);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mTitleView.getText() + "'";
        }
    }

    public interface OnListInteractionListener {
        void onListFragmentInteraction(NotificationEntity item);
    }
}
