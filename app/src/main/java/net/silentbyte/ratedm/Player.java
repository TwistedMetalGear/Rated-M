package net.silentbyte.ratedm;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

public class Player implements Comparable<Player>, Parcelable
{
    public static final String JSON_NAME = "name";
    public static final String JSON_SCORE = "score";
    public static final String JSON_SCORE_CHANGE = "score_change";
    public static final String JSON_WIN_PERCENTAGE = "win_percentage";
    public static final String JSON_BET = "bet";
    public static final String JSON_WINS = "wins";
    public static final String JSON_AUTO_PICKED = "auto_picked";
    public static final String JSON_SKIPPED = "skipped";

    private String mName;
    private int mScore;
    private int mScoreChange;
    private int mWinPercentage;
    private int mBet;
    private int mWins;
    private boolean mAutoPicked;
    private boolean mSkipped;

    public Player(JSONObject player) throws JSONException
    {
        fromJSON(player);
    }

    public Player(Parcel in)
    {
        mName = in.readString();
        mScore = in.readInt();
        mScoreChange = in.readInt();
        mWinPercentage = in.readInt();
        mBet = in.readInt();
        mWins = in.readInt();
        mAutoPicked = in.readByte() != 0;
        mSkipped = in.readByte() != 0;
    }

    public String getName()
    {
        return mName;
    }

    public int getScore()
    {
        return mScore;
    }

    public int getScoreChange()
    {
        return mScoreChange;
    }

    public int getWinPercentage()
    {
        return mWinPercentage;
    }

    public int getBet()
    {
        return mBet;
    }

    public void setBet(int bet)
    {
        mBet = bet;
    }

    public int getWins()
    {
        return mWins;
    }

    public boolean autoPicked()
    {
        return mAutoPicked;
    }

    public boolean skipped()
    {
        return mSkipped;
    }

    public JSONObject toJSON() throws JSONException
    {
        JSONObject jsonPlayer = new JSONObject();

        jsonPlayer.put(JSON_NAME, mName);
        jsonPlayer.put(JSON_SCORE, mScore);
        jsonPlayer.put(JSON_SCORE_CHANGE, mScoreChange);
        jsonPlayer.put(JSON_WIN_PERCENTAGE, mWinPercentage);
        jsonPlayer.put(JSON_BET, mBet);
        jsonPlayer.put(JSON_WINS, mWins);
        jsonPlayer.put(JSON_AUTO_PICKED, mAutoPicked);
        jsonPlayer.put(JSON_SKIPPED, mSkipped);

        return jsonPlayer;
    }

    public void fromJSON(JSONObject player) throws JSONException
    {
        mName = player.getString(JSON_NAME);
        mScore = player.getInt(JSON_SCORE);
        mScoreChange = player.getInt(JSON_SCORE_CHANGE);
        mWinPercentage = player.getInt(JSON_WIN_PERCENTAGE);
        mBet = player.getInt(JSON_BET);
        mWins = player.getInt(JSON_WINS);
        mAutoPicked = player.getBoolean(JSON_AUTO_PICKED);
        mSkipped = player.getBoolean(JSON_SKIPPED);
    }

    @Override
    public int compareTo(Player player)
    {
        if (player == null || mScore > player.getScore())
            return -1;

        if (mScore < player.getScore())
            return 1;

        return mName.compareTo(player.getName());
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mName);
        dest.writeInt(mScore);
        dest.writeInt(mScoreChange);
        dest.writeInt(mWinPercentage);
        dest.writeInt(mBet);
        dest.writeInt(mWins);
        dest.writeByte((byte)(mAutoPicked ? 1 : 0));
        dest.writeByte((byte)(mSkipped ? 1 : 0));
    }

    public static final Creator CREATOR = new Creator()
    {
        public Player createFromParcel(Parcel in)
        {
            return new Player(in);
        }

        public Player[] newArray(int size)
        {
            return new Player[size];
        }
    };
}
