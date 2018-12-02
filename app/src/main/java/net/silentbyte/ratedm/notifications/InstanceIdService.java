package net.silentbyte.ratedm.notifications;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import net.silentbyte.ratedm.RatedMServer;
import net.silentbyte.ratedm.ToggleLog;
import net.silentbyte.ratedm.User;

public class InstanceIdService extends FirebaseInstanceIdService
{
    private static final String TAG = "InstanceIdService";

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onTokenRefresh()
    {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        ToggleLog.d(TAG, "Refreshed token: " + refreshedToken);

        sendRegistrationToServer(refreshedToken);
    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token)
    {
        User user = User.get(getApplicationContext()); // TODO: Is this the right context?

        if (user != null)
        {
            RatedMServer.updateToken(user.getId(), token, new RatedMServer.CallbackHandler()
            {
                @Override
                public void onResponse(int responseCode, String response)
                {
                    if (responseCode == 200)
                        ToggleLog.d(TAG, "Successfully updated token.");
                    else
                        ToggleLog.e(TAG, "Unable to update token. Response code: " + responseCode); // TODO: Set something in shared preferences to retry later?
                }
            });
        }
    }
}
