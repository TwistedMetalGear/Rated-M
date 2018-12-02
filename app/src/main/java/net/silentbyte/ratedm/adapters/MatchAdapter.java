package net.silentbyte.ratedm.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import net.silentbyte.ratedm.Match;
import net.silentbyte.ratedm.R;
import net.silentbyte.ratedm.ToggleLog;

import java.util.List;

public class MatchAdapter extends ArrayAdapter<Match>
{
    private static final String TAG = "MatchAdapter";
    private static final String KEY_LAST_CHAT_MESSAGE_ID = "last_chat_message_id_";
    private static final int TYPE_MATCH_DIVIDER = 0;
    private static final int TYPE_MATCH = 1;
    private static final int VIEW_TYPE_COUNT = 2;
    private LayoutInflater mInflater;
    private int mLoadingPosition;

    // TODO: Is there a better way to get the activity rather than passing it to the constructor?
    public MatchAdapter(Context context, List<Match> matches, int loadingPosition)
    {
        super(context, 0, matches);
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mLoadingPosition = loadingPosition;
    }

    @Override
    public int getItemViewType(int position)
    {
        Match match = getItem(position);

        if (match.isDivider())
            return TYPE_MATCH_DIVIDER;
        else
            return TYPE_MATCH;
    }

    @Override
    public int getViewTypeCount()
    {
        return VIEW_TYPE_COUNT;
    }

    @Override
    public boolean isEnabled(int position)
    {
        if (getItem(position).isDivider())
            return false;
        else
            return super.isEnabled(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        int type = getItemViewType(position);
        Match match = getItem(position);
        ViewHolder holder;

        // If we weren't given a view, inflate one.
        if (convertView == null)
        {
            holder = new ViewHolder();

            switch (type)
            {
                case TYPE_MATCH_DIVIDER:
                    convertView = mInflater.inflate(R.layout.list_item_match_divider, parent, false);
                    holder.dividerText = (TextView)convertView.findViewById(R.id.divider_text);
                    break;
                case TYPE_MATCH:
                    convertView = mInflater.inflate(R.layout.list_item_match, parent, false);
                    holder.picture = (ImageView)convertView.findViewById(R.id.picture);
                    holder.nameText = (TextView)convertView.findViewById(R.id.name_text);
                    holder.stateText = (TextView)convertView.findViewById(R.id.status_text);
                    holder.chatBubble = convertView.findViewById(R.id.chat_bubble);
                    holder.progressBar = (ProgressBar)convertView.findViewById(R.id.progress_bar);
            }

            convertView.setTag(holder);
        }
        else
            holder = (ViewHolder)convertView.getTag();

        if (type == TYPE_MATCH_DIVIDER)
            holder.dividerText.setText(match.getDividerText());
        else
        {
            if (match.getPictureUrl().isEmpty())
            {
                Glide.clear(holder.picture);
                holder.picture.setImageResource(R.drawable.no_picture);
            }
            else
            {
                Glide.with(getContext())
                    .load(match.getPictureUrl())
                    .fitCenter()
                    .into(holder.picture);
            }

            holder.nameText.setText(match.getName());
            holder.stateText.setText(match.getStateText());

            if (position == mLoadingPosition)
            {
                holder.chatBubble.setVisibility(View.GONE);
                holder.progressBar.setVisibility(View.VISIBLE);
            }
            else
            {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                String lastChatMessageId = match.getLastChatMessageId();
                String storedLastChatMessageId = prefs.getString(KEY_LAST_CHAT_MESSAGE_ID + match.getId(), "");

                if (!lastChatMessageId.equals(storedLastChatMessageId))
                    holder.chatBubble.setVisibility(View.VISIBLE);
                else
                    holder.chatBubble.setVisibility(View.GONE);

                holder.progressBar.setVisibility(View.GONE);
            }
        }

        return convertView;
    }

    public void setLoadingPosition(int loadingPosition)
    {
        mLoadingPosition = loadingPosition;
    }

    private static class ViewHolder
    {
        ImageView picture;
        TextView nameText;
        TextView stateText;
        TextView dividerText;
        View chatBubble;
        ProgressBar progressBar;
    }
}
