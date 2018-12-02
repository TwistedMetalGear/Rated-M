package net.silentbyte.ratedm.adapters;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import net.silentbyte.ratedm.R;

public class ImageAdapter extends ArrayAdapter<String>
{
    private LayoutInflater mInflater;

    public ImageAdapter(Context context, List<String> urls)
    {
        super(context, 0, urls);
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        // If we weren't given a view, inflate one.
        if (convertView == null)
            convertView = mInflater.inflate(R.layout.list_item_image, parent, false);

        // final View progressBar = convertView.findViewById(R.id.progress_bar);
        final ImageView imageView = (ImageView)convertView.findViewById(R.id.image);

        // progressBar.setVisibility(View.VISIBLE);

        Glide.with(getContext())
            .load(getItem(position))
            .fitCenter()
            .listener(new RequestListener<String, GlideDrawable>()
            {
                @Override
                public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource)
                {
                    // progressBar.setVisibility(View.GONE);
                    return false;
                }

                @Override
                public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource)
                {
                    // progressBar.setVisibility(View.GONE);
                    return false;
                }
            })
            .into(imageView);

        return convertView;
    }
}
