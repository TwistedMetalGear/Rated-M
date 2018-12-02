package net.silentbyte.ratedm.fragments;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import net.silentbyte.ratedm.CardEditText;
import net.silentbyte.ratedm.Card;
import net.silentbyte.ratedm.Match;
import net.silentbyte.ratedm.R;
import net.silentbyte.ratedm.Round;
import net.silentbyte.ratedm.ToggleLog;
import net.silentbyte.ratedm.activities.GameActivity;
import net.silentbyte.ratedm.activities.ImageSearchActivity;
import net.silentbyte.ratedm.adapters.ReactionAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class CardFragment extends Fragment
{
    private static final String TAG = "CardFragment";
    private static final String KEY_POSITION = "position";

    // Request codes
    private static final int RC_IMAGE_SEARCH = 0;

	public static Fragment newInstance(Context context, int position)
	{
		Bundle b = new Bundle();
        b.putInt(KEY_POSITION, position);
		return Fragment.instantiate(context, CardFragment.class.getName(), b);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
		if (container == null)
			return null;

        float density = getResources().getDisplayMetrics().density;

        if (density == 3.5)
            return inflater.inflate(R.layout.fragment_card_3_5, container, false);
        else
		    return inflater.inflate(R.layout.fragment_card, container, false);
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
        final int position = getArguments().getInt("position");

        // Checking position because there are certain situations where
        // it can get out of range. For instance, performing a config
        // change immediately after submitting a card.
        if (position >= activity.getCards().size())
        {
            ToggleLog.d(TAG, "onStart: Returning because position is out of range.");
            return;
        }

        View v = getView();
        final Card card = activity.getCards().get(position);

        TextView cardText = (TextView)v.findViewById(R.id.white_card_text);
        TextView submittedBy = (TextView)v.findViewById(R.id.submitted_by);
        TextView imageSubmittedBy = (TextView)v.findViewById(R.id.image_submitted_by);
        ImageView cardIcon = (ImageView)v.findViewById(R.id.card_icon);
        final ImageView reaction = (ImageView) v.findViewById(R.id.reaction);

        if (activity.isOnResultsScreen() || activity.isMatchComplete() || activity.isMatchCanceled())
        {
            String cardOwnerId = activity.getCardOwnerId(card);
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

            if (card.getType() == Card.CARD_TYPE_PICTURE)
            {
                imageSubmittedBy.setText("- " + cardOwnerName);
                imageSubmittedBy.setVisibility(View.VISIBLE);
            }
            else
            {
                submittedBy.setText("- " + cardOwnerName);
                submittedBy.setVisibility(View.VISIBLE);
            }

            if  (position == activity.getWinningCardPos())
            {
                // Show appropriate winning card icon.
                Calendar calendar = new GregorianCalendar();
                int month = calendar.get(Calendar.MONTH);
                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
                int winningCardImage = R.drawable.crown_rotated;

                // Thanksgiving day only:
                // if (month == Calendar.NOVEMBER && dayOfWeek == Calendar.THURSDAY && dayOfMonth >= 22 && dayOfMonth <= 28)

                if (month == Calendar.NOVEMBER)
                    winningCardImage = R.drawable.pilgrim_hat_rotated;
                else if (month == Calendar.DECEMBER && dayOfMonth <= 25)
                    winningCardImage = R.drawable.santa_hat_rotated;

                cardIcon.setImageResource(winningCardImage);
                cardIcon.setVisibility(View.VISIBLE);
            }

            if (card.getReaction() > 0)
                reaction.setVisibility(View.VISIBLE);
        }
        else if ((match.getState() == Match.MATCH_STATE_PICKING && !activity.isCardSubmitted()) || match.getState() == Match.MATCH_STATE_WRITING_CARD)
        {
            if (card.getType() == Card.CARD_TYPE_BLANK)
            {
                v.findViewById(R.id.white_card_text).setVisibility(View.GONE);
                cardText = (EditText)v.findViewById(R.id.blank_card_text);
                cardText.setVisibility(View.VISIBLE);

                // This is done so that we can tap anywhere on the card to input text.
                cardText.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT));

                final CardEditText cardEditText = (CardEditText)cardText;

                // Save the text as it changes so that we can restore it later.
                cardEditText.addTextChangedListener(new TextWatcher()
                {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after)
                    {
                    }

                    @Override
                    public void afterTextChanged(Editable s)
                    {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count)
                    {
                        // Too slow on older phones. Disabling for now.
                        /*
                        cardEditText.removeTextChangedListener(this);
                        cardEditText.setText(s.toString().replaceFirst("^ *", ""));
                        cardEditText.addTextChangedListener(this);

                        int numCharactersRemoved = s.length() - cardEditText.length();
                        count -= numCharactersRemoved;

                        if (start < cardEditText.length())
                            cardEditText.setSelection(start + count);
                        else
                            cardEditText.setSelection(cardEditText.length());
                        */

                        activity.setBlankCardText(cardEditText.getText().toString());
                    }
                });

                // Show a pencil icon on the card to indicate that it is editable.
                cardIcon.setImageResource(R.drawable.pencil);
                cardIcon.setVisibility(View.VISIBLE);
            }
            else if (card.getType() == Card.CARD_TYPE_PICTURE)
            {
                View imagePanel = v.findViewById(R.id.image_panel);

                // This is done so that we can tap anywhere on the card to select an image.
                imagePanel.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT));

                imagePanel.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        Intent intent = new Intent(getContext(), ImageSearchActivity.class);
                        startActivityForResult(intent, RC_IMAGE_SEARCH);
                    }
                });

                cardIcon.setImageResource(R.drawable.image);
                cardIcon.setVisibility(View.VISIBLE);
            }
        }
        else if (match.getState() == Match.MATCH_STATE_JUDGING && activity.isJudge())
        {
            reaction.setVisibility(View.VISIBLE);

            // Construct the reaction popup window.
            View popupView = activity.getLayoutInflater().inflate(R.layout.popup_react, null);
            GridView reactions = (GridView) popupView.findViewById(R.id.reactions);

            List<String> reactionList = new ArrayList<String>();

            for (int i = 0; i <= 85; i++)
            {
                reactionList.add("reaction_" + i);
            }

            final ReactionAdapter reactionAdapter = new ReactionAdapter(getContext(), reactionList);
            reactions.setAdapter(reactionAdapter);

            final PopupWindow reactionPopup = new PopupWindow(popupView, ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT);
            reactionPopup.setBackgroundDrawable(new ShapeDrawable());
            reactionPopup.setOutsideTouchable(true);
            reactionPopup.setFocusable(true);

            reaction.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    reactionPopup.showAsDropDown(reaction, 0, 0);
                }
            });

            reactions.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                {
                    /*
                    String resourceName = "reaction";

                    if (position != 0)
                        resourceName = reactionAdapter.getItem(position);

                    int resourceId = getContext().getResources().getIdentifier(resourceName, "drawable", getContext().getPackageName());

                    Glide.with(getContext())
                            .load(resourceId)
                            .fitCenter()
                            .into(reaction);
                    */

                    if (position != card.getReaction())
                        activity.react(card.getId(), position);

                    reactionPopup.dismiss();
                }
            });
        }

        // Load card reaction image.
        if (reaction.getVisibility() == View.VISIBLE)
        {
            String resourceName = "reaction_" + card.getReaction();
            int resourceId = getContext().getResources().getIdentifier(resourceName, "drawable", getContext().getPackageName());

            Glide.with(getContext())
                .load(resourceId)
                .fitCenter()
                .into(reaction);
        }

        if (card.getType() == Card.CARD_TYPE_PICTURE)
        {
            v.findViewById(R.id.white_card_text).setVisibility(View.GONE);
            v.findViewById(R.id.image_panel).setVisibility(View.VISIBLE);

            // Load the card image.
            if (!card.isEmpty())
            {
                loadImage(getContext(), card.getText(), v, 3);
            }
        }
        else
        {
            // Set the card text.
            cardText.setText(card.getText());
        }

        if (position == activity.getCards().size() - 1)
            activity.onCardPagerReady();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == Activity.RESULT_OK)
        {
            String url = data.getStringExtra(ImageSearchActivity.KEY_IMAGE_URL);
            GameActivity activity = (GameActivity)getActivity();
            activity.setPictureCardUrl(url);
            activity.updateGameData();
        }
    }

    private void loadImage(final Context context, final String url, final View view, final int attempts)
    {
        final View progressBar = view.findViewById(R.id.progress_bar);
        final View imageContainer = view.findViewById(R.id.image_container);
        final ImageView imageView = (ImageView)view.findViewById(R.id.image);
        final View error = view.findViewById(R.id.error);

        if (context == null)
        {
            progressBar.setVisibility(View.GONE);
            imageContainer.setVisibility(View.GONE);
            error.setVisibility(View.VISIBLE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        error.setVisibility(View.GONE);

        Glide.with(context)
            .load(url)
            .fitCenter()
            .listener(new RequestListener<String, GlideDrawable>()
            {
                @Override
                public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource)
                {
                    if (attempts > 0)
                        loadImage(context, url, view, attempts - 1);
                    else
                    {
                        progressBar.setVisibility(View.GONE);
                        imageContainer.setVisibility(View.GONE);
                        error.setVisibility(View.VISIBLE);

                        error.setOnClickListener(new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View v)
                            {
                                loadImage(context, url, view, 3);
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
                    error.setVisibility(View.GONE);
                    return false;
                }
            })
            .into(imageView);
    }
}
