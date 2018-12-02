package net.silentbyte.ratedm.adapters;

import android.content.Context;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import net.silentbyte.ratedm.R;
import net.silentbyte.ratedm.ToggleLog;
import net.silentbyte.ratedm.activities.GameActivity;
import net.silentbyte.ratedm.ChatMessage;

import java.util.List;

public class ChatAdapter extends ArrayAdapter<ChatMessage>
{
    private static final int TYPE_CHAT_DATE = 0;
    private static final int TYPE_CHAT_ROUND_RESULT = 1;
    private static final int TYPE_CHAT_MESSAGE_BLACK = 2;
    private static final int TYPE_CHAT_MESSAGE_WHITE = 3;
    private static final int VIEW_TYPE_COUNT = 4;
    private LayoutInflater mInflater;

    public ChatAdapter(Context context, List<ChatMessage> messages)
    {
        super(context, 0, messages);
        mInflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getItemViewType(int position)
    {
        ChatMessage message = getItem(position);
        String currentParticipantId = ((GameActivity)getContext()).getPlayerId();

        if (message.getId().isEmpty())
            return TYPE_CHAT_DATE;
        else if (message.getPlayerId().isEmpty())
            return TYPE_CHAT_ROUND_RESULT;
        else if (currentParticipantId.equals(message.getPlayerId()))
            return TYPE_CHAT_MESSAGE_BLACK;
        else
            return TYPE_CHAT_MESSAGE_WHITE;
    }

    @Override
    public int getViewTypeCount()
    {
        return VIEW_TYPE_COUNT;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        int type = getItemViewType(position);
        ChatMessage message = getItem(position);
        ViewHolder holder;

        // If we weren't given a view, inflate one.
        if (convertView == null)
        {
            holder = new ViewHolder();

            switch (type)
            {
                case TYPE_CHAT_DATE:
                    convertView = mInflater.inflate(R.layout.list_item_chat_date, parent, false);
                    holder.chatMonthDay = (TextView)convertView.findViewById(R.id.chat_month_day);
                    break;
                case TYPE_CHAT_ROUND_RESULT:
                    convertView = mInflater.inflate(R.layout.list_item_chat_round_result, parent, false);
                    holder.chatRoundResult = (TextView)convertView.findViewById(R.id.chat_round_result);
                    break;
                case TYPE_CHAT_MESSAGE_BLACK:
                    convertView = mInflater.inflate(R.layout.list_item_chat_message_black, parent, false);
                    holder.chatBubbleBlack = convertView.findViewById(R.id.chat_bubble_black);
                    holder.chatName = (TextView)holder.chatBubbleBlack.findViewById(R.id.chat_name);
                    holder.chatMessage = (TextView)holder.chatBubbleBlack.findViewById(R.id.chat_message);
                    holder.chatHourMinute = (TextView)holder.chatBubbleBlack.findViewById(R.id.chat_hour_minute);
                    break;
                case TYPE_CHAT_MESSAGE_WHITE:
                    convertView = mInflater.inflate(R.layout.list_item_chat_message_white, parent, false);
                    holder.chatBubbleWhite = convertView.findViewById(R.id.chat_bubble_white);
                    holder.chatName = (TextView)holder.chatBubbleWhite.findViewById(R.id.chat_name);
                    holder.chatMessage = (TextView)holder.chatBubbleWhite.findViewById(R.id.chat_message);
                    holder.chatHourMinute = (TextView)holder.chatBubbleWhite.findViewById(R.id.chat_hour_minute);
            }

            convertView.setTag(holder);
        }
        else
            holder = (ViewHolder)convertView.getTag();

        if (type == TYPE_CHAT_DATE)
            holder.chatMonthDay.setText(message.getMonthDay());
        else if (type == TYPE_CHAT_ROUND_RESULT)
        {
            holder.chatRoundResult.setText(Html.fromHtml(message.getMessage()));
            holder.chatRoundResult.setMovementMethod(LinkMovementMethod.getInstance());
        }
        else
        {
            holder.chatName.setText(message.getName());
            holder.chatMessage.setText(message.getMessage());
            holder.chatHourMinute.setText(message.getHourMinute());
        }

        return convertView;
    }

    private static class ViewHolder
    {
        TextView chatName;
        TextView chatMessage;
        TextView chatMonthDay;
        TextView chatHourMinute;
        TextView chatRoundResult;
        View chatBubbleBlack;
        View chatBubbleWhite;
    }
}
