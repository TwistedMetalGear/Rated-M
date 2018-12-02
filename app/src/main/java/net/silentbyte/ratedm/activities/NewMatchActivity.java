package net.silentbyte.ratedm.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import net.silentbyte.ratedm.GameConstants;
import net.silentbyte.ratedm.GameFunctions;
import net.silentbyte.ratedm.Match;
import net.silentbyte.ratedm.R;
import net.silentbyte.ratedm.ToggleLog;
import net.silentbyte.ratedm.User;
import net.silentbyte.ratedm.adapters.FriendAdapter;
import net.silentbyte.ratedm.fragments.NewMatchFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class NewMatchActivity extends FragmentActivity implements NewMatchFragment.CallbackHandler
{
    private static final String TAG = "NewMatchActivity";
    private static final String TAG_NEW_MATCH_FRAGMENT = "new_match_fragment";
    private static final String KEY_FRIENDS = "friends";
    private static final String KEY_PLAYERS = "players";
    private static final String KEY_FRIENDS_LIST_VIEW_STATE = "friends_list_view_state";
    private static final String KEY_PLAYERS_LIST_VIEW_STATE = "players_list_view_state";
    private static final String KEY_MATCH_NAME = "match_name";
    private static final String KEY_AUTO_MATCH_SLOTS = "auto_match_slots";
    private static final String KEY_CURRENT_SCREEN = "current_screen";
    private static final String KEY_SHOW_RETRIEVE_FRIENDS_PROGRESS_BAR = "show_retrieve_friends_progress_bar";
    private static final String KEY_RETRIEVE_FRIENDS_ERROR_OCCURRED = "retrieve_friends_error_occurred";
    private static final String KEY_FIND_PLAYERS_ERROR_OCCURRED = "find_players_error_occurred";
    private static final String KEY_FIRST_ENTRY = "first_entry";
    private static final String KEY_SHOW_INVITE_WARNING = "show_invite_warning";
    public static final String KEY_ACTION = "action";
    public static final String KEY_MATCH_SETTINGS_READ_ONLY = "match_settings_read_only";
    public static final String KEY_AUTO_MATCH = "auto_match";
    public static final String KEY_MIN_NUM_PLAYERS = "min_num_players";
    public static final String KEY_MAX_NUM_PLAYERS = "max_num_players";
    public static final String KEY_EXCLUSIONS = "exclusions";
    private static final int SEARCH_DELAY = 500;

    public static final int ACTION_NEW_MATCH = 0;
    public static final int ACTION_INVITE = 1;
    public static final int ACTION_KICK = 2;
    public static final int ACTION_SETTINGS = 3;

    private static final int SCREEN_NEW_MATCH = 0;
    private static final int SCREEN_FRIENDS = 1;
    private static final int SCREEN_PLAYERS = 2;

    private NewMatchFragment mNewMatchFragment;

    private View mNewMatchPanel;
    private View mAutoPickSkipPanel;
    private View mInvitePanel;
    private View mFindPlayersEmptyView;
    private View mRetrieveFriendsErrorView;
    private View mFindPlayersErrorView;
    private View mFriendsPanel;
    private View mPlayersPanel;
    private Button mNextButton;
    private View mTrashButton;
    private Button mPlayButton;
    private View mRetrieveFriendsProgressBar;
    private View mFindPlayersProgressBar;
    private View mCreatingMatchProgressBar;
    private View mSearchContainer;
    private EditText mMatchNameText;
    private TextView mBlackCardTypeDescription;
    private TextView mWhiteCardTypeDescription;
    private TextView mInactiveModeDescription;
    private TextView mAutoPickSkipTimeoutLabel;
    private TextView mAutoPickSkipTimeoutDescription;
    private Spinner mBlackCardTypeView;
    private Spinner mWhiteCardTypeView;
    private Spinner mInactiveModeView;
    private Spinner mAutoPickSkipTimeoutView;
    private CheckBox mExcludeHoursView;
    private Spinner mExcludeHoursBeginView;
    private Spinner mExcludeHoursEndView;
    private TextView mHeaderTextView;
    private TextView mSearchTextView;
    private View mAutoMatchSlotsRow;
    private TextView mAutoMatchSlotsView;
    private TextView mRetrieveFriendsErrorText;

    private ListView mFriendsListView;
    private ListView mPlayersListView;
    private List<User> mFriends;
    private List<User> mPlayers;
    private FriendAdapter mFriendAdapter;
    private FriendAdapter mPlayerAdapter;

    private User mUser;
    private String mMatchName;
    private int mAutoMatchSlots = 0;
    private int mCurrentScreen = 0;
    private boolean mFirstEntry = true;
    private boolean mShowRetrieveFriendsProgressBar = true;
    private boolean mRetrieveFriendsErrorOccurred = false;
    private boolean mFindPlayersErrorOccurred = false;

    // Intent fields
    private String mIntentMatchId;
    private String mIntentMatchName;
    private int mIntentAction = 0;
    private int mIntentAutoMatchSlots;
    private int mIntentBlackCardType;
    private int mIntentWhiteCardType;
    private int mIntentInactiveMode;
    private int mIntentAutoPickSkipTimeout;
    private int mIntentExcludeHoursBegin;
    private int mIntentExcludeHoursEnd;
    private int mIntentMinNumPlayers;
    private int mIntentMaxNumPlayers;
    private boolean mIntentAutoMatch = false;
    private boolean mIntentMatchSettingsReadOnly = false;
    private List<String> mIntentExclusions;


    private Dialog mNotificationDialog;

    private Handler mHandler;
    private Runnable mRunnable;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Lock the device in portrait mode.
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_new_match);

        mIntentAction = getIntent().getIntExtra(KEY_ACTION, 0);
        mIntentMatchId = getIntent().getStringExtra(Match.KEY_MATCH_ID);
        mIntentMatchName = getIntent().getStringExtra(Match.JSON_NAME);
        mIntentAutoMatchSlots = getIntent().getIntExtra(Match.JSON_AUTO_MATCH_SLOTS, 0);
        mIntentBlackCardType = getIntent().getIntExtra(Match.JSON_BLACK_CARD_TYPE, 0);
        mIntentWhiteCardType = getIntent().getIntExtra(Match.JSON_WHITE_CARD_TYPE, 0);
        mIntentInactiveMode = getIntent().getIntExtra(Match.JSON_INACTIVE_MODE, 0);
        mIntentAutoPickSkipTimeout = getIntent().getIntExtra(Match.JSON_AUTO_PICK_SKIP_TIMEOUT, 0);
        mIntentExcludeHoursBegin = getIntent().getIntExtra(Match.JSON_EXCLUDE_HOURS_BEGIN, 0);
        mIntentExcludeHoursEnd = getIntent().getIntExtra(Match.JSON_EXCLUDE_HOURS_END, 0);
        mIntentMinNumPlayers = getIntent().getIntExtra(KEY_MIN_NUM_PLAYERS, GameConstants.MIN_NUM_PLAYERS - 1);
        mIntentMaxNumPlayers = getIntent().getIntExtra(KEY_MAX_NUM_PLAYERS, GameConstants.MAX_NUM_PLAYERS - 1);
        mIntentAutoMatch = getIntent().getBooleanExtra(KEY_AUTO_MATCH, false);
        mIntentMatchSettingsReadOnly = getIntent().getBooleanExtra(KEY_MATCH_SETTINGS_READ_ONLY, false);
        mIntentExclusions = getIntent().getStringArrayListExtra(KEY_EXCLUSIONS);

        mNewMatchPanel = findViewById(R.id.new_match_panel);
        mAutoPickSkipPanel = findViewById(R.id.auto_pick_skip_panel);
        mInvitePanel = findViewById(R.id.invite_panel);
        mFindPlayersEmptyView = findViewById(R.id.find_players_empty);
        mRetrieveFriendsErrorView = findViewById(R.id.retrieve_friends_error);
        mFindPlayersErrorView = findViewById(R.id.find_players_error);
        mFriendsPanel = findViewById(R.id.friends_panel);
        mPlayersPanel = findViewById(R.id.players_panel);
        mNextButton = (Button)findViewById(R.id.next_button);
        mTrashButton = findViewById(R.id.trash_button);
        mPlayButton = (Button)findViewById(R.id.play_button);
        mRetrieveFriendsProgressBar = findViewById(R.id.retrieve_friends_progress_bar);
        mFindPlayersProgressBar = findViewById(R.id.find_players_progress_bar);
        mCreatingMatchProgressBar = findViewById(R.id.creating_match_progress_bar);
        mSearchContainer = findViewById(R.id.search_container);
        mMatchNameText = (EditText)findViewById(R.id.match_name);
        mMatchNameText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(GameConstants.MAX_MATCH_NAME_LENGTH)});
        mBlackCardTypeDescription = (TextView)findViewById(R.id.black_card_type_description);
        mWhiteCardTypeDescription = (TextView)findViewById(R.id.white_card_type_description);
        mInactiveModeDescription = (TextView)findViewById(R.id.inactive_mode_description);
        mAutoPickSkipTimeoutLabel = (TextView)findViewById(R.id.auto_pick_skip_timeout_label);
        mAutoPickSkipTimeoutDescription = (TextView)findViewById(R.id.auto_pick_skip_timeout_description);
        mBlackCardTypeView = (Spinner)findViewById(R.id.black_card_type);
        mWhiteCardTypeView = (Spinner)findViewById(R.id.white_card_type);
        mInactiveModeView = (Spinner)findViewById(R.id.inactive_mode);
        mAutoPickSkipTimeoutView = (Spinner)findViewById(R.id.auto_pick_skip_timeout);
        mExcludeHoursView = (CheckBox)findViewById(R.id.exclude_hours);
        mExcludeHoursBeginView = (Spinner)findViewById(R.id.exclude_hours_begin);
        mExcludeHoursEndView = (Spinner)findViewById(R.id.exclude_hours_end);
        mHeaderTextView = (TextView)findViewById(R.id.header_text);
        mSearchTextView = (TextView)findViewById(R.id.search_text);
        mAutoMatchSlotsView = (TextView)findViewById(R.id.auto_match_slots);
        mRetrieveFriendsErrorText = (TextView)findViewById(R.id.retrieve_friends_error_text);

        mBlackCardTypeView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                switch (position)
                {
                    case Match.BLACK_CARD_TYPE_STANDARD:
                        mBlackCardTypeDescription.setText(getString(R.string.black_card_standard_description));
                        break;
                    case Match.BLACK_CARD_TYPE_CUSTOM:
                        mBlackCardTypeDescription.setText(getString(R.string.black_card_custom_description));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        mWhiteCardTypeView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                switch (position)
                {
                    case Match.WHITE_CARD_TYPE_STANDARD:
                        mWhiteCardTypeDescription.setText(getString(R.string.white_card_standard_description));
                        break;
                    case Match.WHITE_CARD_TYPE_CUSTOM:
                        mWhiteCardTypeDescription.setText(getString(R.string.white_card_custom_description));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        mInactiveModeView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                mAutoPickSkipPanel.setVisibility(View.VISIBLE);

                switch (position)
                {
                    case Match.INACTIVE_MODE_AUTO_PICK:
                        mInactiveModeDescription.setText(getString(R.string.auto_pick_description));
                        mAutoPickSkipTimeoutLabel.setText(getString(R.string.auto_pick_after));
                        mAutoPickSkipTimeoutDescription.setText(getString(R.string.auto_pick_after_description));
                        mExcludeHoursView.setText(getString(R.string.auto_pick_exclude_hours));
                        break;
                    case Match.INACTIVE_MODE_SKIP:
                        mInactiveModeDescription.setText(getString(R.string.skip_description));
                        mAutoPickSkipTimeoutLabel.setText(getString(R.string.skip_after));
                        mAutoPickSkipTimeoutDescription.setText(getString(R.string.skip_after_description));
                        mExcludeHoursView.setText(getString(R.string.skip_exclude_hours));
                        break;
                    case Match.INACTIVE_MODE_WAIT:
                        mInactiveModeDescription.setText(getString(R.string.wait_description));
                        mAutoPickSkipPanel.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        mExcludeHoursView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked)
                {
                    mExcludeHoursBeginView.setEnabled(true);
                    mExcludeHoursEndView.setEnabled(true);
                }
                else
                {
                    mExcludeHoursBeginView.setEnabled(false);
                    mExcludeHoursEndView.setEnabled(false);
                }
            }
        });

        mAutoMatchSlotsRow = findViewById(R.id.auto_match_slots_row);
        mAutoMatchSlotsRow.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                int autoMatchSlots = Integer.parseInt(mAutoMatchSlotsView.getText().toString());

                if (mIntentAction == ACTION_KICK)
                {
                    if (++autoMatchSlots <= mIntentAutoMatchSlots)
                    {
                        mAutoMatchSlots = autoMatchSlots;
                        mAutoMatchSlotsView.setText(String.valueOf(mAutoMatchSlots));
                        mTrashButton.setVisibility(View.VISIBLE);
                        refreshHeaderText();
                    }
                }
                else if (++autoMatchSlots + getCheckedItemPositions().size() <= mIntentMaxNumPlayers)
                {
                    mAutoMatchSlots = autoMatchSlots;
                    mAutoMatchSlotsView.setText(String.valueOf(mAutoMatchSlots));
                    mTrashButton.setVisibility(View.VISIBLE);
                    refreshHeaderText();
                }
            }
        });

        mNextButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mMatchName = mMatchNameText.getText().toString().trim();

                if (mMatchName.isEmpty())
                {
                    mNotificationDialog = GameFunctions.showDialog(NewMatchActivity.this, getString(R.string.invalid_match_name),
                        String.format(getString(R.string.invalid_match_name_description), 1, GameConstants.MAX_MATCH_NAME_LENGTH));
                }
                else
                {
                    GameFunctions.hideKeyboard(NewMatchActivity.this);

                    if (mIntentAction != ACTION_NEW_MATCH)
                        updateMatchSettings();
                    else
                        showInvitePanel();
                }
            }
        });

        mTrashButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                int autoMatchSlots = Integer.parseInt(mAutoMatchSlotsView.getText().toString());

                if (--autoMatchSlots >= 0)
                {
                    mAutoMatchSlots = autoMatchSlots;
                    mAutoMatchSlotsView.setText(String.valueOf(mAutoMatchSlots));

                    if (mAutoMatchSlots == 0)
                        mTrashButton.setVisibility(View.GONE);

                    refreshHeaderText();
                }
            }
        });

        mFriendsListView = (ListView)findViewById(R.id.friends);
        mFriendsListView.setDividerHeight(1);
        mFriendsListView.setItemsCanFocus(false);
        mFriendsListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                int numSelectedPlayers = getCheckedItemPositions().size() + mAutoMatchSlots;

                if (numSelectedPlayers > mIntentMaxNumPlayers)
                    mFriendsListView.setItemChecked(position, false);

                refreshHeaderText();
            }
        });

        mPlayersListView = (ListView)findViewById(R.id.players);
        mPlayersListView.setDividerHeight(1);
        mPlayersListView.setItemsCanFocus(false);
        mPlayersListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                User friend = mPlayers.get(position);
                int insertedPosition = -1;
                int numSelectedPlayers = getCheckedItemPositions().size() + mAutoMatchSlots;

                mSearchContainer.requestFocus();
                GameFunctions.hideKeyboard(NewMatchActivity.this);

                if (mFriends == null)
                {
                    List<User> friends = new ArrayList<User>();
                    friends.add(friend);
                    initializeFriendAdapter(friends);
                    insertedPosition = 0;
                }
                else if (!mFriends.contains(friend))
                {
                    mFriends.add(friend);
                    Collections.sort(mFriends);
                    mFriendAdapter.notifyDataSetChanged();
                    insertedPosition = mFriends.indexOf(friend);
                }

                if (insertedPosition == -1)
                    insertedPosition = mFriends.indexOf(friend);
                else
                    updateCheckedItemPositions(insertedPosition);

                if (numSelectedPlayers < mIntentMaxNumPlayers)
                    mFriendsListView.setItemChecked(insertedPosition, true);

                mFriendsListView.setSelection(insertedPosition);
                mSearchTextView.setText("");
                refreshHeaderText();
            }
        });

        if (mIntentAction == ACTION_INVITE)
        {
            showInvitePanel();
            mPlayButton.setText(getString(R.string.invite));
        }
        else if (mIntentAction == ACTION_KICK)
        {
            showInvitePanel();

            mSearchContainer.setVisibility(View.GONE);

            if (mPlayerAdapter == null || mIntentAutoMatchSlots == 0)
                mAutoMatchSlotsRow.setVisibility(View.GONE);

            mPlayButton.setText(getString(R.string.kick));
        }
        else if (mIntentAction == ACTION_SETTINGS)
        {
            TextView bannerText = (TextView)findViewById(R.id.banner_text);
            bannerText.setText(getString(R.string.match_settings));
            mNextButton.setText(getString(R.string.done));
        }

        mPlayButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (mIntentAction == ACTION_KICK)
                    kick();
                else
                    showInviteWarning();
            }
        });

        Button retrieveFriendsRetryButton = (Button)findViewById(R.id.retrieve_friends_retry_button);
        retrieveFriendsRetryButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (mIntentAction == ACTION_KICK)
                    retrieveAttendees();
                else
                    retrieveFriends();
            }
        });

        Button findPlayersRetryButton = (Button)findViewById(R.id.find_players_retry_button);
        findPlayersRetryButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                findPlayers(mSearchTextView.getText().toString());
            }
        });

        FragmentManager fm = getSupportFragmentManager();

        // Add new match fragment to fragment manager.
        mNewMatchFragment = (NewMatchFragment) fm.findFragmentByTag(TAG_NEW_MATCH_FRAGMENT);

        if (mNewMatchFragment == null)
        {
            mNewMatchFragment = new NewMatchFragment();
            fm.beginTransaction().add(mNewMatchFragment, TAG_NEW_MATCH_FRAGMENT).commit();
        }

        if (savedInstanceState != null)
        {
            mFriends = savedInstanceState.getParcelableArrayList(KEY_FRIENDS);
            mPlayers = savedInstanceState.getParcelableArrayList(KEY_PLAYERS);
            mMatchName = savedInstanceState.getString(KEY_MATCH_NAME);
            mAutoMatchSlots = savedInstanceState.getInt(KEY_AUTO_MATCH_SLOTS);
            mCurrentScreen = savedInstanceState.getInt(KEY_CURRENT_SCREEN);
            mFirstEntry = savedInstanceState.getBoolean(KEY_FIRST_ENTRY);
            mShowRetrieveFriendsProgressBar = savedInstanceState.getBoolean(KEY_SHOW_RETRIEVE_FRIENDS_PROGRESS_BAR);
            mRetrieveFriendsErrorOccurred = savedInstanceState.getBoolean(KEY_RETRIEVE_FRIENDS_ERROR_OCCURRED);
            mFindPlayersErrorOccurred = savedInstanceState.getBoolean(KEY_FIND_PLAYERS_ERROR_OCCURRED);

            if (mFriends != null)
            {
                initializeFriendAdapter(mFriends);

                // Restore scroll position and checked items.
                Parcelable state = savedInstanceState.getParcelable(KEY_FRIENDS_LIST_VIEW_STATE);
                mFriendsListView.onRestoreInstanceState(state);

                if (mIntentAction == ACTION_KICK && mIntentAutoMatchSlots > 0)
                    mAutoMatchSlotsRow.setVisibility(View.VISIBLE);
            }

            if (mPlayers != null && !mNewMatchFragment.isFindingPlayers())
            {
                // Restore scroll position.
                Parcelable state = savedInstanceState.getParcelable(KEY_PLAYERS_LIST_VIEW_STATE);
                mPlayersListView.onRestoreInstanceState(state);
            }

            mAutoMatchSlotsView.setText(String.valueOf(mAutoMatchSlots));

            if (mAutoMatchSlots > 0)
                mTrashButton.setVisibility(View.VISIBLE);

            if (!mShowRetrieveFriendsProgressBar)
                hideRetrieveFriendsProgressBar();

            if (mRetrieveFriendsErrorOccurred)
            {
                if (mIntentAction == ACTION_KICK)
                    mRetrieveFriendsErrorText.setText(getString(R.string.retrieve_attendees_failure));
                else
                    mRetrieveFriendsErrorText.setText(getString(R.string.retrieve_friends_failure));

                mRetrieveFriendsErrorView.setVisibility(View.VISIBLE);
            }

            if (mFindPlayersErrorOccurred)
            {
                mFindPlayersProgressBar.setVisibility(View.GONE);
                mFindPlayersErrorView.setVisibility(View.VISIBLE);
            }

            if (mCurrentScreen == SCREEN_FRIENDS)
                showFriends();
            else if (mCurrentScreen == SCREEN_PLAYERS)
                showPlayers();
        }

        if (mNewMatchFragment.isCreatingMatch())
        {
            mNewMatchPanel.setVisibility(View.GONE);
            mInvitePanel.setVisibility(View.GONE);
            mCreatingMatchProgressBar.setVisibility(View.VISIBLE);
        }

        if (mNewMatchFragment.isFindingPlayers())
            mFindPlayersProgressBar.setVisibility(View.VISIBLE);

        refreshHeaderText();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        mUser = User.get(this);

        if (mUser != null)
        {
            // Request focus to the parent of the search box so that the keyboard doesn't pop up upon launching or resuming activity.
            if (mIntentAction != ACTION_KICK)
                mSearchContainer.requestFocus();

            // Placing this here rather than onCreate so that onTextChanged doesn't get triggered upon configuration change.
            mSearchTextView.addTextChangedListener(new TextWatcher()
            {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void afterTextChanged(Editable s) {}

                @Override
                public void onTextChanged(final CharSequence s, int start, int before, int count)
                {
                    final String text = s.toString().trim();
                    int searchDelay = SEARCH_DELAY;

                    if (text.isEmpty())
                        searchDelay = 0;

                    if (mHandler != null)
                        mHandler.removeCallbacks(mRunnable);
                    else
                        mHandler = new Handler();

                    mRunnable = new Runnable()
                    {
                        public void run()
                        {
                            findPlayers(text);
                        }
                    };

                    mHandler.postDelayed(mRunnable, searchDelay);
                }
            });

            if (mIntentAction != ACTION_KICK && mUser.getType() == User.TYPE_EMAIL)
                hideRetrieveFriendsProgressBar();
            else if (!mNewMatchFragment.retrievedFriends() && mFriends == null && !mRetrieveFriendsErrorOccurred)
            {
                if (mIntentAction == ACTION_KICK)
                    retrieveAttendees();
                else
                    retrieveFriends();
            }

            if (mFirstEntry)
            {
                mFirstEntry = false;

                mMatchNameText.setText(mUser.getDisplayName() + "'s Match");
                mAutoPickSkipTimeoutView.setSelection(5);
                mExcludeHoursBeginView.setSelection(22);
                mExcludeHoursEndView.setSelection(10);

                if (mIntentAction == ACTION_NEW_MATCH)
                    mExcludeHoursView.setChecked(true);
                else
                {
                    mMatchNameText.setText(mIntentMatchName);
                    mBlackCardTypeView.setSelection(mIntentBlackCardType);
                    mWhiteCardTypeView.setSelection(mIntentWhiteCardType);
                    mInactiveModeView.setSelection(mIntentInactiveMode);
                    mAutoPickSkipTimeoutView.setSelection(getAutoPickSkipTimeoutPosition(mIntentAutoPickSkipTimeout));

                    if (mIntentExcludeHoursBegin != 0 || mIntentExcludeHoursEnd != 0)
                    {
                        mExcludeHoursView.setChecked(true);
                        mExcludeHoursBeginView.setSelection(GameFunctions.convertEstToLocalTime(mIntentExcludeHoursBegin));
                        mExcludeHoursEndView.setSelection(GameFunctions.convertEstToLocalTime(mIntentExcludeHoursEnd));
                    }
                }

                if (mIntentAutoMatch)
                {
                    // TODO: Setting default parameters here to get the match created ASAP (no user input required).
                    // After the match is created, before finishing, we should maybe show the match options screen to allow
                    // the host to change the match name and auto pick timeout. This would require some server updates to
                    // allow the match name and auto pick timeout to be changed mid-match.
                    mMatchName = mUser.getDisplayName() + "'s Match";
                    mAutoMatchSlots = GameConstants.MAX_NUM_PLAYERS - 1;
                    createMatch();
                }
            }

            if (mIntentMatchSettingsReadOnly)
            {
                mMatchNameText.setEnabled(false);
                mBlackCardTypeView.setEnabled(false);
                mWhiteCardTypeView.setEnabled(false);
                mInactiveModeView.setEnabled(false);
                mAutoPickSkipTimeoutView.setEnabled(false);
                mExcludeHoursView.setEnabled(false);
                mExcludeHoursBeginView.setEnabled(false);
                mExcludeHoursEndView.setEnabled(false);
                mNextButton.setVisibility(View.GONE);
            }
            else
            {
                if (mIntentAction != ACTION_NEW_MATCH)
                {
                    mBlackCardTypeView.setEnabled(false);
                    mWhiteCardTypeView.setEnabled(false);
                }

                if (mExcludeHoursView.isChecked())
                {
                    mExcludeHoursBeginView.setEnabled(true);
                    mExcludeHoursEndView.setEnabled(true);
                }
                else
                {
                    mExcludeHoursBeginView.setEnabled(false);
                    mExcludeHoursEndView.setEnabled(false);
                }
            }

            if (mPlayers != null && !mNewMatchFragment.isFindingPlayers())
                initializePlayerAdapter(mPlayers);
        }
        else
        {
            ToggleLog.e(TAG, "Unable to start a new match. User has been logged out.");
            finish();
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();

        if (mNotificationDialog != null)
            mNotificationDialog.dismiss();

        if (mHandler != null)
            mHandler.removeCallbacks(mRunnable);

        GameFunctions.hideKeyboard(this);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putParcelableArrayList(KEY_FRIENDS, (ArrayList)mFriends);
        savedInstanceState.putParcelableArrayList(KEY_PLAYERS, (ArrayList)mPlayers);
        savedInstanceState.putParcelable(KEY_FRIENDS_LIST_VIEW_STATE, mFriendsListView.onSaveInstanceState());
        savedInstanceState.putParcelable(KEY_PLAYERS_LIST_VIEW_STATE, mPlayersListView.onSaveInstanceState());
        savedInstanceState.putString(KEY_MATCH_NAME, mMatchName);
        savedInstanceState.putInt(KEY_AUTO_MATCH_SLOTS, mAutoMatchSlots);
        savedInstanceState.putInt(KEY_CURRENT_SCREEN, mCurrentScreen);
        savedInstanceState.putBoolean(KEY_FIRST_ENTRY, mFirstEntry);
        savedInstanceState.putBoolean(KEY_SHOW_RETRIEVE_FRIENDS_PROGRESS_BAR, mShowRetrieveFriendsProgressBar);
        savedInstanceState.putBoolean(KEY_RETRIEVE_FRIENDS_ERROR_OCCURRED, mRetrieveFriendsErrorOccurred);
        savedInstanceState.putBoolean(KEY_FIND_PLAYERS_ERROR_OCCURRED, mFindPlayersErrorOccurred);
    }

    @Override
    public void onBackPressed()
    {
        if (mNewMatchFragment.isCreatingMatch())
            finish();
        else if (mCurrentScreen == SCREEN_FRIENDS && mIntentAction == ACTION_NEW_MATCH)
            showNewMatchPanel();
        else if (mCurrentScreen == SCREEN_PLAYERS)
        {
            mSearchTextView.setText("");
            mSearchContainer.requestFocus();
        }
        else
            finish();
    }

    private void showNewMatchPanel()
    {
        mInvitePanel.setVisibility(View.GONE);
        mNewMatchPanel.setVisibility(View.VISIBLE);
        mCurrentScreen = SCREEN_NEW_MATCH;
    }

    private void showInvitePanel()
    {
        mNewMatchPanel.setVisibility(View.GONE);
        mInvitePanel.setVisibility(View.VISIBLE);
        mCurrentScreen = SCREEN_FRIENDS;
    }

    private void showFriends()
    {
        showInvitePanel();
        mPlayersPanel.setVisibility(View.GONE);
        mFriendsPanel.setVisibility(View.VISIBLE);
        mCurrentScreen = SCREEN_FRIENDS;
    }

    private void showPlayers()
    {
        showInvitePanel();
        mFriendsPanel.setVisibility(View.GONE);
        mPlayersPanel.setVisibility(View.VISIBLE);
        mCurrentScreen = SCREEN_PLAYERS;
    }

    private void showRetrieveFriendsProgressBar()
    {
        mRetrieveFriendsProgressBar.setVisibility(View.VISIBLE);
        mShowRetrieveFriendsProgressBar = true;
    }

    private void hideRetrieveFriendsProgressBar()
    {
        mRetrieveFriendsProgressBar.setVisibility(View.GONE);
        mShowRetrieveFriendsProgressBar = false;
    }

    public void initializeFriendAdapter(List<User> friends)
    {
        mFriends = friends;
        mFriendAdapter = new FriendAdapter(this, mFriends, true);
        mFriendsListView.setAdapter(mFriendAdapter);
    }

    public void initializePlayerAdapter(List<User> players)
    {
        mPlayers = players;
        mPlayerAdapter = new FriendAdapter(this, mPlayers, false);
        mPlayersListView.setAdapter(mPlayerAdapter);
        mFindPlayersProgressBar.setVisibility(View.GONE);

        if (!mFindPlayersErrorOccurred && mPlayers.isEmpty() && !mSearchTextView.getText().toString().trim().isEmpty())
            mFindPlayersEmptyView.setVisibility(View.VISIBLE);
        else
            mFindPlayersEmptyView.setVisibility(View.GONE);
    }

    private void refreshHeaderText()
    {
        if (mIntentAction == ACTION_KICK)
        {
            refreshKickHeaderText();
            return;
        }

        int numSelectedPlayers = getCheckedItemPositions().size() + mAutoMatchSlots;
        String playerText = "player";

        if (numSelectedPlayers == 0)
        {
            if (mIntentMaxNumPlayers > 1)
                playerText += "s";

            if (mIntentMinNumPlayers == mIntentMaxNumPlayers)
                mHeaderTextView.setText("Invite " + mIntentMinNumPlayers + " " + playerText + ".");
            else
                mHeaderTextView.setText("Invite " + mIntentMinNumPlayers + " to " + mIntentMaxNumPlayers + " " + playerText + ".");
        }
        else if (numSelectedPlayers < mIntentMinNumPlayers)
        {
            int numRequiredSelections = mIntentMinNumPlayers - numSelectedPlayers;

            if (numRequiredSelections > 1)
                playerText += "s";

            mHeaderTextView.setText("Invite at least " + numRequiredSelections + " more " + playerText + ".");
        }
        else if (numSelectedPlayers < mIntentMaxNumPlayers)
        {
            int numOptionalSelections = mIntentMaxNumPlayers - numSelectedPlayers;

            if (numOptionalSelections > 1)
                playerText += "s";

            mHeaderTextView.setText("Invite up to " + numOptionalSelections + " more " + playerText + ".");
        }
        else
            mHeaderTextView.setText("All slots full.");

        if (numSelectedPlayers >= mIntentMinNumPlayers)
            mPlayButton.setVisibility(View.VISIBLE);
        else
            mPlayButton.setVisibility(View.GONE);
    }

    private void refreshKickHeaderText()
    {
        if (mFriendAdapter == null)
        {
            mHeaderTextView.setText("Kick Players");
            return;
        }

        if (mFriends.isEmpty() && mIntentAutoMatchSlots == 0)
        {
            mHeaderTextView.setText("No players to kick");
            return;
        }

        int numSelectedPlayers = getCheckedItemPositions().size() + mAutoMatchSlots;
        int maxNumKicks = mFriends.size() + mIntentAutoMatchSlots;
        String playerText = "player";

        if (numSelectedPlayers == 0)
        {
            if (maxNumKicks > 1)
                playerText += "s";

            mHeaderTextView.setText("Kick up to " + maxNumKicks + " " + playerText + ".");
        }
        else if (numSelectedPlayers < maxNumKicks)
        {
            int numRemainingKicks = maxNumKicks - numSelectedPlayers;

            if (numRemainingKicks > 1)
                playerText += "s";

            mHeaderTextView.setText("Kick up to " + numRemainingKicks + " more " + playerText + ".");
        }
        else
            mHeaderTextView.setText("All players selected.");

        if (numSelectedPlayers > 0)
            mPlayButton.setVisibility(View.VISIBLE);
        else
            mPlayButton.setVisibility(View.GONE);
    }

    // IMPORTANT: Any modifications to this will need to be reflected in getAutoPickSkipTimeoutPosition.
    private int getAutoPickSkipTimeout()
    {
        switch (mAutoPickSkipTimeoutView.getSelectedItemPosition())
        {
            case 0:
                return 60;
            case 1:
                return 300;
            case 2:
                return 600;
            case 3:
                return 1200;
            case 4:
                return 1800;
            case 5:
                return 3600;
            case 6:
                return 7200;
            case 7:
                return 10800;
            case 8:
                return 21600;
            case 9:
                return 28800;
            case 10:
                return 36000;
            case 11:
                return 43200;
            case 12:
                return 86400;
        }

        return 0;
    }

    // IMPORTANT: Any modifications to this will need to be reflected in getAutoPickSkipTimeout.
    private int getAutoPickSkipTimeoutPosition(int autoPickSkipTimeout)
    {
        switch (autoPickSkipTimeout)
        {
            case 60:
                return 0;
            case 300:
                return 1;
            case 600:
                return 2;
            case 1200:
                return 3;
            case 1800:
                return 4;
            case 3600:
                return 5;
            case 7200:
                return 6;
            case 10800:
                return 7;
            case 21600:
                return 8;
            case 43200:
                return 9;
            case 86400:
                return 10;
        }

        return 0;
    }

    private List<Integer> getCheckedItemPositions()
    {
        SparseBooleanArray checkedItemPositions = mFriendsListView.getCheckedItemPositions();
        List<Integer> positions = new ArrayList<Integer>();

        for (int i = 0; i < checkedItemPositions.size(); i++)
        {
            // ToggleLog.d(TAG, "Checked @" + checkedItemPositions.keyAt(i) + ": " + checkedItemPositions.valueAt(i));

            if (checkedItemPositions.valueAt(i) == true)
            {
                int position = checkedItemPositions.keyAt(i);
                positions.add(position);
            }
        }

        return positions;
    }

    private void updateCheckedItemPositions(int insertedPosition)
    {
        List<Integer> checkedItemPositions = getCheckedItemPositions();
        Collections.reverse(checkedItemPositions);

        for (Integer position : checkedItemPositions)
        {
            if (position >= insertedPosition)
            {
                mFriendsListView.setItemChecked(position, false);
                mFriendsListView.setItemChecked(position + 1, true);
            }
        }
    }

    private List<User> getInvitees()
    {
        List<User> invitees = new ArrayList<User>();

        for (Integer position : getCheckedItemPositions())
        {
            User invitee = mFriends.get(position);
            invitees.add(invitee);
        }

        return invitees;
    }

    private List<String> getInviteeIds()
    {
        List<User> invitees = getInvitees();
        List<String> inviteeIds = new ArrayList<String>();

        for (User invitee : invitees)
        {
            inviteeIds.add(invitee.getId());
        }

        return inviteeIds;
    }

    private boolean invitingClosedForInvitePlayers()
    {
        List<User> invitees = getInvitees();

        for (User user : invitees)
        {
            if (user.playsWithFriendsOnly())
                return true;
        }

        return false;
    }

    private void showInviteWarning()
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean showInviteWarning = prefs.getBoolean(KEY_SHOW_INVITE_WARNING, true);

        if (showInviteWarning && invitingClosedForInvitePlayers())
        {
            LayoutInflater inflater = LayoutInflater.from(this);
            View layout = inflater.inflate(R.layout.checkbox, null);
            final CheckBox checkbox = (CheckBox)layout.findViewById(R.id.dont_show_again);
            checkbox.setChecked(!showInviteWarning);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(layout);
            builder.setMessage(getString(R.string.invite_warning));

            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    prefs.edit().putBoolean(KEY_SHOW_INVITE_WARNING, !checkbox.isChecked()).commit();

                    if (mIntentAction == ACTION_INVITE)
                        invite();
                    else
                        createMatch();
                }
            });

            builder.setNegativeButton("No", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    prefs.edit().putBoolean(KEY_SHOW_INVITE_WARNING, !checkbox.isChecked()).commit();
                }
            });

            mNotificationDialog = builder.create();
            mNotificationDialog.setCancelable(false);
            mNotificationDialog.show();
        }
        else
        {
            if (mIntentAction == ACTION_INVITE)
                invite();
            else
                createMatch();
        }
    }

    public void findPlayers(String text)
    {
        text = text.trim();

        mFindPlayersErrorView.setVisibility(View.GONE);
        mFindPlayersErrorOccurred = false;

        if (text.isEmpty())
        {
            showFriends();
            List<User> players = new ArrayList<User>();
            mNewMatchFragment.incrementSearchId();
            onFindPlayersSuccess(players, mNewMatchFragment.getSearchId());
        }
        else
        {
            showPlayers();
            mFindPlayersEmptyView.setVisibility(View.GONE);
            mFindPlayersProgressBar.setVisibility(View.VISIBLE);
            mNewMatchFragment.setFindingPlayers(true);
            mNewMatchFragment.findPlayers(text);
        }
    }

    public void retrieveFriends()
    {
        mRetrieveFriendsErrorView.setVisibility(View.GONE);
        mRetrieveFriendsErrorOccurred = false;
        showRetrieveFriendsProgressBar();
        mNewMatchFragment.setRetrievedFriends(true);
        mNewMatchFragment.retrieveFriends();
    }

    public void retrieveAttendees()
    {
        mRetrieveFriendsErrorView.setVisibility(View.GONE);
        mRetrieveFriendsErrorOccurred = false;
        showRetrieveFriendsProgressBar();
        mNewMatchFragment.setRetrievedFriends(true);
        mNewMatchFragment.retrieveAttendees(mIntentMatchId);
    }

    // TODO: Separate the invite code into a new method?
    private void createMatch()
    {
        List<User> invitees = getInvitees();

        if (invitees.size() + mAutoMatchSlots >= mIntentMinNumPlayers)
        {
            mNewMatchPanel.setVisibility(View.GONE);
            mInvitePanel.setVisibility(View.GONE);
            mCreatingMatchProgressBar.setVisibility(View.VISIBLE);

            int blackCardType = mBlackCardTypeView.getSelectedItemPosition();
            int whiteCardType = mWhiteCardTypeView.getSelectedItemPosition();
            int inactiveMode = mInactiveModeView.getSelectedItemPosition();
            int autoPickSkipTimeout = 0;
            int excludeHoursBegin = 0;
            int excludeHoursEnd = 0;

            if (mIntentAutoMatch)
                whiteCardType = mIntentWhiteCardType;

            if (inactiveMode != Match.INACTIVE_MODE_WAIT)
            {
                autoPickSkipTimeout = getAutoPickSkipTimeout();

                if (mExcludeHoursView.isChecked())
                {
                    excludeHoursBegin = GameFunctions.convertLocalToEstTime(mExcludeHoursBeginView.getSelectedItemPosition());
                    excludeHoursEnd = GameFunctions.convertLocalToEstTime(mExcludeHoursEndView.getSelectedItemPosition());
                }
            }

            // TODO: Hard coding CAH decks for now. You will need to add support for custom decks.
            List<String> decks = new ArrayList<String>();
            decks.add("CAH Base");
            decks.add("CAH Green Box");
            decks.add("CAH Red Box");
            decks.add("CAH Blue Box");
            decks.add("CAH 90s Nostalgia Pack");
            decks.add("CAH Geek Pack");
            decks.add("CAH Sci-Fi Pack");
            decks.add("CAH Fantasy Pack");
            decks.add("CAH Jew Pack");
            decks.add("CAH World Wide Web Pack");
            decks.add("CAH Food Pack");
            decks.add("CAH Science Pack");
            decks.add("CAH Post-Trump Pack");
            decks.add("CAH Vote for Hillary Pack");
            decks.add("CAH Vote for Trump Pack");
            decks.add("CAH 2012 Holiday Pack");
            decks.add("CAH 2013 Holiday Pack");
            decks.add("CAH 2014 Holiday Pack");
            decks.add("CAH Fascism Pack");
            decks.add("CAH House of Cards Pack");
            decks.add("CAH Reject Pack");

            mNewMatchFragment.setCreatingMatch(true);
            mNewMatchFragment.createMatch(mMatchName, mIntentAutoMatch, mAutoMatchSlots, blackCardType, whiteCardType, inactiveMode, autoPickSkipTimeout, excludeHoursBegin, excludeHoursEnd, decks, invitees);
        }
    }

    private void updateMatchSettings()
    {
        mNewMatchPanel.setVisibility(View.GONE);
        mInvitePanel.setVisibility(View.GONE);
        mCreatingMatchProgressBar.setVisibility(View.VISIBLE);
        mNewMatchFragment.setCreatingMatch(true);

        int inactiveMode = mInactiveModeView.getSelectedItemPosition();
        int autoPickSkipTimeout = 0;
        int excludeHoursBegin = 0;
        int excludeHoursEnd = 0;

        if (inactiveMode != Match.INACTIVE_MODE_WAIT)
        {
            autoPickSkipTimeout = getAutoPickSkipTimeout();

            if (mExcludeHoursView.isChecked())
            {
                excludeHoursBegin = GameFunctions.convertLocalToEstTime(mExcludeHoursBeginView.getSelectedItemPosition());
                excludeHoursEnd = GameFunctions.convertLocalToEstTime(mExcludeHoursEndView.getSelectedItemPosition());
            }
        }

        if (mIntentMatchName.equals(mMatchName) && mIntentInactiveMode == inactiveMode && mIntentAutoPickSkipTimeout == autoPickSkipTimeout &&
            mIntentExcludeHoursBegin == excludeHoursBegin && mIntentExcludeHoursEnd == excludeHoursEnd)
        {
            onUpdateMatchSettingsSuccess();
        }
        else
        {
            mNewMatchFragment.updateMatchSettings(mIntentMatchId, mMatchName, inactiveMode, autoPickSkipTimeout, excludeHoursBegin, excludeHoursEnd);
        }
    }

    private void invite()
    {
        List<String> inviteeIds = getInviteeIds();

        if (inviteeIds.size() + mAutoMatchSlots >= mIntentMinNumPlayers)
        {
            mNewMatchPanel.setVisibility(View.GONE);
            mInvitePanel.setVisibility(View.GONE);
            mCreatingMatchProgressBar.setVisibility(View.VISIBLE);

            mNewMatchFragment.setCreatingMatch(true);
            mNewMatchFragment.invite(mIntentMatchId, mAutoMatchSlots, inviteeIds);
        }
    }

    private void kick()
    {
        List<String> kickIds = getInviteeIds();
        int numKicks = kickIds.size() + mAutoMatchSlots;

        if (numKicks > 0)
        {
            mNewMatchPanel.setVisibility(View.GONE);
            mInvitePanel.setVisibility(View.GONE);
            mCreatingMatchProgressBar.setVisibility(View.VISIBLE);

            mNewMatchFragment.setCreatingMatch(true);
            mNewMatchFragment.kick(mIntentMatchId, mAutoMatchSlots, kickIds);
        }
    }

    public void onFindPlayersSuccess(List<User> players, int searchId)
    {
        if (searchId == mNewMatchFragment.getSearchId())
        {
            ToggleLog.d(TAG, "Successfully found players.");

            if (mIntentExclusions != null)
            {
                Iterator<User> iter = players.iterator();

                while (iter.hasNext())
                {
                    User friend = iter.next();

                    if (mIntentExclusions.contains(friend.getId()))
                        iter.remove();
                }
            }

            initializePlayerAdapter(players);
            mNewMatchFragment.setFindingPlayers(false);
            // TODO: Possibly call refreshHeaderText and have it display something like "Choose a player to invite" assuming mPlayers is non-empty.
        }
    }

    public void onFindPlayersFailure(int searchId)
    {
        if (searchId == mNewMatchFragment.getSearchId())
        {
            ToggleLog.d(TAG, "Failed to find players.");

            if (mPlayerAdapter != null)
            {
                mPlayers.clear();
                mPlayerAdapter.notifyDataSetChanged();
            }

            mNewMatchFragment.setFindingPlayers(false);
            mFindPlayersProgressBar.setVisibility(View.GONE);
            mFindPlayersErrorView.setVisibility(View.VISIBLE);
            mFindPlayersErrorOccurred = true;
        }
    }

    public void onRetrieveFriendsSuccess(List<User> friends)
    {
        ToggleLog.d(TAG, "Successfully retrieved friends. Total friends: " + friends.size());

        if (mIntentExclusions != null)
        {
            Iterator<User> iter = friends.iterator();

            while (iter.hasNext())
            {
                User friend = iter.next();

                if (mIntentExclusions.contains(friend.getId()))
                    iter.remove();
            }
        }

        if (mFriends == null)
            initializeFriendAdapter(friends);
        else
        {
            friends.removeAll(mFriends);

            for (User friend : friends)
            {
                mFriends.add(friend);
                Collections.sort(mFriends);
                int insertedPosition = mFriends.indexOf(friend);
                updateCheckedItemPositions(insertedPosition);
            }

            mFriendAdapter.notifyDataSetChanged();
        }

        refreshHeaderText();
        hideRetrieveFriendsProgressBar();
    }

    public void onRetrieveFriendsFailure()
    {
        ToggleLog.e(TAG, "Failed to retrieve friends.");
        hideRetrieveFriendsProgressBar();
        mNewMatchFragment.setRetrievedFriends(false);
        mRetrieveFriendsErrorText.setText(getString(R.string.retrieve_friends_failure));
        mRetrieveFriendsErrorView.setVisibility(View.VISIBLE);
        mRetrieveFriendsErrorOccurred = true;
    }

    public void onRetrieveAttendeesSuccess(List<User> attendees)
    {
        ToggleLog.d(TAG, "Successfully retrieved attendees. Total attendees: " + attendees.size());

        int index = attendees.indexOf(mUser);

        if (index != -1)
            attendees.remove(index);

        initializeFriendAdapter(attendees);

        if (mIntentAutoMatchSlots > 0)
            mAutoMatchSlotsRow.setVisibility(View.VISIBLE);

        refreshHeaderText();
        hideRetrieveFriendsProgressBar();
    }

    public void onRetrieveAttendeesFailure()
    {
        ToggleLog.e(TAG, "Failed to retrieve attendees.");
        hideRetrieveFriendsProgressBar();
        mNewMatchFragment.setRetrievedFriends(false);
        mRetrieveFriendsErrorText.setText(getString(R.string.retrieve_attendees_failure));
        mRetrieveFriendsErrorView.setVisibility(View.VISIBLE);
        mRetrieveFriendsErrorOccurred = true;
    }

    public void onCreateMatchSuccess(Match match)
    {
        ToggleLog.d(TAG, "Match created successfully! Match id: " + match.getId());
        Intent intent = new Intent();
        intent.putExtra(Match.KEY_MATCH_ID, match.getId());
        setResult(RESULT_OK, intent);
        finish();
    }

    public void onCreateMatchFailure(String message)
    {
        ToggleLog.d(TAG, "Failed to create match!");
        mNewMatchFragment.setCreatingMatch(false);
        mCreatingMatchProgressBar.setVisibility(View.GONE);

        // TODO: Test that this condition works (i.e. you are brought back to the create match screen).
        // Probably want to force a failure in NewMatchFragment.createMatch.
        if (mIntentAutoMatch)
            mNewMatchPanel.setVisibility(View.VISIBLE);

        mInvitePanel.setVisibility(View.VISIBLE);
        mNotificationDialog = GameFunctions.showDialog(this, getString(R.string.unable_to_create_match), message);
    }

    public void onUpdateMatchSettingsSuccess()
    {
        ToggleLog.d(TAG, "Update match settings success.");
        setResult(RESULT_OK);
        finish();
    }

    public void onUpdateMatchSettingsFailure(String message)
    {
        ToggleLog.d(TAG, "Failed to update match settings.");
        mNewMatchFragment.setCreatingMatch(false);
        mCreatingMatchProgressBar.setVisibility(View.GONE);
        mNewMatchPanel.setVisibility(View.VISIBLE);
        mNotificationDialog = GameFunctions.showDialog(this, getString(R.string.unable_to_update_match_settings), message);
    }

    // TODO: Finish implementing if necessary.
    public void onInviteSuccess()
    {
        ToggleLog.d(TAG, "Invite success.");
        setResult(RESULT_OK);
        finish();
    }

    // TODO: Finish implementing if necessary.
    public void onInviteFailure(String message)
    {
        ToggleLog.d(TAG, "Failed to invite.");
        mNewMatchFragment.setCreatingMatch(false);
        mCreatingMatchProgressBar.setVisibility(View.GONE);
        mInvitePanel.setVisibility(View.VISIBLE);
        mNotificationDialog = GameFunctions.showDialog(this, getString(R.string.unable_to_invite), message);
    }

    public void onKickSuccess()
    {
        ToggleLog.d(TAG, "Kick success.");
        setResult(RESULT_OK);
        finish();
    }

    public void onKickFailure(String message)
    {
        ToggleLog.d(TAG, "Failed to kick.");
        mNewMatchFragment.setCreatingMatch(false);
        mCreatingMatchProgressBar.setVisibility(View.GONE);
        mInvitePanel.setVisibility(View.VISIBLE);
        mNotificationDialog = GameFunctions.showDialog(this, getString(R.string.unable_to_kick), message);
    }
}
