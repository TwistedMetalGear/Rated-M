package net.silentbyte.ratedm.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class NotificationBroadcastReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        // Clear notification ids.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (prefs.contains(NotificationService.KEY_NOTIFICATION_IDS))
            prefs.edit().remove(NotificationService.KEY_NOTIFICATION_IDS).commit();
    }
}
