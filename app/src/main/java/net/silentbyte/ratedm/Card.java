package net.silentbyte.ratedm;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Card implements Parcelable
{
    public static final String JSON_ID = "_id";
    public static final String JSON_DECK = "deck";
    public static final String JSON_TEXT = "text";
    public static final String JSON_REACTION = "reaction";
    public static final int CARD_TYPE_PICK_ONE = 0;
    public static final int CARD_TYPE_PICK_TWO = 1;
    public static final int CARD_TYPE_QUESTION = 2;
    public static final int CARD_TYPE_BLANK = 3;
    public static final int CARD_TYPE_PICTURE = 4;

    private String mId;
    private String mDeck;
    private String mText;
    private int mReaction;

    public Card(JSONObject card) throws JSONException
    {
        fromJSON(card);
    }

    public Card(Card card)
    {
        mId = card.getId();
        mDeck = card.getDeck();
        mText = card.getText();
        mReaction = card.getReaction();
    }

    public Card(Parcel in)
    {
        mId = in.readString();
        mDeck = in.readString();
        mText = in.readString();
        mReaction = in.readInt();
    }

    public String getId()
    {
        return mId;
    }

    public void setId(String id)
    {
        mId = id;
    }

    public String getDeck()
    {
        return mDeck;
    }

    public String getText()
    {
        return mText;
    }

    public void setText(String text)
    {
        mText = text;
    }

    public int getReaction()
    {
        return mReaction;
    }

    public void setReaction(int reaction)
    {
        mReaction = reaction;
    }

    public int getType()
    {
        if (mId.startsWith("blank_"))
            return CARD_TYPE_BLANK;

        if (mId.startsWith("picture_"))
            return CARD_TYPE_PICTURE;

        String regEx = "_{3,}";
        Pattern pattern = Pattern.compile(regEx);

        Matcher matcher = pattern.matcher(mText);
        int matches = 0;

        while (matcher.find())
            matches++;

        if (matches == 0)
            return CARD_TYPE_QUESTION;
        else if (matches == 1)
            return CARD_TYPE_PICK_ONE;
        else
            return CARD_TYPE_PICK_TWO;
    }

    public boolean isEmpty()
    {
        return mText.trim().isEmpty();
    }

    public JSONObject toJSON() throws JSONException
    {
        JSONObject jsonCard = new JSONObject();

        jsonCard.put(JSON_ID, mId);
        jsonCard.put(JSON_DECK, mDeck);
        jsonCard.put(JSON_TEXT, mText);
        jsonCard.put(JSON_REACTION, mReaction);

        return jsonCard;
    }

    public void fromJSON(JSONObject card) throws JSONException
    {
        mId = card.getString(JSON_ID);
        mDeck = card.getString(JSON_DECK);
        mText = card.getString(JSON_TEXT);
        mReaction = card.getInt(JSON_REACTION);
    }

    @Override
    public boolean equals(Object object)
    {
        if (object != null && object instanceof Card)
        {
            Card card = (Card)object;
            return card.getId().equals(mId);
        }

        return false;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mId);
        dest.writeString(mDeck);
        dest.writeString(mText);
        dest.writeInt(mReaction);
    }

    public static final Creator CREATOR = new Creator()
    {
        public Card createFromParcel(Parcel in)
        {
            return new Card(in);
        }

        public Card[] newArray(int size)
        {
            return new Card[size];
        }
    };
}
