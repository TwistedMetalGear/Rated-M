package net.silentbyte.ratedm;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Round implements Parcelable
{
    public static final String JSON_PLAYERS = "players";
    public static final String JSON_JUDGE_ID = "judge_id";
    public static final String JSON_WINNER_ID = "winner_id";
    public static final String JSON_BLACK_CARD = "black_card";
    public static final String JSON_SUBMITTED_CARDS = "submitted_cards";

    private Map<String, Player> mPlayers;
    private String mJudgeId;
    private String mWinnerId;
    private Card mBlackCard;
    private Map<String, List<Card>> mSubmittedCards;

    public Round(JSONObject round) throws JSONException
    {
        fromJSON(round);
    }

    public Round(Parcel in)
    {
        mPlayers = new HashMap<String, Player>();
        readPlayerMap(mPlayers, in);
        mJudgeId = in.readString();
        mWinnerId = in.readString();
        mBlackCard = in.readParcelable(Card.class.getClassLoader());
        mSubmittedCards = new HashMap<String, List<Card>>();
        readCardMap(mSubmittedCards, in);
    }

    private void fromJSON(JSONObject round) throws JSONException
    {
        mPlayers = jsonPlayersToMap(round.getJSONObject(JSON_PLAYERS));
        mJudgeId = round.getString(JSON_JUDGE_ID);
        mWinnerId = round.getString(JSON_WINNER_ID);
        mBlackCard = new Card(round.getJSONObject(JSON_BLACK_CARD));
        mSubmittedCards = submittedCardsToMap(round.getJSONArray(JSON_SUBMITTED_CARDS));
    }

    private Map jsonPlayersToMap(JSONObject jsonPlayerMap) throws JSONException
    {
        HashMap<String, Player> players = new HashMap<String, Player>();

        Iterator it = jsonPlayerMap.keys();

        while (it.hasNext())
        {
            String playerId = (String)it.next();
            JSONObject jsonPlayer = jsonPlayerMap.getJSONObject(playerId);
            players.put(playerId, new Player(jsonPlayer));
        }

        List<Map.Entry<String, Player>> entries = new ArrayList<Map.Entry<String, Player>>(players.entrySet());

        Collections.sort(entries, new Comparator<Map.Entry<String, Player>>()
        {
            public int compare(Map.Entry<String, Player> a, Map.Entry<String, Player> b)
            {
                return a.getValue().compareTo(b.getValue());
            }
        });

        Map<String, Player> sortedPlayers = new LinkedHashMap<String, Player>();

        for (Map.Entry<String, Player> entry : entries)
        {
            sortedPlayers.put(entry.getKey(), entry.getValue());
        }

        return sortedPlayers;
    }

    private Map submittedCardsToMap(JSONArray jsonCardMaps) throws JSONException
    {
        // Using LinkedHashMap to preserve ordering of submitted cards.
        LinkedHashMap<String, List<Card>> cardMap = new LinkedHashMap<String, List<Card>>();

        for (int i = 0; i < jsonCardMaps.length(); i++)
        {
            JSONObject jsonCardMap = jsonCardMaps.getJSONObject(i);
            addCardsToMap(cardMap, jsonCardMap);
        }

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

    private void readPlayerMap(Map playerMap, Parcel in)
    {
        int size = in.readInt();

        for (int i = 0; i < size; i++)
        {
            String key = in.readString();
            Player player = in.readParcelable(Player.class.getClassLoader());
            playerMap.put(key, player);
        }
    }

    private void writePlayerMap(Parcel dest, int flags)
    {
        dest.writeInt(mPlayers.size());

        for (String key : mPlayers.keySet())
        {
            dest.writeString(key);
            dest.writeParcelable(mPlayers.get(key), flags);
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

    public Map<String, Player> getPlayers()
    {
        return mPlayers;
    }

    public String getJudgeId()
    {
        return mJudgeId;
    }

    public String getWinnerId()
    {
        return mWinnerId;
    }

    public Card getBlackCard()
    {
        return mBlackCard;
    }

    public Map<String, List<Card>> getSubmittedCards()
    {
        return mSubmittedCards;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        writePlayerMap(dest, flags);
        dest.writeString(mJudgeId);
        dest.writeString(mWinnerId);
        dest.writeParcelable(mBlackCard, flags);
        writeCardMap(mSubmittedCards, dest);
    }

    public static final Creator CREATOR = new Creator()
    {
        public Round createFromParcel(Parcel in)
        {
            return new Round(in);
        }

        public Round[] newArray(int size)
        {
            return new Round[size];
        }
    };
}
