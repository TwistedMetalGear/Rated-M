package net.silentbyte.ratedm;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WinningCardPair implements Parcelable
{
    private static final String JSON_BLACK_CARD_TEXT = "black_card_text";
    private static final String JSON_WHITE_CARD_TEXT = "white_card_text";

    private String mBlackCardText;
    private String mWhiteCardText;

    public WinningCardPair(String blackCardText, String whiteCardText)
    {
        mBlackCardText = blackCardText;
        mWhiteCardText = whiteCardText;
    }

    public WinningCardPair(JSONObject winningCardPair) throws JSONException
    {
        fromJSON(winningCardPair);
    }

    public WinningCardPair(Parcel in)
    {
        mBlackCardText = in.readString();
        mWhiteCardText = in.readString();
    }

    public String getBlackCardText()
    {
        return mBlackCardText;
    }

    public void setBlackCardText(String blackCardText)
    {
        mBlackCardText = blackCardText;
    }

    public String getWhiteCardText()
    {
        return mWhiteCardText;
    }

    public void setWhiteCardText(String whiteCardText)
    {
        mWhiteCardText = whiteCardText;
    }

    public JSONArray toJSON() throws JSONException
    {
        JSONArray jsonWinningCardPair = new JSONArray();

        jsonWinningCardPair.put(mBlackCardText);
        jsonWinningCardPair.put(mWhiteCardText);

        return jsonWinningCardPair;
    }

    public void fromJSON(JSONObject jsonWinningCardPair) throws JSONException
    {
        mBlackCardText = jsonWinningCardPair.getString(JSON_BLACK_CARD_TEXT);
        mWhiteCardText = jsonWinningCardPair.getString(JSON_WHITE_CARD_TEXT);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mBlackCardText);
        dest.writeString(mWhiteCardText);
    }

    public static final Creator CREATOR = new Creator()
    {
        public WinningCardPair createFromParcel(Parcel in)
        {
            return new WinningCardPair(in);
        }

        public WinningCardPair[] newArray(int size)
        {
            return new WinningCardPair[size];
        }
    };
}
