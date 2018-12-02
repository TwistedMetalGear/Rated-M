package net.silentbyte.ratedm;

import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.net.ssl.HttpsURLConnection;

import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

public class RatedMServer
{
    private static final String TAG = "RatedMServer";
    private static final String HOST = "SERVER_URL";

    // TODO: Remove me when done testing.
    public static void index(CallbackHandler handler)
    {
        HttpRequestTask task = new HttpRequestTask(HOST, "GET", "", handler);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void getUsers(String name, CallbackHandler handler)
    {
        try
        {
            name = URLEncoder.encode(name, "UTF-8");
            HttpRequestTask task = new HttpRequestTask(HOST + "/users?like=" + name, "GET", "", handler);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        catch (UnsupportedEncodingException e)
        {
            ToggleLog.e(TAG, "Unable to get users. UnsupportedEncodingException: " + e.getMessage());
            handler.onResponse(-1, "");
        }
    }

    public static void getFacebookUsers(List<String> ids, CallbackHandler handler)
    {
        String body = "{\"ids\":" + convertToString(ids) + "}";
        HttpRequestTask task = new HttpRequestTask(HOST + "/users", "POST", body, handler);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void getAttendees(String matchId, CallbackHandler handler)
    {
        HttpRequestTask task = new HttpRequestTask(HOST + "/matches/" + matchId + "/attendees", "GET", "", handler);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void getMatches(String playerId, CallbackHandler handler)
    {
        HttpRequestTask task = new HttpRequestTask(HOST + "/matches?player_id=" + playerId + "&version=" + GameConstants.VERSION, "GET", "", handler);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void getMatch(String matchId, CallbackHandler handler)
    {
        HttpRequestTask task = new HttpRequestTask(HOST + "/matches/" + matchId, "GET", "", handler);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void getDecks(CallbackHandler handler)
    {
        HttpRequestTask task = new HttpRequestTask(HOST + "/decks", "GET", "", handler);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void getWinningCards(CallbackHandler handler)
    {
        HttpRequestTask task = new HttpRequestTask(HOST + "/cards/winning", "GET", "", handler);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void getChatMessages(String matchId, int limit, CallbackHandler handler)
    {
        String targetUrl = HOST + "/chat/" + matchId;

        if (limit > 0)
            targetUrl += "?limit=" + limit;

        HttpRequestTask task = new HttpRequestTask(targetUrl, "GET", "", handler);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void createEmailUser(String email, String password, String token, boolean paid, CallbackHandler handler)
    {
        email = JSONObject.quote(email);
        password = JSONObject.quote(password);
        String body = "{\"email\":" + email + ", \"password\":" + password + ", \"tokens\":[\"" + token + "\"], \"paid\":" + paid + "}";
        HttpRequestTask task = new HttpRequestTask(HOST + "/users/email", "POST", body, handler);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void createFacebookUser(String facebookId, String fullName, String pictureUrl, String token, boolean paid, CallbackHandler handler)
    {
        fullName = JSONObject.quote(fullName);
        String body = "{\"facebook_id\":\"" + facebookId + "\", \"full_name\":" + fullName + ", \"picture_url\":\"" + pictureUrl +
                      "\", \"tokens\":[\"" +token + "\"], \"paid\":" + paid + "}";
        HttpRequestTask task = new HttpRequestTask(HOST + "/users/facebook", "POST", body, handler);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void createMatch(String name, int version, boolean autoMatch, int autoMatchSlots, int blackCardType, int whiteCardType, int inactiveMode, int autoPickSkipTimeout, int excludeHoursBegin, int excludeHoursEnd, List<String> decks, List<String> inviteeIds, List<String> playerIds, List<String> names, List<String> pictureUrls, CallbackHandler handler)
    {
        name = JSONObject.quote(name);
        String body = "{\"name\":" + name + ", \"version\":" + version + ", \"auto_match\":" + autoMatch + ", \"auto_match_slots\":" + autoMatchSlots +
                      ", \"black_card_type\":" + blackCardType + ", \"white_card_type\":" + whiteCardType + ", \"inactive_mode\":" + inactiveMode +
                      ", \"auto_pick_skip_timeout\":" + autoPickSkipTimeout +  ", \"exclude_hours_begin\":" + excludeHoursBegin + ", \"exclude_hours_end\":" +
                      excludeHoursEnd + ", \"decks\":" + convertToString(decks) + ", \"invitee_ids\":" + convertToString(inviteeIds) + ", \"player_ids\":" +
                      convertToString(playerIds) + ", " + "\"names\":" + convertToString(names) + ", \"picture_urls\":" + convertToString(pictureUrls) + "}";
        HttpRequestTask task = new HttpRequestTask(HOST + "/matches", "POST", body, handler);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void updateMatchSettings(String matchId, String name, int inactiveMode, int autoPickSkipTimeout, int excludeHoursBegin, int excludeHoursEnd, CallbackHandler handler)
    {
        name = JSONObject.quote(name);
        String body = "{\"name\":" + name + ", \"inactive_mode\":" + inactiveMode + ", \"auto_pick_skip_timeout\":" + autoPickSkipTimeout +
                      ", \"exclude_hours_begin\":" + excludeHoursBegin + ", \"exclude_hours_end\":" + excludeHoursEnd + "}";
        HttpRequestTask task = new HttpRequestTask(HOST + "/matches/" + matchId + "/settings", "PUT", body, handler);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void createChatMessage(String matchId, String playerId, String name, String message, CallbackHandler handler)
    {
        name = JSONObject.quote(name);
        message = JSONObject.quote(message);
        String body = "{\"match_id\":\"" + matchId + "\", \"player_id\":\"" + playerId + "\", \"name\":" + name + ", \"message\":" + message + "}";
        HttpRequestTask task = new HttpRequestTask(HOST + "/chat", "POST", body, handler);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void login(String email, String password, String token, CallbackHandler handler)
    {
        email = JSONObject.quote(email);
        password = JSONObject.quote(password);
        String body = "{\"email\":" + email + ", \"password\":" + password + ", \"token\":\"" + token + "\"}";
        HttpRequestTask task = new HttpRequestTask(HOST + "/login", "PUT", body, handler);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void updateToken(String userId, String token, CallbackHandler handler)
    {
        String body = "{\"token\":\"" + token + "\"}";
        HttpRequestTask task = new HttpRequestTask(HOST + "/users/" + userId + "/token", "PUT", body, handler);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void updateUserSettings(String userId, String displayName, boolean hideEmailPrefix, boolean hideFacebookName, boolean hideFacebookPicture, boolean friendsOnly, CallbackHandler handler)
    {
        displayName = JSONObject.quote(displayName);
        String body = "{\"display_name\":" + displayName + ", \"hide_email_prefix\":" + hideEmailPrefix + ", \"hide_facebook_name\":" + hideFacebookName +
                      ", \"hide_facebook_picture\":" + hideFacebookPicture + ", \"friends_only\":" + friendsOnly + "}";
        HttpRequestTask task = new HttpRequestTask(HOST + "/users/" + userId + "/settings", "PUT", body, handler);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void sendPasswordEmail(String email, CallbackHandler handler)
    {
        email = JSONObject.quote(email);
        String body = "{\"email\":" + email + "}";
        HttpRequestTask task = new HttpRequestTask(HOST + "/password", "PUT", body, handler);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void invite(String matchId, int autoMatchSlots, List<String> inviteeIds, CallbackHandler handler)
    {
        String body = "{\"auto_match_slots\":" + autoMatchSlots + ", \"invitee_ids\":" + convertToString(inviteeIds) + "}";
        HttpRequestTask task = new HttpRequestTask(HOST + "/invite/" + matchId, "PUT", body, handler);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void kick(String matchId, int autoMatchRemovals, List<String> kickIds, CallbackHandler handler)
    {
        String body = "{\"auto_match_removals\":" + autoMatchRemovals + ", \"kick_ids\":" + convertToString(kickIds) + "}";
        HttpRequestTask task = new HttpRequestTask(HOST + "/kick/" + matchId, "PUT", body, handler);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void autoMatch(String playerId, int whiteCardType, CallbackHandler handler)
    {
        String body = "{\"player_id\":\"" + playerId + "\", \"white_card_type\":" + whiteCardType + "}";
        HttpRequestTask task = new HttpRequestTask(HOST + "/join/auto", "PUT", body, handler);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void joinMatch(String matchId, String playerId, CallbackHandler handler)
    {
        String body = "{\"player_id\":\"" + playerId + "\"}";
        HttpRequestTask task = new HttpRequestTask(HOST + "/join/" + matchId, "PUT", body, handler);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void forceAutoPickSkip(String matchId, CallbackHandler handler)
    {
        HttpRequestTask task = new HttpRequestTask(HOST + "/force/" + matchId, "PUT", "", handler);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void leaveMatch(String matchId, String playerId, CallbackHandler handler)
    {
        String body = "{\"player_id\":\"" + playerId + "\"}";
        HttpRequestTask task = new HttpRequestTask(HOST + "/leave/" + matchId, "PUT", body, handler);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void updateMatch(String matchId, String playerId, int round, int bet, List<Card> cards, CallbackHandler handler)
    {
        try
        {
            String body = "{\"player_id\":\"" + playerId + "\", \"round\":" + round + ", \"bet\":" + bet + ", \"submitted_cards\":" + cardsToString(cards) + "}";
            HttpRequestTask task = new HttpRequestTask(HOST + "/matches/" + matchId, "PUT", body, handler);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        catch (JSONException e)
        {
            ToggleLog.e(TAG, "Unable to update match. JSONException: " + e.getMessage());
            handler.onResponse(-1, "");
        }
    }

    public static void updateBlackCard(String matchId, String playerId, int round, Card blackCard, CallbackHandler handler)
    {
        try
        {
            String body = "{\"player_id\":\"" + playerId + "\", \"round\":" + round + ", \"black_card\":" + blackCard.toJSON().toString() + "}";
            HttpRequestTask task = new HttpRequestTask(HOST + "/matches/" + matchId + "/black", "PUT", body, handler);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        catch (JSONException e)
        {
            ToggleLog.e(TAG, "Unable to update black card. JSONException: " + e.getMessage());
            handler.onResponse(-1, "");
        }
    }

    public static void react(String matchId, String playerId, String cardId, int reaction, int round, CallbackHandler handler)
    {
        String body = "{\"player_id\":\"" + playerId + "\", \"card_id\":\"" + cardId + "\", \"reaction\":" + reaction + ", \"round\":" + round + "}";
        HttpRequestTask task = new HttpRequestTask(HOST + "/matches/" + matchId + "/react", "PUT", body, handler);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void tossCard(String matchId, String playerId, int round, List<Card> cards, CallbackHandler handler)
    {
        try
        {
            String body = "{\"player_id\":\"" + playerId + "\", \"round\":" + round + ", \"submitted_cards\":" + cardsToString(cards) + "}";
            HttpRequestTask task = new HttpRequestTask(HOST + "/matches/" + matchId + "/toss", "PUT", body, handler);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        catch (JSONException e)
        {
            ToggleLog.e(TAG, "Unable to toss card. JSONException: " + e.getMessage());
            handler.onResponse(-1, "");
        }
    }

    public static void rematch(String matchId, String playerId, CallbackHandler handler)
    {
        String body = "{\"player_id\":\"" + playerId + "\"}";
        HttpRequestTask task = new HttpRequestTask(HOST + "/rematch/" + matchId, "PUT", body, handler);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void deleteToken(String userId, String token, CallbackHandler handler)
    {
        String body = "{\"token\":\"" + token + "\"}";
        HttpRequestTask task = new HttpRequestTask(HOST + "/users/" + userId + "/token", "DELETE", body, handler);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static String convertToString(List<String> list)
    {
        StringBuilder builder = new StringBuilder();

        builder.append("[");

        for (int i = 0; i < list.size(); i++)
        {
            if (i < list.size() - 1)
                builder.append(JSONObject.quote(list.get(i))).append(", ");
            else
                builder.append(JSONObject.quote(list.get(i)));
        }

        builder.append("]");

        return builder.toString();
    }

    private static String cardsToString(List<Card> cards) throws JSONException
    {
        StringBuilder builder = new StringBuilder();

        builder.append("[");

        for (int i = 0; i < cards.size(); i++)
        {
            /*
            if (i < cards.size() - 1)
                builder.append("{_id: \"").append(card.getId()).append("\", deck: \"").append(card.getDeck()).append("\", text: \"").append(card.getText()).append("\"}, ");
            else
                builder.append("{_id: \"").append(card.getId()).append("\", deck: \"").append(card.getDeck()).append("\", text: \"").append(card.getText()).append("\"}");
            */

            Card card = cards.get(i);

            if (i < cards.size() - 1)
                builder.append(card.toJSON().toString()).append(", ");
            else
                builder.append(card.toJSON().toString());
        }

        builder.append("]");

        return builder.toString();
    }

    private static class HttpRequestTask extends AsyncTask<String, Void, Boolean>
    {
        String targetUrl;
        String method;
        String body;
        String response = "";
        int responseCode = -1;

        CallbackHandler handler;

        public HttpRequestTask(String targetUrl, String method, String body, CallbackHandler handler)
        {
            super();
            this.targetUrl = targetUrl;
            this.method = method;
            this.body = body;
            this.handler = handler;
        }

        @Override
        protected Boolean doInBackground(String... params)
        {
            URL url;
            HttpsURLConnection connection = null;

            try
            {
                url = new URL(targetUrl);
                connection = (HttpsURLConnection)url.openConnection();

                if (!method.equals("GET"))
                {
                    connection.setRequestMethod(method);
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("Content-Length", "" + Integer.toString(body.getBytes().length));
                    connection.setRequestProperty("Content-Language", "en-US");

                    connection.setUseCaches(false);
                    connection.setDoInput(true);
                    connection.setDoOutput(true);

                    // Send request.
                    DataOutputStream dos = new DataOutputStream(connection.getOutputStream());

                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(dos, "UTF-8"));
                    bw.write(body);
                    bw.close();

                    dos.close();
                }

                // Get response.
                InputStream is;
                responseCode = connection.getResponseCode();

                if (responseCode >= 200 && responseCode <= 299)
                    is = connection.getInputStream();
                else
                    is = connection.getErrorStream();

                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                StringBuffer buffer = new StringBuffer();
                String line;

                while ((line = br.readLine()) != null)
                {
                    buffer.append(line);
                }

                br.close();

                response = buffer.toString();
            }
            catch (Exception e)
            {
                ToggleLog.e(TAG, "Unable to complete http request. Exception: " + e.getMessage());
            }
            finally
            {
                if (connection != null)
                    connection.disconnect();
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean valid)
        {
            if (handler != null)
                handler.onResponse(responseCode, response);
        }
    }

    public interface CallbackHandler
    {
        public void onResponse(int responseCode, String response);
    }
}
