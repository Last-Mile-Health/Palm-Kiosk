package org.lastmilehealth.kiosk;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Thamizhan on 28/02/17.
 */

public class OrderingAdapter extends RecyclerView.Adapter<OrderingAdapter.AppViewHolder> {

    public List<AppDetail> alowedApps = new ArrayList<>();

    public OrderingAdapter(List<AppDetail> alowedApps) {
        this.alowedApps = alowedApps;
    }

    @Override
    public AppViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_allowed_apps, parent, false);
        return new AppViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(AppViewHolder holder, int position) {
        AppDetail itemAppDetail = alowedApps.get(position);
        holder.tvAppName.setText(itemAppDetail.label);
        holder.ivAppIcon.setImageDrawable(itemAppDetail.icon);
    }

    @Override
    public int getItemCount() {
        return this.alowedApps.size();
    }

    public class AppViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivAppIcon;
        private TextView tvAppName;
        private View itemView;

        public AppViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            ivAppIcon = (ImageView) itemView.findViewById(R.id.item_app_icon);
            tvAppName = (TextView) itemView.findViewById(R.id.item_app_label);
        }

        public void setSelected() {
            Animation shakeAnimation = AnimationUtils.loadAnimation(itemView.getContext(), R.anim.shake);
            ivAppIcon.startAnimation(shakeAnimation);
        }

        public void clearSelection() {
            ivAppIcon.clearAnimation();
        }
    }
}
