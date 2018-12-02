package net.silentbyte.ratedm.activities;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.PopupMenu;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;

import net.silentbyte.ratedm.ButteryProgressBar;
import net.silentbyte.ratedm.Card;
import net.silentbyte.ratedm.ChatMessage;
import net.silentbyte.ratedm.Match;
import net.silentbyte.ratedm.Player;
import net.silentbyte.ratedm.Round;
import net.silentbyte.ratedm.User;
import net.silentbyte.ratedm.adapters.CardPagerAdapter;
import net.silentbyte.ratedm.CardViewPager;
import net.silentbyte.ratedm.GameConstants;
import net.silentbyte.ratedm.GameFunctions;
import net.silentbyte.ratedm.adapters.PlayerAdapter;
import net.silentbyte.ratedm.R;
import net.silentbyte.ratedm.ToggleLog;
import net.silentbyte.ratedm.fragments.ChatFragment;
import net.silentbyte.ratedm.fragments.GameFragment;
import net.silentbyte.ratedm.notifications.NotificationService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

public class GameActivity extends FragmentActivity implements GameFragment.CallbackHandler, NotificationService.CallbackHandler
{
    private static final String TAG = "GameActivity";
    private static final String TAG_CHAT_FRAGMENT = "chat_fragment";
    private static final String TAG_GAME_FRAGMENT = "game_fragment";
    private static final String KEY_SHOWN_CARD_IDS = "shown_card_ids";
    private static final String KEY_PREVIOUS_SHOWN_CARD_IDS = "previous_shown_card_ids";
    private static final String KEY_PREVIOUS_NOTIFICATIONS = "previous_notifications";
    private static final String KEY_ROUND_NUM = "round_num";
    private static final String KEY_ROUND_TO_SHOW = "round_to_show";
    private static final String KEY_CURRENT_SCREEN = "current_screen";
    private static final String KEY_CURRENT_CARD_POS = "current_card_pos";
    private static final String KEY_CURRENT_BET = "current_bet";
    private static final String KEY_FIRST_UPDATE = "first_update";
    private static final String KEY_SHOW_RESULTS = "show_results";
    private static final String KEY_ROUND_RESULTS_SEEN = "round_results_seen_";
    private static final String KEY_SPOKEN_CARD_TEXT = "spoken_card_text";
    private static final String KEY_BLANK_CARD_TEXT = "blank_card_text";
    private static final String KEY_PICTURE_CARD_URL = "picture_card_url";
    private static final String KEY_SHOW_TOSS_CARD_DIALOG = "show_toss_card_dialog";
    private static final String KEY_LAST_CHAT_MESSAGE_ID = "last_chat_message_id_";
    private static final String KEY_NEW_CHAT_MESSAGE_RECEIVED = "new_chat_message_received";
    private static final String KEY_LAUNCH_COUNT = "launch_count";
    private static final String KEY_RATE_TARGET = "rate_target";

    private static final int LAST_CHAT_MESSAGE_INTERVAL = 8000;
    private static final int AUTO_PICK_SKIP_TIMEOUT_INTERVAL = 1000;

    // Request codes
    private static final int RC_TTS = 0;
    private static final int RC_INVITE = 1;
    private static final int RC_KICK = 2;
    private static final int RC_MATCH_SETTINGS = 3;

    private static final int SCREEN_GAME = 0;
    private static final int SCREEN_CHAT = 1;

    private Match mMatch;

    // Retained fragments
    private GameFragment mGameFragment;
    private ChatFragment mChatFragment;

    private PlayerAdapter mPlayerAdapter;
    private ListView mPlayerListView;

    private CardPagerAdapter mCardAdapter;
    private CardViewPager mCardPager;

    private TextView mBlackCardText;
    private EditText mBlackCardTextInput;
    private LinearLayout mBlackCardBottom;

    private TextToSpeech mTts;
    private boolean mTtsReady = false;

    private boolean mPaused = false;

    private String mPlayerId;
    private String mPlayerName;

    private ArrayList<String> mShownCardIds = new ArrayList<String>();
    private ArrayList<String> mPreviousShownCardIds = new ArrayList<String>();
    private ArrayList<String> mPreviousNotifications;
    private boolean mClearNotifications = false;
    private boolean mNotificationsInProgress = false;
    private int mRound = 1;
    private int mRoundToShow = 1;
    private int mPreviousRoundToShow = 1;
    private int mCurrentScreen = 0;
    private int mCurrentCardPos = 0;
    private int mPauseCount = 0;
    private int mSleepCount = 0;

    private Spinner mRoundSelector;
    private TextView mSubmitButton;
    private TextView mBetButton;
    private TextView mTossButton;
    private TextView mChatButton;
    private TextView mNotification;
    private TextView mTimer;
    private View mControlPanelOverlay;
    private ButteryProgressBar mProgressBar;

    private PopupWindow mBetPopup;
    private TextView mBetAmountView;
    private int mCurrentBet = 0;
    private static String mMatchId;
    private boolean mFirstUpdate = true;
    private boolean mShowResults = false;
    private boolean mNewChatMessageReceived = false;
    private boolean mBlinkAutoPickSkipTimer = false;

    // TODO: These only need to be initialized once (on match entry). Would it be cleaner to initialize them in a method?
    private ArrayList<String> mSpokenCardText = new ArrayList<String>();
    private String mBlankCardText = "";
    private String mPictureCardUrl = "";

    private Handler mLastChatMessageHandler;
    private Handler mAutoPickSkipTimeoutHandler;
    private Runnable mLastChatMessageRunnable;
    private Runnable mAutoPickSkipTimeoutRunnable;

    private Dialog mNotificationDialog;

    // Create animations. These are global so that we have the ability to stop them mid-animation.
    Animation a1 = GameFunctions.createAnimation(0, 1, 360, 0);     // Chat origin --> right
    Animation a2 = GameFunctions.createAnimation(0, 1, 360, 120);   // Toss origin --> right
    Animation a3 = GameFunctions.createAnimation(0, 1, 360, 240);   // Bet origin --> right
    Animation a4 = GameFunctions.createAnimation(0, 1, 360, 360);   // Submit origin --> right
    Animation a5 = GameFunctions.createAnimation(-1, 0, 360, 0);    // Notification left --> origin
    Animation a6 = GameFunctions.createAnimation(0, 1, 360, 1500);  // Notification origin --> right
    Animation a7 = GameFunctions.createAnimation(-1, 0, 360, 0);    // Chat left --> origin
    Animation a8 = GameFunctions.createAnimation(-1, 0, 360, 120);  // Toss left --> origin
    Animation a9 = GameFunctions.createAnimation(-1, 0, 360, 240);  // Bet left --> origin
    Animation a10 = GameFunctions.createAnimation(-1, 0, 360, 360); // Submit left --> origin

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Lock the device in portrait mode.
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        ToggleLog.d(TAG, "onCreate");

        User user = User.get(this);

        if (user != null)
        {
            // Need to set these here rather than in onResume, because CardFragment.onStart()
            // gets called before onResume, and relies on mPlayerId being set.
            mPlayerId = user.getId();
            mPlayerName = user.getDisplayName();
        }
        else
        {
            ToggleLog.e(TAG, "Unable to continue match. User has been logged out.");
            finish();
        }

        float density = getResources().getDisplayMetrics().density;

        if (density == 3.5)
            setContentView(R.layout.activity_game_3_5);
        else
            setContentView(R.layout.activity_game);

        mBlackCardText = (TextView)findViewById(R.id.black_card_text);
        mBlackCardTextInput = (EditText)findViewById(R.id.black_card_text_input);
        mBlackCardBottom = (LinearLayout)findViewById(R.id.black_card_bottom);
        mCardPager = (CardViewPager)findViewById(R.id.card_view_pager);

        mBlackCardText.setMovementMethod(LinkMovementMethod.getInstance());

        // Add listeners to control panel buttons.
        mSubmitButton = (TextView)findViewById(R.id.submit_button);
        mSubmitButton.setOnClickListener(new ButtonClickListener());
        mSubmitButton.setClickable(false);

        mBetButton = (TextView)findViewById(R.id.bet_button);
        mBetButton.setOnClickListener(new ButtonClickListener());
        mBetButton.setClickable(false);

        mTossButton = (TextView)findViewById(R.id.toss_button);
        mTossButton.setOnClickListener(new ButtonClickListener());
        mTossButton.setClickable(false);

        mChatButton = (TextView)findViewById(R.id.chat_button);
        mChatButton.setOnClickListener(new ButtonClickListener());

        mNotification = (TextView)findViewById(R.id.notification);
        mTimer = (TextView)findViewById(R.id.timer);

