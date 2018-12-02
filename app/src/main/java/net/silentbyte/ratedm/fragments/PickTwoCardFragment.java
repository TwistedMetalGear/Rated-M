package net.silentbyte.ratedm.fragments;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import net.silentbyte.ratedm.Card;
import net.silentbyte.ratedm.Match;
import net.silentbyte.ratedm.R;
import net.silentbyte.ratedm.Round;
import net.silentbyte.ratedm.ToggleLog;
import net.silentbyte.ratedm.activities.GameActivity;
import net.silentbyte.ratedm.adapters.ReactionAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class PickTwoCardFragment extends Fragment
{
    private static final String TAG = "PickTwoCardFragment";
    private static final String KEY_POSITION = "position";

    public static Fragment newInstance(Context context, int position)
    {
        Bundle b = new Bundle();
        b.putInt(KEY_POSITION, position);
        return Fragment.instantiate(context, PickTwoCardFragment.class.getName(), b);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        if (container == null)
            return null;

        float density = getResources().getDisplayMetrics().density;

        if (density == 3.5)
            return inflater.inflate(R.layout.fragment_card_pick_two_3_5, container, false);
        else
            return inflater.inflate(R.layout.fragment_card_pick_two, container, false);
    }

    // Refreshing view here instead of onCreateView.
    // This is Because the EditText has a bug where it's unable to refresh in onCreateView.
    @Override
    public void onStart()
    {
        super.onStart();

        final GameActivity activity = (GameActivity)getActivity();
        Match match = activity.getMatch();
        Round round = match.getRound(activity.getRoundToShow());
        int position = getArguments().getInt("position");

        // Checking position because there are certain situations where
        // it can get out of range. For instance, performing a config
        // change immediately after submitting a card.
        if (position >= activity.getCards().size() / 2)
        {
            ToggleLog.d(TAG, "onStart: Returning because position is out of range.");
            return;
        }

        View v = getView();
        final Card cardLeft = activity.getCards().get(position * 2);
        final Card cardRight = activity.getCards().get(position * 2 + 1);

        final View cardLeftView = v.findViewById(R.id.card_left);
        final View cardRightView = v.findViewById(R.id.card_right);
        View cardLeftTouchArea = v.findViewById(R.id.card_left_touch_area);
        View cardRightTouchArea = v.findViewById(R.id.card_right_touch_area);
        TextView cardTextLeft = (TextView)v.findViewById(R.id.card_text_left);
        TextView cardTextRight = (TextView)v.findViewById(R.id.card_text_right);
        View imagePanelLeft =  v.findViewById(R.id.image_panel_left);
        View imagePanelRight = v.findViewById(R.id.image_panel_right);
        View progressBarLeft = v.findViewById(R.id.progress_bar_left);
        View progressBarRight = v.findViewById(R.id.progress_bar_right);
        View imageContainerLeft = v.findViewById(R.id.image_container_left);
        View imageContainerRight = v.findViewById(R.id.image_container_right);
        ImageView imageViewLeft = (ImageView)v.findViewById(R.id.image_left);
        ImageView imageViewRight = (ImageView)v.findViewById(R.id.image_right);
        View errorViewLeft = v.findViewById(R.id.error_left);
        View errorViewRight = v.findViewById(R.id.error_right);
        TextView submittedByLeft = (TextView)v.findViewById(R.id.submitted_by_left);
        TextView submittedByRight = (TextView)v.findViewById(R.id.submitted_by_right);
        TextView imageSubmittedByLeft = (TextView)v.findViewById(R.id.image_submitted_by_left);
        TextView imageSubmittedByRight = (TextView)v.findViewById(R.id.image_submitted_by_right);
        ImageView cardIconLeft = (ImageView)v.findViewById(R.id.card_icon_left);
        ImageView cardIconRight = (ImageView)v.findViewById(R.id.card_icon_right);
        final ImageView reactionLeft = (ImageView)v.findViewById(R.id.reaction_left);
        final ImageView reactionRight = (ImageView)v.findViewById(R.id.reaction_right);

        cardLeftTouchArea.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                cardLeftView.bringToFront();
                ((View)cardLeftView.getParent()).invalidate();
            }
        });

        cardRightTouchArea.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                cardRightView.bringToFront();
                ((View)cardRightView.getParent()).invalidate();
            }
        });

        if (cardLeft.getType() == Card.CARD_TYPE_PICTURE)
        {
            cardTextLeft.setVisibility(View.GONE);
            imagePanelLeft.setVisibility(View.VISIBLE);

            // Load the card image.
            loadImage(getContext(), cardLeft.getText(), imageContainerLeft, imageViewLeft, progressBarLeft, errorViewLeft, 3);
        }
        else
        {
            // Set the card text.
            cardTextLeft.setText(cardLeft.getText());
        }

        if (cardRight.getType() == Card.CARD_TYPE_PICTURE)
        {
            cardTextRight.setVisibility(View.GONE);
            imagePanelRight.setVisibility(View.VISIBLE);

            // Load the card image.
            loadImage(getContext(), cardRight.getText(), imageContainerRight, imageViewRight, progressBarRight, errorViewRight, 3);
        }
        else
        {
            // Set the card text.
            cardTextRight.setText(cardRight.getText());
        }

        if (activity.isOnResultsScreen() || activity.isMatchComplete() || activity.isMatchCanceled())
        {
            String cardOwnerId = activity.getCardOwnerId(cardLeft);
            String cardOwnerName;

            // Check if cardOwnerId belongs to a bot. If so, it's possible that the
            // bot has been replaced by a human player, in which case the id no longer
            // resides in the players map, and thus we can't retrieve the bot name. In
            // this case, we will regenerate the bot name using cardOwnerId.
            if (cardOwnerId.startsWith("bot_"))
            {
                cardOwnerName = "Bot " + cardOwnerId.substring(cardOwnerId.indexOf("_") + 1);
                cardOwnerName += " (auto pick)";
            }
            else
            {
                cardOwnerName = round.getPlayers().get(cardOwnerId).getName();

                if (round.getPlayers().get(cardOwnerId).autoPicked())
                    cardOwnerName += " (auto pick)";
            }

            if (cardLeft.getType() == Card.CARD_TYPE_PICTURE)
            {
                imageSubmittedByLeft.setText("- " + cardOwnerName);
                imageSubmittedByLeft.setVisibility(View.VISIBLE);
            }
            else
            {
                submittedByLeft.setText("- " + cardOwnerName);
                submittedByLeft.setVisibility(View.VISIBLE);
            }

            if (cardRight.getType() == Card.CARD_TYPE_PICTURE)
            {
                imageSubmittedByRight.setText("- " + cardOwnerName);
                imageSubmittedByRight.setVisibility(View.VISIBLE);
            }
            else
            {
                submittedByRight.setText("- " + cardOwnerName);
                submittedByRight.setVisibility(View.VISIBLE);
            }

            if  (position == activity.getWinningCardPos())
            {
                // Show appropriate winning card icon.
                Calendar calendar = new GregorianCalendar();
                int month = calendar.get(Calendar.MONTH);
                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
                int winningCardImage = R.drawable.crown;

               // Thanksgiving day only:
               // if (month == Calendar.NOVEMBER && dayOfWeek == Calendar.THURSDAY && dayOfMonth >= 22 && dayOfMonth <= 28)

                if (month == Calendar.NOVEMBER)
                    winningCardImage = R.drawable.pilgrim_hat;
                else if (month == Calendar.DECEMBER && dayOfMonth <= 25)
                    winningCardImage = R.drawable.santa_hat;

                cardIconLeft.setImageResource(winningCardImage);
                cardIconLeft.setVisibility(View.VISIBLE);

                cardIconRight.setImageResource(winningCardImage);
                cardIconRight.setVisibility(View.VISIBLE);
            }

            if (cardLeft.getReaction() > 0)
                reactionLeft.setVisibility(View.VISIBLE);

            if (cardRight.getReaction() > 0)
                reactionRight.setVisibility(View.VISIBLE);
        }
        else if (match.getState() == Match.MATCH_STATE_JUDGING && activity.isJudge())
        {
            reactionLeft.setVisibility(View.VISIBLE);
            reactionRight.setVisibility(View.VISIBLE);

            // Construct the reaction popup windows.
            View popupViewLeft = activity.getLayoutInflater().inflate(R.layout.popup_react, null);
            GridView reactionsLeft = (GridView)popupViewLeft.findViewById(R.id.reactions);

            View popupViewRight = activity.getLayoutInflater().inflate(R.layout.popup_react, null);
            GridView reactionsRight = (GridView)popupViewRight.findViewById(R.id.reactions);

            List<String> reactionList = new ArrayList<String>();

            for (int i = 0; i <= 85; i++)
            {
                reactionList.add("reaction_" + i);
            }

            final ReactionAdapter reactionAdapter = new ReactionAdapter(getContext(), reactionList);
            reactionsLeft.setAdapter(reactionAdapter);
            reactionsRight.setAdapter(reactionAdapter);

            final PopupWindow reactionPopupLeft = new PopupWindow(popupViewLeft, ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT);
            final PopupWindow reactionPopupRight = new PopupWindow(popupViewRight, ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT);

            reactionPopupLeft.setBackgroundDrawable(new ShapeDrawable());
            reactionPopupLeft.setOutsideTouchable(true);
            reactionPopupLeft.setFocusable(true);

            reactionPopupRight.setBackgroundDrawable(new ShapeDrawable());
            reactionPopupRight.setOutsideTouchable(true);
            reactionPopupRight.setFocusable(true);

            reactionLeft.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    reactionPopupLeft.showAsDropDown(reactionLeft, 0, 0);
                }
            });

            reactionRight.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    reactionPopupRight.showAsDropDown(reactionRight, 0, 0);
                }
            });

            reactionsLeft.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                {
                    if (position != cardLeft.getReaction())
                        activity.react(cardLeft.getId(), position);

                    reactionPopupLeft.dismiss();
                }
            });

            reactionsRight.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                {
                    if (position != cardRight.getReaction())
                        activity.react(cardRight.getId(), position);

                    reactionPopupRight.dismiss();
                }
            });
        }

        if (reactionLeft.getVisibility() == View.VISIBLE)
        {
            // Load card reaction image.
            String resourceName = "reaction_" + cardLeft.getReaction();
            int resourceId = getContext().getResources().getIdentifier(resourceName, "drawable", getContext().getPackageName());

            Glide.with(getContext())
                .load(resourceId)
                .fitCenter()
                .into(reactionLeft);
        }

        if (reactionRight.getVisibility() == View.VISIBLE)
        {
            // Load card reaction image.
            String resourceName = "reaction_" + cardRight.getReaction();
            int resourceId = getContext().getResources().getIdentifier(resourceName, "drawable", getContext().getPackageName());

            Glide.with(getContext())
                .load(resourceId)
                .fitCenter()
                .into(reactionRight);
        }

        if (position == activity.getCards().size() / 2 - 1)
            activity.onCardPagerReady();
    }

    private void loadImage(final Context context, final String url, final View imageContainer, final ImageView imageView, final View progressBar, final View errorView, final int attempts)
    {
        if (context == null)
        {
            progressBar.setVisibility(View.GONE);
            imageContainer.setVisibility(View.GONE);
            errorView.setVisibility(View.VISIBLE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);

        Glide.with(context)
            .load(url)
            .fitCenter()
            .listener(new RequestListener<String, GlideDrawable>()
            {
                @Override
                public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource)
                {
                    if (attempts > 0)
                        loadImage(context, url, imageContainer, imageView, progressBar, errorView, attempts - 1);
                    else
                    {
                        progressBar.setVisibility(View.GONE);
                        imageContainer.setVisibility(View.GONE);
                        errorView.setVisibility(View.VISIBLE);

                        errorView.setOnClickListener(new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View v)
                            {
                                loadImage(context, url, imageContainer, imageView, progressBar, errorView, 3);
                            }
                        });
                    }

                    return false;
                }

                @Override
                public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource)
                {
                    progressBar.setVisibility(View.GONE);
                    imageContainer.setVisibility(View.VISIBLE);
                    errorView.setVisibility(View.GONE);
                    return false;
                }
            })
            .into(imageView);
    }
}
