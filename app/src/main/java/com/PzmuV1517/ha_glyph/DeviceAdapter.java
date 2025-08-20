package com.PzmuV1517.ha_glyph;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.PzmuV1517.ha_glyph.model.HomeAssistantEntity;

import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {
    private List<HomeAssistantEntity> devices;
    private OnDeviceClickListener listener;

    public interface OnDeviceClickListener {
        void onDeviceClick(HomeAssistantEntity entity);
    }

    public DeviceAdapter(List<HomeAssistantEntity> devices, OnDeviceClickListener listener) {
        this.devices = devices;
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
        HomeAssistantEntity entity = devices.get(position);
        holder.bind(entity, listener);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public void updateDevices(List<HomeAssistantEntity> newDevices) {
        this.devices = newDevices;
        notifyDataSetChanged();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        private final CardView cardView;
        private final TextView tvName;
        private final TextView tvEntityId;
        private final TextView tvState;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_device);
            tvName = itemView.findViewById(R.id.tv_device_name);
            tvEntityId = itemView.findViewById(R.id.tv_entity_id);
            tvState = itemView.findViewById(R.id.tv_state);
        }

        public void bind(HomeAssistantEntity entity, OnDeviceClickListener listener) {
            tvName.setText(entity.getFriendlyName());
            tvEntityId.setText(entity.getEntityId());
            tvState.setText(entity.getState().toUpperCase());

            // Color code the state
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
