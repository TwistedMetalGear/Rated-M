package net.silentbyte.ratedm.fragments;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import net.silentbyte.ratedm.ToggleLog;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class ImageSearchFragment extends Fragment
{
    private static final String TAG = "ImageFragment";
    private static final String HOST = "https://www.google.com";

    private CallbackHandler mCallbackHandler;
    private boolean mSearching = false;
    private int mSearchId = 0;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
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

    public boolean isSearching()
    {
        return mSearching;
    }

    public void setSearching(boolean searching)
    {
        mSearching = searching;
    }

    public int getSearchId()
    {
        return mSearchId;
    }

    public void incrementSearchId()
    {
        mSearchId++;
    }

    public void search(final String text)
    {
        final int searchId = ++mSearchId;

        if (text.startsWith("http://") || text.startsWith("https://") || text.startsWith("ftp://"))
        {
            Glide.with(getContext())
                .load(text)
                .listener(new RequestListener<String, GlideDrawable>()
                {
                    @Override
                    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource)
                    {
                        ArrayList<String> urls = new ArrayList<String>();

                        if (mCallbackHandler != null)
                            mCallbackHandler.onImageSearchFailure(searchId);

                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource)
                    {
                        ArrayList<String> urls = new ArrayList<String>();
                        urls.add(text);

                        if (mCallbackHandler != null)
                            mCallbackHandler.onImageSearchSuccess(urls, searchId);

                        return false;
                    }
                }).preload();
        }
        else
        {
            try
            {
                String encodedText = URLEncoder.encode(text, "UTF-8");
                HttpRequestTask task = new HttpRequestTask(HOST + "/search?q=" + encodedText + "&tbm=isch", searchId);
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            catch (UnsupportedEncodingException e)
            {
                ToggleLog.e(TAG, "Unable to search for images. Exception: " + e.getMessage());

                if (mCallbackHandler != null)
                    mCallbackHandler.onImageSearchFailure(searchId);
            }
        }
    }

    private class HttpRequestTask extends AsyncTask<String, Void, Boolean>
    {
        String targetUrl;
        String response = "";
        ArrayList<String> urls;
        int searchId;
        boolean success = true;

        public HttpRequestTask(String targetUrl, int searchId)
        {
            super();
            this.targetUrl = targetUrl;
            urls = new ArrayList<String>();
            this.searchId = searchId;
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

                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:49.0) Gecko/20100101 Firefox/49.0");

                // Get response.
                int responseCode = connection.getResponseCode();

                if (responseCode == 200)
                {
                    InputStream is = connection.getInputStream();

                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    StringBuffer buffer = new StringBuffer();
                    String line;

                    while ((line = br.readLine()) != null)
                    {
                        buffer.append(line);
                    }

                    br.close();

                    response = buffer.toString();

                    String regEx = "((?<=\"ou\":\")[^\"]*)";
                    Pattern pattern = Pattern.compile(regEx);
                    Matcher matcher = pattern.matcher(response);

                    while (matcher.find())
                    {
                        for (int i = 1; i <= matcher.groupCount(); i++)
                        {
                            urls.add(unescape(matcher.group(i)));
                        }
                    }
                }
                else
                {
                    ToggleLog.e(TAG, "Unable to search for images. Response code: " + responseCode);
                    success = false;
                }
            }
            catch (Exception e)
            {
                ToggleLog.e(TAG, "Unable to search for images. Exception: " + e.getMessage());
                success = false;
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
            if (success)
            {
                if (mCallbackHandler != null)
                    mCallbackHandler.onImageSearchSuccess(urls, searchId);
            }
            else
            {
                if (mCallbackHandler != null)
                    mCallbackHandler.onImageSearchFailure(searchId);
            }
        }

        public String unescape(String text)
        {
            StringBuilder sb = new StringBuilder(text.length());

            for (int i = 0; i < text.length(); i++)
            {
                char ch = text.charAt(i);
                if (ch == '\\')
                {
                    char nextChar = (i == text.length() - 1) ? '\\' : text.charAt(i + 1);

                    // Octal escape?
                    if (nextChar >= '0' && nextChar <= '7')
                    {
                        String code = "" + nextChar;
                        i++;

                        if ((i < text.length() - 1) && text.charAt(i + 1) >= '0' && text.charAt(i + 1) <= '7')
                        {
                            code += text.charAt(i + 1);
                            i++;

                            if ((i < text.length() - 1) && text.charAt(i + 1) >= '0' && text.charAt(i + 1) <= '7')
                            {
                                code += text.charAt(i + 1);
                                i++;
                            }
                        }

                        sb.append((char) Integer.parseInt(code, 8));
                        continue;
                    }

                    switch (nextChar)
                    {
                        case '\\':
                            ch = '\\';
                            break;
                        case 'b':
                            ch = '\b';
                            break;
                        case 'f':
                            ch = '\f';
                            break;
                        case 'n':
                            ch = '\n';
                            break;
                        case 'r':
                            ch = '\r';
                            break;
                        case 't':
                            ch = '\t';
                            break;
                        case '\"':
                            ch = '\"';
                            break;
                        case '\'':
                            ch = '\'';
                            break;
                        // Hex Unicode: u????
                        case 'u':
                            if (i >= text.length() - 5)
                            {
                                ch = 'u';
                                break;
                            }

                            int code = Integer.parseInt("" + text.charAt(i + 2) + text.charAt(i + 3) +
                                                        text.charAt(i + 4) + text.charAt(i + 5), 16);
                            sb.append(Character.toChars(code));
                            i += 5;
                            continue;
                    }

                    i++;
                }

                sb.append(ch);
            }

            return sb.toString();
        }
    }

    public interface CallbackHandler
    {
        public void onImageSearchSuccess(ArrayList<String> urls, int searchId);
        public void onImageSearchFailure(int searchId);
    }
}
