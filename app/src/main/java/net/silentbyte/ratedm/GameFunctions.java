package net.silentbyte.ratedm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class GameFunctions
{
    private static final String TAG = "GameFunctions";

    public static int convertLocalToEstTime(int hourOfDay)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("H");
        sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));

        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);

        return Integer.valueOf(sdf.format(calendar.getTime()));
    }

    public static int convertEstToLocalTime(int hourOfDay)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("H");

        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.setTimeZone(TimeZone.getTimeZone("America/New_York"));

        return Integer.valueOf(sdf.format(calendar.getTime()));
    }

    public static Animation createAnimation(int fromX, int toX, int duration, int offset)
    {
        Animation anim = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, fromX, Animation.RELATIVE_TO_PARENT, toX, 0, 0, 0, 0);
        anim.setDuration(duration);
        anim.setStartOffset(offset);
        anim.setFillAfter(true);
        return anim;
    }

    public static Dialog showDialog(Context context, String title, String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.show();

        return dialog;
    }

    public static void hideKeyboard(Activity activity)
    {
        View v = activity.getCurrentFocus();

        if (v != null)
        {
            InputMethodManager imm = (InputMethodManager)(activity.getSystemService(Context.INPUT_METHOD_SERVICE));
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }
}
