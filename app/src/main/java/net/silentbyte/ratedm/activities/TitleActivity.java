package net.silentbyte.ratedm.activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.login.LoginManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import net.silentbyte.ratedm.GameConstants;
import net.silentbyte.ratedm.GameFunctions;
import net.silentbyte.ratedm.Match;
import net.silentbyte.ratedm.R;
import net.silentbyte.ratedm.ToggleLog;
import net.silentbyte.ratedm.User;
import net.silentbyte.ratedm.WinningCardPair;
import net.silentbyte.ratedm.fragments.TitleFragment;
import net.silentbyte.ratedm.notifications.NotificationService;

import java.util.ArrayList;
import java.util.List;

// TODO: Need a retry upon failure to receive cards (both main game cards and winning cards).
public class TitleActivity extends FragmentActivity implements TitleFragment.CallbackHandler, DialogInterface.OnCancelListener
{
    private static final String TAG = "TitleFragment";
    private static final String TAG_TITLE_FRAGMENT = "title_fragment";
    private static final int RC_SIGN_IN = 0;
    private static final int RC_NEW_MATCH = 1;
    private static final int RC_MATCHES = 2;
    private static final int RC_SETTINGS = 3;
    private static final int RC_GAME = 4;
    private static final int CARD_CYCLE_INTERVAL = 5000;
    private static final String KEY_WINNING_CARDS = "winning_cards";
    private static final String KEY_WINNING_CARD_POS = "winning_card_pos";
    private static final String KEY_FIRST_ENTRY = "first_entry";

    private Handler mHandler;
    private Runnable mRunnable;
    private List<WinningCardPair> mWinningCards;
    private int mWinningCardPos = 0;
    private boolean mFirstEntry = true;
    private boolean mPaused = false;
    private String mMatchId;

    private TextView newMatchButton;
    private TextView autoMatchButton;
    private TextView matchesButton;
    private TextView settingsButton;
    private TextView signButton;
    private TextView welcomeText;
    private TextView blackCardText;
    private TextView whiteCardText;
    private View blackCard;
    private View whiteCard;
    private View blackCardProgressBar;
    private View whiteCardProgressBar;

    // Retained fragments
    private TitleFragment mTitleFragment;

    private ProgressDialog mProgressDialog;
    private Dialog mNotificationDialog;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Lock the device in portrait mode.
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        ToggleLog.e(TAG, "onCreate()");

        float density = getResources().getDisplayMetrics().density;

        if (density == 3.5)
            setContentView(R.layout.activity_title_3_5);
        else
            setContentView(R.layout.activity_title);

        newMatchButton = (TextView) findViewById(R.id.new_match_button);
        newMatchButton.setOnClickListener(new ButtonClickListener());
        newMatchButton.setClickable(false);

        autoMatchButton = (TextView) findViewById(R.id.auto_match_button);
        autoMatchButton.setOnClickListener(new ButtonClickListener());
        autoMatchButton.setClickable(false);

        matchesButton = (TextView) findViewById(R.id.matches_button);
        matchesButton.setOnClickListener(new ButtonClickListener());
        matchesButton.setClickable(false);

        settingsButton = (TextView) findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(new ButtonClickListener());

        signButton = (TextView) findViewById(R.id.sign_button);
        signButton.setOnClickListener(new ButtonClickListener());

        welcomeText = (TextView) findViewById(R.id.welcome_text);
        blackCardText = (TextView) findViewById(R.id.black_card_text);
        whiteCardText = (TextView) findViewById(R.id.white_card_text);
        blackCard = findViewById(R.id.black_card);
        whiteCard = findViewById(R.id.white_card);
        blackCardProgressBar = findViewById(R.id.black_card_progress_bar);
        whiteCardProgressBar = findViewById(R.id.white_card_progress_bar);

