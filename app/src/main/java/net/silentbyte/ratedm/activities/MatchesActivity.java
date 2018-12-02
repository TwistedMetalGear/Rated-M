package net.silentbyte.ratedm.activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.silentbyte.ratedm.GameConstants;
import net.silentbyte.ratedm.GameFunctions;
import net.silentbyte.ratedm.Match;
import net.silentbyte.ratedm.R;
import net.silentbyte.ratedm.ToggleLog;
import net.silentbyte.ratedm.User;
import net.silentbyte.ratedm.adapters.MatchAdapter;
import net.silentbyte.ratedm.fragments.MatchesFragment;
import net.silentbyte.ratedm.notifications.NotificationService;

import java.util.ArrayList;
import java.util.List;

public class MatchesActivity extends FragmentActivity implements MatchesFragment.CallbackHandler, NotificationService.CallbackHandler
{
    private static final String TAG = "MatchesActivity";
    private static final String TAG_MATCHES_FRAGMENT = "matches_fragment";
    private static final String KEY_MATCH_LIST_ITEMS = "match_list_items";
    private static final String KEY_LIST_VIEW_STATE = "list_view_state";
    private static final String KEY_FIRST_RETRIEVAL = "first_retrieval";
    private static final String KEY_ERROR_OCCURRED = "error_occurred";
    public static final String KEY_ENTERED_FROM_LAUNCH = "entered_from_launch";
    private static final int RC_GAME = 0;

    private MatchesFragment mMatchesFragment;

    private ProgressBar mProgressBar;
    private View mContentView;
    private View mEmptyView;
    private View mErrorView;
    private View mPlayButton;
    private TextView mHeaderTextView;

    private ListView mMatchesListView;
    private List<Match> mMatches;
    private MatchAdapter mMatchAdapter;

    private boolean mEnteredFromLaunch = false;
    private boolean mFirstRetrieval = true;
    private boolean mPaused = false;
    private boolean mErrorOccurred = false;

