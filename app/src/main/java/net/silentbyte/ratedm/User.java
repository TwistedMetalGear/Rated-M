package net.silentbyte.ratedm;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

public class User implements Comparable<User>, Parcelable
{
    private static final String TAG = "User";
    private static final String KEY_JSON_USER = "json_user";
    public static final String JSON_ID = "_id";
    public static final String JSON_FULL_NAME = "full_name";
    public static final String JSON_DISPLAY_NAME = "display_name";
    public static final String JSON_EMAIL = "email";
    public static final String JSON_PICTURE_URL = "picture_url";
    public static final String JSON_HIDE_EMAIL_PREFIX = "hide_email_prefix";
    public static final String JSON_HIDE_FACEBOOK_NAME = "hide_facebook_name";
    public static final String JSON_HIDE_FACEBOOK_PICTURE = "hide_facebook_picture";
    public static final String JSON_FRIENDS_ONLY = "friends_only";
    public static final String JSON_TYPE = "type";
    public static final String JSON_WINS = "wins";
    public static final String JSON_LOSSES = "losses";
    public static final String JSON_SUBMITS = "submits";
    public static final String JSON_AUTO_PICK_SKIPS = "auto_pick_skips";
    public static final int TYPE_EMAIL = 0;
    public static final int TYPE_FACEBOOK = 1;

    private static User mUser = null;
    private String mId;
    private String mFullName;
    private String mDisplayName;
    private String mEmail;
    private String mPictureUrl;
    private boolean mHideEmailPrefix;
    private boolean mHideFacebookName;
    private boolean mHideFacebookPicture;
    private boolean mFriendsOnly;
    private int mType;
    private int mWins;
    private int mLosses;
    private int mSubmits;
    private int mAutoPickSkips;

    public User(String id, String name, String displayName, String email, String pictureUrl, boolean hideEmailPrefix, boolean hideFacebookName, boolean hideFacebookPicture, boolean friendsOnly, int wins, int losses, int submits, int autoPickSkips)
    {
        mId = id;
        mFullName = name;
        mDisplayName = displayName;
        mEmail = email;
        mPictureUrl = pictureUrl;
        mHideEmailPrefix = hideEmailPrefix;
        mHideFacebookName = hideFacebookName;
        mHideFacebookPicture = hideFacebookPicture;
        mFriendsOnly = friendsOnly;

        if (!mEmail.isEmpty())
            mType = TYPE_EMAIL;
        else
            mType = TYPE_FACEBOOK;

        mWins = wins;
        mLosses = losses;
        mSubmits = submits;
        mAutoPickSkips = autoPickSkips;
    }

    public User(JSONObject user) throws JSONException
    {
        mId = user.getString(JSON_ID);
        mFullName = user.getString(JSON_FULL_NAME);
        mDisplayName = user.getString(JSON_DISPLAY_NAME);
        mEmail = user.getString(JSON_EMAIL);
        mPictureUrl = user.getString(JSON_PICTURE_URL);
        mHideEmailPrefix = user.getBoolean(JSON_HIDE_EMAIL_PREFIX);
        mHideFacebookName = user.getBoolean(JSON_HIDE_FACEBOOK_NAME);
        mHideFacebookPicture = user.getBoolean(JSON_HIDE_FACEBOOK_PICTURE);
        mFriendsOnly = user.getBoolean(JSON_FRIENDS_ONLY);
        mType = user.getInt(JSON_TYPE);
        mWins = user.getInt(JSON_WINS);
        mLosses = user.getInt(JSON_LOSSES);
        mSubmits = user.getInt(JSON_SUBMITS);
        mAutoPickSkips = user.getInt(JSON_AUTO_PICK_SKIPS);
    }

    public User(Parcel in)
    {
        mId = in.readString();
        mFullName = in.readString();
        mDisplayName = in.readString();
        mEmail = in.readString();
        mPictureUrl = in.readString();
        mHideEmailPrefix = in.readByte() != 0;
        mHideFacebookName = in.readByte() != 0;
        mHideFacebookPicture = in.readByte() != 0;
        mFriendsOnly = in.readByte() != 0;
        mType = in.readInt();
        mWins = in.readInt();
        mLosses = in.readInt();
        mSubmits = in.readInt();
        mAutoPickSkips = in.readInt();
    }

