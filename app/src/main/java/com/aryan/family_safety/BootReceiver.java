package com.aryan.family_safety;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.core.content.ContextCompat;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        // Check if this is a boot event
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            // Check if user had sharing ON before reboot
            SharedPreferences prefs = context.getSharedPreferences(
                    "FamilySafety", Context.MODE_PRIVATE);
            boolean wasSharing = prefs.getBoolean("isSharing", false);

            // If was sharing before reboot, restart service
            if (wasSharing) {
                Intent serviceIntent = new Intent(
                        context, LocationService.class);
                ContextCompat.startForegroundService(
                        context, serviceIntent);
            }
        }
    }
}
