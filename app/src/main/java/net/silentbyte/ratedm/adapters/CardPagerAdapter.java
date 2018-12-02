package net.silentbyte.ratedm.adapters;

import android.graphics.Point;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;

import net.silentbyte.ratedm.CardViewPager;
import net.silentbyte.ratedm.GameConstants;
import net.silentbyte.ratedm.GameFunctions;
import net.silentbyte.ratedm.R;
import net.silentbyte.ratedm.ToggleLog;
import net.silentbyte.ratedm.activities.GameActivity;
import net.silentbyte.ratedm.fragments.CardFragment;
import net.silentbyte.ratedm.fragments.PickTwoCardFragment;

public class CardPagerAdapter extends FragmentPagerAdapter implements CardViewPager.OnPageChangeListener, CardViewPager.PageTransformer
{
    private static final String TAG = "CardPagerAdapter";

	private View cur = null;
	private View next = null;
	private GameActivity mActivity;
	private FragmentManager fm;
    private int mCount;
    private int mCardYPos;
	private CardViewPager mCardPager;

	public CardPagerAdapter(GameActivity activity, FragmentManager fm, CardViewPager pager, int count)
    {
		super(fm);
        mActivity = activity;
		this.fm = fm;
		mCardPager = pager;
        mCount = count;

        // Calculate page margins.
        TypedValue value = new TypedValue();
        mActivity.getResources().getValue(R.dimen.card_weight, value, true);

        Display display = mActivity.getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);

        float cardWeight = value.getFloat();
        float totalWeight = cardWeight + 2;
        float multiplier = cardWeight / totalWeight;
        float cardWidth = point.x * multiplier;

        if (mActivity.shouldShowPickTwoCards())
            cardWidth = cardWidth * 1.425f;

        float sideMargin = (point.x - cardWidth) / 2;
        float pageMargin = -(sideMargin * 2) - sideMargin - (sideMargin / 10);

        mCardPager.setPageMargin(Math.round(pageMargin));

        // Calculate the Y position of the outer cards.
        float cardMarginTop = mActivity.getResources().getDimension(R.dimen.card_margin_top);

        if (mActivity.shouldShowPickTwoCards())
            cardMarginTop = cardMarginTop * 1.9f;

        float density = mActivity.getResources().getDisplayMetrics().density;

        if (density == 3.5)
            cardMarginTop = cardMarginTop * 1.1f;

        mCardYPos = Math.round(cardMarginTop + (cardMarginTop / 2) * 1.2f);
	}

	@Override
	public Fragment getItem(int position)
	{
        Fragment fragment;

        if (mActivity.shouldShowPickTwoCards())
            fragment = PickTwoCardFragment.newInstance(mActivity, position);
        else
            fragment = CardFragment.newInstance(mActivity, position);

		cur = getRootView(position);
		next = getRootView(position + 1);

		return fragment;
	}

	@Override
	public int getCount()
	{
		return mCount;
	}

	//@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
	{
        // Empty implementation. Transformation is now done in the transformPage method.
	}

    public void transformPage(int position, float positionOffset, int positionOffsetPixels)
	{
	    if (positionOffset >= 0f && positionOffset <= 1f)
        {
            positionOffset = positionOffset * positionOffset;
            cur = getRootView(position);
            next = getRootView(position + 1);

            if (next != null)
            {
                next.setRotation(GameConstants.CARD_ROTATION - GameConstants.CARD_ROTATION * positionOffset);
                // NOTE: The 400 here means set the next card 100 pixels lower than the current card which is at 300.
                // 530 - 100*positionOffset
                next.setY(mCardYPos - mCardYPos * positionOffset);
            }

            if (cur != null)
            {
                cur.setRotation(-GameConstants.CARD_ROTATION * positionOffset);
                // NOTE: The 300 here is # of pixels from the top of the screen.
                // 430 + 100*positionOffset
                cur.setY(mCardYPos * positionOffset);
            }
        }
    }

    @Override
	public void onPageSelected(int position)
	{
        // Dismiss keyboard.
        GameFunctions.hideKeyboard(mActivity);

        // Set current card position so that we can restore it later.
        mActivity.setCurrentCardPosition(position);

        // Speak the card.
        mActivity.speak();
	}

    @Override
	public void onPageScrollStateChanged(int state)
	{
	    if (state == ViewPager.SCROLL_STATE_IDLE)
	        resetPagePositions();
	}

	// Called by the ViewPager after the pages have been laid out.
	public void onDoneInitializing()
    {
        // mActivity.refreshUI();
        // mActivity.syncRoundNum();
        resetPagePositions();
        onPageSelected(mCardPager.getCurrentItem());
    }

	public void resetPagePositions()
	{
	    int position = mCardPager.getCurrentItem();

        for (int i = 0; i < getCount(); i++)
        {
            View v = getRootView(i);

            if (v != null)
            {
                if (position < i)
                {
                    v.setRotation(GameConstants.CARD_ROTATION);
                    v.setY(mCardYPos);
                }
                else if (position > i)
                {
                    v.setRotation(-GameConstants.CARD_ROTATION);
                    v.setY(mCardYPos);
                }
                else
                {
                    v.setRotation(0);
                    v.setY(0);
                }
            }
        }
	}

    public void removeAllFragments(int count)
    {
        for (int i = count - 1; i >= 0; i--)
        {
            Fragment fragment = fm.findFragmentByTag(getFragmentTag(i));

            if (fragment != null)
                fm.beginTransaction().remove(fragment).commitAllowingStateLoss();
        }
    }

    private View getRootView(int position)
    {
        View ly;

        try
        {
            ly = fm.findFragmentByTag(this.getFragmentTag(position)).getView().findViewById(R.id.root);
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            return null;
        }

        return ly;
    }

	private String getFragmentTag(int position)
	{
		return "android:switcher:" + mCardPager.getId() + ":" + position;
	}
}
