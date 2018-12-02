package net.silentbyte.ratedm;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.TextView;

public class EllipsisView extends TextView
{
    private int mEllipsisCount = 0;
    private static final long DELAY = 500;

    public EllipsisView(Context context)
    {
        super(context);
    }

    public EllipsisView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    private Handler mHandler = new Handler();
    private Runnable mRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            if (++mEllipsisCount > 3)
                mEllipsisCount = 0;

            StringBuilder builder = new StringBuilder(3);

            for (int i = 0; i < mEllipsisCount; i++)
            {
                builder.append(".");
            }

            setText(builder.toString());
            mHandler.postDelayed(this, DELAY);
        }
    };

    public void startEllipsisCycle()
    {
        stopEllipsisCycle();
        mHandler.postDelayed(mRunnable, DELAY);
    }

    public void stopEllipsisCycle()
    {
        mHandler.removeCallbacks(mRunnable);
        mEllipsisCount = 0;
        setText("");
    }
}
