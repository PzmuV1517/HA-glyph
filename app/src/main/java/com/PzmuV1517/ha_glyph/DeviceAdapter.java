package com.PzmuV1517.ha_glyph;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.PzmuV1517.ha_glyph.model.HomeAssistantEntity;

import java.util.ArrayList;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {
    private List<HomeAssistantEntity> originalDevices; // all devices
    private List<HomeAssistantEntity> filteredDevices; // filtered view
    private OnDeviceClickListener listener;
    private String selectedEntityId; // currently selected
    private String lastAnimatedSelectedId; // track newly selected to animate once

    public interface OnDeviceClickListener {
        void onDeviceClick(HomeAssistantEntity entity);
    }

    public DeviceAdapter(List<HomeAssistantEntity> devices, OnDeviceClickListener listener) {
        this.originalDevices = new ArrayList<>(devices);
        this.filteredDevices = new ArrayList<>(devices);
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        HomeAssistantEntity entity = filteredDevices.get(position);
        boolean selected = selectedEntityId != null && selectedEntityId.equals(entity.getEntityId());
        holder.bind(entity, listener, selected);
        if (selected && lastAnimatedSelectedId != null && lastAnimatedSelectedId.equals(entity.getEntityId())) {
            holder.playSelectionAnimation();
        }
    }

    @Override
    public int getItemCount() {
        return filteredDevices.size();
    }

    public void updateDevices(List<HomeAssistantEntity> newDevices) {
        this.originalDevices = new ArrayList<>(newDevices);
        this.filteredDevices = new ArrayList<>(newDevices);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        filteredDevices.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredDevices.addAll(originalDevices);
        } else {
            String q = query.toLowerCase();
            for (HomeAssistantEntity e : originalDevices) {
                if (e.getFriendlyName().toLowerCase().contains(q) || e.getEntityId().toLowerCase().contains(q)) {
                    filteredDevices.add(e);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void setSelectedEntityId(String id) {
        if (id == null || (selectedEntityId != null && selectedEntityId.equals(id))) {
            selectedEntityId = id;
            lastAnimatedSelectedId = null;
        } else {
            selectedEntityId = id;
            lastAnimatedSelectedId = id; // animate this selection
        }
        notifyDataSetChanged();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        final CardView cardView;
        final TextView tvName;
        final TextView tvEntityId;
        final TextView tvState;
        final int defaultCardColor;
        final ImageView ivSelected;
        final FrameLayout rootContainer;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_device);
            tvName = itemView.findViewById(R.id.tv_device_name);
            tvEntityId = itemView.findViewById(R.id.tv_entity_id);
            tvState = itemView.findViewById(R.id.tv_state);
            ivSelected = itemView.findViewById(R.id.iv_selected_indicator);
            rootContainer = itemView.findViewById(R.id.root_container);
            defaultCardColor = cardView.getCardBackgroundColor().getDefaultColor();
        }

        public void bind(HomeAssistantEntity entity, OnDeviceClickListener listener, boolean selected) {
            tvName.setText(entity.getFriendlyName());
            tvEntityId.setText(entity.getEntityId());
            tvState.setText(entity.getState().toUpperCase());
            ivSelected.setVisibility(selected ? View.VISIBLE : View.GONE);
            // restore default background (in case previously tinted)
            cardView.setCardBackgroundColor(defaultCardColor);

            if (entity.isOn()) {
                tvState.setTextColor(itemView.getContext().getColor(android.R.color.holo_green_dark));
            } else {
                tvState.setTextColor(itemView.getContext().getColor(android.R.color.holo_red_dark));
            }

            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeviceClick(entity);
                }
            });
        }

        void playSelectionAnimation() {
            // Animate the green dot popping in
            ivSelected.setScaleX(0f);
            ivSelected.setScaleY(0f);
            ivSelected.setAlpha(0f);
            ivSelected.setVisibility(View.VISIBLE);
            AnimatorSet popSet = new AnimatorSet();
            ObjectAnimator sx = ObjectAnimator.ofFloat(ivSelected, View.SCALE_X, 0f, 1f);
            ObjectAnimator sy = ObjectAnimator.ofFloat(ivSelected, View.SCALE_Y, 0f, 1f);
            ObjectAnimator a = ObjectAnimator.ofFloat(ivSelected, View.ALPHA, 0f, 1f);
            sx.setInterpolator(new OvershootInterpolator());
            sy.setInterpolator(new OvershootInterpolator());
            popSet.setDuration(300);
            popSet.playTogether(sx, sy, a);
            popSet.start();

            // Particle explosion
            int particleCount = 10;
            float maxRadius = dp(36);
            for (int i = 0; i < particleCount; i++) {
                final View particle = new View(itemView.getContext());
                int size = (int) dp(6);
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size, Gravity.END | Gravity.BOTTOM);
                lp.setMargins(0,0, (int) dp(12), (int) dp(12)); // start near dot
                particle.setLayoutParams(lp);
                particle.setBackground(createParticleBackground());
                particle.setAlpha(0f);
                rootContainer.addView(particle);

                double angle = (2 * Math.PI / particleCount) * i + Math.random()*0.5; // slight randomness
                float targetX = (float) (Math.cos(angle) * maxRadius);
                float targetY = (float) (Math.sin(angle) * maxRadius);

                ObjectAnimator tx = ObjectAnimator.ofFloat(particle, View.TRANSLATION_X, 0f, -targetX); // negative because Gravity.END
                ObjectAnimator ty = ObjectAnimator.ofFloat(particle, View.TRANSLATION_Y, 0f, -targetY); // negative because Gravity.BOTTOM
                ObjectAnimator psx = ObjectAnimator.ofFloat(particle, View.SCALE_X, 1f, 0.2f);
                ObjectAnimator psy = ObjectAnimator.ofFloat(particle, View.SCALE_Y, 1f, 0.2f);
                ObjectAnimator pa = ObjectAnimator.ofFloat(particle, View.ALPHA, 0f, 1f, 0f);
                AnimatorSet set = new AnimatorSet();
                set.playTogether(tx, ty, psx, psy, pa);
                set.setInterpolator(new AccelerateInterpolator());
                set.setStartDelay(i * 15L);
                set.setDuration(450);
                set.addListener(new AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(Animator animation) { rootContainer.removeView(particle); }
                });
                set.start();
            }
        }

        private float dp(float v) {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, itemView.getResources().getDisplayMetrics());
        }

        private android.graphics.drawable.Drawable createParticleBackground() {
            android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
            d.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            d.setColor(0xFF4CAF50); // green
            d.setStroke((int) dp(1), 0xFF2E7D32);
            return d;
        }
    }
}