    public static synchronized User get(Context context)
    {
        if (mUser != null)
            return mUser;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String json = prefs.getString(KEY_JSON_USER, null);

        if (json != null)
        {
            try
            {
                mUser = new User(new JSONObject(json));
                return mUser;
            }
            catch (JSONException e)
            {
                ToggleLog.e(TAG, "Unable to get user. JSONException: " + e.getMessage());
            }
        }

        return null;
    }

    public static synchronized boolean set(Context context, String id, String fullName, String displayName, String email, String pictureUrl, boolean hideEmailPrefix, boolean hideFacebookName, boolean hideFacebookPicture, boolean friendsOnly, int type, int wins, int losses, int submits, int autoPickSkips)
    {
        fullName = JSONObject.quote(fullName);
        displayName = JSONObject.quote(displayName);
        email = JSONObject.quote(email);

        String json = "{\"_id\":\"" + id + "\", \"full_name\":" + fullName + ", \"display_name\":" + displayName + ", \"email\":" + email +
                      ", \"picture_url\":\"" + pictureUrl + "\", \"hide_email_prefix\":" + hideEmailPrefix +  ", \"hide_facebook_name\":" + hideFacebookName +
                      ", \"hide_facebook_picture\":" + hideFacebookPicture + ", \"friends_only\":" + friendsOnly + ", \"type\":" + type + ", \"wins\":" + wins +
                      ", \"losses\":" + losses + ", \"submits\":" + submits + ", \"auto_pick_skips\":" + autoPickSkips + "}";

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (prefs.edit().putString(KEY_JSON_USER, json).commit())
        {
            try
            {
                mUser = new User(new JSONObject(json));
                return true;
            }
            catch (JSONException e)
            {
                ToggleLog.e(TAG, "Unable to set user. JSONException: " + e.getMessage());
                clear(context);
            }
        }

        return false;
    }

    public static synchronized void clear(Context context)
    {
        mUser = null;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().remove(KEY_JSON_USER).commit();
    }

    public String getId()
    {
        return mId;
    }

    public String getFullName()
    {
        return mFullName;
    }

    public String getDisplayName()
    {
        return mDisplayName;
    }

    public String getEmail()
    {
        return mEmail;
    }

    public String getPictureUrl()
    {
        return mPictureUrl;
    }

    public boolean hideEmailPrefix()
    {
        return mHideEmailPrefix;
    }

    public boolean hideFacebookName()
    {
        return mHideFacebookName;
    }

    public boolean hideFacebookPicture()
    {
        return mHideFacebookPicture;
    }

    public boolean playsWithFriendsOnly()
    {
        return mFriendsOnly;
    }

    public int getType()
    {
        return mType;
    }

    public int getWins()
    {
        return mWins;
    }

    public int getLosses()
    {
        return mLosses;
    }

    public int getSubmits()
    {
        return mSubmits;
    }

    public int getAutoPickSkips()
    {
        return mAutoPickSkips;
    }

    public int getActivePercentage()
    {
        float totalSubmits = mSubmits + mAutoPickSkips;

        // -1 means that active percentage is unknown since the user hasn't played yet.
        if (totalSubmits == 0)
            return -1;

        return Math.round((mSubmits / totalSubmits) * 100);
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
        dest.writeString(mFullName);
        dest.writeString(mDisplayName);
        dest.writeString(mEmail);
        dest.writeString(mPictureUrl);
        dest.writeByte((byte)(mHideEmailPrefix ? 1 : 0));
        dest.writeByte((byte)(mHideFacebookName ? 1 : 0));
        dest.writeByte((byte)(mHideFacebookPicture ? 1 : 0));
        dest.writeByte((byte)(mFriendsOnly ? 1 : 0));
        dest.writeInt(mType);
        dest.writeInt(mWins);
        dest.writeInt(mLosses);
        dest.writeInt(mSubmits);
        dest.writeInt(mAutoPickSkips);
    }

    public static final Creator CREATOR = new Creator()
    {
        public User createFromParcel(Parcel in)
        {
            return new User(in);
        }

        public User[] newArray(int size)
        {
            return new User[size];
        }
    };

    @Override
    public boolean equals(Object object)
    {
        if (object != null && object instanceof User)
        {
            User user = (User)object;
            return (user.getId().equals(mId));
        }

        return false;
    }

    @Override
    public int compareTo(User user)
    {
        return mFullName.compareTo(user.getFullName());
    }
}
