package net.silentbyte.ratedm.activities;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import net.silentbyte.ratedm.R;

public class SettingsActivity extends PreferenceActivity
{
    public static final String KEY_AUTO_UPDATE = "auto_update";
    public static final String KEY_SHOW_MATCHES = "show_matches";
    public static final String KEY_DISMISS_KEYBOARD = "dismiss_keyboard";
    public static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    public static final String KEY_NOTIFICATION_SOUND = "notification_sound";
    public static final String KEY_NOTIFICATION_VIBRATION = "notification_vibration";
    public static final String KEY_TTS_ENABLED = "tts_enabled";
    public static final String KEY_TTS_READ_WHITE_ONLY = "tts_read_white_only";
    public static final String KEY_TTS_READ_ONCE = "tts_read_once";
    public static final String KEY_PLAYER_LIST_NUM_ROWS = "player_list_num_rows";
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Lock the device in portrait mode.
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        // Deprecated but there doesn't seem to be another choice when it comes to supporting older devices.
        addPreferencesFromResource(R.xml.preferences);
    }
}
