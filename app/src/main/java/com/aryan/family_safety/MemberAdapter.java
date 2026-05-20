package com.aryan.family_safety;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MemberAdapter extends
        RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    Context context;
    List<DeviceModel> deviceList;

    public MemberAdapter(Context context, List<DeviceModel> deviceList) {
        this.context    = context;
        this.deviceList = deviceList;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull MemberViewHolder holder, int position) {
        DeviceModel device = deviceList.get(position);

        // Set initial letter
        holder.tvInitial.setText(
                device.nickname.substring(0, 1).toUpperCase());

        // Set name
        holder.tvName.setText(device.nickname);

        // Set address
        holder.tvAddress.setText(device.address);

        // Set battery
        holder.tvBattery.setText(device.battery + "%");

        // Set last seen
        holder.tvLastSeen.setText(getTimeAgo(device.lastSeen));

        // Set status
        long diff = System.currentTimeMillis() - device.lastSeen;
        if (diff < 5 * 60 * 1000) {
            holder.tvStatus.setText("Online");
            holder.tvStatus.setTextColor(
                    Color.parseColor("#2E7D32"));
            holder.tvStatus.setBackgroundResource(
                    R.drawable.badge_green);
        } else {
            holder.tvStatus.setText("Offline");
            holder.tvStatus.setTextColor(
                    Color.parseColor("#757575"));
            holder.tvStatus.setBackgroundResource(
                    R.drawable.badge_grey);
        }

        // Set avatar color
        try {
            holder.viewColor.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.parseColor(device.color)));
        } catch (Exception e) {
            holder.viewColor.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.parseColor("#43A047")));
        }

        // Click → open PersonDetailActivity
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(
                    context, PersonDetailActivity.class);
            intent.putExtra("deviceId",  device.deviceId);
            intent.putExtra("nickname",  device.nickname);
            intent.putExtra("color",     device.color);
            intent.putExtra("latitude",  device.latitude);
            intent.putExtra("longitude", device.longitude);
            intent.putExtra("address",   device.address);
            intent.putExtra("battery",   device.battery);
            intent.putExtra("lastSeen",  device.lastSeen);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    private String getTimeAgo(long timestamp) {
        if (timestamp == 0) return "Never";
        long diff    = System.currentTimeMillis() - timestamp;
        long minutes = diff / 60000;
        long hours   = minutes / 60;
        if (minutes < 1)  return "Just now";
        if (minutes < 60) return minutes + " mins ago";
        if (hours < 24)   return hours + " hrs ago";
        return "Over a day ago";
    }

    public static class MemberViewHolder
            extends RecyclerView.ViewHolder {
        View viewColor;
        TextView tvInitial, tvName, tvAddress,
                tvLastSeen, tvBattery, tvStatus;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            viewColor    = itemView.findViewById(R.id.viewMemberColor);
            tvInitial    = itemView.findViewById(R.id.tvMemberInitial);
            tvName       = itemView.findViewById(R.id.tvMemberName);
            tvAddress    = itemView.findViewById(R.id.tvMemberAddress);
            tvLastSeen   = itemView.findViewById(R.id.tvMemberLastSeen);
            tvBattery    = itemView.findViewById(R.id.tvMemberBattery);
            tvStatus     = itemView.findViewById(R.id.tvMemberStatus);
        }
    }
}
