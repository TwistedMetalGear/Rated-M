package net.silentbyte.ratedm.activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.facebook.AccessToken;

import net.silentbyte.ratedm.GameConstants;
import net.silentbyte.ratedm.GameFunctions;
import net.silentbyte.ratedm.R;
import net.silentbyte.ratedm.User;
import net.silentbyte.ratedm.fragments.SignInFragment;

public class SignInActivity extends FragmentActivity implements SignInFragment.CallbackHandler
{
    private static final String TAG = "SignInActivity";
    private static final String TAG_SIGN_IN_FRAGMENT = "sign_in_fragment";
    private static final String KEY_CURRENT_SCREEN = "current_screen";
    private static final String KEY_ID = "id";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PICTURE_URL = "picture_url";
    private static final String KEY_TYPE = "type";
    private static final String KEY_WINS = "wins";
    private static final String KEY_LOSSES = "losses";
    private static final String KEY_SUBMITS = "submits";
    private static final String KEY_AUTO_PICK_SKIPS = "auto_pick_skips";

    private static final int SCREEN_SIGN_IN = 0;
    private static final int SCREEN_REGISTER = 1;
    private static final int SCREEN_USER_SETTINGS = 2;
    private static final int MIN_PASSWORD_LENGTH = 8;

    private EditText mEmailView;
    private EditText mPasswordView;
    private EditText mConfirmPasswordView;
    private EditText mNameView;
    private View mSignInButton;
    private View mRegisterButton;
    private View mCreateAccountButton;
    private View mForgotPasswordButton;
    private View mFacebookSignInButton;
    private View mDoneButton;
    private View mMainPanel;
    private View mSignInPanel;
    private View mRegisterPanel;
    private View mUserSettingsPanel;
    private View mEmailSettingsPanel;
    private View mFacebookSettingsPanel;
    private CheckBox mHideEmailPrefixView;
    private CheckBox mHideFacebookNameView;
    private CheckBox mHideFacebookPictureView;
    private CheckBox mFriendsOnlyView;

    private SignInFragment mSignInFragment;

    private int mCurrentScreen = 0;

    // User attributes which will be written to SharedPreferences upon signing in and updating user settings.
    private String mId;
    private String mFullName;
    private String mDisplayName;
    private String mEmail;
    private String mPictureUrl;
    private int mType;
    private int mWins;
    private int mLosses;
    private int mSubmits;
    private int mAutoPickSkips;

