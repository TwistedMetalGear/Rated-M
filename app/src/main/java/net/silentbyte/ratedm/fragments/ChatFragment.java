package net.silentbyte.ratedm.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import net.silentbyte.ratedm.GameFunctions;
import net.silentbyte.ratedm.R;
import net.silentbyte.ratedm.ToggleLog;
import net.silentbyte.ratedm.activities.GameActivity;
import net.silentbyte.ratedm.activities.SettingsActivity;
import net.silentbyte.ratedm.adapters.ChatAdapter;
import net.silentbyte.ratedm.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment
{
    private static final String TAG = "ChatFragment";
    private static final String KEY_SENDING_MESSAGE = "sending_message";
    private static final long CHAT_UPDATE_INTERVAL = 5000;

    private Handler mHandler;
    private Runnable mRunnable;
    private List<ChatMessage> mChatMessages;
    private ChatAdapter mChatAdapter;
    private ListView mChatMessageList;
    private EditText mChatMessage;
    private View mSendButton;
    private View mChatProgressBar;
    private View mSendProgressBar;
    private boolean mSendingMessage = false;
    private int mPauseCount = 0;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mChatMessages = new ArrayList<ChatMessage>();

        if (savedInstanceState != null)
            mSendingMessage = savedInstanceState.getBoolean(KEY_SENDING_MESSAGE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_chat, null);

        mChatMessageList = (ListView)v.findViewById(R.id.chat_message_list);

        mChatMessage = (EditText)v.findViewById(R.id.chat_message);
        mChatMessage.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if (actionId == EditorInfo.IME_ACTION_SEND)
                    sendChatMessage();

                return true;
            }
        });

        mSendButton = v.findViewById(R.id.send_message_button);
        mSendButton.setOnClickListener(new ButtonClickListener());

        mChatProgressBar = v.findViewById(R.id.chat_progress_bar);
        mSendProgressBar = v.findViewById(R.id.send_progress_bar);

        mChatAdapter = new ChatAdapter(getActivity(), mChatMessages);
        ListView chatMessageList = (ListView)v.findViewById(R.id.chat_message_list);
        chatMessageList.setAdapter(mChatAdapter);
        // chatMessageList.setDivider(null);

        if (mSendingMessage)
            setSendProgressBarVisibility(View.VISIBLE);

        return v;
    }

    @Override
    public void onPause()
    {
        super.onPause();

        if (mPauseCount > 0)
            stopUpdateCycle();

        if (mPauseCount < 1)
            mPauseCount++;
    }

    @Override
    public void onStop()
    {
        super.onStop();
        // onPause should take care of this.
        // stopUpdateCycle();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putBoolean(KEY_SENDING_MESSAGE, mSendingMessage);
    }

    public void setChatMessage(String message)
    {
        mChatMessage.setText(message);
    }

    public void setChatProgressBarVisibility(int visibility)
    {
        mChatProgressBar.setVisibility(visibility);
    }

    public void setSendProgressBarVisibility(int visibility)
    {
        mSendProgressBar.setVisibility(visibility);

        if (visibility == View.VISIBLE)
        {
            mSendButton.setClickable(false);
            mSendButton.setVisibility(View.GONE);
        }
        else
        {
            mSendButton.setClickable(true);
            mSendButton.setVisibility(View.VISIBLE);
        }
    }

    private void sendChatMessage()
    {
        String chatMessage = mChatMessage.getText().toString();

        if (!chatMessage.trim().equals(""))
        {
            mSendingMessage = true;
            setSendProgressBarVisibility(View.VISIBLE);
            ((GameActivity)getActivity()).sendChatMessage(chatMessage);
        }

        mChatMessage.setText("");
        mChatMessageList.setSelection(mChatAdapter.getCount() - 1);

        // Dismiss keyboard if configured to do so.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        if (prefs.getBoolean(SettingsActivity.KEY_DISMISS_KEYBOARD, true))
            GameFunctions.hideKeyboard(getActivity());
    }

    public void updateChatMessages(List<ChatMessage> chatMessages)
    {
        if (!chatMessages.equals(mChatMessages))
        {
            boolean atBottomOfList = true;

            if (mChatAdapter.getCount() > 0)
            {
                atBottomOfList = mChatMessageList.getLastVisiblePosition() == mChatMessageList.getAdapter().getCount() - 1 &&
                                 mChatMessageList.getChildAt(mChatMessageList.getChildCount() - 1).getBottom() <= mChatMessageList.getHeight();
            }

            mChatMessages.clear();
            mChatMessages.addAll(chatMessages);
            mChatAdapter.notifyDataSetChanged();

            if (atBottomOfList)
                mChatMessageList.setSelection(mChatAdapter.getCount() - 1);
        }
    }

    public void clearChatMessages()
    {
        mChatMessages.clear();
        mChatAdapter.notifyDataSetChanged();
        mChatMessage.setText("");
    }

    public void setSendingMessage(boolean sendingMessage)
    {
        mSendingMessage = sendingMessage;
    }

    public void startUpdateCycle()
    {
        ToggleLog.e(TAG, "Starting chat update cycle.");

        // Stop existing update cycle.
        stopUpdateCycle();

        mChatProgressBar.setVisibility(View.VISIBLE);

        mHandler = new Handler();

        mRunnable = new Runnable()
        {
            public void run()
            {
                GameActivity activity = (GameActivity)getActivity();

                // Checking for null here because there was a situation where
                // I was able to get it to be null upon rapid config changes.
                if (activity != null)
                    activity.updateChatMessages();

                mHandler.postDelayed(this, CHAT_UPDATE_INTERVAL);
            }
        };

        mHandler.post(mRunnable);
    }

    public void stopUpdateCycle()
    {
        ToggleLog.e(TAG, "Stopping chat update cycle.");

        if (mHandler != null)
            mHandler.removeCallbacks(mRunnable);
    }

    private class ButtonClickListener implements View.OnClickListener
    {
        @Override
        public void onClick(View view)
        {
            // Show the button animation.
            /*
            Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.pulse);
            anim.reset();
            view.clearAnimation();
            view.startAnimation(anim);
            */

            switch (view.getId())
            {
                case R.id.send_message_button:
                    sendChatMessage();
            }
        }
    }
}
