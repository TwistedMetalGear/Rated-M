package net.silentbyte.ratedm;

import android.app.Application;

import com.facebook.FacebookSdk;

public class RatedMApplication extends Application
{
    private static final String TAG = "RatedMApplication";

    // The request code offset that Facebook activities will be called with. Please do not use the range between the value you set and another 100 entries after it in your other requests.
    public static final int RC_FACEBOOK = 1000;

    @Override
    public void onCreate()
    {
        super.onCreate();
        FacebookSdk.sdkInitialize(getApplicationContext());
    }
}