    private Dialog mNotificationDialog;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Lock the device in portrait mode.
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_matches);

        mEnteredFromLaunch = getIntent().getBooleanExtra(KEY_ENTERED_FROM_LAUNCH, false);

        mProgressBar = (ProgressBar)findViewById(R.id.progress_bar);
        mContentView = findViewById(R.id.content);
        mEmptyView = findViewById(R.id.empty);
        mErrorView = findViewById(R.id.error);
        mHeaderTextView = (TextView)findViewById(R.id.header_text);

        mMatchesListView = (ListView)findViewById(R.id.matches);
        mMatchesListView.setDividerHeight(1);
        mMatchesListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if (mMatchesFragment.getLoadingPosition() == -1)
                {
                    Match match = mMatches.get(position);

                    // Check to see if match has expired.
                    if (match.getState() == Match.MATCH_STATE_EXPIRED)
                        mNotificationDialog = GameFunctions.showDialog(MatchesActivity.this, getString(R.string.expired_title), getString(R.string.expired_description));
                    else
                    {
                        User user = User.get(MatchesActivity.this);

                        if (user != null)
                        {
                            showListItemProgressBar(position);
                            startGameActivity(match.getId());
                        }
                        else
                            mNotificationDialog = GameFunctions.showDialog(MatchesActivity.this, getString(R.string.unable_to_load_match), getString(R.string.sign_in_again));
                    }
                }
            }
        });

        Button retryButton = (Button)findViewById(R.id.retry_button);
        retryButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mMatchesFragment.setRetrievedMatches(false);
                mErrorOccurred = false;
                retrieveMatches();
            }
        });

        FragmentManager fm = getSupportFragmentManager();

        // Add matches fragment to fragment manager.
        mMatchesFragment = (MatchesFragment)fm.findFragmentByTag(TAG_MATCHES_FRAGMENT);

        if (mMatchesFragment == null)
        {
            mMatchesFragment = new MatchesFragment();
            fm.beginTransaction().add(mMatchesFragment, TAG_MATCHES_FRAGMENT).commit();
        }

        if (savedInstanceState != null)
        {
            List<Match> matches = savedInstanceState.getParcelableArrayList(KEY_MATCH_LIST_ITEMS);

            if (matches != null)
            {
                mMatches = matches;
                initializeAdapter(mMatches);

                // Restore scroll position and checked items.
                Parcelable state = savedInstanceState.getParcelable(KEY_LIST_VIEW_STATE);
                mMatchesListView.onRestoreInstanceState(state);
            }

            mFirstRetrieval = savedInstanceState.getBoolean(KEY_FIRST_RETRIEVAL);
            mErrorOccurred = savedInstanceState.getBoolean(KEY_ERROR_OCCURRED);

            if (mErrorOccurred)
            {
                mProgressBar.setVisibility(View.GONE);
                mErrorView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        mPaused = false;

        NotificationService.setCallbackHandler(this);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (prefs.contains(NotificationService.KEY_NOTIFICATION_IDS))
            prefs.edit().remove(NotificationService.KEY_NOTIFICATION_IDS).commit();

        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(0);

        // Commenting this out. It is preferable to always retrieve matches on resuming to ensure that we are always up to date.
        // if (!mMatchesFragment.retrievedMatches() && mMatches == null && !mErrorOccurred)
        retrieveMatches();
    }

    @Override
    public void onDestroy()
    {
        // Clear the handler for the push receiver.
        if (NotificationService.getCallbackHandler() == this)
            NotificationService.setCallbackHandler(null);

        super.onDestroy();
    }

    @Override
    public void onPause()
    {
        super.onPause();

        mPaused = true;

        if (mNotificationDialog != null)
            mNotificationDialog.dismiss();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putParcelableArrayList(KEY_MATCH_LIST_ITEMS, (ArrayList) mMatches);
        savedInstanceState.putParcelable(KEY_LIST_VIEW_STATE, mMatchesListView.onSaveInstanceState());
        savedInstanceState.putBoolean(KEY_FIRST_RETRIEVAL, mFirstRetrieval);
        savedInstanceState.putBoolean(KEY_ERROR_OCCURRED, mErrorOccurred);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        mMatches = null;
        hideListItemProgressBar();
        retrieveMatches();

        if (requestCode == RC_GAME)
        {
            if (resultCode == GameConstants.RESULT_EXPIRED)
                mNotificationDialog = GameFunctions.showDialog(this, getString(R.string.expired_title), getString(R.string.expired_description));
            else if (resultCode == GameConstants.RESULT_REMOVED)
                mNotificationDialog = GameFunctions.showDialog(this, getString(R.string.removed_title), getString(R.string.removed_description));
            else if (resultCode == GameConstants.RESULT_LOAD_FAILED)
                mNotificationDialog = GameFunctions.showDialog(this, getString(R.string.unable_to_load_match), getString(R.string.connection_lost));
        }
    }

    @Override
    public void finish()
    {
        // Clear the handler for the push receiver.
        if (NotificationService.getCallbackHandler() == this)
            NotificationService.setCallbackHandler(null);

        super.finish();
    }

    private void startGameActivity(String matchId)
    {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra(Match.KEY_MATCH_ID, matchId);
        startActivityForResult(intent, RC_GAME);
    }

    private void initializeAdapter(List<Match> matches)
    {
        mMatches = matches;
        mMatchAdapter = new MatchAdapter(this, mMatches, mMatchesFragment.getLoadingPosition());
        mMatchesListView.setAdapter(mMatchAdapter);
        mProgressBar.setVisibility(View.GONE);

        if (mMatches.isEmpty())
            mEmptyView.setVisibility(View.VISIBLE);
        else
            mContentView.setVisibility(View.VISIBLE);
    }

    private void showProgressBar(int position, boolean showProgressBar)
    {
        View v = mMatchesListView.getChildAt(position);
        View progressBarView = v.findViewById(R.id.progress_bar);

        if (showProgressBar)
            progressBarView.setVisibility(View.VISIBLE);
        else
            progressBarView.setVisibility(View.GONE);
    }

    private void showListItemProgressBar(int position)
    {
        mMatchAdapter.setLoadingPosition(position);
        mMatchAdapter.notifyDataSetChanged();
        mMatchesFragment.setLoadingPosition(position);
    }

    private void hideListItemProgressBar()
    {
        mMatchAdapter.setLoadingPosition(-1);
        mMatchAdapter.notifyDataSetChanged();
        mMatchesFragment.setLoadingPosition(-1);
    }

    private void retrieveMatches()
    {
        mErrorView.setVisibility(View.GONE);
        mErrorOccurred = false;
        mContentView.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
        mMatchesFragment.setRetrievedMatches(true);
        mMatchesFragment.retrieveMatches();
    }

    public void onRetrieveMatchesSuccess(List<Match> matches)
    {
        ToggleLog.d(TAG, "Successfully retrieved matches.");

        if (mEnteredFromLaunch && mFirstRetrieval && matches.isEmpty())
        {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        initializeAdapter(matches);
        mFirstRetrieval = false;
        mErrorOccurred = false;
    }

    public void onRetrieveMatchesFailure()
    {
        ToggleLog.e(TAG, "Unable to retrieve matches.");
        mMatchesFragment.setRetrievedMatches(false);
        mProgressBar.setVisibility(View.GONE);
        mErrorView.setVisibility(View.VISIBLE);
        mFirstRetrieval = false;
        mErrorOccurred = true;
    }

    public boolean isPaused()
    {
        return mPaused;
    }

    public void onMatchUpdate()
    {
        // TODO: Commenting this out for now.
        // It's annoying to have the match list disappear while it refreshes every time a player takes a turn.
        // A better solution would be to update the existing list as updates come in.
        /*
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                retrieveMatches();
            }
        });
        */
    }
}
