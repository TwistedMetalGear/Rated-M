package net.silentbyte.ratedm.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import net.silentbyte.ratedm.Card;
import net.silentbyte.ratedm.ChatMessage;
import net.silentbyte.ratedm.Match;
import net.silentbyte.ratedm.R;
import net.silentbyte.ratedm.RatedMServer;
import net.silentbyte.ratedm.ToggleLog;
import net.silentbyte.ratedm.WinningCardPair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GameFragment extends Fragment
{
    private static final String TAG = "GameFragment";

    private CallbackHandler mCallbackHandler;
    private Card mFirstSubmittedCard;
    private int mFirstSubmittedCardPos = -1;
    private int mLoadMatchCount = 0;
    private boolean mNewInstance = true;
    private boolean mBusy = false;

    // We set this upon submit or toss success/failure, prior to loading the match.
    // This prevents the controls from being re-enabled before the updated match is loaded.
    private boolean mRetrievingUpdates = false;

    // We set this to true upon toss success. After a successful toss, we will load the match
    // and eventually call updateGameData which calls setCardPagerAdapter. In setCardPagerAdapter,
    // we will check the value of mTossed, and if true, keep the currently selected card position
    // the same. This prevents jumping back to the first card upon tossing.
    private boolean mTossed = false;

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

    public Card getFirstSubmittedCard()
    {
        return mFirstSubmittedCard;
    }

    public void setFirstSubmittedCard(Card firstSubmittedCard)
    {
        if (firstSubmittedCard == null)
            mFirstSubmittedCard = null;
        else
            mFirstSubmittedCard = new Card(firstSubmittedCard);
    }

    public int getFirstSubmittedCardPosition()
    {
        return mFirstSubmittedCardPos;
    }

    public void setFirstSubmittedCardPosition(int position)
    {
        mFirstSubmittedCardPos = position;
    }

    public int getLoadMatchCount()
    {
        return mLoadMatchCount;
    }

    public void incrementLoadMatchCount()
    {
        mLoadMatchCount++;
    }

    public boolean isNewInstance()
    {
        return mNewInstance;
    }

    public void setNewInstance(boolean newInstance)
    {
        mNewInstance = newInstance;
    }

    public boolean isBusy()
    {
        return mBusy;
    }

    public boolean isRetrievingUpdates()
    {
        return mRetrievingUpdates;
    }

    public void setRetrievingUpdates(boolean retrievingUpdates)
    {
        mRetrievingUpdates = retrievingUpdates;
    }

    public boolean hasTossed()
    {
        return mTossed;
    }

    public void setTossed(boolean tossed)
    {
        mTossed = tossed;
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

    public void loadMatch(String matchId, String playerId)
    {
        ToggleLog.d(TAG, "Loading match: " + matchId);

        // This call takes care of two things.
        // 1) It joins the match (removes playerId from inviteeIds) if necessary.
        // 2) It loads the match.
        RatedMServer.joinMatch(matchId, playerId, new RatedMServer.CallbackHandler()
        {
            @Override
            public void onResponse(int responseCode, String response)
            {
                mLoadMatchCount--;

                if (responseCode == 200)
                {
                    try
                    {
                        JSONObject jsonMatch = new JSONObject(response);
                        Match match = new Match(jsonMatch);

                        if (mCallbackHandler != null)
                            mCallbackHandler.onLoadMatchSuccess(match);
                    }
                    catch (JSONException e)
                    {
                        ToggleLog.e(TAG, "Unable to load match. JSONException: " + e.getMessage());

                        if (mCallbackHandler != null)
                            mCallbackHandler.onLoadMatchFailure();
                    }
                }
                else
                {
                    ToggleLog.e(TAG, "Unable to load match. Response code: " + responseCode);

                    if (mCallbackHandler != null)
                        mCallbackHandler.onLoadMatchFailure();
                }
            }
        });
    }

    public void submit(String matchId, String playerId, int round, int bet, List<Card> cards)
    {
        if (!mBusy)
        {
            mBusy = true;

            RatedMServer.updateMatch(matchId, playerId, round, bet, cards, new RatedMServer.CallbackHandler()
            {
                @Override
                public void onResponse(int responseCode, String response)
                {
                    mBusy = false;

                    if (responseCode == 200)
                    {
                        if (mCallbackHandler != null)
                            mCallbackHandler.onSubmitSuccess();
                    }
                    else if (responseCode == 226)
                    {
                        if (mCallbackHandler != null)
                            mCallbackHandler.onSubmitFailure(getStringSafe(R.string.already_submitted));
                    }
                    else
                    {
                        ToggleLog.e(TAG, "Unable to submit. Response code: " + responseCode);

                        if (mCallbackHandler != null)
                            mCallbackHandler.onSubmitFailure(getStringSafe(R.string.submit_failed));
                    }
                }
            });
        }
    }

    public void submitBlackCard(String matchId, String playerId, int round, Card blackCard)
    {
        if (!mBusy)
        {
            mBusy = true;

            RatedMServer.updateBlackCard(matchId, playerId, round, blackCard, new RatedMServer.CallbackHandler()
            {
                @Override
                public void onResponse(int responseCode, String response)
                {
                    mBusy = false;

                    if (responseCode == 200)
                    {
                        if (mCallbackHandler != null)
                            mCallbackHandler.onSubmitBlackCardSuccess();
                    }
                    else if (responseCode == 226)
                    {
                        if (mCallbackHandler != null)
                            mCallbackHandler.onSubmitBlackCardFailure(getStringSafe(R.string.already_submitted));
                    }
                    else
                    {
                        ToggleLog.e(TAG, "Unable to submit black card. Response code: " + responseCode);

                        if (mCallbackHandler != null)
                            mCallbackHandler.onSubmitBlackCardFailure(getStringSafe(R.string.submit_failed));
                    }
                }
            });
        }
    }

    public void toss(String matchId, String playerId, int round, List<Card> cards)
    {
        if (!mBusy)
        {
            mBusy = true;

            RatedMServer.tossCard(matchId, playerId, round, cards, new RatedMServer.CallbackHandler()
            {
                @Override
                public void onResponse(int responseCode, String response)
                {
                    mBusy = false;

                    if (responseCode == 200)
                    {
                        if (mCallbackHandler != null)
                            mCallbackHandler.onTossSuccess();
                    }
                    else if (responseCode == 226)
                    {
                        if (mCallbackHandler != null)
                            mCallbackHandler.onTossFailure(getStringSafe(R.string.already_tossed));
                    }
                    else if (responseCode == 227)
                    {
                        if (mCallbackHandler != null)
                            mCallbackHandler.onTossFailure(getStringSafe(R.string.already_tossed));
                    }
                    else
                    {
                        ToggleLog.e(TAG, "Unable to toss card. Response code: " + responseCode);

                        if (mCallbackHandler != null)
                            mCallbackHandler.onTossFailure(getStringSafe(R.string.toss_failed));
                    }
                }
            });
        }
    }

    public void react(String matchId, String playerId, String cardId, int reaction, int round)
    {
        if (!mBusy)
        {
            mBusy = true;

            RatedMServer.react(matchId, playerId, cardId, reaction, round, new RatedMServer.CallbackHandler()
            {
                @Override
                public void onResponse(int responseCode, String response)
                {
                    mBusy = false;

                    if (responseCode == 200)
                    {
                        if (mCallbackHandler != null)
                            mCallbackHandler.onReactSuccess();
                    }
                    else
                    {
                        ToggleLog.e(TAG, "Unable to react. Response code: " + responseCode);

                        if (mCallbackHandler != null)
                            mCallbackHandler.onReactFailure();
                    }
                }
            });
        }
    }

    public void forceAutoPickSkip(String matchId)
    {
        if (!mBusy)
        {
            mBusy = true;

            RatedMServer.forceAutoPickSkip(matchId, new RatedMServer.CallbackHandler()
            {
                @Override
                public void onResponse(int responseCode, String response)
                {
                    mBusy = false;

                    if (responseCode == 200)
                    {
                        if (mCallbackHandler != null)
                            mCallbackHandler.onForceAutoPickSkipSuccess();
                    }
                    else
                    {
                        ToggleLog.e(TAG, "Unable to force auto pick skip. Response code: " + responseCode);

                        if (mCallbackHandler != null)
                            mCallbackHandler.onForceAutoPickSkipFailure();
                    }
                }
            });
        }
    }

    public void leaveMatch(String matchId, String playerId)
    {
        if (!mBusy)
        {
            mBusy = true;

            RatedMServer.leaveMatch(matchId, playerId, new RatedMServer.CallbackHandler()
            {
                @Override
                public void onResponse(int responseCode, String response)
                {
                    mBusy = false;

                    if (responseCode == 200)
                    {
                        if (mCallbackHandler != null)
                            mCallbackHandler.onLeaveMatchSuccess();
                    }
                    else
                    {
                        ToggleLog.e(TAG, "Unable to leave match. Response code: " + responseCode);

                        if (mCallbackHandler != null)
                            mCallbackHandler.onLeaveMatchFailure();
                    }
                }
            });
        }
    }

    public void rematch(String matchId, String playerId)
    {
        if (!mBusy)
        {
            mBusy = true;

            RatedMServer.rematch(matchId, playerId, new RatedMServer.CallbackHandler()
            {
                @Override
                public void onResponse(int responseCode, String response)
                {
                    mBusy = false;

                    if (responseCode == 200)
                    {
                        try
                        {
                            JSONObject jsonMatch = new JSONObject(response);
                            Match match = new Match(jsonMatch);

                            if (mCallbackHandler != null)
                                mCallbackHandler.onRematchSuccess(match);
                        }
                        catch (JSONException e)
                        {
                            ToggleLog.e(TAG, "Unable to rematch. JSONException: " + e.getMessage());

                            if (mCallbackHandler != null)
                                mCallbackHandler.onRematchFailure();
                        }
                    }
                    else if (responseCode == 228)
                    {
                        ToggleLog.d(TAG, "Match has already been rematched.");

                        if (mCallbackHandler != null)
                            mCallbackHandler.onRematchSuccess(null);
                    }
                    else
                    {
                        ToggleLog.e(TAG, "Unable to rematch. Response code: " + responseCode);

                        if (mCallbackHandler != null)
                            mCallbackHandler.onRematchFailure();
                    }
                }
            });
        }
    }

    public void retrieveChatMessages(String matchId)
    {
        RatedMServer.getChatMessages(matchId, 0, new RatedMServer.CallbackHandler()
        {
            @Override
            public void onResponse(int responseCode, String response)
            {
                if (responseCode == 200)
                {
                    try
                    {
                        List<ChatMessage> chatMessages = convertToChatMessages(response);
                        formatChatMessages(chatMessages);

                        if (mCallbackHandler != null)
                            mCallbackHandler.onRetrieveChatMessagesSuccess(chatMessages);
                    }
                    catch (JSONException e)
                    {
                        ToggleLog.e(TAG, "Unable to retrieve chat messages. JSONException: " + e.getMessage());

                        if (mCallbackHandler != null)
                            mCallbackHandler.onRetrieveChatMessagesFailure();
                    }
                }
                else
                {
                    ToggleLog.e(TAG, "Unable to retrieve chat messages. Response code: " + responseCode);

                    if (mCallbackHandler != null)
                        mCallbackHandler.onRetrieveChatMessagesFailure();
                }
            }
        });
    }

    public void retrieveLastChatMessage(String matchId)
    {
        RatedMServer.getChatMessages(matchId, 1, new RatedMServer.CallbackHandler()
        {
            @Override
            public void onResponse(int responseCode, String response)
            {
                if (responseCode == 200)
                {
                    try
                    {
                        JSONArray jsonChatMessages = new JSONArray(response);
                        ChatMessage chatMessage = null;

                        if (jsonChatMessages.length() == 1)
                        {
                            JSONObject jsonChatMessage = jsonChatMessages.getJSONObject(0);
                            chatMessage = new ChatMessage(jsonChatMessage);
                        }

                        if (mCallbackHandler != null)
                            mCallbackHandler.onRetrieveLastChatMessageSuccess(chatMessage);
                    }
                    catch (JSONException e)
                    {
                        ToggleLog.e(TAG, "Unable to retrieve last chat message. JSONException: " + e.getMessage());

                        if (mCallbackHandler != null)
                            mCallbackHandler.onRetrieveLastChatMessageFailure();
                    }
                }
                else
                {
                    ToggleLog.e(TAG, "Unable to retrieve last chat message. Response code: " + responseCode);

                    if (mCallbackHandler != null)
                        mCallbackHandler.onRetrieveLastChatMessageFailure();
                }
            }
        });
    }

    public void sendChatMessage(String matchId, String playerId, String name, final String message)
    {
        RatedMServer.createChatMessage(matchId, playerId, name, message, new RatedMServer.CallbackHandler()
        {
            @Override
            public void onResponse(int responseCode, String response)
            {
                if (responseCode == 200)
                {
                    if (mCallbackHandler != null)
                        mCallbackHandler.onSendChatMessageSuccess();
                }
                else
                {
                    ToggleLog.e(TAG, "Unable to send chat message. Response code: " + responseCode);

                    if (mCallbackHandler != null)
                        mCallbackHandler.onSendChatMessageFailure(message);
                }
            }
        });
    }

    private List<ChatMessage> convertToChatMessages(String json) throws JSONException
    {
        JSONArray jsonChatMessages = new JSONArray(json);
        List<ChatMessage> chatMessages = new ArrayList<ChatMessage>();

        for (int i = 0; i < jsonChatMessages.length(); i++)
        {
            JSONObject jsonChatMessage = jsonChatMessages.getJSONObject(i);
            chatMessages.add(new ChatMessage(jsonChatMessage));
        }

        return chatMessages;
    }

    private void formatChatMessages(List<ChatMessage> chatMessages)
    {
        ChatMessage currentChatMessage;
        ChatMessage previousChatMessage;
        String currentMonthDay;
        String previousMonthDay = null;

        for (int i = 0; i < chatMessages.size(); i++)
        {
            currentChatMessage = chatMessages.get(i);
            currentMonthDay = currentChatMessage.getMonthDay();

            if (i - 1 >= 0)
            {
                previousChatMessage = chatMessages.get(i - 1);
                previousMonthDay = previousChatMessage.getMonthDay();
            }

            if (!currentMonthDay.equals(previousMonthDay))
            {
                chatMessages.add(i++, new ChatMessage(currentMonthDay));
            }
        }
    }

    public interface CallbackHandler
    {
        public void onLoadMatchSuccess(Match match);
        public void onLoadMatchFailure();

        public void onSubmitSuccess();
        public void onSubmitFailure(String message);

        public void onSubmitBlackCardSuccess();
        public void onSubmitBlackCardFailure(String message);

        public void onTossSuccess();
        public void onTossFailure(String message);

        public void onReactSuccess();
        public void onReactFailure();

        public void onForceAutoPickSkipSuccess();
        public void onForceAutoPickSkipFailure();

        public void onLeaveMatchSuccess();
        public void onLeaveMatchFailure();

        public void onRematchSuccess(Match match);
        public void onRematchFailure();

        public void onRetrieveChatMessagesSuccess(List<ChatMessage> chatMessages);
        public void onRetrieveChatMessagesFailure();

        public void onSendChatMessageSuccess();
        public void onSendChatMessageFailure(String message);

        public void onRetrieveLastChatMessageSuccess(ChatMessage lastChatMessage);
        public void onRetrieveLastChatMessageFailure();
    }
}
