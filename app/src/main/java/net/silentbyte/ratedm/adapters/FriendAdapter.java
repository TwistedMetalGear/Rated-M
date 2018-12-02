package net.silentbyte.ratedm.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import net.silentbyte.ratedm.R;
import net.silentbyte.ratedm.User;

import java.util.List;

public class FriendAdapter extends ArrayAdapter<User>
{
    private static final String TAG = "FriendAdapter";
    private LayoutInflater mInflater;
    private boolean mShowCheckbox;

    // JS: Is there a better way to get the activity rather than passing it to the constructor?
    public FriendAdapter(Context context, List<User> friends, boolean showCheckbox)
    {
        super(context, 0, friends);
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mShowCheckbox = showCheckbox;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        // If we weren't given a view, inflate one.
        if (convertView == null)
        {
            convertView = mInflater.inflate(R.layout.list_item_friend, parent, false);
        }

        User friend = getItem(position);

        ImageView friendPicture = (ImageView)convertView.findViewById(R.id.friend_picture);
        TextView friendName = (TextView)convertView.findViewById(R.id.friend_name);
        TextView friendDisplayName = (TextView)convertView.findViewById(R.id.friend_display_name);
        TextView friendOpenForInvites = (TextView)convertView.findViewById(R.id.friend_open_for_invites);
        TextView friendWins = (TextView)convertView.findViewById(R.id.friend_wins);
        TextView friendLosses = (TextView)convertView.findViewById(R.id.friend_losses);
        TextView friendActivePercentage = (TextView)convertView.findViewById(R.id.friend_active_percentage);

        if (friend.getPictureUrl().isEmpty() || (friend.getType() == User.TYPE_FACEBOOK && friend.hideFacebookPicture()))
        {
            Glide.clear(friendPicture);
            friendPicture.setImageResource(R.drawable.no_picture);
        }
        else
        {
            Glide.with(getContext())
                .load(friend.getPictureUrl())
                .fitCenter()
                .into(friendPicture);
        }

        if ((friend.getType() == User.TYPE_EMAIL && friend.hideEmailPrefix()) ||
            (friend.getType() == User.TYPE_FACEBOOK && friend.hideFacebookName()))
        {
            friendName.setText(friend.getDisplayName());
            friendDisplayName.setText("");
            friendDisplayName.setVisibility(View.GONE);
        }
        else
        {
            friendName.setText(friend.getFullName());
            friendDisplayName.setText(friend.getDisplayName());
            friendDisplayName.setVisibility(View.VISIBLE);
        }

        if (friend.playsWithFriendsOnly())
            friendOpenForInvites.setText(getContext().getString(R.string.closed_for_invites));
        else
            friendOpenForInvites.setText(getContext().getString(R.string.open_for_invites));

        friendWins.setText(" " + friend.getWins());
        friendLosses.setText(" " + friend.getLosses());

        if (friend.getActivePercentage() == -1)
            friendActivePercentage.setText("");
        else
            friendActivePercentage.setText(friend.getActivePercentage() + "% Active");

        // Hide checkbox if necessary.
        if (!mShowCheckbox)
        {
            View checkbox = convertView.findViewById(R.id.checkbox);
            checkbox.setVisibility(View.GONE);
        }

        return convertView;
    }
}
