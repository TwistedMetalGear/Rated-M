package net.silentbyte.ratedm;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatMessage
{
    public static final String JSON_ID = "_id";
    public static final String JSON_PLAYER_ID = "player_id";
    public static final String JSON_NAME = "name";
    public static final String JSON_MESSAGE = "message";
    public static final String JSON_UPDATED_AT = "updated_at";

    private String mId;
    private String mPlayerId;
    private String mName;
    private String mMessage;
    private String mMonthDay;
    private String mHourMinute;

    // Constructs a dummy chat message which the ChatAdapter will use to create a month/day view.
    public ChatMessage(String monthDay)
    {
        mId = "";
        mPlayerId = "";
        mName = "";
        mMessage = "";
        mMonthDay = monthDay;
        mHourMinute = "";
    }

    public ChatMessage(JSONObject chatMessage) throws JSONException
    {
        fromJSON(chatMessage);
    }

    public String getId()
    {
        return mId;
    }

    public String getPlayerId()
    {
        return mPlayerId;
    }

    public String getName()
    {
        return mName;
    }

    public String getMessage()
    {
        return mMessage;
    }

    public String getMonthDay()
    {
        return mMonthDay;
    }

    public String getHourMinute()
    {
        return mHourMinute;
    }

    public void fromJSON(JSONObject chatMessage) throws JSONException
    {
        mId = chatMessage.getString(JSON_ID);
        mPlayerId = chatMessage.getString(JSON_PLAYER_ID);
        mName = chatMessage.getString(JSON_NAME);
        mMessage = chatMessage.getString(JSON_MESSAGE);

        long updatedAt = chatMessage.getLong(JSON_UPDATED_AT);
        Date date = new Date(updatedAt * 1000);

        // Convert epoch to a human readable month and day.
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d");
        mMonthDay = sdf.format(date);

        // Convert epoch to a human readable hour and minute.
        sdf.applyPattern("h:mm a");
        mHourMinute = sdf.format(date);
    }

    @Override
    public boolean equals(Object object)
    {
        if (object != null && object instanceof ChatMessage)
        {
            ChatMessage chatMessage = (ChatMessage)object;
            return (chatMessage.getId().equals(mId) && chatMessage.getPlayerId().equals(mPlayerId) && chatMessage.getName().equals(mName) && chatMessage.getMessage().equals(mMessage));
        }

        return false;
    }
}
