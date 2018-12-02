package net.silentbyte.ratedm.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import net.silentbyte.ratedm.Match;
import net.silentbyte.ratedm.R;
import net.silentbyte.ratedm.ToggleLog;
import net.silentbyte.ratedm.User;
import net.silentbyte.ratedm.activities.GameActivity;
import net.silentbyte.ratedm.activities.MatchesActivity;
import net.silentbyte.ratedm.activities.SettingsActivity;
import net.silentbyte.ratedm.activities.TitleActivity;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class NotificationService extends FirebaseMessagingService
{
    private static final String TAG = "NotificationService";
    public static final String KEY_NOTIFICATION = "notification";
    public static final String KEY_NOTIFICATION_IDS = "notification_ids";
    public static final String KEY_MULTIPLE_NOTIFICATIONS = "multiple_notifications";

    private static CallbackHandler mCallbackHandler;
    private static String mMatchIdToClear;

    public static CallbackHandler getCallbackHandler()
    {
        return mCallbackHandler;
    }

    public static void setCallbackHandler(CallbackHandler callbackHandler)
    {
        mCallbackHandler = callbackHandler;
    }

    public static String getMatchIdToClear()
    {
        return mMatchIdToClear;
    }

    public static void setMatchIdToClear(String matchIdToClear)
    {
        mMatchIdToClear = matchIdToClear;
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage)
    {
        Map<String, String> data = remoteMessage.getData();

        if (!data.containsKey("matchId")) // Global notification
        {
            notify(data);
            return;
        }

        User user = User.get(getApplicationContext());

        // If we are not logged in, or the message is not targeted at the logged in user, return.
        if (user == null || !user.getId().equals(data.get("userId")))
            return;

        String matchId = data.get("matchId");
        String currentMatchId = GameActivity.getMatchId(); // TODO: Is this a good way to get match id? What if a configuration change occurs while in GameActivity? The matchId may briefly be null.

        if (data.containsKey(KEY_NOTIFICATION))
        {
            if (mCallbackHandler == null || !matchId.equals(currentMatchId))
                notify(data);
            else if (mCallbackHandler.isPaused())
                notify(data);
        }

        if (mCallbackHandler != null && (mCallbackHandler instanceof MatchesActivity || matchId.equals(currentMatchId)))
            mCallbackHandler.onMatchUpdate();
    }

    private void notify(Map<String, String> data)
    {
        Context context = getApplicationContext(); // TODO: Is this the right context?
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (prefs.getBoolean(SettingsActivity.KEY_NOTIFICATIONS_ENABLED, true))
        {
            Intent contentIntent = new Intent(this, TitleActivity.class);
            contentIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

            String notificationText = data.get(KEY_NOTIFICATION);
            String appName = context.getString(R.string.app_name);
            int notificationId = 0;

            if (data.containsKey("matchId"))
            {
                String matchId = data.get("matchId");
                Set<String> notificationIds = prefs.getStringSet(KEY_NOTIFICATION_IDS, new HashSet<String>());
                Set<String> newNotificationIds = new HashSet<>(notificationIds);

                contentIntent.putExtra(Match.KEY_MATCH_ID, matchId);

                if (!newNotificationIds.contains(matchId))
                {
                    newNotificationIds.add(matchId);
                    prefs.edit().putStringSet(KEY_NOTIFICATION_IDS, newNotificationIds).commit();
                }

                if (newNotificationIds.size() > 1)
                {
                    mMatchIdToClear = null;
                    contentIntent.putExtra(KEY_MULTIPLE_NOTIFICATIONS, true);
                    notificationText = context.getString(R.string.multiple_notifications);
                }
                else
                    mMatchIdToClear = matchId;
            }
            else
            {
                notificationId = 1;
                contentIntent.putExtra(KEY_NOTIFICATION, notificationText);
            }

            Random random = new Random();
            int contentIntentRequestCode = random.nextInt();
            int deleteIntentRequestCode = random.nextInt();

            Intent deleteIntent = new Intent(context, NotificationBroadcastReceiver.class);
            deleteIntent.setAction("notification_dismissed");

            PendingIntent pContentIntent = PendingIntent.getActivity(context, contentIntentRequestCode, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent pDeleteIntent = PendingIntent.getBroadcast(context, deleteIntentRequestCode, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentTitle(appName)
                .setContentText(notificationText)
                .setTicker(appName + ": " + notificationText)
                .setSmallIcon(R.drawable.push_icon)
                .setContentIntent(pContentIntent)
                .setAutoCancel(true);

            if (notificationId == 0)
                builder.setDeleteIntent(pDeleteIntent);

            Notification notification = builder.build();

            if (prefs.getBoolean(SettingsActivity.KEY_NOTIFICATION_SOUND, true))
                notification.defaults |= Notification.DEFAULT_SOUND;

            if (prefs.getBoolean(SettingsActivity.KEY_NOTIFICATION_VIBRATION, true))
                notification.defaults |= Notification.DEFAULT_VIBRATE;

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(notificationId, notification);
        }
    }

    public interface CallbackHandler
    {
        public boolean isPaused();
        public void onMatchUpdate();
    }
}