    private ProgressDialog mProgressDialog;
    private Dialog mNotificationDialog;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Lock the device in portrait mode.
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_sign_in);

        mEmailView = (EditText)findViewById(R.id.email);
        mPasswordView = (EditText)findViewById(R.id.password);
        mConfirmPasswordView = (EditText)findViewById(R.id.confirm_password);
        mNameView = (EditText)findViewById(R.id.name);
        mNameView.setFilters(new InputFilter[]{new InputFilter.LengthFilter(GameConstants.MAX_PLAYER_NAME_LENGTH)});
        mSignInButton = findViewById(R.id.sign_in_button);
        mRegisterButton = findViewById(R.id.register_button);
        mCreateAccountButton = findViewById(R.id.create_account_button);
        mForgotPasswordButton = findViewById(R.id.forgot_password_button);
        mFacebookSignInButton = findViewById(R.id.facebook_sign_in_button);
        mDoneButton = findViewById(R.id.done_button);
        mMainPanel = findViewById(R.id.main_panel);
        mSignInPanel = findViewById(R.id.sign_in_panel);
        mRegisterPanel = findViewById(R.id.register_panel);
        mUserSettingsPanel = findViewById(R.id.user_settings_panel);
        mEmailSettingsPanel = findViewById(R.id.email_settings_panel);
        mFacebookSettingsPanel = findViewById(R.id.facebook_settings_panel);
        mHideEmailPrefixView = (CheckBox)findViewById(R.id.hide_email_prefix);
        mHideFacebookNameView = (CheckBox)findViewById(R.id.hide_facebook_name);
        mHideFacebookPictureView = (CheckBox)findViewById(R.id.hide_facebook_picture);
        mFriendsOnlyView = (CheckBox)findViewById(R.id.friends_only);

        mSignInButton.setOnClickListener(new ButtonClickListener());
        mRegisterButton.setOnClickListener(new ButtonClickListener());
        mCreateAccountButton.setOnClickListener(new ButtonClickListener());
        mForgotPasswordButton.setOnClickListener(new ButtonClickListener());
        mFacebookSignInButton.setOnClickListener(new ButtonClickListener());
        mDoneButton.setOnClickListener(new ButtonClickListener());

        FragmentManager fm = getSupportFragmentManager();

        // Add sign in fragment to fragment manager.
        mSignInFragment = (SignInFragment)fm.findFragmentByTag(TAG_SIGN_IN_FRAGMENT);

        if (mSignInFragment == null)
        {
            mSignInFragment = new SignInFragment();
            fm.beginTransaction().add(mSignInFragment, TAG_SIGN_IN_FRAGMENT).commit();
        }

        if (savedInstanceState != null)
        {
            mCurrentScreen = savedInstanceState.getInt(KEY_CURRENT_SCREEN);
            mId = savedInstanceState.getString(KEY_ID);
            mFullName = savedInstanceState.getString(KEY_FULL_NAME);
            mDisplayName = savedInstanceState.getString(KEY_DISPLAY_NAME);
            mEmail = savedInstanceState.getString(KEY_EMAIL);
            mPictureUrl = savedInstanceState.getString(KEY_PICTURE_URL);
            mType = savedInstanceState.getInt(KEY_TYPE);
            mWins = savedInstanceState.getInt(KEY_WINS);
            mLosses = savedInstanceState.getInt(KEY_LOSSES);
            mSubmits = savedInstanceState.getInt(KEY_SUBMITS);
            mAutoPickSkips = savedInstanceState.getInt(KEY_AUTO_PICK_SKIPS);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        String progressText = mSignInFragment.getProgressText();

        if (progressText != null && (mProgressDialog == null || !mProgressDialog.isShowing()))
            showProgressDialog(progressText);

        if (mCurrentScreen == SCREEN_REGISTER)
            showRegisterPanel();
        else if (mCurrentScreen == SCREEN_USER_SETTINGS)
        {
            boolean hideEmailPrefix = mHideEmailPrefixView.isChecked();
            boolean hideFacebookName = mHideFacebookNameView.isChecked();
            boolean hideFacebookPicture = mHideFacebookPictureView.isChecked();
            boolean friendsOnly = mFriendsOnlyView.isChecked();
            showUserSettingsPanel(hideEmailPrefix, hideFacebookName, hideFacebookPicture, friendsOnly);
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();

        if (mProgressDialog != null)
            mProgressDialog.dismiss();

        if (mNotificationDialog != null)
            mNotificationDialog.dismiss();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(KEY_CURRENT_SCREEN, mCurrentScreen);
        savedInstanceState.putString(KEY_ID, mId);
        savedInstanceState.putString(KEY_FULL_NAME, mFullName);
        savedInstanceState.putString(KEY_DISPLAY_NAME, mDisplayName);
        savedInstanceState.putString(KEY_EMAIL, mEmail);
        savedInstanceState.putString(KEY_PICTURE_URL, mPictureUrl);
        savedInstanceState.putInt(KEY_TYPE, mType);
        savedInstanceState.putInt(KEY_WINS, mWins);
        savedInstanceState.putInt(KEY_LOSSES, mLosses);
        savedInstanceState.putInt(KEY_SUBMITS, mSubmits);
        savedInstanceState.putInt(KEY_AUTO_PICK_SKIPS, mAutoPickSkips);
    }

    @Override
    public void onBackPressed()
    {
        if (mCurrentScreen == SCREEN_REGISTER)
            showSignInPanel();
        else if (mCurrentScreen == SCREEN_USER_SETTINGS)
        {
            if (AccessToken.getCurrentAccessToken() != null)
                mSignInFragment.facebookSignOut();

            showSignInPanel();
        }
        else
            finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK)
            mSignInFragment.setBusy(false);
    }

    private void showSignInPanel()
    {
        mUserSettingsPanel.setVisibility(View.GONE);
        mMainPanel.setVisibility(View.VISIBLE);
        mRegisterPanel.setVisibility(View.GONE);
        mSignInPanel.setVisibility(View.VISIBLE);
        mCurrentScreen = SCREEN_SIGN_IN;
    }

    private void showRegisterPanel()
    {
        mSignInPanel.setVisibility(View.GONE);
        mRegisterPanel.setVisibility(View.VISIBLE);
        mCurrentScreen = SCREEN_REGISTER;
    }

    private void showUserSettingsPanel(boolean hideEmailPrefix, boolean hideFacebookName, boolean hideFacebookPicture, boolean friendsOnly)
    {
        mMainPanel.setVisibility(View.GONE);
        mUserSettingsPanel.setVisibility(View.VISIBLE);
        mEmailSettingsPanel.setVisibility(View.GONE);
        mFacebookSettingsPanel.setVisibility(View.GONE);

        if (mType == User.TYPE_EMAIL)
            mEmailSettingsPanel.setVisibility(View.VISIBLE);
        else
            mFacebookSettingsPanel.setVisibility(View.VISIBLE);

        mHideEmailPrefixView.setChecked(hideEmailPrefix);
        mHideFacebookNameView.setChecked(hideFacebookName);
        mHideFacebookPictureView.setChecked(hideFacebookPicture);
        mFriendsOnlyView.setChecked(friendsOnly);

        mCurrentScreen = SCREEN_USER_SETTINGS;
    }

    private void signIn()
    {
        String email = mEmailView.getText().toString().toLowerCase();
        String password = mPasswordView.getText().toString();

        if (!validateEmail(email))
        {
            mNotificationDialog = GameFunctions.showDialog(SignInActivity.this, getString(R.string.invalid_email_title),
                                                           getString(R.string.invalid_email_description));
            mSignInFragment.setBusy(false);
            return;
        }

        if (password.length() < MIN_PASSWORD_LENGTH)
        {
            String message = String.format(getString(R.string.invalid_password_description), MIN_PASSWORD_LENGTH);
            mNotificationDialog = GameFunctions.showDialog(SignInActivity.this, getString(R.string.invalid_password_title), message);
            mSignInFragment.setBusy(false);
            return;
        }

        showProgressDialog(getString(R.string.signing_in));
        mSignInFragment.emailSignIn(email, password);
    }

    private void createAccount()
    {
        String email = mEmailView.getText().toString().toLowerCase();
        String password = mPasswordView.getText().toString();
        String confirmPassword = mConfirmPasswordView.getText().toString();

        if (!validateEmail(email))
        {
            mNotificationDialog = GameFunctions.showDialog(SignInActivity.this, getString(R.string.invalid_email_title),
                                                           getString(R.string.invalid_email_description));
            mSignInFragment.setBusy(false);
            return;
        }

        if (password.length() < MIN_PASSWORD_LENGTH)
        {
            String message = String.format(getString(R.string.invalid_password_description), MIN_PASSWORD_LENGTH);
            mNotificationDialog = GameFunctions.showDialog(SignInActivity.this, getString(R.string.invalid_password_title), message);
            mSignInFragment.setBusy(false);
            return;
        }

        if (!password.equals(confirmPassword))
        {
            mNotificationDialog = GameFunctions.showDialog(SignInActivity.this, getString(R.string.password_mismatch_title),
                                                           getString(R.string.password_mismatch_description));
            mSignInFragment.setBusy(false);
            return;
        }

        showProgressDialog(getString(R.string.creating_account));
        mSignInFragment.createAccount(email, password);
    }

    private void updateUserSettings()
    {
        mDisplayName = mNameView.getText().toString().trim();

        if (mDisplayName.isEmpty() || mDisplayName.length() > GameConstants.MAX_PLAYER_NAME_LENGTH)
        {
            String message = String.format(getString(R.string.invalid_name_description), 1, GameConstants.MAX_PLAYER_NAME_LENGTH);
            mNotificationDialog = GameFunctions.showDialog(SignInActivity.this, getString(R.string.invalid_name_title), message);
            mSignInFragment.setBusy(false);
            return;
        }

        boolean hideEmailPrefix = mHideEmailPrefixView.isChecked();
        boolean hideFacebookName = mHideFacebookNameView.isChecked();
        boolean hideFacebookPicture = mHideFacebookPictureView.isChecked();
        boolean friendsOnly = mFriendsOnlyView.isChecked();

        showProgressDialog(getString(R.string.updating_user));
        mSignInFragment.updateUserSettings(mId, mDisplayName, hideEmailPrefix, hideFacebookName, hideFacebookPicture, friendsOnly);
    }

    private void sendPasswordEmail()
    {
        String email = mEmailView.getText().toString().toLowerCase();

        if (!validateEmail(email))
        {
            mNotificationDialog = GameFunctions.showDialog(SignInActivity.this, getString(R.string.invalid_email_title),
                                                           getString(R.string.invalid_email_description));
            mSignInFragment.setBusy(false);
            return;
        }

        showProgressDialog(getString(R.string.sending_email));
        mSignInFragment.sendPasswordEmail(email);
    }

    private void showProgressDialog(String progressText)
    {
        mSignInFragment.setProgressText(progressText);
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(progressText);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    private void dismissProgressDialog()
    {
        mSignInFragment.setProgressText(null);

        if (mProgressDialog != null)
            mProgressDialog.dismiss();
    }

    private boolean validateEmail(CharSequence email)
    {
        return (!TextUtils.isEmpty(email) &&
                 android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches());
    }

    public void onCreateAccountSuccess(String id, String fullName, String displayName, String email, String pictureUrl, boolean hideEmailPrefix, boolean hideFacebookName, boolean hideFacebookPicture, boolean friendsOnly, int type, int wins, int losses, int submits, int autoPickSkips)
    {
        onSignInSuccess(id, fullName, displayName, email, pictureUrl, hideEmailPrefix, hideFacebookName, hideFacebookPicture, friendsOnly, type, wins, losses, submits, autoPickSkips);
    }

    public void onCreateAccountFailure(String message)
    {
        dismissProgressDialog();
        mNotificationDialog = GameFunctions.showDialog(this, getString(R.string.unable_to_create_account), message);
        mSignInFragment.setBusy(false);
    }

    public void onSignInSuccess(String id, String fullName, String displayName, String email, String pictureUrl, boolean hideEmailPrefix, boolean hideFacebookName, boolean hideFacebookPicture, boolean friendsOnly, int type, int wins, int losses, int submits, int autoPickSkips)
    {
        mId = id;
        mFullName = fullName;
        mDisplayName = displayName;
        mEmail = email;
        mPictureUrl = pictureUrl;
        mType = type;
        mWins = wins;
        mLosses = losses;
        mSubmits = submits;
        mAutoPickSkips = autoPickSkips;

        dismissProgressDialog();
        mSignInFragment.setBusy(false);

        mNameView.setText(mDisplayName);
        showUserSettingsPanel(hideEmailPrefix, hideFacebookName, hideFacebookPicture, friendsOnly);
    }

    public void onSignInFailure(String message)
    {
        dismissProgressDialog();
        mNotificationDialog = GameFunctions.showDialog(this, getString(R.string.unable_to_sign_in), message);
        mSignInFragment.setBusy(false);
    }

    public void onUpdateUserSettingsSuccess()
    {
        dismissProgressDialog();

        boolean hideEmailPrefix = mHideEmailPrefixView.isChecked();
        boolean hideFacebookName = mHideFacebookNameView.isChecked();
        boolean hideFacebookPicture = mHideFacebookPictureView.isChecked();
        boolean friendsOnly = mFriendsOnlyView.isChecked();

        // Persist user to SharedPreferences.
        User.set(this, mId, mFullName, mDisplayName, mEmail, mPictureUrl, hideEmailPrefix, hideFacebookName, hideFacebookPicture, friendsOnly, mType, mWins, mLosses, mSubmits, mAutoPickSkips);

        setResult(RESULT_OK);
        finish();
    }

    public void onUpdateUserSettingsFailure(String message)
    {
        dismissProgressDialog();
        mNotificationDialog = GameFunctions.showDialog(this, getString(R.string.unable_to_update_user), message);
        mSignInFragment.setBusy(false);
        showSignInPanel();
    }

    public void onSendPasswordEmailSuccess()
    {
        dismissProgressDialog();
        mNotificationDialog = GameFunctions.showDialog(this, getString(R.string.password_reset_title),
                                                       getString(R.string.password_reset_description));
        mSignInFragment.setBusy(false);
    }

    public void onSendPasswordEmailFailure(String message)
    {
        dismissProgressDialog();
        mNotificationDialog = GameFunctions.showDialog(this, getString(R.string.unable_to_send_email), message);
        mSignInFragment.setBusy(false);
    }

    private class ButtonClickListener implements View.OnClickListener
    {
        @Override
        public void onClick(View view)
        {
            if (!mSignInFragment.isBusy())
            {
                mSignInFragment.setBusy(true);
                GameFunctions.hideKeyboard(SignInActivity.this);

                switch (view.getId())
                {
                    case R.id.sign_in_button:
                        signIn();
                        break;
                    case R.id.register_button:
                        showRegisterPanel();
                        mSignInFragment.setBusy(false);
                        break;
                    case R.id.create_account_button:
                        createAccount();
                        break;
                    case R.id.forgot_password_button:
                        sendPasswordEmail();
                        break;
                    case R.id.facebook_sign_in_button:
                        mSignInFragment.facebookSignIn();
                        break;
                    case R.id.done_button:
                        updateUserSettings();
                }
            }
        }
    }
}
