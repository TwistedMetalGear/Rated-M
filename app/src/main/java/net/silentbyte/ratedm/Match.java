package net.silentbyte.ratedm;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Match implements Parcelable
{
    public static final String KEY_MATCH = "match";
    public static final String KEY_MATCH_ID = "match_id";
    public static final String JSON_ID = "_id";
    public static final String JSON_NAME = "name";
    public static final String JSON_CREATOR_ID = "creator_id";
    public static final String JSON_STATE = "state";
    public static final String JSON_VERSION = "version";
    public static final String JSON_AUTO_MATCH_SLOTS = "auto_match_slots";
    public static final String JSON_BLACK_CARD_TYPE = "black_card_type";
    public static final String JSON_WHITE_CARD_TYPE = "white_card_type";
    public static final String JSON_INACTIVE_MODE = "inactive_mode";
    public static final String JSON_AUTO_PICK_SKIP_TIMEOUT = "auto_pick_skip_timeout";
    public static final String JSON_EXCLUDE_HOURS_BEGIN = "exclude_hours_begin";
    public static final String JSON_EXCLUDE_HOURS_END = "exclude_hours_end";
    public static final String JSON_ROUND = "round";
    public static final String JSON_STATE_START_TIME = "state_start_time";
    public static final String JSON_UPDATED_AT = "updated_at";
    public static final String JSON_REMATCHED = "rematched";
    public static final String JSON_DECKS = "decks";
    public static final String JSON_PENDING_JOIN_IDS = "pending_join_ids";
    public static final String JSON_PENDING_LEAVE_IDS = "pending_leave_ids";
    public static final String JSON_PENDING_INVITEE_IDS = "pending_invitee_ids";
    public static final String JSON_INVITEE_IDS = "invitee_ids";
    public static final String JSON_PLAYER_IDS = "player_ids";
    public static final String JSON_NAMES = "names";
    public static final String JSON_PICTURE_URLS = "picture_urls";
    public static final String JSON_DEALT_CARDS = "dealt_cards";
    public static final String JSON_ROUNDS = "rounds";
    public static final String JSON_LAST_CHAT_MESSAGE_ID = "last_chat_message_id";

    public static final int BLACK_CARD_TYPE_STANDARD = 0;
    public static final int BLACK_CARD_TYPE_CUSTOM = 1;

    public static final int WHITE_CARD_TYPE_STANDARD = 0;
    public static final int WHITE_CARD_TYPE_CUSTOM = 1;

    public static final int INACTIVE_MODE_AUTO_PICK = 0;
    public static final int INACTIVE_MODE_SKIP = 1;
    public static final int INACTIVE_MODE_WAIT = 2;

    public static final int MATCH_STATE_PICKING = 0;
    public static final int MATCH_STATE_JUDGING = 1;
    public static final int MATCH_STATE_COMPLETE = 2;
    public static final int MATCH_STATE_CANCELED = 3;
    public static final int MATCH_STATE_EXPIRED = 4;
    public static final int MATCH_STATE_WRITING_CARD = 5;

    private String mId;
    private String mName;
    private String mCreatorId;
    private int mState;
    private int mVersion;
    private int mAutoMatchSlots;
    private int mBlackCardType;
    private int mWhiteCardType;
    private int mInactiveMode;
    private int mAutoPickSkipTimeout;
    private int mExcludeHoursBegin;
    private int mExcludeHoursEnd;
    private int mRound;
    private long mStateStartTime;
    private long mUpdatedAt;
    private boolean mRematched;
    private List<String> mDecks;
    private List<String> mPendingJoinIds;
    private List<String> mPendingLeaveIds;
    private List<String> mPendingInviteeIds;
    private List<String> mInviteeIds;
    private List<String> mPlayerIds;
    private List<String> mNames;
    private List<String> mPictureUrls;
    private Map<String, List<Card>> mDealtCards;
    private List<Round> mRounds;
    private String mLastChatMessageId;
    private String mPictureUrl;
    private String mStateText;
    private String mDividerText;

    public Match(JSONObject match) throws JSONException
    {
        fromJSON(match);
    }

    public Match(String dividerText)
    {
        mDividerText = dividerText;
    }

    public Match(Parcel in)
    {
        if (mDividerText == null)
        {
            mId = in.readString();
            mName = in.readString();
            mCreatorId = in.readString();
            mState = in.readInt();
            mVersion = in.readInt();
            mAutoMatchSlots = in.readInt();
            mBlackCardType = in.readInt();
            mWhiteCardType = in.readInt();
            mInactiveMode = in.readInt();
            mAutoPickSkipTimeout = in.readInt();
            mExcludeHoursBegin = in.readInt();
            mExcludeHoursEnd = in.readInt();
            mRound = in.readInt();
            mStateStartTime = in.readLong();
            mUpdatedAt = in.readLong();
            mRematched = in.readByte() != 0;
            mDecks = new ArrayList<String>();
            mPendingJoinIds = new ArrayList<String>();
            mPendingLeaveIds = new ArrayList<String>();
            mPendingInviteeIds = new ArrayList<String>();
            mInviteeIds = new ArrayList<String>();
            mPlayerIds = new ArrayList<String>();
            mNames = new ArrayList<String>();
            mPictureUrls = new ArrayList<String>();
            in.readStringList(mDecks);
            in.readStringList(mPendingJoinIds);
            in.readStringList(mPendingLeaveIds);
            in.readStringList(mPendingInviteeIds);
            in.readStringList(mInviteeIds);
            in.readStringList(mPlayerIds);
            in.readStringList(mNames);
            in.readStringList(mPictureUrls);
            mDealtCards = new HashMap<String, List<Card>>();
            readCardMap(mDealtCards, in);
            mRounds = in.createTypedArrayList(Round.CREATOR);
            mLastChatMessageId = in.readString();

            if (in.readByte() != 0)
                mPictureUrl = in.readString();

            if (in.readByte() != 0)
                mStateText = in.readString();
        }
        else
            mDividerText = in.readString();
    }

    private void fromJSON(JSONObject match) throws JSONException
    {
        mId = match.getString(JSON_ID);
        mName = match.getString(JSON_NAME);
        mCreatorId = match.getString(JSON_CREATOR_ID);
        mState = match.getInt(JSON_STATE);
        mVersion = match.getInt(JSON_VERSION);
        mAutoMatchSlots = match.getInt(JSON_AUTO_MATCH_SLOTS);
        mBlackCardType = match.getInt(JSON_BLACK_CARD_TYPE);
        mWhiteCardType = match.getInt(JSON_WHITE_CARD_TYPE);
        mInactiveMode = match.getInt(JSON_INACTIVE_MODE);
        mAutoPickSkipTimeout = match.getInt(JSON_AUTO_PICK_SKIP_TIMEOUT);
        mExcludeHoursBegin = match.getInt(JSON_EXCLUDE_HOURS_BEGIN);
        mExcludeHoursEnd = match.getInt(JSON_EXCLUDE_HOURS_END);
        mRound = match.getInt(JSON_ROUND);
        mStateStartTime = match.getLong(JSON_STATE_START_TIME);
        mUpdatedAt = match.getLong(JSON_UPDATED_AT);
        mRematched = match.getBoolean(JSON_REMATCHED);
        mDecks = jsonToStringList(match.getJSONArray(JSON_DECKS));
        mPendingJoinIds = jsonToStringList(match.getJSONArray(JSON_PENDING_JOIN_IDS));
        mPendingLeaveIds = jsonToStringList(match.getJSONArray(JSON_PENDING_LEAVE_IDS));
        mPendingInviteeIds = jsonToStringList(match.getJSONArray(JSON_PENDING_INVITEE_IDS));
        mInviteeIds = jsonToStringList(match.getJSONArray(JSON_INVITEE_IDS));
        mPlayerIds = jsonToStringList(match.getJSONArray(JSON_PLAYER_IDS));
        mNames = jsonToStringList(match.getJSONArray(JSON_NAMES));
        mPictureUrls = jsonToStringList(match.getJSONArray(JSON_PICTURE_URLS));
        mDealtCards = dealtCardsToMap(match.getJSONObject(JSON_DEALT_CARDS));
        mRounds = roundsToList(match.getJSONArray(JSON_ROUNDS));
        mLastChatMessageId = match.getString(JSON_LAST_CHAT_MESSAGE_ID);
    }

    private List<String> jsonToStringList(JSONArray jsonArray) throws JSONException
    {
        List<String> list = new ArrayList<String>();

        for (int i = 0; i < jsonArray.length(); i++)
        {
            list.add(jsonArray.getString(i));
        }

        return list;
    }

    private Map dealtCardsToMap(JSONObject jsonCardMap) throws JSONException
    {
        HashMap<String, List<Card>> cardMap = new HashMap<String, List<Card>>();
        addCardsToMap(cardMap, jsonCardMap);
        return cardMap;
    }

    private void addCardsToMap(Map cardMap, JSONObject jsonCardMap) throws JSONException
    {
        Iterator it = jsonCardMap.keys();

        while (it.hasNext())
        {
            String playerId = (String)it.next();
            JSONArray jsonCards = jsonCardMap.getJSONArray(playerId);
            List<Card> cards = new ArrayList<Card>();

            for (int i = 0; i < jsonCards.length(); i++)
            {
                JSONObject jsonCard = jsonCards.getJSONObject(i);
                cards.add(new Card(jsonCard));
            }

            cardMap.put(playerId, cards);
        }
    }

    private void readCardMap(Map cardMap, Parcel in)
    {
        int size = in.readInt();

        for (int i = 0; i < size; i++)
        {
            String key = in.readString();
            List<Card> cards = new ArrayList<Card>();
            in.readList(cards, Card.class.getClassLoader());
            cardMap.put(key, cards);
        }
    }

    private void writeCardMap(Map<String, List<Card>> cardMap, Parcel dest)
    {
        dest.writeInt(cardMap.size());

        for (String key : cardMap.keySet())
        {
            dest.writeString(key);
            dest.writeList(cardMap.get(key));
        }
    }

    private List<Round> roundsToList(JSONArray jsonRounds) throws JSONException
    {
        List<Round> rounds = new ArrayList<Round>();

        for (int i = 0; i < jsonRounds.length(); i++)
        {
            JSONObject jsonRound = jsonRounds.getJSONObject(i);
            Round round = new Round(jsonRound);
            rounds.add(round);
        }

        return rounds;
    }

    public String getId()
    {
        return mId;
    }

    public String getName()
    {
        return mName;
    }

    public String getCreatorId()
    {
        return mCreatorId;
    }

    public int getState()
    {
        return mState;
    }

    public int getVersion()
    {
        return mVersion;
    }

    public int getAutoMatchSlots()
    {
        return mAutoMatchSlots;
    }

    public int getBlackCardType()
    {
        return mBlackCardType;
    }

    public int getWhiteCardType()
    {
        return mWhiteCardType;
    }

    public int getInactiveMode()
    {
        return mInactiveMode;
    }

    public int getAutoPickSkipTimeout()
    {
        return mAutoPickSkipTimeout;
    }

    public int getExcludeHoursBegin()
    {
        return mExcludeHoursBegin;
    }

    public int getExcludeHoursEnd()
    {
        return mExcludeHoursEnd;
    }

    public int getRound()
    {
        return mRound;
    }

    public long getStateStartTime()
    {
        return mStateStartTime;
    }

    public long getUpdatedAt()
    {
        return mUpdatedAt;
    }

    public boolean hasRematched()
    {
        return mRematched;
    }

    public List<String> getDecks()
    {
        return mDecks;
    }

    public List<String> getPendingJoinIds()
    {
        return mPendingJoinIds;
    }

    public List<String> getPendingLeaveIds()
    {
        return mPendingLeaveIds;
    }

    public List<String> getPendingInviteeIds()
    {
        return mPendingInviteeIds;
    }

    public List<String> getInviteeIds()
    {
        return mInviteeIds;
    }

    public List<String> getPlayerIds()
    {
        return mPlayerIds;
    }

    public List<String> getNames()
    {
        return mNames;
    }

    public List<String> getPictureUrls()
    {
        return mPictureUrls;
    }

    public Map<String, List<Card>> getDealtCards()
    {
        return mDealtCards;
    }

    public List<Round> getRounds()
    {
        return mRounds;
    }

    public Round getRound(int round)
    {
        if (round - 1 >= mRounds.size())
            return null;

        return mRounds.get(round - 1);
    }

    public Round getCurrentRound()
    {
        return mRounds.get(mRounds.size() - 1);
    }

    public String getLastChatMessageId()
    {
        return mLastChatMessageId;
    }

    public String getPictureUrl()
    {
        return mPictureUrl;
    }

    public void setPictureUrl(String url)
    {
        mPictureUrl = url;
    }

    public String getStateText()
    {
        return mStateText;
    }

    public void setStateText(String stateText)
    {
        mStateText = stateText;
    }

    public boolean isDivider()
    {
        return mDividerText != null;
    }

    public String getDividerText()
    {
        return mDividerText;
    }

    public void setDividerText(String dividerText)
    {
        mDividerText = dividerText;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        if (mDividerText == null)
        {
            dest.writeString(mId);
            dest.writeString(mName);
            dest.writeString(mCreatorId);
            dest.writeInt(mState);
            dest.writeInt(mVersion);
            dest.writeInt(mAutoMatchSlots);
            dest.writeInt(mBlackCardType);
            dest.writeInt(mWhiteCardType);
            dest.writeInt(mInactiveMode);
            dest.writeInt(mAutoPickSkipTimeout);
            dest.writeInt(mExcludeHoursBegin);
            dest.writeInt(mExcludeHoursEnd);
            dest.writeInt(mRound);
            dest.writeLong(mStateStartTime);
            dest.writeLong(mUpdatedAt);
            dest.writeByte((byte) (mRematched ? 1 : 0));
            dest.writeStringList(mDecks);
            dest.writeStringList(mPendingJoinIds);
            dest.writeStringList(mPendingLeaveIds);
            dest.writeStringList(mPendingInviteeIds);
            dest.writeStringList(mInviteeIds);
            dest.writeStringList(mPlayerIds);
            dest.writeStringList(mNames);
            dest.writeStringList(mPictureUrls);
            writeCardMap(mDealtCards, dest);

            Round[] rounds = new Round[mRounds.size()];
            rounds = mRounds.toArray(rounds);
            dest.writeTypedArray(rounds, 0);

            dest.writeString(mLastChatMessageId);

            dest.writeByte((byte) (mPictureUrl != null ? 1 : 0));

            if (mPictureUrl != null)
                dest.writeString(mPictureUrl);

            dest.writeByte((byte) (mStateText != null ? 1 : 0));

            if (mStateText != null)
                dest.writeString(mStateText);
        }
        else
            dest.writeString(mDividerText);
    }

    public static final Creator CREATOR = new Creator()
    {
        public Match createFromParcel(Parcel in)
        {
            return new Match(in);
        }

        public Match[] newArray(int size)
        {
            return new Match[size];
        }
    };
}
