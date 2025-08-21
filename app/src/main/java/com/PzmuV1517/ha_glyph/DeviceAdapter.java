package com.PzmuV1517.ha_glyph;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        this.selectedEntityId = id;
        notifyDataSetChanged();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        final CardView cardView;
        final TextView tvName;
        final TextView tvEntityId;
        final TextView tvState;
        final int defaultCardColor;
        final ImageView ivSelected;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_device);
            tvName = itemView.findViewById(R.id.tv_device_name);
            tvEntityId = itemView.findViewById(R.id.tv_entity_id);
            tvState = itemView.findViewById(R.id.tv_state);
            ivSelected = itemView.findViewById(R.id.iv_selected_indicator);
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
    }
}
