package net.silentbyte.ratedm.adapters;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import net.silentbyte.ratedm.R;

public class ReactionAdapter extends ArrayAdapter<String>
{
    private LayoutInflater mInflater;

    public ReactionAdapter(Context context, List<String>reactions)
    {
        super(context, 0, reactions);
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        // If we weren't given a view, inflate one.
        if (convertView == null)
            convertView = mInflater.inflate(R.layout.list_item_reaction, parent, false);

        final ImageView imageView = (ImageView)convertView.findViewById(R.id.image);

        if (position == 0)
        {
            Glide.clear(imageView);
            imageView.setImageResource(android.R.color.transparent);
        }
        else
        {
            String resourceName = "reaction_" + position;

            int resourceId = getContext().getResources().getIdentifier(resourceName, "drawable", getContext().getPackageName());

            Glide.with(getContext())
                    .load(resourceId)
                    .fitCenter()
                    .into(imageView);
        }

        return convertView;
    }
}
