package net.silentbyte.ratedm.notifications;

import android.content.Context;
import android.os.AsyncTask;

import com.google.firebase.iid.FirebaseInstanceId;

import net.silentbyte.ratedm.ToggleLog;

public class GetTokenTask extends AsyncTask<String, Void, Boolean>
{
    private static final String TAG = "GetTokenTask";

    String token = null;
    Context mContext;
    CallbackHandler mHandler;

    public GetTokenTask(Context context, CallbackHandler handler)
    {
        super();
        mContext = context;
        mHandler = handler;
    }

    @Override
    protected Boolean doInBackground(String... params)
    {
        token = FirebaseInstanceId.getInstance().getToken();
        ToggleLog.d(TAG, "Successfully retrieved token: " + token);
        return true;
    }

    @Override
    protected void onPostExecute(Boolean valid)
    {
        if (token != null)
            mHandler.onGetTokenSuccess(token);
        else
            mHandler.onGetTokenFailure();
    }

    public interface CallbackHandler
    {
        public void onGetTokenSuccess(String token);
        public void onGetTokenFailure();
    }
}