        View logo = findViewById(R.id.logo);
        logo.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mNotificationDialog = showAboutDialog();
            }
        });

        FragmentManager fm = getSupportFragmentManager();

        // Add title fragment to fragment manager.
        mTitleFragment = (TitleFragment) fm.findFragmentByTag(TAG_TITLE_FRAGMENT);

        if (mTitleFragment == null)
        {
            mTitleFragment = new TitleFragment();
            fm.beginTransaction().add(mTitleFragment, TAG_TITLE_FRAGMENT).commit();
        }

        if (savedInstanceState != null)
        {
            mWinningCards = savedInstanceState.getParcelableArrayList(KEY_WINNING_CARDS);
            mWinningCardPos = savedInstanceState.getInt(KEY_WINNING_CARD_POS);
            mFirstEntry = savedInstanceState.getBoolean(KEY_FIRST_ENTRY);
        }
    }

    @Override
    public void onResume()
    {
        ToggleLog.d(TAG, "onResume()");
        super.onResume();

        if (isPlayServicesAvailable())
        {
            ToggleLog.d(TAG, "Google Play Services found!");
        }

        mPaused = false;

        String progressText = mTitleFragment.getProgressText();

        if (progressText != null && (mProgressDialog == null || !mProgressDialog.isShowing()))
        {
            showProgressDialog(progressText);

            // If a configuration occurs while the auto-matching dialog is showing,
            // the cards will go blank. This prevents that from happening.
            if (mWinningCards != null)
                startCardCycle();
        }
        else
        {
            Intent intent = getIntent();
            int flags = intent.getFlags();
            String notification = null;
            boolean multipleNotifications = false;

            // Only set mMatchId if not launched from history.
            // When launching from history, the intent is never cleared,
            // even if we exited the activity by pressing back.
            if ((flags & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0)
            {
                Bundle extras = intent.getExtras();

                if (extras != null)
                    notification = extras.getString(NotificationService.KEY_NOTIFICATION);

                if (notification == null)
                    notification = intent.getStringExtra(NotificationService.KEY_NOTIFICATION);

                String matchId = intent.getStringExtra(Match.KEY_MATCH_ID);
                multipleNotifications = intent.getBooleanExtra(NotificationService.KEY_MULTIPLE_NOTIFICATIONS, false);

                if (matchId != null && !multipleNotifications)
                {
                    mMatchId = matchId;
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

                    if (prefs.contains(NotificationService.KEY_NOTIFICATION_IDS))
                        prefs.edit().remove(NotificationService.KEY_NOTIFICATION_IDS).commit();
                }
            }

            if (mFirstEntry)
            {
                mFirstEntry = false;

                if (notification != null)
                    mNotificationDialog = GameFunctions.showDialog(this, getString(R.string.app_name), notification);

                if (User.get(this) != null)
                {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

                    if ((prefs.getBoolean(SettingsActivity.KEY_SHOW_MATCHES, false) && mMatchId == null) || multipleNotifications)
                        showMatches(true);
                    else if (mMatchId != null)
                        startGameActivity(mMatchId);
                }
            }

            if (mWinningCards == null)
            {
                showProgressBars();

                if (mTitleFragment.shouldRetrieveWinningCards())
                {
                    mTitleFragment.setRetrieveWinningCards(false);
                    mTitleFragment.retrieveWinningCards();
                }
            }
            else
                startCardCycle();
        }

        updateSignInState();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        mPaused = true;
        stopCardCycle();

        if (mProgressDialog != null)
            mProgressDialog.dismiss();

        if (mNotificationDialog != null)
            mNotificationDialog.dismiss();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putParcelableArrayList(KEY_WINNING_CARDS, (ArrayList) mWinningCards);
        savedInstanceState.putInt(KEY_WINNING_CARD_POS, mWinningCardPos);
        savedInstanceState.putBoolean(KEY_FIRST_ENTRY, mFirstEntry);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        mTitleFragment.setBusy(false);

        if (requestCode == RC_NEW_MATCH)
        {
            if (resultCode != Activity.RESULT_OK)
                return;

            String matchId = data.getStringExtra(Match.KEY_MATCH_ID);
            startGameActivity(matchId);
        }
        else if (requestCode == RC_GAME)
        {
            if (resultCode == GameConstants.RESULT_EXPIRED)
                mNotificationDialog = GameFunctions.showDialog(this, getString(R.string.expired_title), getString(R.string.expired_description));
            else if (resultCode == GameConstants.RESULT_REMOVED)
                mNotificationDialog = GameFunctions.showDialog(this, getString(R.string.removed_title), getString(R.string.removed_description));
            else if (resultCode == GameConstants.RESULT_LOAD_FAILED)
                mNotificationDialog = GameFunctions.showDialog(this, getString(R.string.unable_to_load_match), getString(R.string.connection_lost));
        }
    }

    public void onCancel(DialogInterface dialog)
    {
        // TODO: Implement
    }

    public boolean isPlayServicesAvailable()
    {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int result = apiAvailability.isGooglePlayServicesAvailable(this);

        if (result != ConnectionResult.SUCCESS)
        {
            ToggleLog.e(TAG, "Google Play Services unavailable. Result: " + result);

            if (apiAvailability.isUserResolvableError(result))
            {
                Dialog dialog = apiAvailability.getErrorDialog(this, result, 0, this);
                dialog.setCancelable(false);
                dialog.show();
            }
            else
                finish();

            return false;
        }

        return true;
    }

    private void startGameActivity(String matchId)
    {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra(Match.KEY_MATCH_ID, matchId);
        startActivityForResult(intent, RC_GAME);
    }

    private void signOut()
    {
        // Delete user token.
        mTitleFragment.deleteToken();

        // Log out of Facebook if necessary.
        if (AccessToken.getCurrentAccessToken() != null)
            LoginManager.getInstance().logOut();

        // Clear stored user data.
        User.clear(this);

        disableControls();

        mTitleFragment.setBusy(false);
    }

    private void setDefaultWinningCards()
    {
        String blackCardText = getString(R.string.default_black_card_text);
        String whiteCardText = getString(R.string.default_white_card_text);
        mWinningCards = new ArrayList<WinningCardPair>();
        mWinningCards.add(new WinningCardPair(blackCardText, whiteCardText));
    }

    private void showMatches(boolean enteredFromLaunch)
    {
        Intent intent = new Intent(TitleActivity.this, MatchesActivity.class);
        intent.putExtra(MatchesActivity.KEY_ENTERED_FROM_LAUNCH, enteredFromLaunch);
        startActivityForResult(intent, RC_MATCHES);
    }

    private Dialog showAboutDialog()
    {
        final Dialog dialog = new Dialog(this);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_about);

        TextView title = (TextView)dialog.findViewById(R.id.title);

        try
        {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = info.versionName;
            title.setText("Rated M Version " + version);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            ToggleLog.e(TAG, "Unable to retrieve version. NameNotFoundException: " + e.getMessage());
            title.setText("Rated M");
        }

        TextView link1 = (TextView)dialog.findViewById(R.id.link_1);
        TextView link2 = (TextView)dialog.findViewById(R.id.link_2);
        TextView link3 = (TextView)dialog.findViewById(R.id.link_3);
        TextView link4 = (TextView)dialog.findViewById(R.id.link_4);

        link1.setMovementMethod(LinkMovementMethod.getInstance());
        link2.setMovementMethod(LinkMovementMethod.getInstance());
        link3.setMovementMethod(LinkMovementMethod.getInstance());
        link4.setMovementMethod(LinkMovementMethod.getInstance());

        View okButton = dialog.findViewById(R.id.ok_button);

        okButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                dialog.dismiss();
            }
        });

        dialog.show();
        return dialog;
    }

    private Dialog showAutoMatchDialog()
    {
        final Dialog dialog = new Dialog(this);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_auto_match);

        final Spinner whiteCardTypeView = (Spinner)dialog.findViewById(R.id.white_card_type);
        final TextView whiteCardTypeDescription = (TextView)dialog.findViewById(R.id.white_card_type_description);
        View okButton = dialog.findViewById(R.id.ok_button);
        View cancelButton = dialog.findViewById(R.id.cancel_button);

        whiteCardTypeView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                switch (position)
                {
                    case Match.WHITE_CARD_TYPE_STANDARD:
                        whiteCardTypeDescription.setText(getString(R.string.white_card_standard_description));
                        break;
                    case Match.WHITE_CARD_TYPE_CUSTOM:
                        whiteCardTypeDescription.setText(getString(R.string.white_card_custom_description));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        okButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                dialog.dismiss();
                int whiteCardType = whiteCardTypeView.getSelectedItemPosition();
                mTitleFragment.setBusy(true);
                autoMatch(whiteCardType);
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                dialog.dismiss();
            }
        });

        dialog.show();
        return dialog;
    }

    private void showProgressDialog(String progressText)
    {
        mTitleFragment.setProgressText(progressText);
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(progressText);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    private void dismissProgressDialog()
    {
        ToggleLog.e(TAG, "dismissProgressDialog()");
        mTitleFragment.setProgressText(null);

        if (mProgressDialog != null)
            mProgressDialog.dismiss();
    }

    public void onWinningCardsReceived(List<WinningCardPair> winningCards)
    {
        ToggleLog.e(TAG, "onWinningCardsReceived()");

        if (!mTitleFragment.isAutoMatching())
            dismissProgressDialog();

        if (winningCards.size() > 0)
            mWinningCards = winningCards;
        else
            setDefaultWinningCards();

        startCardCycle();
    }

    public void onWinningCardsReceivedFailure()
    {
        ToggleLog.e(TAG, "Failed to receive winning cards.");

        if (!mTitleFragment.isAutoMatching())
            dismissProgressDialog();

        setDefaultWinningCards();
        startCardCycle();
    }

    private void showProgressBars()
    {
        blackCardText.setVisibility(View.GONE);
        whiteCardText.setVisibility(View.GONE);
        blackCardProgressBar.setVisibility(View.VISIBLE);
        whiteCardProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBars()
    {
        blackCardProgressBar.setVisibility(View.GONE);
        whiteCardProgressBar.setVisibility(View.GONE);
        blackCardText.setVisibility(View.VISIBLE);
        whiteCardText.setVisibility(View.VISIBLE);
    }

    public void enableControls()
    {
        newMatchButton.setClickable(true);
        newMatchButton.setTextColor(ContextCompat.getColor(this, R.color.white));

        autoMatchButton.setClickable(true);
        autoMatchButton.setTextColor(ContextCompat.getColor(this, R.color.white));

        matchesButton.setClickable(true);
        matchesButton.setTextColor(ContextCompat.getColor(this, R.color.white));

        signButton.setText(R.string.sign_out);
    }

    public void disableControls()
    {
        newMatchButton.setClickable(false);
        newMatchButton.setTextColor(ContextCompat.getColor(this, R.color.disabled_text));

        autoMatchButton.setClickable(false);
        autoMatchButton.setTextColor(ContextCompat.getColor(this, R.color.disabled_text));

        matchesButton.setClickable(false);
        matchesButton.setTextColor(ContextCompat.getColor(this, R.color.disabled_text));

        welcomeText.setText("");
        signButton.setText(R.string.sign_in);
    }

    private void showWinningCardText(int position)
    {
        blackCardText.setText(mWinningCards.get(position).getBlackCardText());
        whiteCardText.setText(mWinningCards.get(position).getWhiteCardText());
    }

    private void startCardCycle()
    {
        hideProgressBars();

        // Set the initial winning card pair text.
        showWinningCardText(mWinningCardPos);

        if (!mPaused && mWinningCards.size() > 1)
        {
            // Stop existing card cycle.
            stopCardCycle();

            ToggleLog.e(TAG, "Starting card cycle.");

            mHandler = new Handler();

            mRunnable = new Runnable()
            {
                public void run()
                {
                    if (++mWinningCardPos >= mWinningCards.size())
                        mWinningCardPos = 0;

                    cycleCards(mWinningCardPos);

                    mHandler.postDelayed(this, CARD_CYCLE_INTERVAL);
                }
            };

            mHandler.postDelayed(mRunnable, CARD_CYCLE_INTERVAL);
        }
    }

    public void stopCardCycle()
    {
        ToggleLog.e(TAG, "Stopping card cycle.");

        if (mHandler != null)
            mHandler.removeCallbacks(mRunnable);
    }

    private void cycleCards(final int position)
    {
        final Animation a1 = GameFunctions.createAnimation(0, 1, 360, 0);  // Black card origin --> right
        final Animation a2 = GameFunctions.createAnimation(0, -1, 360, 0); // White  card origin --> left
        final Animation a3 = GameFunctions.createAnimation(-1, 0, 360, 0); // Black card left --> origin
        final Animation a4 = GameFunctions.createAnimation(1, 0, 360, 0);  // White card right --> origin

        a1.setAnimationListener(new Animation.AnimationListener()
        {
            @Override
            public void onAnimationStart(Animation animation)
            {
            }

            @Override
            public void onAnimationEnd(Animation animation)
            {
                showWinningCardText(position);
                //blackCardText.setText(mWinningCards.get(position).getBlackCardText());
                blackCard.startAnimation(a3);
            }

            @Override
            public void onAnimationRepeat(Animation animation)
            {
            }
        });

        a2.setAnimationListener(new Animation.AnimationListener()
        {
            @Override
            public void onAnimationStart(Animation animation)
            {
            }

            @Override
            public void onAnimationEnd(Animation animation)
            {
                //whiteCardText.setText(mWinningCards.get(position).getWhiteCardText());
                whiteCard.startAnimation(a4);
            }

            @Override
            public void onAnimationRepeat(Animation animation)
            {
            }
        });

        blackCard.startAnimation(a1);
        whiteCard.startAnimation(a2);
    }

    private void autoMatch(int whiteCardType)
    {
        showProgressDialog("Joining Match...");
        mTitleFragment.setAutoMatching(true);
        mTitleFragment.autoMatch(whiteCardType);
    }

    public void updateSignInState()
    {
        User user = User.get(this);

        if (user != null)
        {
            welcomeText.setText("Welcome, " + user.getDisplayName() + "!");
            enableControls();
        }
        else
            disableControls();
    }

    public void onAutoMatchSuccess(Match match, int whiteCardType)
    {
        dismissProgressDialog();

        if (mWinningCards == null)
            showProgressBars();

        if (match == null)
        {
            // No matches found. Open NewMatchActivity to start a new one.
            Intent intent = new Intent(this, NewMatchActivity.class);
            intent.putExtra(NewMatchActivity.KEY_AUTO_MATCH, true);
            intent.putExtra(Match.JSON_WHITE_CARD_TYPE, whiteCardType);
            startActivityForResult(intent, RC_NEW_MATCH);
        }
        else
            startGameActivity(match.getId());

        mTitleFragment.setAutoMatching(false);
        mTitleFragment.setBusy(false);
    }

    public void onAutoMatchFailure(String message)
    {
        dismissProgressDialog();

        if (mWinningCards == null)
            showProgressBars();

        mNotificationDialog = GameFunctions.showDialog(this, getString(R.string.unable_to_join_match), message);

        mTitleFragment.setAutoMatching(false);
        mTitleFragment.setBusy(false);
    }

    private class ButtonClickListener implements View.OnClickListener
    {
        @Override
        public void onClick(View view)
        {
            if (!mTitleFragment.isBusy())
            {
                mTitleFragment.setBusy(true);

                // Show the button animation.
                Animation anim = AnimationUtils.loadAnimation(TitleActivity.this, R.anim.pulse_title);
                anim.reset();
                view.clearAnimation();
                view.startAnimation(anim);

                Intent intent;
                hideProgressBars();

                switch (view.getId())
                {
                    case R.id.new_match_button:
                        intent = new Intent(TitleActivity.this, NewMatchActivity.class);
                        startActivityForResult(intent, RC_NEW_MATCH);
                        break;
                    case R.id.auto_match_button:
                        mNotificationDialog = showAutoMatchDialog();
                        mTitleFragment.setBusy(false);
                        break;
                    case R.id.matches_button:
                        showMatches(false);
                        break;
                    case R.id.settings_button:
                        intent = new Intent(TitleActivity.this, SettingsActivity.class);
                        startActivityForResult(intent, RC_SETTINGS);
                        break;
                    case R.id.sign_button:
                        if (signButton.getText().equals(getString(R.string.sign_in)))
                        {
                            hideProgressBars();
                            intent = new Intent(TitleActivity.this, SignInActivity.class);
                            startActivityForResult(intent, RC_SIGN_IN);
                        }
                        else
                            signOut();
                }
            }
        }
    }
}