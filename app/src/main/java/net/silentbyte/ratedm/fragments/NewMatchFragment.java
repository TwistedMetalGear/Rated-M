package net.silentbyte.ratedm.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;

import net.silentbyte.ratedm.GameConstants;
import net.silentbyte.ratedm.Match;
import net.silentbyte.ratedm.R;
import net.silentbyte.ratedm.RatedMServer;
import net.silentbyte.ratedm.ToggleLog;
import net.silentbyte.ratedm.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NewMatchFragment extends Fragment
{
    private static final String TAG = "NewMatchFragment";
    private CallbackHandler mCallbackHandler;
    private boolean mRetrievedFriends = false;
    private boolean mFindingPlayers = false;
    private boolean mCreatingMatch = false;
    private int mSearchId = 0;

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

    public boolean retrievedFriends()
    {
        return mRetrievedFriends;
    }

    public void setRetrievedFriends(boolean retrievedFriends)
    {
        mRetrievedFriends = retrievedFriends;
    }

    public boolean isFindingPlayers()
    {
        return mFindingPlayers;
    }

    public void setFindingPlayers(boolean findingPlayers)
    {
        mFindingPlayers = findingPlayers;
    }

    public boolean isCreatingMatch()
    {
        return mCreatingMatch;
    }

    public void setCreatingMatch(boolean creatingMatch)
    {
        mCreatingMatch = creatingMatch;
    }

    public int getSearchId()
    {
        return mSearchId;
    }

    public void incrementSearchId()
    {
        mSearchId++;
    }

    public void findPlayers(String name)
    {
        User user = User.get(getContext());
        final int searchId = ++mSearchId;

        if (user != null)
        {
            final String userId = user.getId();

            RatedMServer.getUsers(name, new RatedMServer.CallbackHandler()
            {
                @Override
                public void onResponse(int responseCode, String response)
                {
                    if (responseCode == 200)
                    {
                        try
                        {
                            JSONArray jsonPlayers = new JSONArray(response);
                            List<User> players = fromJSON(jsonPlayers);

                            if (mCallbackHandler != null)
                                mCallbackHandler.onFindPlayersSuccess(players, searchId);
                        }
                        catch (JSONException e)
                        {
                            ToggleLog.e(TAG, "Unable to find players. JSONException: " + e.getMessage());

                            if (mCallbackHandler != null)
                                mCallbackHandler.onFindPlayersFailure(searchId);
                        }
                    }
                    else
                    {
                        ToggleLog.e(TAG, "Unable to find players. Response code: " + responseCode);

                        if (mCallbackHandler != null)
                            mCallbackHandler.onFindPlayersFailure(searchId);
                    }
                }

                private List<User> fromJSON(JSONArray jsonPlayers) throws JSONException
                {
                    ArrayList<User> players = new ArrayList<User>();

                    for (int i = 0; i < jsonPlayers.length(); i++)
                    {
                        JSONObject jsonPlayer = jsonPlayers.getJSONObject(i);
                        String id = jsonPlayer.getString(User.JSON_ID);

                        if (!id.equals(userId))
                            players.add(new User(jsonPlayer));
                    }

                    Collections.sort(players);
                    return players;
                }
            });
        }
        else
        {
            ToggleLog.e(TAG, "Unable to find players. User is null.");

            if (mCallbackHandler != null)
                mCallbackHandler.onFindPlayersFailure(searchId);
        }
    }

    public void retrieveFriends()
    {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();

        if (accessToken != null)
        {
            GraphRequest request = GraphRequest.newMyFriendsRequest(accessToken, new GraphRequest.GraphJSONArrayCallback()
            {
                @Override
                public void onCompleted(JSONArray jsonArray, GraphResponse graphResponse)
                {
                    List<String> ids = new ArrayList<String>();
                    boolean success = true;

                    if (jsonArray != null)
                    {
                        try
                        {
                            for (int i = 0; i < jsonArray.length(); i++)
                            {
                                JSONObject jsonFriend = (JSONObject)jsonArray.get(i);
                                ids.add(jsonFriend.getString("id"));
                            }
                        }
                        catch (JSONException e)
                        {
                            ToggleLog.e(TAG, "Unable to retrieve friends. Exception: " + e.getMessage());
                            success = false;
                        }
                    }
                    else
                    {
                        ToggleLog.e(TAG, "Unable to retrieve friends. The JSON array is null.");
                        success = false;
                    }

                    if (success)
                    {
                        RatedMServer.getFacebookUsers(ids, new RatedMServer.CallbackHandler()
                        {
                            @Override
                            public void onResponse(int responseCode, String response)
                            {
                                if (responseCode == 200)
                                {
                                    try
                                    {
                                        List<User> friends = new ArrayList<User>();
                                        JSONArray jsonFriends = new JSONArray(response);

                                        for (int i = 0; i < jsonFriends.length(); i++)
                                        {
                                            JSONObject jsonFriend = jsonFriends.getJSONObject(i);
                                            User friend = new User(jsonFriend);
                                            friends.add(friend);
                                        }

                                        Collections.sort(friends);

                                        if (mCallbackHandler != null)
                                            mCallbackHandler.onRetrieveFriendsSuccess(friends);
                                    }
                                    catch (JSONException e)
                                    {
                                        ToggleLog.e(TAG, "Unable to retrieve friends. JSONException: " + e.getMessage());

                                        if (mCallbackHandler != null)
                                            mCallbackHandler.onRetrieveFriendsFailure();
                                    }
                                }
                                else
                                {
                                    ToggleLog.e(TAG, "Unable to retrieve friends. Response code: " + responseCode);

                                    if (mCallbackHandler != null)
                                        mCallbackHandler.onRetrieveFriendsFailure();
                                }
                            }
                        });
                    }
                    else
                    {
                        if (mCallbackHandler != null)
                            mCallbackHandler.onRetrieveFriendsFailure();
                    }
                }
            });

            Bundle parameters = new Bundle();
            parameters.putString("fields", "id");
            request.setParameters(parameters);
            request.executeAsync();
        }
        else
        {
            ToggleLog.e(TAG, "Unable to retrieve friends. Access token is null.");

            if (mCallbackHandler != null)
                mCallbackHandler.onRetrieveFriendsFailure();
        }
    }

    public void retrieveAttendees(String matchId)
    {
        RatedMServer.getAttendees(matchId, new RatedMServer.CallbackHandler()
        {
            @Override
            public void onResponse(int responseCode, String response)
            {
                if (responseCode == 200)
                {
                    try
                    {
                        List<User> attendees = new ArrayList<User>();
                        JSONArray jsonAttendees = new JSONArray(response);

                        for (int i = 0; i < jsonAttendees.length(); i++)
                        {
                            JSONObject jsonAttendee = jsonAttendees.getJSONObject(i);
                            User attendee = new User(jsonAttendee);
                            attendees.add(attendee);
                        }

                        Collections.sort(attendees);

                        if (mCallbackHandler != null)
                            mCallbackHandler.onRetrieveAttendeesSuccess(attendees);
                    }
                    catch (JSONException e)
                    {
                        ToggleLog.e(TAG, "Unable to retrieve attendees. JSONException: " + e.getMessage());

                        if (mCallbackHandler != null)
                            mCallbackHandler.onRetrieveAttendeesFailure();
                    }
                }
                else
                {
                    ToggleLog.e(TAG, "Unable to retrieve attendees. Response code: " + responseCode);

                    if (mCallbackHandler != null)
                        mCallbackHandler.onRetrieveAttendeesFailure();
                }
            }
        });
    }

    public void createMatch(String name, boolean autoMatch, int autoMatchSlots, int blackCardType, int whiteCardType, int inactiveMode, int autoPickSkipTimeout, int excludeHoursBegin, int excludeHoursEnd, List<String> decks, List<User> invitees)
    {
        User user = User.get(getContext());

        if (user != null)
        {
            List<String> inviteeIds = new ArrayList<String>();
            List<String> playerIds = new ArrayList<String>();
            List<String> names = new ArrayList<String>();
            List<String> pictureUrls = new ArrayList<String>();

            playerIds.add(user.getId());
            names.add(user.getDisplayName());

            if (user.hideFacebookPicture())
                pictureUrls.add("");
            else
                pictureUrls.add(user.getPictureUrl());

            for (User invitee : invitees)
            {
                inviteeIds.add(invitee.getId());
                playerIds.add(invitee.getId());
                names.add(invitee.getDisplayName());
                pictureUrls.add(invitee.getPictureUrl());
            }

            RatedMServer.createMatch(name, GameConstants.VERSION, autoMatch, autoMatchSlots, blackCardType, whiteCardType, inactiveMode, autoPickSkipTimeout, excludeHoursBegin, excludeHoursEnd, decks, inviteeIds, playerIds, names, pictureUrls, new RatedMServer.CallbackHandler()
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
                                mCallbackHandler.onCreateMatchSuccess(match);
                        }
                        catch (JSONException e)
                        {
                            ToggleLog.e(TAG, "Unable to create match. JSONException: " + e.getMessage());

                            if (mCallbackHandler != null)
                                mCallbackHandler.onCreateMatchFailure(getStringSafe(R.string.try_again_later));
                        }
                    }
                    else
                    {
                        ToggleLog.e(TAG, "Unable to create match. Response code: " + responseCode);

                        String message = getStringSafe(R.string.try_again_later);

                        if (responseCode == 230)
                            message = getStringSafe(R.string.match_creation_limit_reached);

                        if (mCallbackHandler != null)
                            mCallbackHandler.onCreateMatchFailure(message);
                    }
                }
            });
        }
        else
        {
            ToggleLog.e(TAG, "Unable to create match. User is null.");

            if (mCallbackHandler != null)
                mCallbackHandler.onCreateMatchFailure(getStringSafe(R.string.sign_in_again));
        }
    }

    public void updateMatchSettings(String matchId, String name, int inactiveMode, int autoPickSkipTimeout, int excludeHoursBegin, int excludeHoursEnd)
    {
        RatedMServer.updateMatchSettings(matchId, name, inactiveMode, autoPickSkipTimeout, excludeHoursBegin, excludeHoursEnd, new RatedMServer.CallbackHandler()
        {
            @Override
            public void onResponse(int responseCode, String response)
            {
                if (responseCode == 200)
                {
                    if (mCallbackHandler != null)
                        mCallbackHandler.onUpdateMatchSettingsSuccess();
                }
                else
                {
                    ToggleLog.e(TAG, "Unable to update match settings. Response code: " + responseCode);

                    if (mCallbackHandler != null)
                        mCallbackHandler.onUpdateMatchSettingsFailure(getStringSafe(R.string.try_again_later));
                }
            }
        });
    }

    public void invite(String matchId, int autoMatchSlots, List<String> inviteeIds)
    {
        RatedMServer.invite(matchId, autoMatchSlots, inviteeIds, new RatedMServer.CallbackHandler()
        {
            @Override
            public void onResponse(int responseCode, String response)
            {
                if (responseCode == 200)
                {
                    if (mCallbackHandler != null)
                        mCallbackHandler.onInviteSuccess();
                }
                else if (responseCode == 413)
                {
                    ToggleLog.e(TAG, "Unable to invite. Too many invitees.");

                    if (mCallbackHandler != null)
                        mCallbackHandler.onInviteFailure(getStringSafe(R.string.too_many_invitees));
                }
                else
                {
                    ToggleLog.e(TAG, "Unable to invite. Response code: " + responseCode);

                    if (mCallbackHandler != null)
                        mCallbackHandler.onInviteFailure(getStringSafe(R.string.try_again_later));
                }
            }
        });
    }

    public void kick(String matchId, int autoMatchRemovals, List<String> kickIds)
    {
        RatedMServer.kick(matchId, autoMatchRemovals, kickIds, new RatedMServer.CallbackHandler()
        {
            @Override
            public void onResponse(int responseCode, String response)
            {
                if (responseCode == 200)
                {
                    if (mCallbackHandler != null)
                        mCallbackHandler.onKickSuccess();
                }
                else
                {
                    ToggleLog.e(TAG, "Unable to kick. Response code: " + responseCode);

                    if (mCallbackHandler != null)
                        mCallbackHandler.onKickFailure(getStringSafe(R.string.try_again_later));
                }
            }
        });
    }

    public interface CallbackHandler
    {
        public void onFindPlayersSuccess(List<User> players, int searchId);
        public void onFindPlayersFailure(int searchId);
        public void onRetrieveFriendsSuccess(List<User> friends);
        public void onRetrieveFriendsFailure();
        public void onRetrieveAttendeesSuccess(List<User> attendees);
        public void onRetrieveAttendeesFailure();
        public void onCreateMatchSuccess(Match match);
        public void onCreateMatchFailure(String message);
        public void onUpdateMatchSettingsSuccess();
        public void onUpdateMatchSettingsFailure(String message);
        public void onInviteSuccess();
        public void onInviteFailure(String message);
        public void onKickSuccess();
        public void onKickFailure(String message);
    }
}