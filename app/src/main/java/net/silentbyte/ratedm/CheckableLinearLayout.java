package net.silentbyte.ratedm;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

public class CheckableLinearLayout extends LinearLayout implements Checkable
{
    public boolean isChecked;
    private List<Checkable> checkableViews;

    public CheckableLinearLayout(Context context)
    {
        super(context);
        initialize();
    }

    public CheckableLinearLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initialize();
    }

    public CheckableLinearLayout(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize()
    {
        isChecked = false;
        checkableViews = new ArrayList<Checkable>();
    }

    public boolean isChecked()
    {
        return isChecked;
    }


    public void setChecked(boolean isChecked)
    {
        this.isChecked = isChecked;

        for (Checkable c : checkableViews)
        {
            c.setChecked(isChecked);
        }
    }

    public void toggle()
    {
        isChecked = !isChecked;

        for (Checkable c : checkableViews)
        {
            c.toggle();
        }
    }

    @Override
    protected void onFinishInflate()
    {
        super.onFinishInflate();

        final int childCount = getChildCount();

        for (int i = 0; i < childCount; ++i)
        {
            findCheckableChildren(getChildAt(i));
        }
    }

    private void findCheckableChildren(View v)
    {
        if (v instanceof Checkable)
            checkableViews.add((Checkable) v);

        if (v instanceof ViewGroup)
        {
            ViewGroup vg = (ViewGroup) v;
            int childCount = vg.getChildCount();

            for (int i = 0; i < childCount; ++i)
            {
                findCheckableChildren(vg.getChildAt(i));
            }
        }
    }
}
