package net.silentbyte.ratedm.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import net.silentbyte.ratedm.Card;
import net.silentbyte.ratedm.Match;
import net.silentbyte.ratedm.RatedMServer;
import net.silentbyte.ratedm.Round;
import net.silentbyte.ratedm.ToggleLog;
import net.silentbyte.ratedm.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class MatchesFragment extends Fragment
{
    private static final String TAG = "MatchesFragment";
    private static final String AUTO_MATCH = "auto_match";
    private CallbackHandler mCallbackHandler;
    private boolean mRetrievedMatches = false;
    private int mLoadingPosition = -1;

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

    public boolean retrievedMatches()
    {
        return mRetrievedMatches;
    }

    public void setRetrievedMatches(boolean retrievedMatches)
    {
        mRetrievedMatches = retrievedMatches;
    }

    public int getLoadingPosition()
    {
        return mLoadingPosition;
    }

    public void setLoadingPosition(int loadingPosition)
    {
        mLoadingPosition = loadingPosition;
    }

    public void retrieveMatches()
    {
        final User user = User.get(getContext());

        if (user != null)
        {
            RatedMServer.getMatches(user.getId(), new RatedMServer.CallbackHandler()
            {
                @Override
                public void onResponse(int responseCode, String response)
                {
                    if (responseCode == 200)
                    {
                        try
                        {
                            List<Match> matches = convertToMatches(response);
                            formatMatches(matches, user.getId());
                        }
                        catch (JSONException e)
                        {
                            ToggleLog.e(TAG, "Unable to retrieve matches. JSONException: " + e.getMessage());

                            if (mCallbackHandler != null)
                                mCallbackHandler.onRetrieveMatchesFailure();
                        }
                    }
                    else
                    {
                        ToggleLog.e(TAG, "Unable to retrieve matches. Response code: " + responseCode);

                        if (mCallbackHandler != null)
                            mCallbackHandler.onRetrieveMatchesFailure();
                    }
                }
            });
        }
        else
        {
            ToggleLog.e(TAG, "Unable to retrieve matches. User is null.");

            if (mCallbackHandler != null)
                mCallbackHandler.onRetrieveMatchesFailure();
        }
    }

    private List<Match> convertToMatches(String json) throws JSONException
    {
        JSONArray jsonMatches = new JSONArray(json);
        List<Match> matches = new ArrayList<Match>();

        for (int i = 0; i < jsonMatches.length(); i++)
        {
            try
            {
                JSONObject jsonMatch = jsonMatches.getJSONObject(i);
                matches.add(new Match(jsonMatch));
            }
            catch (JSONException e)
            {
                // Match appears to be corrupt. We won't add this one to the list.
                ToggleLog.e(TAG, "convertToMatches: Unable to convert JSONObject to Match. JSONException: " + e.getMessage());
            }
        }

        return matches;
    }

    private void formatMatches(List<Match> matches, String userId)
    {
        for (Match match : matches)
        {
            Round round = match.getCurrentRound();
            int matchState = match.getState();
            List<String> inviteeIds = match.getInviteeIds();
            List<String> pendingJoinIds = match.getPendingJoinIds();
            List<String> playerIds = match.getPlayerIds();
            List<String> names = match.getNames();
            List<String> pictureUrls = match.getPictureUrls();
            String pictureUrl = pictureUrls.get(0);
            String stateText;

            if (inviteeIds.contains(userId))
                stateText = names.get(0) + " invites you to play.";
            else if (matchState == Match.MATCH_STATE_PICKING)
            {
                Map<String, List<Card>> submittedCards = round.getSubmittedCards();

                if (submittedCards.containsKey(userId) || userId.equals(round.getJudgeId()) || pendingJoinIds.contains(userId))
                    stateText = "Waiting for players to submit cards.";
                else
                {
                    if (round.getBlackCard().getType() == Card.CARD_TYPE_PICK_TWO)
                        stateText = "Pick two cards.";
                    else
                        stateText = "Pick a card.";
                }
            }
            else if (matchState == Match.MATCH_STATE_JUDGING)
            {
                int position = playerIds.indexOf(round.getJudgeId());

                if (userId.equals(round.getJudgeId()))
                {
                    if (round.getBlackCard().getType() == Card.CARD_TYPE_PICK_TWO)
                        stateText = "Pick a winning card pair.";
                    else
                        stateText = "Pick a winning card.";
                }
                else
                {
                    String judgeName = names.get(position);
                    stateText = "Waiting for " + judgeName + " to pick a winner.";
                }
            }
            else if (matchState == Match.MATCH_STATE_WRITING_CARD)
            {
                int position = playerIds.indexOf(round.getJudgeId());

                if (userId.equals(round.getJudgeId()))
                    stateText = "Write black card.";
                else
                {
                    String judgeName = names.get(position);
                    stateText = "Waiting for " + judgeName + " to write card.";
                }
            }
            else if (matchState == Match.MATCH_STATE_COMPLETE)
            {
                String winnerId = round.getWinnerId();
                String winnerName;
                int position = playerIds.indexOf(winnerId);

                if (userId.equals(winnerId))
                    winnerName = "You";
                else
                    winnerName = names.get(position);

                stateText = winnerName + " won the match.";
            }
            else if (matchState == Match.MATCH_STATE_CANCELED)
            {
                if (!match.getPlayerIds().contains(match.getCreatorId()))
                    stateText = "Match creator (" + names.get(0) + ") has left.";
                else if (match.getPlayerIds().size() < 3)
                    stateText = "Insufficient player count.";
                else
                    stateText = "Exceeded max number of rounds";
            }
            else // MATCH_STATE_EXPIRED
                stateText = "Match has expired.";

            match.setPictureUrl(pictureUrl);
            match.setStateText(stateText);
        }

        formatMatchListItems(matches, userId);

        if (mCallbackHandler != null)
            mCallbackHandler.onRetrieveMatchesSuccess(matches);
    }

    private void formatMatchListItems(List<Match> matches, String userId)
    {
        List<Match> activeMatches = new ArrayList<Match>();
        List<Match> invitedMatches = new ArrayList<Match>();
        List<Match> completedMatches = new ArrayList<Match>();
        List<Match> canceledMatches = new ArrayList<Match>();
        List<Match> expiredMatches = new ArrayList<Match>();

        boolean hasActiveMatches = false;
        boolean hasInvitedMatches = false;
        boolean hasCompletedMatches = false;
        boolean hasCanceledMatches = false;
        boolean hasExpiredMatches = false;

        for (Match match : matches)
        {
            if (match.getState() == Match.MATCH_STATE_PICKING ||
                match.getState() == Match.MATCH_STATE_JUDGING ||
                match.getState() == Match.MATCH_STATE_WRITING_CARD)
            {
                if (match.getInviteeIds().contains(userId))
                {
                    if (!hasInvitedMatches)
                    {
                        invitedMatches.add(new Match("Invitations"));
                        hasInvitedMatches = true;
                    }

                    invitedMatches.add(match);
                }
                else
                {
                    if (!hasActiveMatches)
                    {
                        activeMatches.add(new Match("Active Matches"));
                        hasActiveMatches = true;
                    }

                    activeMatches.add(match);
                }
            }
            else if (match.getState() == Match.MATCH_STATE_COMPLETE)
            {
                if (!hasCompletedMatches)
                {
                    completedMatches.add(new Match("Completed Matches"));
                    hasCompletedMatches = true;
                }

                completedMatches.add(match);
            }
            else if (match.getState() == Match.MATCH_STATE_CANCELED)
            {
                if (!hasCanceledMatches)
                {
                    canceledMatches.add(new Match("Canceled Matches"));
                    hasCanceledMatches = true;
                }

                canceledMatches.add(match);
            }
            else // MATCH_STATE_EXPIRED
            {
                if (!hasExpiredMatches)
                {
                    expiredMatches.add(new Match("Expired Matches"));
                    hasExpiredMatches = true;
                }

                expiredMatches.add(match);
            }
        }

        // TODO: Lots of redundancy here. Can probably create helper functions to shorten this.
        // Another option is to do away with this sort and separate out sections of activeMatches into individual lists, then sort those by updatedAt.
        Collections.sort(matches, new Comparator<Match>()
        {
            String userId = "playerIdA";

            @Override
            public int compare(Match lhs, Match rhs)
            {
                if (lhs.isDivider() || rhs.isDivider())
                    return 0;

                Round lhsRound = lhs.getCurrentRound();
                Round rhsRound = rhs.getCurrentRound();

                int lhsState = lhs.getState();
                int rhsState = rhs.getState();

                if (lhsState == Match.MATCH_STATE_JUDGING && rhsState == Match.MATCH_STATE_JUDGING)
                {
                    if (lhsRound.getJudgeId().equals(userId) && rhsRound.getJudgeId().equals(userId))
                        return longCompare(rhs.getUpdatedAt(), lhs.getUpdatedAt());
                    else if (lhsRound.getJudgeId().equals(userId))
                        return -1;
                    else if (rhsRound.getJudgeId().equals(userId))
                        return 1;
                    else
                    {
                        int result = lhsRound.getJudgeId().compareTo(rhsRound.getJudgeId());

                        if (result == 0)
                            return longCompare(rhs.getUpdatedAt(), lhs.getUpdatedAt());
                        else
                            return result;
                    }
                }
                else if (lhsState == Match.MATCH_STATE_JUDGING)
                {
                    if (lhsRound.getJudgeId().equals(userId))
                        return -1;

                    if (rhsState == Match.MATCH_STATE_PICKING)
                    {
                        if (!rhsRound.getSubmittedCards().containsKey(userId))
                            return 1;
                    }
                    else
                        return -1;
                }
                else if (rhsState == Match.MATCH_STATE_JUDGING)
                {
                    if (rhsRound.getJudgeId().equals(userId))
                        return 1;

                    if (lhsState == Match.MATCH_STATE_PICKING)
                    {
                        if (!lhsRound.getSubmittedCards().containsKey(userId))
                            return -1;
                    }
                    else
                        return 1;
                }
                else if (lhsState == Match.MATCH_STATE_PICKING && rhsState == Match.MATCH_STATE_PICKING)
                {
                    if (!lhsRound.getSubmittedCards().containsKey(userId) && !rhsRound.getSubmittedCards().containsKey(userId))
                        return longCompare(rhs.getUpdatedAt(), lhs.getUpdatedAt());
                    else if (!lhsRound.getSubmittedCards().containsKey(userId))
                        return -1;
                    else if (!rhsRound.getSubmittedCards().containsKey(userId))
                        return 1;
                    else
                        return longCompare(rhs.getUpdatedAt(), lhs.getUpdatedAt());
                }
                else if (lhsState == Match.MATCH_STATE_PICKING)
                {
                    if (!lhsRound.getSubmittedCards().containsKey(userId))
                        return -1;
                    else
                        return 1;
                }
                else if (rhsState == Match.MATCH_STATE_PICKING)
                {
                    if (!rhsRound.getSubmittedCards().containsKey(userId))
                        return 1;
                    else
                        return -1;
                }

                return 0;
            }
        });

        sortByLastUpdated(activeMatches);
        sortByLastUpdated(invitedMatches);
        sortByLastUpdated(completedMatches);
        sortByLastUpdated(canceledMatches);
        sortByLastUpdated(expiredMatches);

        matches.clear();
        matches.addAll(activeMatches);
        matches.addAll(invitedMatches);
        matches.addAll(completedMatches);
        matches.addAll(canceledMatches);
        matches.addAll(expiredMatches);
    }

    private void sortByLastUpdated(List<Match> matches)
    {
        Collections.sort(matches, new Comparator<Match>()
        {
            @Override
            public int compare(Match lhs, Match rhs)
            {
                if (lhs.isDivider() || rhs.isDivider())
                    return 0;

                return longCompare(rhs.getUpdatedAt(), lhs.getUpdatedAt());
            }
        });
    }

    private int longCompare(long lhs, long rhs)
    {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

    public interface CallbackHandler
    {
        public void onRetrieveMatchesSuccess(List<Match> matches);
        public void onRetrieveMatchesFailure();
    }
}
