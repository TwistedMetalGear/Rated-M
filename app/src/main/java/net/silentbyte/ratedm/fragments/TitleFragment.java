package net.silentbyte.ratedm.fragments;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import net.silentbyte.ratedm.Match;
import net.silentbyte.ratedm.R;
import net.silentbyte.ratedm.RatedMServer;
import net.silentbyte.ratedm.ToggleLog;
import net.silentbyte.ratedm.User;
import net.silentbyte.ratedm.WinningCardPair;
import net.silentbyte.ratedm.notifications.GetTokenTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

// TODO: Do we need to handle any cases where the PHP returns SUCCESS/FAILURE strings?
public class TitleFragment extends Fragment
{
    private static final String TAG = "TitleFragment";

    private CallbackHandler mCallbackHandler;

    // The following are globals that we need to keep track of across configuration changes.
    // Why are they here? Well, they were originally in MainActivity and got saved/restored
    // through the bundle. However, there was a chance for a problem when a configuration
    // change occurred with async tasks running in the background:
    //  1) Configuration change started, onSavedInstanceState() called.
    //  2) Async task returned and we updated a global as a result.
    //  3) onDestroy() called, activity recreated, and old/stale value of global restored.
    // So now we are putting the globals in here since this is a retained fragment and we
    // don't have to rely on onSavedInstanceState().
    private int mVersion;
    private String mProgressText;

    // Like the above, we need to keep track of these globals across configuration changes.
    // Under certain conditions, it can be dangerous if these are true and the process gets
    // destroyed. Hence, they have been moved into this class so that when the process gets
    // destroyed, they are set back to their default value (false).
    private boolean mRetrieveWinningCards = true;
    private boolean mAutoMatching = false;
    private boolean mBusy = false;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        mCallbackHandler = (CallbackHandler)activity;
    }

    @Override
    public void onDetach()
    {
        super.onDetach();

        // Set mCallbackHandler to null so we don't accidentally leak the activity instance.
        mCallbackHandler = null;
    }

    public String getStringSafe(int resId)
    {
        try
        {
            return getString(resId);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public void retrieveWinningCards()
    {
        RatedMServer.getWinningCards(new RatedMServer.CallbackHandler()
        {
            @Override
            public void onResponse(int responseCode, String response)
            {
                if (responseCode == 200)
                {
                    try
                    {
                        List<WinningCardPair> winningCards = convertToWinningCardPairs(response);

                        if (mCallbackHandler != null)
                            mCallbackHandler.onWinningCardsReceived(winningCards);
                    }
                    catch (JSONException e)
                    {
                        ToggleLog.e(TAG, "Unable to convert json to winning cards. JSONException: " + e.getMessage());

                        if (mCallbackHandler != null)
                            mCallbackHandler.onWinningCardsReceivedFailure();
                    }
                }
                else
                {
                    ToggleLog.e(TAG, "Unable to retrieve winning cards. Response code: " + responseCode);

                    if (mCallbackHandler != null)
                        mCallbackHandler.onWinningCardsReceivedFailure();
                }
            }
        });
    }

    public void autoMatch(final int whiteCardType)
    {
        User user = User.get(getContext());

        if (user != null)
        {
            RatedMServer.autoMatch(user.getId(), whiteCardType, new RatedMServer.CallbackHandler()
            {
                @Override
                public void onResponse(int responseCode, String response)
                {
                    if (responseCode == 200)
                    {
                        try
                        {
                            JSONObject jsonMatch = new JSONObject(response);
                            Match match = new Match(jsonMatch);

                            if (mCallbackHandler != null)
                                mCallbackHandler.onAutoMatchSuccess(match, whiteCardType);
                        }
                        catch (JSONException e)
                        {
                            ToggleLog.e(TAG, "Unable to auto match. JSONException: " + e.getMessage());

                            if (mCallbackHandler != null)
                                mCallbackHandler.onAutoMatchFailure(getStringSafe(R.string.try_again_later));
                        }
                    }
                    else if (responseCode == 225)
                    {
                        ToggleLog.d(TAG, "No matches with available auto match slots.");

                        if (mCallbackHandler != null)
                            mCallbackHandler.onAutoMatchSuccess(null, whiteCardType);
                    }
                    else
                    {
                        ToggleLog.e(TAG, "Unable to auto match. Response code: " + responseCode);

                        if (mCallbackHandler != null)
                            mCallbackHandler.onAutoMatchFailure(getStringSafe(R.string.try_again_later));
                    }
                }
            });
        }
        else
        {
            ToggleLog.e(TAG, "Unable to auto match. User is null.");

            if (mCallbackHandler != null)
                mCallbackHandler.onAutoMatchFailure(getStringSafe(R.string.sign_in_again));
        }
    }

    public void deleteToken()
    {
        User user = User.get(getContext());

        if (user != null)
        {
            final String userId = user.getId();

            GetTokenTask task = new GetTokenTask(getContext(), new GetTokenTask.CallbackHandler()
            {
                @Override
                public void onGetTokenSuccess(String token)
                {
                    RatedMServer.deleteToken(userId, token, new RatedMServer.CallbackHandler()
                    {
                        @Override
                        public void onResponse(int responseCode, String response)
                        {
                            if (responseCode == 200)
                                ToggleLog.d(TAG, "Successfully deleted token.");
                            else
                                ToggleLog.e(TAG, "Unable to delete token. Response code: " + responseCode);
                        }
                    });
                }

                @Override
                public void onGetTokenFailure()
                {
                    ToggleLog.e(TAG, "Unable to delete token. No token available to delete.");
                }
            });

            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    public int getVersion()
    {
        return mVersion;
    }

    public void setVersion(int version)
    {
        mVersion = version;
    }

    public String getProgressText()
    {
        return mProgressText;
    }

    public void setProgressText(String progressText)
    {
        mProgressText = progressText;
    }

    public boolean shouldRetrieveWinningCards()
    {
        return mRetrieveWinningCards;
    }

    public void setRetrieveWinningCards(boolean retrieveWinningCards)
    {
        mRetrieveWinningCards = retrieveWinningCards;
    }

    public boolean isAutoMatching()
    {
        return mAutoMatching;
    }

    public void setAutoMatching(boolean autoMatching)
    {
        mAutoMatching = autoMatching;
    }

    public boolean isBusy()
    {
        return mBusy;
    }

    public void setBusy(boolean busy)
    {
        mBusy = busy;
    }

    private List<WinningCardPair> convertToWinningCardPairs(String json) throws JSONException
    {
        JSONArray jsonCardPairs = new JSONArray(json);
        List<WinningCardPair> winningCardPairs = new ArrayList<WinningCardPair>();

        for (int i = 0; i < jsonCardPairs.length(); i++)
        {
            JSONObject jsonCardPair = jsonCardPairs.getJSONObject(i);
            winningCardPairs.add(new WinningCardPair(jsonCardPair));
        }

        return winningCardPairs;
    }

    public interface CallbackHandler
    {
        public void onWinningCardsReceived(List<WinningCardPair> winningCards);
        public void onWinningCardsReceivedFailure();

        public void onAutoMatchSuccess(Match match, int whiteCardType);
        public void onAutoMatchFailure(String message);
    }
}
