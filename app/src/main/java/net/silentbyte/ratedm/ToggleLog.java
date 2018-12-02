package net.silentbyte.ratedm;

import android.util.Log;

public class ToggleLog
{
    // IMPORTANT: Set to false when ready to distribute.
    private static final boolean LOGGING_ENABLED = false;

    public static void d(String tag, String msg)
    {
        if (LOGGING_ENABLED)
            Log.d(tag, msg);
    }

    public static void d(String tag, String msg, Throwable tr)
    {
        if (LOGGING_ENABLED)
            Log.d(tag, msg, tr);
    }

    public static void e(String tag, String msg)
    {
        if (LOGGING_ENABLED)
            Log.e(tag, msg);
    }

    public static void e(String tag, String msg, Throwable tr)
    {
        if (LOGGING_ENABLED)
            Log.e(tag, msg, tr);
    }

    public static void i(String tag, String msg)
    {
        if (LOGGING_ENABLED)
            Log.i(tag, msg);
    }

    public static void i(String tag, String msg, Throwable tr)
    {
        if (LOGGING_ENABLED)
            Log.i(tag, msg, tr);
    }

    public static void v(String tag, String msg)
    {
        if (LOGGING_ENABLED)
            Log.v(tag, msg);
    }

    public static void v(String tag, String msg, Throwable tr)
    {
        if (LOGGING_ENABLED)
            Log.v(tag, msg, tr);
    }

    public static void w(String tag, String msg)
    {
        if (LOGGING_ENABLED)
            Log.w(tag, msg);
    }

    public static void w(String tag, String msg, Throwable tr)
    {
        if (LOGGING_ENABLED)
            Log.w(tag, msg, tr);
    }

    public static void wtf(String tag, String msg)
    {
        if (LOGGING_ENABLED)
            Log.wtf(tag, msg);
    }

    public static void wtf(String tag, String msg, Throwable tr)
    {
        if (LOGGING_ENABLED)
            Log.wtf(tag, msg, tr);
    }
}