        mControlPanelOverlay = findViewById(R.id.control_panel_overlay);
        mControlPanelOverlay.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                clearNotifications();
            }
        });

        mProgressBar = (ButteryProgressBar)findViewById(R.id.progressBar);

        mPlayerListView = (ListView)findViewById(R.id.players);

        mRoundSelector = (Spinner)findViewById(R.id.round_selector);
        mRoundSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                mRoundToShow = position + 1;

                if (mRoundToShow < mMatch.getRound())
                {
                    mShowResults = true;
                    resetTurnData();
                }
                else
                    mShowResults = false;

                // One or more rounds may have the same notification (e.g. "You won the round").
                // Clear previous notifications when switching between rounds to ensure that we
                // always show the notification, even if it is the same.
                if (mRoundToShow != mPreviousRoundToShow)
                {
                    // Do not clear mPreviousNotifications! Set it to a new instance instead.
                    // This avoids a crash in showNotifications where we attempt to retrieve an item
                    // from the notifications list but it has been cleared here. The crash was
                    // happening upon rapid switching between rounds.
                    mPreviousNotifications = new ArrayList<String>();
                }

                updateGameData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        View menuButton = findViewById(R.id.menu_button);
        menuButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                GameFunctions.hideKeyboard(GameActivity.this);
                showPopup(view);
            }
        });

        FragmentManager fm = getSupportFragmentManager();

        // Add game fragment to fragment manager.
        mGameFragment = (GameFragment)fm.findFragmentByTag(TAG_GAME_FRAGMENT);

        if (mGameFragment == null)
        {
            mGameFragment = new GameFragment();
            fm.beginTransaction().add(mGameFragment, TAG_GAME_FRAGMENT).commit();
        }

        // Add chat fragment to fragment manager.
        mChatFragment = (ChatFragment)getSupportFragmentManager().findFragmentByTag(TAG_CHAT_FRAGMENT);

        if (mChatFragment == null)
        {
            mChatFragment = new ChatFragment();
            fm.beginTransaction().add(R.id.fragment_chat_container, mChatFragment, TAG_CHAT_FRAGMENT).commit();
        }

        // Initialize TTS.
        Intent intent = new Intent();
        intent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(intent, RC_TTS);

        if (savedInstanceState != null)
        {
            mShownCardIds = savedInstanceState.getStringArrayList(KEY_SHOWN_CARD_IDS);
            mPreviousShownCardIds = savedInstanceState.getStringArrayList(KEY_PREVIOUS_SHOWN_CARD_IDS);
            mPreviousNotifications = savedInstanceState.getStringArrayList(KEY_PREVIOUS_NOTIFICATIONS);
            mRound = savedInstanceState.getInt(KEY_ROUND_NUM);
            mRoundToShow = savedInstanceState.getInt(KEY_ROUND_TO_SHOW);
            mCurrentScreen = savedInstanceState.getInt(KEY_CURRENT_SCREEN);
            mCurrentCardPos = savedInstanceState.getInt(KEY_CURRENT_CARD_POS);
            mCurrentBet = savedInstanceState.getInt(KEY_CURRENT_BET);
            mFirstUpdate = savedInstanceState.getBoolean(KEY_FIRST_UPDATE);
            mShowResults = savedInstanceState.getBoolean(KEY_SHOW_RESULTS);
            mSpokenCardText = savedInstanceState.getStringArrayList(KEY_SPOKEN_CARD_TEXT);
            mBlankCardText = savedInstanceState.getString(KEY_BLANK_CARD_TEXT);
            mPictureCardUrl = savedInstanceState.getString(KEY_PICTURE_CARD_URL);
            mNewChatMessageReceived = savedInstanceState.getBoolean(KEY_NEW_CHAT_MESSAGE_RECEIVED);
            mMatch = savedInstanceState.getParcelable(Match.KEY_MATCH);
            mMatchId = savedInstanceState.getString(Match.KEY_MATCH_ID);
        }
        else
            mMatchId = getIntent().getStringExtra(Match.KEY_MATCH_ID);

        ToggleLog.d(TAG, "mPreviousNotifications: " + mPreviousNotifications + "\n" +
                         "mRound: " + mRound + "\n" +
                         "mCurrentScreen: " + mCurrentScreen + "\n" +
                         "mCurrentCardPos: " + mCurrentCardPos + "\n" +
                         "mCurrentBet: " + mCurrentBet + "\n" +
                         "mMatchId: " + mMatchId + "\n" +
                         "mFirstUpdate: " + mFirstUpdate + "\n" +
                         "mShowResults: " + mShowResults + "\n" +
                         "mRoundToShow: " + mRoundToShow + "\n" +
                         "mSpokenCardText: " + mSpokenCardText + "\n" +
                         "mBlankCardText: " + mBlankCardText + "\n" +
                         "mPictureCardUrl: " + mPictureCardUrl + "\n" +
                         "mNewChatMessageReceived: " + mNewChatMessageReceived + "\n");

        // Construct the bet popup window.
        View popupView = getLayoutInflater().inflate(R.layout.popup_bet, null);
        mBetPopup = new PopupWindow(popupView, ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT);
        mBetPopup.setBackgroundDrawable(new ShapeDrawable());
        mBetPopup.setOutsideTouchable(true);
        mBetPopup.setFocusable(true);

        // Set up bet button listeners.
        View betMinusButton = mBetPopup.getContentView().findViewById(R.id.bet_minus_button);
        View betPlusButton = mBetPopup.getContentView().findViewById(R.id.bet_plus_button);
        betMinusButton.setOnClickListener(new ButtonClickListener());
        betPlusButton.setOnClickListener(new ButtonClickListener());

        // Update the bet amount view with the current bet amount.
        mBetAmountView = (TextView)mBetPopup.getContentView().findViewById(R.id.bet_amount);
        mBetAmountView.setText(String.valueOf(mCurrentBet));
    }

    // TODO: Showing old round upon resuming from a destroyed process.
    @Override
    public void onResume()
    {
        super.onResume();

        if (mPauseCount != 1)
        {
            ToggleLog.d(TAG, "onResume()");

            mPaused = false;

            NotificationService.setCallbackHandler(this);

            if (mGameFragment.isNewInstance())
            {
                mGameFragment.setNewInstance(false);
                mChatFragment.setSendingMessage(false);

                // If process is destroyed when a second blank or picture card has been filled (but not yet submitted),
                // and then subsequently resumed, the second card text or picture will be set on the first blank or picture card.
                // This prevents that from happening.
                mBlankCardText = "";
                mPictureCardUrl = "";

                loadMatch();
            }
            else
                updateGameData();

            if (mCurrentScreen == SCREEN_GAME)
                showGame();
            else if (mCurrentScreen == SCREEN_CHAT)
                showChat();

            // If we got a notification for this match while we were paused, we need to clear it.
            if (mMatchId.equals(NotificationService.getMatchIdToClear()))
            {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

                if (prefs.contains(NotificationService.KEY_NOTIFICATION_IDS))
                    prefs.edit().remove(NotificationService.KEY_NOTIFICATION_IDS).commit();

                NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(0);

                NotificationService.setMatchIdToClear(null);
            }
        }
    }

    @Override
    public void onDestroy()
    {
        ToggleLog.d(TAG, "onDestroy()");
        cleanup();
        super.onDestroy();
    }

    @Override
    public void onPause()
    {
        super.onPause();

        if (mPauseCount > 0)
        {
            ToggleLog.d(TAG, "onPause()");

            mPaused = true;

            if (mTts != null)
                mTts.stop();

            clearNotifications();
            stopLastChatMessageCycle();
            stopAutoPickSkipTimeoutCycle();

            mBetPopup.dismiss();

            if (mNotificationDialog != null)
                mNotificationDialog.dismiss();
        }

        if (mPauseCount < 2)
            mPauseCount++;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putStringArrayList(KEY_SHOWN_CARD_IDS, mShownCardIds);
        savedInstanceState.putStringArrayList(KEY_PREVIOUS_SHOWN_CARD_IDS, mPreviousShownCardIds);
        savedInstanceState.putStringArrayList(KEY_PREVIOUS_NOTIFICATIONS, mPreviousNotifications);
        savedInstanceState.putInt(KEY_ROUND_NUM, mRound);
        savedInstanceState.putInt(KEY_ROUND_TO_SHOW, mRoundToShow);
        savedInstanceState.putInt(KEY_CURRENT_SCREEN, mCurrentScreen);
        savedInstanceState.putInt(KEY_CURRENT_CARD_POS, mCurrentCardPos);
        savedInstanceState.putInt(KEY_CURRENT_BET, mCurrentBet);
        savedInstanceState.putBoolean(KEY_FIRST_UPDATE, mFirstUpdate);
        savedInstanceState.putBoolean(KEY_SHOW_RESULTS, mShowResults);
        savedInstanceState.putStringArrayList(KEY_SPOKEN_CARD_TEXT, mSpokenCardText);
        savedInstanceState.putString(KEY_BLANK_CARD_TEXT, mBlankCardText);
        savedInstanceState.putString(KEY_PICTURE_CARD_URL, mPictureCardUrl);
        savedInstanceState.putBoolean(KEY_NEW_CHAT_MESSAGE_RECEIVED, mNewChatMessageReceived);
        savedInstanceState.putParcelable(Match.KEY_MATCH, mMatch);
        savedInstanceState.putString(Match.KEY_MATCH_ID, mMatchId);
    }

    @Override
    public void onBackPressed()
    {
        Card firstSubmittedCard = mGameFragment.getFirstSubmittedCard();

        if (mCurrentScreen == SCREEN_CHAT)
        {
            showGame();
            determineControls();
        }
        else if (firstSubmittedCard != null)
        {
            if (mMatch.getWhiteCardType() == Match.WHITE_CARD_TYPE_STANDARD)
                addCard(mGameFragment.getFirstSubmittedCardPosition(), firstSubmittedCard);
            else
            {
                if (firstSubmittedCard.getType() == Card.CARD_TYPE_BLANK)
                    setBlankCardText(firstSubmittedCard.getText());
                else if (firstSubmittedCard.getType() == Card.CARD_TYPE_PICTURE)
                    setPictureCardUrl(mPictureCardUrl = firstSubmittedCard.getText());
            }

            mGameFragment.setFirstSubmittedCard(null);
            updateGameData();
        }
        else
            finish();
    }

    @Override
    public void finish()
    {
        ToggleLog.d(TAG, "finish()");
        cleanup();
        super.finish();
    }

    private void cleanup()
    {
        // Clear the handler for the push receiver.
        // We only do this if the callback handler is this, because there is the potential for an old
        // GameActivity to be destroyed *after* a new GameActivity is created. This sometimes happens
        // when joining a match via notification when there is a GameActivity already in the foreground.
        if (NotificationService.getCallbackHandler() == this)
            NotificationService.setCallbackHandler(null);

        // Shut down TTS.
        if (mTts != null)
            mTts.shutdown();

        // Stop any running cycles.
        stopLastChatMessageCycle();
        stopAutoPickSkipTimeoutCycle();
        mChatFragment.stopUpdateCycle();
    }

    public boolean isPaused()
    {
        return mPaused;
    }

    public void onMatchUpdate()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                loadMatch();
            }
        });
    }

    private void loadMatch()
    {
        int loadMatchCount = mGameFragment.getLoadMatchCount();

        // TODO: No longer disabling submit buttons here. Instead, we will want to disable them after we submit a card.
        // When submitting a card, make sure not to hide the progress bar if loading is still in progress (loadCount > 0).

        mProgressBar.setVisibility(View.VISIBLE);
        mGameFragment.incrementLoadMatchCount();

        if (loadMatchCount == 0)
            mGameFragment.loadMatch(mMatchId, mPlayerId);
    }

    /**
     * Resets local data that was modified during a turn.
     */
    private void resetTurnData()
    {
        mCurrentBet = 0;
        mBetAmountView.setText("0");
        mBlackCardTextInput.setText("");
        mBlankCardText = "";
        mPictureCardUrl = "";
        mGameFragment.setFirstSubmittedCard(null);
        mGameFragment.setFirstSubmittedCardPosition(-1);
    }

    public void onSubmitSuccess()
    {
        ToggleLog.d(TAG, "onSubmitSuccess()");
        resetTurnData();
        mGameFragment.setRetrievingUpdates(true);
        loadMatch();
    }

    public void onSubmitFailure(String message)
    {
        ToggleLog.d(TAG, "onSubmitFailure()");
        showNotification(message);
        mGameFragment.setRetrievingUpdates(true);
        loadMatch();
    }

    public void onSubmitBlackCardSuccess()
    {
        ToggleLog.d(TAG, "onSubmitBlackCardSuccess()");

        // Don't resetTurnData here because it causes the blackCardTextInput to go blank until the match loads.
        // Added a condition to onLoadMatchSuccess to resetTurnData in this case.
        // resetTurnData();

        mGameFragment.setRetrievingUpdates(true);
        loadMatch();
    }

    public void onSubmitBlackCardFailure(String message)
    {
        ToggleLog.d(TAG, "onSubmitBlackCardFailure()");
        showNotification(message);
        mGameFragment.setRetrievingUpdates(true);
        loadMatch();
    }

    public void onTossSuccess()
    {
        ToggleLog.d(TAG, "onTossSuccess()");
        resetTurnData();
        mGameFragment.setTossed(true);
        mGameFragment.setRetrievingUpdates(true);
        loadMatch();
    }

    public void onTossFailure(String message)
    {
        ToggleLog.d(TAG, "onTossFailure()");
        showNotification(message);
        mGameFragment.setRetrievingUpdates(true);
        loadMatch();
    }

    public void onReactSuccess()
    {
        ToggleLog.d(TAG, "onReactSuccess()");
        mGameFragment.setRetrievingUpdates(true);
        loadMatch();
    }

    public void onReactFailure()
    {
        ToggleLog.d(TAG, "onReactFailure()");
        hideProgressBar();
        updateGameData();
    }

    public void onForceAutoPickSkipSuccess()
    {
        ToggleLog.d(TAG, "onForceAutoPickSkipSuccess()");

        // Not entirely necessary to load match since we should be retrieving notifications.
        // But on the off chance that notifications aren't received...
        mGameFragment.setRetrievingUpdates(true);
        loadMatch();
    }

    public void onForceAutoPickSkipFailure()
    {
        ToggleLog.e(TAG, "onForceAutoPickSkipFailure()");
        hideProgressBar();

        ArrayList<String> notifications = new ArrayList<String>();
        notifications.add(getString(R.string.unable_to_force_auto_pick_skip));
        notifications.add(getString(R.string.try_again));
        showNotifications(notifications);

        updateGameData(); // TODO: Probably not necessary to call this, but can't hurt.
    }

    public void onLeaveMatchSuccess()
    {
        ToggleLog.d(TAG, "onLeaveMatchSuccess()");
        finish();
    }

    public void onLeaveMatchFailure()
    {
        ToggleLog.d(TAG, "onLeaveMatchFailure()");
        hideProgressBar();
        showNotification(getString(R.string.unable_to_leave_match));
        updateGameData(); // TODO: Probably not necessary to call this, but can't hurt.
    }

    public void onLoadMatchSuccess(Match match)
    {
        ToggleLog.d(TAG, "onLoadMatchSuccess()");

        if (mGameFragment.getLoadMatchCount() == 0)
        {
            mGameFragment.setRetrievingUpdates(false);

            hideProgressBar();

            mMatch = match;

            // TODO: Does this check still work now that you are skipping updateGameData while paused?
            if (mRound != mMatch.getRound() || isCardSubmitted() ||
               (mMatch.getState() == Match.MATCH_STATE_PICKING &&  mMatch.getBlackCardType() == Match.BLACK_CARD_TYPE_CUSTOM && isJudge()))
            {
                resetTurnData();
            }

            // Prevent jumping to the first card upon toss.
            if (mGameFragment.hasTossed())
            {
                mPreviousShownCardIds = getCardIds();
                mGameFragment.setTossed(false);
            }

            updateGameData();
        }
        else
            mGameFragment.loadMatch(mMatchId, mPlayerId);
    }

    public void onLoadMatchFailure()
    {
        ToggleLog.d(TAG, "onLoadMatchFailure()");
        hideProgressBar();
        setResult(GameConstants.RESULT_LOAD_FAILED);
        finish();
    }

    public void onRematchSuccess(Match match)
    {
        ToggleLog.d(TAG, "onRematchSuccess()");

        if (match == null)
            showNotification("Match has already been rematched.");
        else
        {
            mMatch = match;
            mMatchId = mMatch.getId();
            mRoundSelector.setAdapter(null);
            mChatFragment.clearChatMessages();
            updateGameData();
        }

        mGameFragment.setRetrievingUpdates(true);
        loadMatch();
    }

    public void onRematchFailure()
    {
        ToggleLog.d(TAG, "onRematchFailure()");
        hideProgressBar();
        showNotification(getString(R.string.unable_to_rematch));
        updateGameData(); // TODO: Probably not necessary to call this, but can't hurt.
    }

    public void showGame()
    {
        // Hide chat view.
        View chatView = findViewById(R.id.fragment_chat_container);
        chatView.setVisibility(View.GONE);

        // Stop chat update cycle and start last chat message cycle.
        mChatFragment.stopUpdateCycle();
        startLastChatMessageCycle();

        // Show progress bar if we are busy taking a turn or loading a match.
        if (mGameFragment.isBusy() || mGameFragment.isRetrievingUpdates() || mGameFragment.getLoadMatchCount() > 0)
            mProgressBar.setVisibility(View.VISIBLE);

        mCurrentScreen = SCREEN_GAME;
    }

    public void showChat()
    {
        // Show chat view.
        View chatView = findViewById(R.id.fragment_chat_container);
        chatView.setVisibility(View.VISIBLE);

        // Stop last chat message cycle.
        stopLastChatMessageCycle();

        // Remove glow from the chat button.
        mNewChatMessageReceived = false;
        hideChatGlow();

        // Start the chat update cycle.
        mChatFragment.startUpdateCycle();

        mCurrentScreen = SCREEN_CHAT;
    }

    private void hideProgressBar()
    {
        // Only hide the progress bar if we're not busy taking a turn or loading a match.
        if (!mGameFragment.isBusy() && !mGameFragment.isRetrievingUpdates() && mGameFragment.getLoadMatchCount() == 0)
            mProgressBar.setVisibility(View.GONE);
    }

    public int getRoundToShow()
    {
        return mRoundToShow;
    }

    public boolean isOnResultsScreen()
    {
        return mShowResults;
    }

    public boolean isMatchComplete()
    {
        return (mMatch.getState() == Match.MATCH_STATE_COMPLETE);
    }

    public boolean isMatchCanceled()
    {
        return mMatch.getState() == Match.MATCH_STATE_CANCELED;
    }

    // TODO: Saw a round "2" in the list while still in round 1. This happened after resuming some time later on N6P, after one player had submitted a card.
    private void setRoundAdapter()
    {
        if (mRoundSelector.getAdapter() == null)
        {
            List<String> rounds = new ArrayList<String>();

            for (int i = 1; i <= mMatch.getRound(); i++)
            {
                rounds.add(String.valueOf(i));
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(mRoundSelector.getContext(), android.R.layout.simple_spinner_item, rounds);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mRoundSelector.setAdapter(adapter);
        }
        else
        {
            ArrayAdapter<String> adapter = (ArrayAdapter<String>)mRoundSelector.getAdapter();

            if (adapter.getCount() < mMatch.getRound())
            {
                for (int i = adapter.getCount() + 1; i <= mMatch.getRound(); i++)
                {
                    adapter.add(String.valueOf(i));
                }

                adapter.notifyDataSetChanged();
            }
        }

        if (mRoundToShow != mPreviousRoundToShow)
            mRoundSelector.setSelection(mRoundToShow - 1);
    }

    private void setCardPagerAdapter()
    {
        ToggleLog.d(TAG, "setCardPagerAdapter()");

        int numCardsToShow = 0;

        if (mShowResults || isMatchComplete() || isMatchCanceled() || mMatch.getState() == Match.MATCH_STATE_JUDGING)
            numCardsToShow = mMatch.getRound(mRoundToShow).getSubmittedCards().size();
        else if (isCardSubmitted())
            numCardsToShow = 1;
        else if (!isJudge() && isParticipant())
            numCardsToShow = mMatch.getDealtCards().get(mPlayerId).size();

        mShownCardIds = getCardIds();

        if (mShowResults || isMatchComplete() || isMatchCanceled())
            mShownCardIds.add("");

        if (!mShownCardIds.equals(mPreviousShownCardIds))
        {
            if (mShowResults || isMatchComplete() || isMatchCanceled())
            {
                mCurrentCardPos = getWinningCardPos();

                // Should never be -1, but just in case...
                if (mCurrentCardPos == -1)
                    mCurrentCardPos = 0;
            }
            else
            {
                int firstSubmittedCardPos = mGameFragment.getFirstSubmittedCardPosition();

                if (firstSubmittedCardPos != -1)
                {
                    if (mGameFragment.getFirstSubmittedCard() == null && mCurrentCardPos >= firstSubmittedCardPos)
                        mCurrentCardPos++;
                }
                else
                    mCurrentCardPos = 0;
            }

            mPreviousShownCardIds = mShownCardIds;
        }

        if (mCardAdapter != null)
            mCardAdapter.removeAllFragments(numCardsToShow);

        mCardAdapter = new CardPagerAdapter(this, getSupportFragmentManager(), mCardPager, numCardsToShow);
        mCardPager.setAdapter(mCardAdapter);
        mCardPager.setOnPageChangeListener(mCardAdapter);
        mCardPager.setOffscreenPageLimit(10); // Since we will have 10 cards at most.
        mCardPager.setPageTransformer(true, mCardAdapter);
    }

    private void setPlayerAdapter()
    {
        mPlayerAdapter = new PlayerAdapter(this, new ArrayList<String>(mMatch.getRound(mRoundToShow).getPlayers().keySet()));
        mPlayerListView.setAdapter(mPlayerAdapter);
        mPlayerListView.setDivider(null);
    }

    public void onCardPagerReady()
    {
        mCardPager.setCurrentItem(mCurrentCardPos);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_TTS)
        {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS)
            {
                // The TextToSpeech object and associated functionality will be handled in the CardList.
                mTts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener()
                {
                    @Override
                    public void onInit(int status)
                    {
                        if (status == TextToSpeech.SUCCESS)
                        {
                            // This freezes the UI for a few seconds on the HTC One X.
                            // mTts.setLanguage(Locale.US);
                            mTtsReady = true;
                        }
                        else if (status == TextToSpeech.ERROR)
                        {
                            // TODO: Add error handling here.
                        }
                    }
                });
            }
            else
            {
                Intent intent = new Intent();
                intent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(intent);
            }
        }
        else if (requestCode == RC_INVITE || requestCode == RC_KICK || requestCode == RC_MATCH_SETTINGS)
        {
            // Refresh the match whether or not the result was ok, since we may have
            // sent a request to the server and then hit back before it returned.
            // We set retrieving updates to true here to prevent a subsequent invite/kick
            // or match settings update before the new match updates are retrieved.
            // This prevents being able to invite/kick the same players more
            // than once, or showing the match settings screen with stale data.
            mGameFragment.setRetrievingUpdates(true);
            loadMatch();

            if (resultCode == RESULT_OK)
            {
                if (requestCode == RC_INVITE)
                    mNotificationDialog = GameFunctions.showDialog(this, getString(R.string.invite_success_title), getString(R.string.invite_success_description));
                else if (requestCode == RC_KICK)
                    mNotificationDialog = GameFunctions.showDialog(this, getString(R.string.kick_success_title), getString(R.string.kick_success_description));
            }
        }
    }

    /*
        Any local changes to the match object will be overwritten when other players take their turns (because of notifications / load match requests coming in and overwriting the match object).
        We need to ignore loads (queue them up) while the player is in the middle of updating the match. Loads should only be done after the player has updated the match.
     */
    public void updateGameData()
    {
        ToggleLog.d(TAG, "updateGameData: Entered");

        if (mPaused || mMatch == null)
            return;

        int matchState = mMatch.getState();
        int round = mMatch.getRound();
        int latestUnseenRound = round;

        if (matchState == Match.MATCH_STATE_EXPIRED)
        {
            ToggleLog.d(TAG, "updateGameData: Match expired.");
            setResult(GameConstants.RESULT_EXPIRED);
            finish();
            return;
        }

        if ((!mMatch.getPlayerIds().contains(mPlayerId) && !mMatch.getPendingJoinIds().contains(mPlayerId)) ||
            mMatch.getPendingLeaveIds().contains(mPlayerId))
        {
            ToggleLog.d(TAG, "updateGameData: Removed from match.");
            setResult(GameConstants.RESULT_REMOVED);
            finish();
            return;
        }

        if (mFirstUpdate && !isParticipant())
            markAllRoundsSeen();

        // TODO: Test process being destroyed while viewing current and previous rounds.
        if (!mShowResults)
        {
            List<Integer> roundResultsSeen = readRoundResultsSeen();

            for (int i = round - 1; i > 0; i--)
            {
                if (!roundResultsSeen.contains(i))
                {
                    latestUnseenRound = i;
                    break;
                }
            }

            // TODO: Maybe adding !mShowResults to this condition, to avoid messages being shown while on results screen. Think about this and make sure that it won't break anything.
            // if (prefs.get(KEY_LAST_ROUND_RESULTS_SEEN + mMatchId, 1) != round && !isMatchComplete())
            if (latestUnseenRound < round) // TODO: May need to add "|| matchState == COMPLETE" to this. But test first to see if it's necessary.
            {
                roundResultsSeen.add(latestUnseenRound);
                writeRoundResultsSeen(roundResultsSeen);
                mShowResults = true;
            }

            mRoundToShow = latestUnseenRound;
        }

        if (isParticipant())
        {
            if (matchState == Match.MATCH_STATE_PICKING && !isJudge())
            {
                Card firstSubmittedCard = mGameFragment.getFirstSubmittedCard();

                if (mMatch.getWhiteCardType() == Match.WHITE_CARD_TYPE_STANDARD && getBlackCard().getType() == Card.CARD_TYPE_PICK_TWO && firstSubmittedCard != null)
                    removeCard(firstSubmittedCard);

                if (!isCardSubmitted())
                    getCurrentPlayer().setBet(mCurrentBet);
            }

            setBlankCardText(mBlankCardText);
            setPictureCardUrl(mPictureCardUrl);
        }

        setRoundAdapter();

        // TODO: This is a hack.
        setCardPagerAdapter();
        setCardPagerAdapter();

        setPlayerAdapter();

        refreshBlackCard();

        // Determine which controls should be active.
        determineControls();

        // Show notifications as necessary.
        // TODO: Need to avoid showing any new notifications while on result screen.
        showNotifications();

        // Start auto pick timeout cycle.
        startAutoPickSkipTimeoutCycle();

        if (mFirstUpdate)
            updateLaunchCount();

        // Sync up local round number with global round number.
        mRound = round;
        mPreviousRoundToShow = mRoundToShow;
        mFirstUpdate = false;
    }

    private void updateLaunchCount()
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int launchCount = prefs.getInt(KEY_LAUNCH_COUNT, 0);
        final int rateTarget = prefs.getInt(KEY_RATE_TARGET, 10);

        if (++launchCount == rateTarget)
        {
            final Dialog dialog = new Dialog(this);

            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_rate);

            int width = (int)(getResources().getDisplayMetrics().widthPixels * .9);
            int height = dialog.getWindow().getAttributes().height;

            dialog.getWindow().setLayout(width, height);

            View rateButton = dialog.findViewById(R.id.rate_button);
            View laterButton = dialog.findViewById(R.id.later_button);
            View noButton = dialog.findViewById(R.id.no_button);

            rateButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    prefs.edit().putInt(KEY_RATE_TARGET, 0).commit();
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
                    dialog.dismiss();
                }
            });

            laterButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    dialog.dismiss();
                }
            });

            noButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    prefs.edit().putInt(KEY_RATE_TARGET, 0).commit();
                    dialog.dismiss();
                }
            });

            // Set the rate target to later. This handles the user tapping later or otherwise dismissing the dialog.
            prefs.edit().putInt(KEY_RATE_TARGET, rateTarget + 10).commit();

            dialog.show();
        }

        prefs.edit().putInt(KEY_LAUNCH_COUNT, launchCount).commit();
    }

    private List<Integer> readRoundResultsSeen()
    {
        List<Integer> roundResultsSeen = new ArrayList<Integer>();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String roundResultsSeenStr = prefs.getString(KEY_ROUND_RESULTS_SEEN + mMatchId, "");

        if (!roundResultsSeenStr.isEmpty())
        {
            String[] roundResultsSeenArr = roundResultsSeenStr.split(",");

            for (String roundResultSeen : roundResultsSeenArr)
            {
                roundResultsSeen.add(Integer.parseInt(roundResultSeen));
            }
        }

        return roundResultsSeen;
    }

    private void writeRoundResultsSeen(List<Integer> roundResultsSeen)
    {
        if (roundResultsSeen.isEmpty())
            return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        StringBuilder builder = new StringBuilder(roundResultsSeen.size() * 2 - 1);

        for (int i = 0; i < roundResultsSeen.size(); i++)
        {
            if (i < roundResultsSeen.size() - 1)
                builder.append(roundResultsSeen.get(i)).append(",");
            else
                builder.append(roundResultsSeen.get(i));
        }

        prefs.edit().putString(KEY_ROUND_RESULTS_SEEN + mMatchId, builder.toString()).commit();
    }

    private void markAllRoundsSeen()
    {
        List<Integer> roundResultsSeen = new ArrayList<Integer>();

        // Using mMatch.getRound rather than mRound because mRound may not
        // have been synced yet depending on when we call this method.
        for (int i = 1; i < mMatch.getRound(); i++)
        {
            roundResultsSeen.add(i);
        }

        writeRoundResultsSeen(roundResultsSeen);
    }

    public Player getCurrentPlayer()
    {
        return mMatch.getCurrentRound().getPlayers().get(mPlayerId);
    }

    public String getPlayerId()
    {
        return mPlayerId;
    }

    // Returns the number of attendees in the match.
    private int getAttendeeCount()
    {
        int autoMatchSlots = mMatch.getAutoMatchSlots();
        List<String> playerIds = filterUserIds(mMatch.getPlayerIds());
        List<String> pendingJoinIds = mMatch.getPendingJoinIds();
        List<String> pendingLeaveIds = mMatch.getPendingLeaveIds();

        int attendeeCount = autoMatchSlots + playerIds.size() + pendingJoinIds.size() - pendingLeaveIds.size();

        return attendeeCount;
    }

    private List<String> filterUserIds(List<String> ids)
    {
        List<String> userIds = new ArrayList<String>();

        for (String id : ids)
        {
            if (!id.startsWith("bot_"))
                userIds.add(id);
        }

        return userIds;
    }

    public boolean isJudge()
    {
        return mPlayerId.equals(mMatch.getCurrentRound().getJudgeId());
    }

    private boolean isParticipant()
    {
        return mMatch.getPlayerIds().contains(mPlayerId);
    }

    private boolean isPendingJoin()
    {
        return mMatch.getPendingJoinIds().contains(mPlayerId);
    }

    public void setBlankCardText(String text)
    {
        mBlankCardText = text;

        List<Card> cards = mMatch.getDealtCards().get(mPlayerId);

        for (Card card : cards)
        {
            if (card.getId().startsWith("blank_"))
            {
                card.setText(mBlankCardText);
                break;
            }
        }
    }

    public void setPictureCardUrl(String url)
    {
        mPictureCardUrl = url;

        List<Card> cards = mMatch.getDealtCards().get(mPlayerId);

        for (Card card : cards)
        {
            if (card.getId().startsWith("picture_"))
            {
                card.setText(mPictureCardUrl);
                break;
            }
        }
    }

    // TODO: Some of these can probably be moved to GameFunctions.
    private void addCard(int position, Card card)
    {
        List<Card> cards = mMatch.getDealtCards().get(mPlayerId);
        cards.add(position, card);
    }

    private void removeCard(Card card)
    {
        List<Card> cards = mMatch.getDealtCards().get(mPlayerId);
        cards.remove(card);
    }

    public int getWinningCardPos()
    {
        Round round = mMatch.getRound(mRoundToShow);
        String roundWinnerId = round.getWinnerId();
        Iterator<String> iter = round.getSubmittedCards().keySet().iterator();
        int position = 0;

        while (iter.hasNext())
        {
            if (iter.next().equals(roundWinnerId))
                return position;

            position++;
        }

        return -1;
    }

    private String getPlace()
    {
        Round round = mMatch.getCurrentRound();

        int place = 0;
        int previousPlayerScore = -1;

        for (String playerId : round.getPlayers().keySet())
        {
            Player player = round.getPlayers().get(playerId);
            int score = player.getScore();

            if (score != previousPlayerScore)
            {
                place++;
                previousPlayerScore = score;
            }

            if (playerId.equals(mPlayerId))
                break;
        }

        switch (place)
        {
            case 1: return "1st";
            case 2: return "2nd";
            case 3: return "3rd";
            case 4: return "4th";
            case 5: return "5th";
            case 6: return "6th";
            case 7: return "7th";
            case 8: return "8th";
        }

        return "Unknown";
    }

    public boolean isTied()
    {
        Round round = mMatch.getCurrentRound();
        int score = getCurrentPlayer().getScore();

        for (String playerId : round.getPlayers().keySet())
        {
            Player player = round.getPlayers().get(playerId);

            if (!playerId.equals(mPlayerId) && player.getScore() == score)
                return true;
        }

        return false;
    }

    private void refreshBlackCard()
    {
        String blackCardText = mMatch.getRound(mRoundToShow).getBlackCard().getText();
        Card firstSubmittedCard = mGameFragment.getFirstSubmittedCard();
        setBlackCardBottomWeight(1);

        mBlackCardText.setVisibility(View.VISIBLE);
        mBlackCardTextInput.setVisibility(View.GONE);

        if (!mShowResults && !isMatchComplete() && !isMatchCanceled())
        {
            if (mMatch.getState() == Match.MATCH_STATE_WRITING_CARD)
            {
                if (isJudge())
                {
                    mBlackCardText.setVisibility(View.GONE);
                    mBlackCardTextInput.setVisibility(View.VISIBLE);

                    // This is done so that we can tap anywhere on the card to input text.
                    mBlackCardTextInput.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT));

                    setBlackCardBottomWeight(0);
                }
                else
                {
                    Round round = mMatch.getCurrentRound();
                    String judgeId = round.getJudgeId();
                    String judgeName = round.getPlayers().get(judgeId).getName();
                    blackCardText = "Waiting for " + judgeName + " to write card.";
                }
            }
            else if (mMatch.getState() == Match.MATCH_STATE_PICKING && (isJudge() || !isParticipant()))
                setBlackCardBottomWeight(0);
            else if (firstSubmittedCard != null)
            {
                String cardText1 = firstSubmittedCard.getText().replaceAll("\\.$", "");

                if (firstSubmittedCard.getType() == Card.CARD_TYPE_PICTURE)
                    cardText1 = "<a href=\"" + cardText1 + "\">Image</a>";

                blackCardText = blackCardText.replaceFirst("_+", cardText1);
            }
        }

        mBlackCardText.setText(Html.fromHtml(blackCardText));
    }

    private void refreshBet()
    {
        mBetAmountView.setText(String.valueOf(mCurrentBet));
        getCurrentPlayer().setBet(mCurrentBet);
        mPlayerAdapter.notifyDataSetChanged();
    }

    private void setBlackCardBottomWeight(float weight)
    {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, weight);
        mBlackCardBottom.setLayoutParams(lp);
    }

    private void showChatGlow()
    {
        float density = getResources().getDisplayMetrics().density;

        if (density <= 1.5)
            mChatButton.setShadowLayer(6, 0, 0, Color.WHITE);
        else if (density <= 2.0)
            mChatButton.setShadowLayer(20, 0, 0, Color.WHITE);
        else
            mChatButton.setShadowLayer(24, 0, 0, Color.WHITE);
    }

    private void hideChatGlow()
    {
        mChatButton.setShadowLayer(0, 0, 0, 0);
    }

    public Match getMatch()
    {
        return mMatch;
    }

    public static String getMatchId()
    {
        return mMatchId;
    }

    public List<Card> getCards()
    {
        Round round = mMatch.getRound(mRoundToShow);
        List<Card> cards = new ArrayList<Card>();

        if (mShowResults || isMatchComplete() || isMatchCanceled() || mMatch.getState() == Match.MATCH_STATE_JUDGING)
        {
            for (String playerId : round.getSubmittedCards().keySet())
            {
                cards.addAll(round.getSubmittedCards().get(playerId));
            }
        }
        else if (isCardSubmitted())
            cards.addAll(round.getSubmittedCards().get(mPlayerId));
        else if (!isJudge() && isParticipant())
            cards.addAll(mMatch.getDealtCards().get(mPlayerId));

        return cards;
    }

    private ArrayList<String> getCardIds()
    {
        ArrayList<String> cardIds = new ArrayList<String>();
        List<Card> cards = getCards();

        for (Card card : cards)
        {
            cardIds.add(card.getId());
        }

        return cardIds;
    }

    private Card getBlackCard()
    {
        return mMatch.getRound(mRoundToShow).getBlackCard();
    }

    private List<Card> getSelectedCards()
    {
        List<Card> cards = getCards();
        List<Card> selectedCards = new ArrayList<Card>();

        if (!cards.isEmpty())
        {
            if (shouldShowPickTwoCards())
            {
                selectedCards.add(cards.get(mCurrentCardPos * 2));
                selectedCards.add(cards.get(mCurrentCardPos * 2 + 1));
            }
            else
                selectedCards.add(cards.get(mCurrentCardPos));
        }

        return selectedCards;
    }

    public String getCardOwnerId(Card card)
    {
        Round round = mMatch.getRound(mRoundToShow);

        for (String playerId : round.getSubmittedCards().keySet())
        {
            for (Card c : round.getSubmittedCards().get(playerId))
            {
                if (c.equals(card))
                    return playerId;
            }
        }

        return null;
    }

    public boolean shouldShowPickTwoCards()
    {
        if (mShowResults || isMatchComplete() || isMatchCanceled() || mMatch.getState() == Match.MATCH_STATE_JUDGING || isCardSubmitted())
        {
            if (mMatch.getRound(mRoundToShow).getBlackCard().getType() == Card.CARD_TYPE_PICK_TWO)
                return true;
        }

        return false;
    }

    public boolean isCardSubmitted()
    {
        return isCardSubmitted(mPlayerId);
    }

    public boolean isCardSubmitted(String playerId)
    {
        Round round = mMatch.getCurrentRound();
        return round.getSubmittedCards().containsKey(playerId) && !round.getSubmittedCards().get(playerId).isEmpty();
    }

    public boolean actionRequired()
    {
        if ((mMatch.getState() == Match.MATCH_STATE_PICKING && !isJudge() && !isCardSubmitted()) ||
            (mMatch.getState() == Match.MATCH_STATE_JUDGING || mMatch.getState() == Match.MATCH_STATE_WRITING_CARD) && isJudge())
        {
            return true;
        }
        else
            return false;
    }

    private void submit()
    {
        List<Card> selectedCards = getSelectedCards();

        if (!isJudge())
        {
            if (!selectedCards.get(0).isEmpty())
            {
                setBlankCardText(mBlankCardText.trim());

                if (getBlackCard().getType() == Card.CARD_TYPE_PICK_TWO)
                {
                    if (mGameFragment.getFirstSubmittedCard() == null)
                    {
                        mGameFragment.setFirstSubmittedCard(selectedCards.get(0));
                        mGameFragment.setFirstSubmittedCardPosition(mCurrentCardPos);

                        // If this is a custom match, we need to clear the submitted card text.
                        if (mMatch.getWhiteCardType() != Match.WHITE_CARD_TYPE_STANDARD)
                        {
                            if (selectedCards.get(0).getType() == Card.CARD_TYPE_BLANK)
                                setBlankCardText("");
                            else if (selectedCards.get(0).getType() == Card.CARD_TYPE_PICTURE)
                                setPictureCardUrl("");
                        }

                        updateGameData();
                        return;
                    }
                    else
                    {
                        if (mMatch.getWhiteCardType() != Match.WHITE_CARD_TYPE_STANDARD)
                        {
                            Card firstSubmittedCard = mGameFragment.getFirstSubmittedCard();
                            Card secondSubmittedCard = selectedCards.get(0);

                            if (firstSubmittedCard.getType() == secondSubmittedCard.getType())
                            {
                                // Need to increment the second card's id so that it's different from the first card's id.
                                String id = secondSubmittedCard.getId();
                                String idPrefix = id.substring(0, id.lastIndexOf("_") + 1);
                                String idSuffix = id.substring(id.lastIndexOf("_") + 1, id.length());
                                int idNum = Integer.parseInt(idSuffix) + 1;
                                id = idPrefix + idNum;
                                selectedCards.get(0).setId(id);
                            }
                        }

                        selectedCards.add(0, mGameFragment.getFirstSubmittedCard());
                    }
                }
            }
            else
            {
                determineControls();
                return;
            }
        }

        mProgressBar.setVisibility(View.VISIBLE);
        mGameFragment.submit(mMatchId, mPlayerId, mRound, mCurrentBet, selectedCards);
    }

    private void submitBlackCard()
    {
        Card blackCard = getBlackCard();
        String blackCardText = mBlackCardTextInput.getText().toString().trim();
        blackCardText = blackCardText.replaceAll("_{2,}", "_");

        int numBlanks = blackCardText.length() - blackCardText.replace("_", "").length();

        if (numBlanks > 2)
        {
            mNotificationDialog = GameFunctions.showDialog(this, getString(R.string.unable_to_submit), getString(R.string.max_two_blanks));
            determineControls();
        }
        else
        {
            blackCardText = blackCardText.replaceAll("_", "____________");
            blackCard.setText(blackCardText);
            mGameFragment.submitBlackCard(mMatchId, mPlayerId, mRound, blackCard);
        }
    }

    private void toss()
    {
        mProgressBar.setVisibility(View.VISIBLE);
        mGameFragment.toss(mMatchId, mPlayerId, mRound, getSelectedCards());
    }

    public void react(String cardId, int reaction)
    {
        disableSubmitButtons();
        mProgressBar.setVisibility(View.VISIBLE);
        mGameFragment.react(mMatchId, mPlayerId, cardId, reaction, mRound);
    }

    // TODO: May want to return null when selectedCards is empty.
    // Method is safe as is, but future changes to dependent methods may make it not safe.
    private String getCompleteCardText()
    {
        Card blackCard = getBlackCard();
        List<Card> selectedCards = getSelectedCards();
        String completeText = blackCard.getText();

        if (shouldShowPickTwoCards())
        {
            String cardText1 = selectedCards.get(0).getText();
            String cardText2 = selectedCards.get(1).getText();
            completeText = completeText.replaceFirst("_+", cardText1);
            completeText = completeText.replaceFirst("_+", cardText2);
        }
        else if (!selectedCards.isEmpty() && !selectedCards.get(0).isEmpty())
        {
            String whiteCardText = selectedCards.get(0).getText();

            if (blackCard.getType() == Card.CARD_TYPE_PICK_TWO &&
                mGameFragment.getFirstSubmittedCard() != null)
            {
                String cardText1 = mGameFragment.getFirstSubmittedCard().getText();
                completeText = completeText.replaceFirst("_+", cardText1);
                completeText = completeText.replaceFirst("_+", whiteCardText);
            }
            else if (blackCard.getType() == Card.CARD_TYPE_PICK_ONE)
            {
                whiteCardText = whiteCardText.substring(0, whiteCardText.length() - 1);
                completeText = completeText.replaceFirst("_+", whiteCardText);
            }
            else if (blackCard.getType() == Card.CARD_TYPE_QUESTION)
                completeText += " " + whiteCardText;
            else
                completeText = null;
        }
        else
            completeText = null;

        return completeText;
    }

    // TODO: May want to return when selectedCards is empty.
    // Method is safe as is, but future changes to dependent methods may make it not safe.
    public void speak()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Only speak card text if TTS is enabled in settings. Also make sure that TTS is ready and that we're on the game screen.
        if (prefs.getBoolean(SettingsActivity.KEY_TTS_ENABLED, false) && mTtsReady && mCurrentScreen == SCREEN_GAME)
        {
            List<Card> selectedCards = getSelectedCards();

            // Can't read a picture card, so return if we have one selected.
            for (Card card : selectedCards)
            {
                if (card.getType() == Card.CARD_TYPE_PICTURE)
                    return;
            }

            String textToSpeak = null;
            boolean readWhiteOnly = prefs.getBoolean(SettingsActivity.KEY_TTS_READ_WHITE_ONLY, false);

            // No black card to read in MATCH_STATE_WRITING_CARD, so read white cards only.
            if (mMatch.getState() == Match.MATCH_STATE_WRITING_CARD && mRoundToShow == mMatch.getRound())
                readWhiteOnly = true;

            if (shouldShowPickTwoCards())
            {
                if (readWhiteOnly)
                {
                    String cardText1 = selectedCards.get(0).getText();
                    String cardText2 = selectedCards.get(1).getText();
                    textToSpeak = cardText1 + " " + cardText2;
                }
                else
                    textToSpeak = getCompleteCardText();
            }
            else if (!selectedCards.isEmpty() && !selectedCards.get(0).isEmpty())
            {
                if (readWhiteOnly)
                    textToSpeak = selectedCards.get(0).getText();
                else
                    textToSpeak = getCompleteCardText();
            }

            if (textToSpeak != null)
            {
                boolean alreadySpoken = mSpokenCardText.contains(textToSpeak);

                // If the 'read once only' option is set, only read the card if it hasn't already been read.
                if (!prefs.getBoolean(SettingsActivity.KEY_TTS_READ_ONCE, false) || !alreadySpoken)
                {
                    mTts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null);

                    if (!alreadySpoken)
                        mSpokenCardText.add(textToSpeak);
                }
            }
        }
    }

    // TODO: Notifications no longer show after match has been completed and you switch between rounds.
    // TODO: Notifications are wrong after match has been completed and you switch between rounds.
    private void showNotifications()
    {
        Round round = mMatch.getRound(mRoundToShow);
        ArrayList<String> notifications = new ArrayList<String>();

        if (isPendingJoin() && !mShowResults && !isMatchComplete() && !isMatchCanceled())
            notifications.add("You will be added next round.");
        else if (mShowResults || isMatchComplete() || isMatchCanceled())
        {
            String roundWinnerId = round.getWinnerId();
            Player roundWinner = round.getPlayers().get(roundWinnerId);
            String roundWinnerName = roundWinner.getName();

            // A player may join the match and replace the bot name, hence we need to regenerate it.
            if (roundWinnerId.startsWith("bot_"))
                roundWinnerName = "Bot " + roundWinnerId.substring(roundWinnerId.indexOf("_") + 1);

            if (isMatchComplete() && mRoundToShow == mMatch.getRound())
            {
                if (mPlayerId.equals(roundWinnerId))
                    notifications.add("You won the match!");
                else
                {
                    notifications.add(roundWinnerName + " won the match!");

                    // Make sure that player is an active participant of the match before determining
                    // what place they came in. This is to prevent determining the place of a player
                    // who joined after the match had already ended.
                    if (isParticipant())
                    {
                        String place = getPlace();

                        if (isTied())
                            notifications.add("You tied for " + place + " place.");
                        else
                            notifications.add("You came in " + place + " place.");
                    }
                }
            }
            else
            {
                if (isMatchCanceled() && mRoundToShow == mMatch.getRound())
                    notifications.add("Match has been canceled.");

                if (mPlayerId.equals(roundWinnerId))
                    notifications.add("You won the round!");
                else
                    notifications.add(roundWinnerName + " won the round!");
            }
        }
        else
        {
            if (isJudge())
            {
                if (mMatch.getState() == Match.MATCH_STATE_PICKING)
                {
                    if (mMatch.getBlackCardType() == Match.BLACK_CARD_TYPE_STANDARD)
                        notifications.add("You are the judge.");

                    notifications.add("Wait for players to submit cards.");
                }
                else if (mMatch.getState() == Match.MATCH_STATE_JUDGING)
                {
                    notifications.add("Pick a winner.");

                    /*
                    if (getBlackCard().getType() == Card.CARD_TYPE_PICK_TWO)
                        notifications.add("Pick a winning card pair.");
                    else
                        notifications.add("Pick a winning card.");
                    */
                }
                else if (mMatch.getState() == Match.MATCH_STATE_WRITING_CARD)
                {
                    notifications.add("You are the judge.");
                    notifications.add("Write black card.");
                }
            }
            else
            {
                if (mMatch.getState() == Match.MATCH_STATE_PICKING)
                {
                    if (isCardSubmitted())
                        notifications.add("Wait for players to submit cards.");
                    else
                    {
                        if (getBlackCard().getType() == Card.CARD_TYPE_PICK_TWO)
                            notifications.add("Pick two cards.");
                        else
                            notifications.add("Pick a card.");
                    }
                }
                else if (mMatch.getState() == Match.MATCH_STATE_JUDGING)
                {
                    notifications.add("Judging in progress.");
                    notifications.add("See submitted cards above.");
                }
                else if (mMatch.getState() == Match.MATCH_STATE_WRITING_CARD)
                    notifications.add("Wait for judge to write card.");
            }
        }

        if (notifications.size() > 0 && !notifications.equals(mPreviousNotifications))
        {
            showNotifications(notifications);
            mPreviousNotifications = notifications;
        }
    }

    private void showNotification(String notification)
    {
        ArrayList<String> notifications = new ArrayList<String>();
        notifications.add(notification);
        showNotifications(notifications);
    }

    private void showNotifications(final List<String> notifications)
    {
        if (mNotificationsInProgress || notifications.size() == 0)
            return;

        mNotificationsInProgress = true;
        mClearNotifications = false;

        a4.setAnimationListener(new Animation.AnimationListener()
        {
            int i = 0;

            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation)
            {
                if (!mClearNotifications)
                {
                    a5.setAnimationListener(new Animation.AnimationListener()
                    {
                        @Override
                        public void onAnimationStart(Animation animation)
                        {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation)
                        {
                            if (!mClearNotifications)
                            {
                                a6.setAnimationListener(new Animation.AnimationListener()
                                {
                                    @Override
                                    public void onAnimationStart(Animation animation)
                                    {
                                    }

                                    @Override
                                    public void onAnimationEnd(Animation animation)
                                    {
                                        if (!mClearNotifications)
                                        {
                                            if (++i < notifications.size())
                                            {
                                                mNotification.setText(notifications.get(i));
                                                mNotification.startAnimation(a5);
                                                return;
                                            }

                                            a10.setAnimationListener(new Animation.AnimationListener()
                                            {
                                                @Override
                                                public void onAnimationStart(Animation animation)
                                                {
                                                }

                                                @Override
                                                public void onAnimationEnd(Animation animation)
                                                {
                                                    if (!mClearNotifications)
                                                    {
                                                        mSubmitButton.clearAnimation();
                                                        mBetButton.clearAnimation();
                                                        mTossButton.clearAnimation();
                                                        mChatButton.clearAnimation();
                                                        mNotification.clearAnimation();

                                                        onNotificationEnd();
                                                    }
                                                }

                                                @Override
                                                public void onAnimationRepeat(Animation animation)
                                                {
                                                }
                                            });

                                            mSubmitButton.setVisibility(View.VISIBLE);
                                            mBetButton.setVisibility(View.VISIBLE);
                                            mTossButton.setVisibility(View.VISIBLE);
                                            mChatButton.setVisibility(View.VISIBLE);

                                            mNotification.setText("");
                                            mNotification.setVisibility(View.GONE);

                                            mChatButton.startAnimation(a7);
                                            mTossButton.startAnimation(a8);
                                            mBetButton.startAnimation(a9);
                                            mSubmitButton.startAnimation(a10);
                                        }
                                    }

                                    @Override
                                    public void onAnimationRepeat(Animation animation)
                                    {
                                    }
                                });

                                mNotification.startAnimation(a6);
                            }
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation)
                        {
                        }
                    });

                    mSubmitButton.setVisibility(View.GONE);
                    mBetButton.setVisibility(View.GONE);
                    mTossButton.setVisibility(View.GONE);
                    mChatButton.setVisibility(View.GONE);

                    mNotification.setText(notifications.get(i));
                    mNotification.setVisibility(View.VISIBLE);
                    mNotification.startAnimation(a5);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        // Before starting the animations, disable clicking of all buttons on the control panel.
        disableControlPanelClick();

        mChatButton.startAnimation(a1);
        mTossButton.startAnimation(a2);
        mBetButton.startAnimation(a3);
        mSubmitButton.startAnimation(a4);
    }

    private void clearNotifications()
    {
        mClearNotifications = true;

        a1.cancel();
        a2.cancel();
        a3.cancel();
        a4.cancel();
        a5.cancel();
        a6.cancel();
        a7.cancel();
        a8.cancel();
        a9.cancel();
        a10.cancel();

        mSubmitButton.clearAnimation();
        mBetButton.clearAnimation();
        mTossButton.clearAnimation();
        mChatButton.clearAnimation();
        mNotification.clearAnimation();

        mSubmitButton.setVisibility(View.VISIBLE);
        mBetButton.setVisibility(View.VISIBLE);
        mTossButton.setVisibility(View.VISIBLE);
        mChatButton.setVisibility(View.VISIBLE);
        mNotification.setVisibility(View.GONE);

        onNotificationEnd();
    }

    private void onNotificationEnd()
    {
        mNotificationsInProgress = false;
        mControlPanelOverlay.setVisibility(View.GONE);

        // Make a call to determineControls() to re-enable the clicking of the control panel buttons.
        determineControls();
    }

    private void determineControls()
    {
        disableSubmitButtons();

        // Set default submit button text.
        mSubmitButton.setText(getString(R.string.submit));

        // Chat button will always be active.
        mChatButton.setClickable(true);

        // Set the chat button color based on whether or not a new chat message was received.
        if (mNewChatMessageReceived)
            showChatGlow();
        else
            hideChatGlow();

        // mMatch will be null upon initial entry into match while it's loading for the first time.
        // We check for null here to avoid a crash upon config change while the initial load is in progress.
        if (mMatch == null || mGameFragment.isBusy() || mGameFragment.isRetrievingUpdates())
            return;

        if (mShowResults)
        {
            mSubmitButton.setText(getString(R.string.resume));

            if (mCurrentScreen != SCREEN_CHAT)
            {
                mSubmitButton.setClickable(true);
                mSubmitButton.setTextColor(ContextCompat.getColor(this, R.color.control_panel_text));
            }
        }
        else if (isParticipant())
        {
            if (isMatchComplete())
            {
                mSubmitButton.setText(getString(R.string.rematch));

                if (!mMatch.hasRematched() && mCurrentScreen != SCREEN_CHAT)
                {
                    mSubmitButton.setClickable(true);
                    mSubmitButton.setTextColor(ContextCompat.getColor(this, R.color.control_panel_text));
                }
            }
            else
            {
                if (mMatch.getState() == Match.MATCH_STATE_PICKING && getBlackCard().getType() == Card.CARD_TYPE_PICK_TWO && !isJudge() && !isCardSubmitted())
                {
                    if (mGameFragment.getFirstSubmittedCard() == null)
                        mSubmitButton.setText(getString(R.string.submit1));
                    else
                        mSubmitButton.setText(getString(R.string.submit2));
                }

                if (mCurrentScreen != SCREEN_CHAT && actionRequired())
                {
                    mSubmitButton.setClickable(true);
                    mSubmitButton.setTextColor(ContextCompat.getColor(this, R.color.control_panel_text));

                    if (mMatch.getState() == Match.MATCH_STATE_PICKING &&
                        getCurrentPlayer().getScore() >= GameConstants.POINT_VALUE)
                    {
                        mBetButton.setClickable(true);
                        mBetButton.setTextColor(ContextCompat.getColor(this, R.color.control_panel_text));

                        if (mMatch.getWhiteCardType() == Match.WHITE_CARD_TYPE_STANDARD)
                        {
                            mTossButton.setClickable(true);
                            mTossButton.setTextColor(ContextCompat.getColor(this, R.color.control_panel_text));
                        }
                    }
                }
            }
        }
    }

    private void disableControlPanelClick()
    {
        mControlPanelOverlay.setVisibility(View.VISIBLE);

        // Probably not necessary since we have the overlay now, but it can't hurt.
        mSubmitButton.setClickable(false);
        mBetButton.setClickable(false);
        mTossButton.setClickable(false);
        mChatButton.setClickable(false);
    }

    private void disableSubmitButtons()
    {
        mSubmitButton.setClickable(false);
        mBetButton.setClickable(false);
        mTossButton.setClickable(false);

        mSubmitButton.setTextColor(ContextCompat.getColor(this, R.color.disabled_text));
        mBetButton.setTextColor(ContextCompat.getColor(this, R.color.disabled_text));
        mTossButton.setTextColor(ContextCompat.getColor(this, R.color.disabled_text));
    }

    public void setCurrentCardPosition(int position)
    {
        mCurrentCardPos = position;
    }

    private void addMatchExtras(Intent intent)
    {
        intent.putExtra(Match.KEY_MATCH_ID, mMatch.getId());
        intent.putExtra(Match.JSON_NAME, mMatch.getName());
        intent.putExtra(Match.JSON_AUTO_MATCH_SLOTS, mMatch.getAutoMatchSlots());
        intent.putExtra(Match.JSON_BLACK_CARD_TYPE, mMatch.getBlackCardType());
        intent.putExtra(Match.JSON_WHITE_CARD_TYPE, mMatch.getWhiteCardType());
        intent.putExtra(Match.JSON_INACTIVE_MODE, mMatch.getInactiveMode());
        intent.putExtra(Match.JSON_AUTO_PICK_SKIP_TIMEOUT, mMatch.getAutoPickSkipTimeout());
        intent.putExtra(Match.JSON_EXCLUDE_HOURS_BEGIN, mMatch.getExcludeHoursBegin());
        intent.putExtra(Match.JSON_EXCLUDE_HOURS_END, mMatch.getExcludeHoursEnd());
    }

    public void showPopup(View view)
    {
        if (mMatch == null)
            return;

        PopupMenu popup = new PopupMenu(this, view);

        final String creatorId = mMatch.getCreatorId();
        final List<String> inviteeIds = mMatch.getInviteeIds();
        final List<String> playerIds = mMatch.getPlayerIds();
        final List<String> pendingJoinIds = mMatch.getPendingJoinIds();
        final List<String> pendingInviteeIds = mMatch.getPendingInviteeIds();
        final List<String> pendingLeaveIds = mMatch.getPendingLeaveIds();

        int totalPlayers = getAttendeeCount() + pendingInviteeIds.size();
        final int maxNumInvites = GameConstants.MAX_NUM_PLAYERS - totalPlayers;

        // Popup menu click listener
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick(MenuItem item)
            {
                Intent intent;

                switch (item.getItemId())
                {
                    case R.id.invite:
                        // TODO: May be adding duplicate ids here (inviteeIds and playerIds may have ids in common).
                        // Not a big deal though. The duplicates won't affect anything.
                        ArrayList<String> exclusions = new ArrayList<String>();

                        exclusions.addAll(inviteeIds);
                        exclusions.addAll(playerIds);
                        exclusions.addAll(pendingJoinIds);

                        // Remove pendingLeaveIds from exclusions so that we can re-invite
                        // them immediately after they leave or are kicked.
                        exclusions.removeAll(pendingLeaveIds);

                        // Add pendingInviteeIds after pendingLeaveIds have been removed.
                        // Because it's possible that a player is in both pendingInviteeIds
                        // and pendingLeaveIds at the same time. (e.g. player is re-invited
                        // immediately after leaving).
                        exclusions.addAll(pendingInviteeIds);

                        intent = new Intent(GameActivity.this, NewMatchActivity.class);
                        intent.putExtra(NewMatchActivity.KEY_ACTION, NewMatchActivity.ACTION_INVITE);
                        intent.putExtra(NewMatchActivity.KEY_MIN_NUM_PLAYERS, 1);
                        intent.putExtra(NewMatchActivity.KEY_MAX_NUM_PLAYERS, maxNumInvites);
                        intent.putStringArrayListExtra(NewMatchActivity.KEY_EXCLUSIONS, exclusions);
                        addMatchExtras(intent);
                        startActivityForResult(intent, RC_INVITE);

                        break;
                    case R.id.kick:
                        intent = new Intent(GameActivity.this, NewMatchActivity.class);
                        intent.putExtra(NewMatchActivity.KEY_ACTION, NewMatchActivity.ACTION_KICK);
                        addMatchExtras(intent);
                        startActivityForResult(intent, RC_KICK);
                        break;
                    case R.id.auto_pick_skip:
                        showAutoPickSkipDialog();
                        break;
                    case R.id.match_settings:
                        intent = new Intent(GameActivity.this, NewMatchActivity.class);
                        intent.putExtra(NewMatchActivity.KEY_ACTION, NewMatchActivity.ACTION_SETTINGS);

                        if (!mPlayerId.equals(creatorId) || isMatchComplete() || isMatchCanceled())
                            intent.putExtra(NewMatchActivity.KEY_MATCH_SETTINGS_READ_ONLY, true);

                        addMatchExtras(intent);
                        startActivityForResult(intent, RC_MATCH_SETTINGS);
                        break;
                    case R.id.general_settings:
                        intent = new Intent(GameActivity.this, SettingsActivity.class);
                        startActivity(intent);
                        break;
                    case R.id.leave_match:
                        showLeaveMatchDialog();
                        break;
                    default:
                        return false;
                }

                return true;
            }
        });

        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.main, popup.getMenu());

        if (mGameFragment.isBusy() || mGameFragment.isRetrievingUpdates() || !mPlayerId.equals(creatorId) ||
            totalPlayers == GameConstants.MAX_NUM_PLAYERS || isMatchComplete() || isMatchCanceled())
        {
            popup.getMenu().removeItem(R.id.invite);
        }

        if (mGameFragment.isBusy() || mGameFragment.isRetrievingUpdates() || !mPlayerId.equals(creatorId) ||
            getAttendeeCount() - 1 == 0 || isMatchComplete() || isMatchCanceled())
        {
            popup.getMenu().removeItem(R.id.kick);
        }

        if (mGameFragment.isBusy() || mGameFragment.isRetrievingUpdates() || !mPlayerId.equals(creatorId) ||
            isMatchComplete() || isMatchCanceled())
        {
            popup.getMenu().removeItem(R.id.auto_pick_skip);
        }

        if ((mGameFragment.isBusy() || mGameFragment.isRetrievingUpdates()) && mPlayerId.equals(creatorId))
            popup.getMenu().removeItem(R.id.match_settings);

        popup.show();
    }

    private void showRematchDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.rematch_title));
        builder.setMessage(getString(R.string.rematch_description));

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                mProgressBar.setVisibility(View.VISIBLE);
                mGameFragment.rematch(mMatchId, mPlayerId);
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                // Re-enable the submit buttons.
                determineControls();
            }
        });

        mNotificationDialog = builder.create();
        mNotificationDialog.setCancelable(false);
        mNotificationDialog.show();
    }

    // TODO: May want to return when selectedCards is empty.
    // Method is safe as is, but future changes to dependent methods may make it not safe.
    private void showTossCardDialog()
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GameActivity.this);
        boolean showTossCardDialog = prefs.getBoolean(KEY_SHOW_TOSS_CARD_DIALOG, true);

        if (showTossCardDialog)
        {
            LayoutInflater inflater = LayoutInflater.from(this);
            View layout = inflater.inflate(R.layout.checkbox, null);
            final CheckBox checkbox = (CheckBox)layout.findViewById(R.id.dont_show_again);
            checkbox.setChecked(!showTossCardDialog);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(layout);

            int cardType = getSelectedCards().get(0).getType();

            if (cardType == Card.CARD_TYPE_BLANK || cardType == Card.CARD_TYPE_PICTURE)
                builder.setMessage("Discard current card and draw a new one?");
            else
                builder.setMessage("Discard current card and draw a new one in exchange for " + GameConstants.POINT_VALUE + " points?");

            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    prefs.edit().putBoolean(KEY_SHOW_TOSS_CARD_DIALOG, !checkbox.isChecked()).commit();
                    toss();
                }
            });

            builder.setNegativeButton("No", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    prefs.edit().putBoolean(KEY_SHOW_TOSS_CARD_DIALOG, !checkbox.isChecked()).commit();

                    // TODO: Necessary to re-enable submit buttons?
                    // Re-enable the submit buttons.
                    determineControls();
                }
            });

            mNotificationDialog = builder.create();
            mNotificationDialog.setCancelable(false);
            mNotificationDialog.show();
        }
        else
            toss();
    }

    private void showAutoPickSkipDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.auto_pick_skip_title));
        builder.setMessage(getString(R.string.auto_pick_skip_description));

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                mProgressBar.setVisibility(View.VISIBLE);
                mGameFragment.forceAutoPickSkip(mMatchId);
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                // Re-enable the submit buttons.
                determineControls();
            }
        });

        mNotificationDialog = builder.create();
        mNotificationDialog.setCancelable(false);
        mNotificationDialog.show();
    }

    private void showLeaveMatchDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.leave_match_title));

        String message = getString(R.string.leave_match_description);

        if (!isMatchCanceled())
        {
            if (mPlayerId.equals(mMatch.getCreatorId()))
                message += " " + getString(R.string.leave_match_creator_warning);
            else if (getAttendeeCount() - 1 < GameConstants.MIN_NUM_PLAYERS)
                message += " " + getString(R.string.leave_match_warning);
        }

        builder.setMessage(message);

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                mProgressBar.setVisibility(View.VISIBLE);
                mGameFragment.leaveMatch(mMatchId, mPlayerId);
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                // Re-enable the submit buttons.
                determineControls();
            }
        });

        mNotificationDialog = builder.create();
        mNotificationDialog.setCancelable(false);
        mNotificationDialog.show();
    }

    private void startLastChatMessageCycle()
    {
        ToggleLog.d(TAG, "Starting last chat message cycle.");

        // Stop existing last chat message cycle.
        stopLastChatMessageCycle();

        mLastChatMessageHandler = new Handler();

        mLastChatMessageRunnable = new Runnable()
        {
            public void run()
            {
                mGameFragment.retrieveLastChatMessage(mMatchId);
                mLastChatMessageHandler.postDelayed(this, LAST_CHAT_MESSAGE_INTERVAL);
            }
        };

        mLastChatMessageHandler.post(mLastChatMessageRunnable);
    }

    private void stopLastChatMessageCycle()
    {
        ToggleLog.d(TAG, "Stopping last chat message cycle.");

        if (mLastChatMessageHandler != null)
            mLastChatMessageHandler.removeCallbacks(mLastChatMessageRunnable);
    }
    
    private void startAutoPickSkipTimeoutCycle()
    {
        // Stop existing auto pick timeout cycle.
        stopAutoPickSkipTimeoutCycle();
        mTimer.setText("");

        // TODO: Originally had a !mGameFragment.isBusy() at the beginning of this condition. Why?
        // If you need to add that check back, also consider adding !mGameFragment.isRetrievingUpdates().
        if (!isMatchComplete() && !isMatchCanceled() && mMatch.getAutoPickSkipTimeout() != 0)
        {
            ToggleLog.d(TAG, "Starting auto pick timeout cycle.");

            mAutoPickSkipTimeoutHandler = new Handler();

            mAutoPickSkipTimeoutRunnable = new Runnable()
            {
                public void run()
                {
                    determineAutoPickSkipTimeout();
                    mAutoPickSkipTimeoutHandler.postDelayed(this, AUTO_PICK_SKIP_TIMEOUT_INTERVAL);
                }
            };

            mAutoPickSkipTimeoutHandler.post(mAutoPickSkipTimeoutRunnable);
        }
    }

    private void stopAutoPickSkipTimeoutCycle()
    {
        ToggleLog.d(TAG, "Stopping auto pick/skip timeout cycle.");

        if (mAutoPickSkipTimeoutHandler != null)
            mAutoPickSkipTimeoutHandler.removeCallbacks(mAutoPickSkipTimeoutRunnable);
    }

    private void determineAutoPickSkipTimeout()
    {
        Calendar calendar = Calendar.getInstance();
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        int excludeHoursBegin = GameFunctions.convertEstToLocalTime(mMatch.getExcludeHoursBegin());
        int excludeHoursEnd = GameFunctions.convertEstToLocalTime(mMatch.getExcludeHoursEnd());
        boolean snoozing = false;

        // Determine if the current hour is between the exclusion hours.
        // If so, we will show a "zzz" animation instead of the timer.
        if (excludeHoursBegin < excludeHoursEnd)
        {
            if (hourOfDay >= excludeHoursBegin && hourOfDay < excludeHoursEnd)
                snoozing = true;
        }
        else if (excludeHoursBegin > excludeHoursEnd)
        {
            if (hourOfDay >= excludeHoursBegin || hourOfDay < excludeHoursEnd)
                snoozing = true;
        }

        if (snoozing)
        {
            if (++mSleepCount > 3)
                mSleepCount = 0;

            StringBuilder builder = new StringBuilder(3);

            for (int i = 0; i < mSleepCount; i++)
            {
                builder.append("z");
            }

            mTimer.setText(builder.toString());
            return;
        }

        mSleepCount = 0;

        long currentTime = System.currentTimeMillis() / 1000;
        long stateStartTime = mMatch.getStateStartTime();
        long timeElapsed = currentTime - stateStartTime;
        long timeRemaining = mMatch.getAutoPickSkipTimeout() - timeElapsed;

        if (timeRemaining <= 0)
        {
            timeRemaining = 0;

            if (mBlinkAutoPickSkipTimer)
            {
                mBlinkAutoPickSkipTimer = false;
                mTimer.setText("");
                return;
            }

            mBlinkAutoPickSkipTimer = true;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("H:mm:ss");
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, (int)timeRemaining);
        String remaining = sdf.format(calendar.getTime());
        mTimer.setText(remaining.replaceFirst("^0:", "").replaceFirst("^00:", "").replaceFirst("^0", ""));
    }

    public void updateChatMessages()
    {
        mGameFragment.retrieveChatMessages(mMatchId);
    }

    public void sendChatMessage(String chatMessage)
    {
        mGameFragment.sendChatMessage(mMatchId, mPlayerId, mPlayerName, chatMessage);
    }

    public void onRetrieveChatMessagesSuccess(List<ChatMessage> chatMessages)
    {
        mChatFragment.setChatProgressBarVisibility(View.GONE);

        ChatMessage lastChatMessage = null;

        for (int i = chatMessages.size() - 1; i >= 0; i--)
        {
            lastChatMessage = chatMessages.get(i);

            if (!lastChatMessage.getPlayerId().isEmpty())
                break;
        }

        if (lastChatMessage == null)
            return;

        if (!lastChatMessage.getPlayerId().isEmpty())
        {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

            String lastChatMessageId = lastChatMessage.getId();
            String storedLastChatMessageId = prefs.getString(KEY_LAST_CHAT_MESSAGE_ID + mMatchId, null);

            if (!lastChatMessageId.equals(storedLastChatMessageId))
                prefs.edit().putString(KEY_LAST_CHAT_MESSAGE_ID + mMatchId, lastChatMessageId).commit();
        }

        mChatFragment.updateChatMessages(chatMessages);
    }

    public void onRetrieveChatMessagesFailure()
    {
        ToggleLog.d(TAG, "Failed to receive chat messages.");
        mChatFragment.setChatProgressBarVisibility(View.GONE);
    }

    public void onSendChatMessageSuccess()
    {
        updateChatMessages();
        mChatFragment.setSendingMessage(false);
        mChatFragment.setSendProgressBarVisibility(View.GONE);
    }

    public void onSendChatMessageFailure(String message)
    {
        ToggleLog.d(TAG, "Failed to insert chat message.");
        mChatFragment.setChatMessage(message);
        mChatFragment.setSendingMessage(false);
        mChatFragment.setSendProgressBarVisibility(View.GONE);
    }

    public void onRetrieveLastChatMessageSuccess(ChatMessage lastChatMessage)
    {
        if (lastChatMessage == null)
            return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String playerId = lastChatMessage.getPlayerId();
        String lastChatMessageId = lastChatMessage.getId();
        String storedLastChatMessageId = prefs.getString(KEY_LAST_CHAT_MESSAGE_ID + mMatchId, null);

        if (!mPlayerId.equals(playerId) && !lastChatMessageId.equals(storedLastChatMessageId))
        {
            if (mCurrentScreen == SCREEN_GAME)
            {
                // Light up chat button to indicate that a new message was received.
                mNewChatMessageReceived = true;
                showChatGlow();
            }
        }
    }

    public void onRetrieveLastChatMessageFailure()
    {
        ToggleLog.d(TAG, "Failed to receive last chat message.");
    }

    private class ButtonClickListener implements View.OnClickListener
    {
        @Override
        public void onClick(View view)
        {
            final int buttonId = view.getId();

            GameFunctions.hideKeyboard(GameActivity.this);

            // Show the button animation.
            Animation anim = AnimationUtils.loadAnimation(GameActivity.this, R.anim.pulse);
            anim.reset();
            view.clearAnimation();
            view.startAnimation(anim);

            switch (buttonId)
            {
                case R.id.submit_button:
                    disableSubmitButtons();

                    if (mSubmitButton.getText().equals(getString(R.string.resume)))
                    {
                        mShowResults = false;
                        markAllRoundsSeen();
                        updateGameData();
                    }
                    else if (mSubmitButton.getText().equals(getString(R.string.rematch)))
                        showRematchDialog();
                    else if (mMatch.getState() == Match.MATCH_STATE_WRITING_CARD)
                        submitBlackCard();
                    else
                        submit();

                    break;
                case R.id.bet_button:
                    mBetPopup.showAsDropDown(mBetButton, 0, 0);
                    break;
                case R.id.toss_button:
                    disableSubmitButtons();
                    showTossCardDialog();
                    break;
                case R.id.chat_button:
                    View chatView = findViewById(R.id.fragment_chat_container);

                    if (chatView.getVisibility() == View.VISIBLE)
                        showGame();
                    else
                        showChat();

                    determineControls();
                    break;
                case R.id.bet_minus_button:
                    if ((mCurrentBet - GameConstants.POINT_VALUE) >= 0)
                    {
                        mCurrentBet -= GameConstants.POINT_VALUE;
                        refreshBet();
                    }

                    break;
                case R.id.bet_plus_button:
                    int bet = mCurrentBet + GameConstants.POINT_VALUE;
                    int score = getCurrentPlayer().getScore();

                    if (bet <= score && bet + score + GameConstants.POINT_VALUE <= GameConstants.POINTS_TO_WIN)
                    {
                        mCurrentBet = bet;
                        refreshBet();
                    }
            }
        }
    }
}