package net.silentbyte.ratedm.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import net.silentbyte.ratedm.GameConstants;
import net.silentbyte.ratedm.R;
import net.silentbyte.ratedm.RatedMServer;
import net.silentbyte.ratedm.ToggleLog;
import net.silentbyte.ratedm.User;
import net.silentbyte.ratedm.notifications.GetTokenTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SignInFragment extends Fragment
{
    private static final String TAG = "SignInFragment";

    private CallbackHandler mCallbackHandler;
    private CallbackManager mCallbackManager;
    private String mProgressText;
    private boolean mBusy = false;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mCallbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>()
        {
            @Override
            public void onSuccess(LoginResult loginResult)
            {
                CreateFacebookUserTask task = new CreateFacebookUserTask(loginResult.getAccessToken());
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

            @Override
            public void onCancel()
            {
                ToggleLog.d(TAG, "Facebook sign in canceled.");
            }

            @Override
            public void onError(FacebookException error)
            {
                ToggleLog.e(TAG, "Unable to sign in via Facebook. Error: " + error.getMessage());

                if (mCallbackHandler != null)
                    mCallbackHandler.onSignInFailure(getStringSafe(R.string.try_again_later));
            }
        });
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    public String getProgressText()
    {
        return mProgressText;
    }

    public void setProgressText(String progressText)
    {
        mProgressText = progressText;
    }

    public boolean isBusy()
    {
        return mBusy;
    }

    public void setBusy(boolean busy)
    {
        mBusy = busy;
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

    public void createAccount(final String email, final String password)
    {
        GetTokenTask task = new GetTokenTask(getContext(), new GetTokenTask.CallbackHandler()
        {
            @Override
            public void onGetTokenSuccess(String token)
            {
                RatedMServer.createEmailUser(email, password, token, GameConstants.PAID, new RatedMServer.CallbackHandler()
                {
                    @Override
                    public void onResponse(int responseCode, String response)
                    {
                        if (responseCode == 200)
                        {
                            try
                            {
                                JSONObject jsonUser = new JSONObject(response);

                                String id = jsonUser.getString("_id");
                                String fullName = jsonUser.getString(User.JSON_FULL_NAME);
                                boolean hideEmailPrefix = jsonUser.getBoolean(User.JSON_HIDE_EMAIL_PREFIX);
                                boolean hideFacebookName = jsonUser.getBoolean(User.JSON_HIDE_FACEBOOK_NAME);
                                boolean hideFacebookPicture = jsonUser.getBoolean(User.JSON_HIDE_FACEBOOK_PICTURE);
                                boolean friendsOnly = jsonUser.getBoolean(User.JSON_FRIENDS_ONLY);
                                int wins = jsonUser.getInt(User.JSON_WINS);
                                int losses = jsonUser.getInt(User.JSON_LOSSES);
                                int submits = jsonUser.getInt(User.JSON_SUBMITS);
                                int autoPickSkips = jsonUser.getInt(User.JSON_AUTO_PICK_SKIPS);

                                if (mCallbackHandler != null)
                                    mCallbackHandler.onCreateAccountSuccess(id, fullName, fullName, email, "", hideEmailPrefix, hideFacebookName, hideFacebookPicture, friendsOnly, User.TYPE_EMAIL, wins, losses, submits, autoPickSkips);
                            }
                            catch (JSONException e)
                            {
                                ToggleLog.e(TAG, "Unable to create account. JSONException: " + e.getMessage());

                                if (mCallbackHandler != null)
                                    mCallbackHandler.onCreateAccountFailure(getStringSafe(R.string.try_again_later));
                            }
                        }
                        else if (responseCode == 409)
                        {
                            ToggleLog.d(TAG, "Unable to create account. Email address already in use.");

                            if (mCallbackHandler != null)
                                mCallbackHandler.onCreateAccountFailure(getStringSafe(R.string.email_in_use));
                        }
                        else
                        {
                            ToggleLog.e(TAG, "Unable to create account. Response code: " + responseCode);

                            if (mCallbackHandler != null)
                                mCallbackHandler.onCreateAccountFailure(getStringSafe(R.string.try_again_later));
                        }
                    }
                });
            }

            @Override
            public void onGetTokenFailure()
            {
                ToggleLog.e(TAG, "Unable to create account. Unable to retrieve token.");

                if (mCallbackHandler != null)
                    mCallbackHandler.onCreateAccountFailure(getStringSafe(R.string.try_again_later));
            }
        });

        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void emailSignIn(final String email, final String password)
    {
        GetTokenTask task = new GetTokenTask(getContext(), new GetTokenTask.CallbackHandler()
        {
            @Override
            public void onGetTokenSuccess(String token)
            {
                RatedMServer.login(email, password, token, new RatedMServer.CallbackHandler()
                {
                    @Override
                    public void onResponse(int responseCode, String response)
                    {
                        if (responseCode == 200)
                        {
                            try
                            {
                                JSONObject jsonUser = new JSONObject(response);

                                String id = jsonUser.getString("_id");
                                String fullName = jsonUser.getString(User.JSON_FULL_NAME);
                                String displayName = fullName;

                                if (jsonUser.has(User.JSON_DISPLAY_NAME))
                                    displayName = jsonUser.getString(User.JSON_DISPLAY_NAME);

                                boolean hideEmailPrefix = jsonUser.getBoolean(User.JSON_HIDE_EMAIL_PREFIX);
                                boolean hideFacebookName = jsonUser.getBoolean(User.JSON_HIDE_FACEBOOK_NAME);
                                boolean hideFacebookPicture = jsonUser.getBoolean(User.JSON_HIDE_FACEBOOK_PICTURE);
                                boolean friendsOnly = jsonUser.getBoolean(User.JSON_FRIENDS_ONLY);
                                int wins = jsonUser.getInt(User.JSON_WINS);
                                int losses = jsonUser.getInt(User.JSON_LOSSES);
                                int submits = jsonUser.getInt(User.JSON_SUBMITS);
                                int autoPickSkips = jsonUser.getInt(User.JSON_AUTO_PICK_SKIPS);

                                if (mCallbackHandler != null)
                                    mCallbackHandler.onSignInSuccess(id, fullName, displayName, email, "", hideEmailPrefix, hideFacebookName, hideFacebookPicture, friendsOnly, User.TYPE_EMAIL, wins, losses, submits, autoPickSkips);
                            }
                            catch (JSONException e)
                            {
                                ToggleLog.e(TAG, "Unable to sign in via email. JSONException: " + e.getMessage());

                                if (mCallbackHandler != null)
                                    mCallbackHandler.onSignInFailure(getStringSafe(R.string.try_again_later));
                            }
                        }
                        else if (responseCode == 401 || responseCode == 404)
                        {
                            ToggleLog.e(TAG, "Unable to sign in via email. Invalid email and/or password.");

                            if (mCallbackHandler != null)
                                mCallbackHandler.onSignInFailure(getStringSafe(R.string.invalid_credentials));
                        }
                        else
                        {
                            ToggleLog.e(TAG, "Unable to sign in via email. Response code: " + responseCode);

                            if (mCallbackHandler != null)
                                mCallbackHandler.onSignInFailure(getStringSafe(R.string.try_again_later));
                        }
                    }
                });
            }

            @Override
            public void onGetTokenFailure()
            {
                ToggleLog.e(TAG, "Unable to sign in via email. Unable to retrieve token.");

                if (mCallbackHandler != null)
                    mCallbackHandler.onSignInFailure(getStringSafe(R.string.try_again_later));
            }
        });

        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void facebookSignIn()
    {
        List<String> permissions = new ArrayList<String>();
        permissions.add("public_profile");
        permissions.add("user_status");
        permissions.add("user_friends");

        LoginManager.getInstance().logInWithReadPermissions(this, permissions);
    }

    public void facebookSignOut()
    {
        LoginManager.getInstance().logOut();
    }

    public void updateUserSettings(String id, String displayName, boolean hideEmailPrefix, boolean hideFacebookName, boolean hideFacebookPicture, boolean friendsOnly)
    {
        RatedMServer.updateUserSettings(id, displayName, hideEmailPrefix, hideFacebookName, hideFacebookPicture, friendsOnly, new RatedMServer.CallbackHandler()
        {
            @Override
            public void onResponse(int responseCode, String response)
            {
                if (responseCode == 200)
                {
                    if (mCallbackHandler != null)
                        mCallbackHandler.onUpdateUserSettingsSuccess();
                }
                else
                {
                    ToggleLog.e(TAG, "Unable to update user settings. Response code: " + responseCode);

                    if (AccessToken.getCurrentAccessToken() != null)
                        facebookSignOut();

                    if (mCallbackHandler != null)
                        mCallbackHandler.onUpdateUserSettingsFailure(getStringSafe(R.string.try_again_later));
                }
            }
        });
    }

    public void sendPasswordEmail(String email)
    {
        RatedMServer.sendPasswordEmail(email, new RatedMServer.CallbackHandler()
        {
            @Override
            public void onResponse(int responseCode, String response)
            {
                if (responseCode == 200)
                {
                    if (mCallbackHandler != null)
                        mCallbackHandler.onSendPasswordEmailSuccess();
                }
                else
                {
                    ToggleLog.e(TAG, "Unable to send password email. Response code: " + responseCode);

                    String message = getStringSafe(R.string.try_again_later);

                    if (responseCode == 404)
                        message = "The specified email address is not registered with Rated M.";

                    if (mCallbackHandler != null)
                        mCallbackHandler.onSendPasswordEmailFailure(message);
                }
            }
        });
    }

    private class CreateFacebookUserTask extends AsyncTask<String, Void, Boolean>
    {
        AccessToken accessToken;
        String facebookId;
        String fullName;
        String pictureUrl;
        boolean success = true;

        public CreateFacebookUserTask(AccessToken accessToken)
        {
            super();
            this.accessToken = accessToken;
        }

        @Override
        protected Boolean doInBackground(String... params)
        {
            GraphRequest request = GraphRequest.newMeRequest(accessToken, new GraphRequest.GraphJSONObjectCallback()
            {
                @Override
                public void onCompleted(JSONObject jsonObject, GraphResponse graphResponse)
                {
                    if (jsonObject != null)
                    {
                        try
                        {
                            facebookId = jsonObject.getString("id");
                            fullName = jsonObject.getString("name");
                            JSONObject jsonPicture = jsonObject.getJSONObject("picture");
                            JSONObject jsonPictureData = jsonPicture.getJSONObject("data");
                            pictureUrl = jsonPictureData.getString("url");
                        }
                        catch (JSONException e)
                        {
                            ToggleLog.e(TAG, "Unable to create Facebook user. JSONException: " + e.getMessage());
                            success = false;
                        }
                    }
                    else
                    {
                        ToggleLog.e(TAG, "Unable to create Facebook user. The JSON object is null.");
                        success = false;
                    }
                }
            });

            Bundle parameters = new Bundle();
            parameters.putString("fields", "id,name,picture.type(large)");
            request.setParameters(parameters);
            request.executeAndWait();

            return true;
        }

        @Override
        protected void onPostExecute(Boolean valid)
        {
            if (success)
            {
                GetTokenTask task = new GetTokenTask(getContext(), new GetTokenTask.CallbackHandler()
                {
                    @Override
                    public void onGetTokenSuccess(String token)
                    {
                        RatedMServer.createFacebookUser(facebookId, fullName, pictureUrl, token, GameConstants.PAID, new RatedMServer.CallbackHandler()
                        {
                            @Override
                            public void onResponse(int responseCode, String response)
                            {
                                if (responseCode == 200 || responseCode == 409)
                                {
                                    try
                                    {
                                        JSONObject jsonUser = new JSONObject(response);

                                        String id = jsonUser.getString("_id");
                                        String displayName = fullName;

                                        if (jsonUser.has(User.JSON_DISPLAY_NAME))
                                            displayName = jsonUser.getString(User.JSON_DISPLAY_NAME);

                                        boolean hideEmailPrefix = jsonUser.getBoolean(User.JSON_HIDE_EMAIL_PREFIX);
                                        boolean hideFacebookName = jsonUser.getBoolean(User.JSON_HIDE_FACEBOOK_NAME);
                                        boolean hideFacebookPicture = jsonUser.getBoolean(User.JSON_HIDE_FACEBOOK_PICTURE);
                                        boolean friendsOnly = jsonUser.getBoolean(User.JSON_FRIENDS_ONLY);
                                        int wins = jsonUser.getInt(User.JSON_WINS);
                                        int losses = jsonUser.getInt(User.JSON_LOSSES);
                                        int submits = jsonUser.getInt(User.JSON_SUBMITS);
                                        int autoPickSkips = jsonUser.getInt(User.JSON_AUTO_PICK_SKIPS);

                                        if (mCallbackHandler != null)
                                            mCallbackHandler.onSignInSuccess(id, fullName, displayName, "", pictureUrl, hideEmailPrefix, hideFacebookName, hideFacebookPicture, friendsOnly, User.TYPE_FACEBOOK, wins, losses, submits, autoPickSkips);
                                    }
                                    catch (JSONException e)
                                    {
                                        ToggleLog.e(TAG, "Unable to create Facebook user. JSONException: " + e.getMessage());

                                        facebookSignOut();

                                        if (mCallbackHandler != null)
                                            mCallbackHandler.onSignInFailure(getStringSafe(R.string.try_again_later));
                                    }
                                }
                                else
                                {
                                    ToggleLog.e(TAG, "Unable to create Facebook user. Response code: " + responseCode);

                                    facebookSignOut();

                                    if (mCallbackHandler != null)
                                        mCallbackHandler.onSignInFailure(getStringSafe(R.string.try_again_later));
                                }
                            }
                        });
                    }

                    @Override
                    public void onGetTokenFailure()
                    {
                        ToggleLog.e(TAG, "Unable to create Facebook user. Unable to retrieve token.");

                        facebookSignOut();

                        if (mCallbackHandler != null)
                            mCallbackHandler.onSignInFailure(getStringSafe(R.string.try_again_later));
                    }
                });

                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            else
            {
                facebookSignOut();

                if (mCallbackHandler != null)
                    mCallbackHandler.onSignInFailure(getStringSafe(R.string.try_again_later));
            }
        }
    }

    public interface CallbackHandler
    {
        public void onCreateAccountSuccess(String id, String fullName, String displayName, String email, String pictureUrl, boolean hideEmailPrefix, boolean hideFacebookName, boolean hideFacebookPicture, boolean friendsOnly, int type, int wins, int losses, int submits, int autoPickSkips);
        public void onCreateAccountFailure(String message);
        public void onSignInSuccess(String id, String fullName, String displayName, String email, String pictureUrl, boolean hideEmailPrefix, boolean hideFacebookName, boolean hideFacebookPicture, boolean friendsOnly, int type, int wins, int losses, int submits, int autoPickSkips);
        public void onSignInFailure(String message);
        public void onUpdateUserSettingsSuccess();
        public void onUpdateUserSettingsFailure(String message);
        public void onSendPasswordEmailSuccess();
        public void onSendPasswordEmailFailure(String message);
    }
}
