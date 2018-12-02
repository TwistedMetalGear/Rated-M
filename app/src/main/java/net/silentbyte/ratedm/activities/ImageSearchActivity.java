package net.silentbyte.ratedm.activities;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import net.silentbyte.ratedm.GameFunctions;
import net.silentbyte.ratedm.R;
import net.silentbyte.ratedm.ToggleLog;
import net.silentbyte.ratedm.adapters.ImageAdapter;
import net.silentbyte.ratedm.fragments.ImageSearchFragment;

import java.util.ArrayList;

public class ImageSearchActivity extends FragmentActivity implements ImageSearchFragment.CallbackHandler
{
    private static final String TAG = "ImageSearchActivity";
    private static final String TAG_IMAGE_SEARCH_FRAGMENT = "image_search_fragment";
    private static final String KEY_URLS = "urls";
    private static final String KEY_IMAGE_LIST_VIEW_STATE = "image_list_view_state";
    private static final String KEY_FIRST_ENTRY = "first_entry";
    private static final String KEY_ERROR_OCCURRED = "error_occurred";
    private static final int SEARCH_DELAY = 1000;

    public static final String KEY_IMAGE_URL = "image_url";

    private ImageSearchFragment mImageSearchFragment;

    private TextView mSearchTextView;
    private View mProgressBar;
    private View mSearchContainer;
    private GridView mImageListView;
    private View mImageListEmptyView;
    private View mErrorView;

    private ImageAdapter mImageAdapter;

    private ArrayList<String> mUrls;

    private boolean mFirstEntry = true;
    private boolean mErrorOccurred = false;

    private Handler mHandler;
    private Runnable mRunnable;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Lock the device in portrait mode.
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_image_search);

        mSearchTextView = (TextView)findViewById(R.id.search_text);
        mSearchTextView.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if (actionId == EditorInfo.IME_ACTION_SEARCH)
                    search(mSearchTextView.getText().toString());

                return true;
            }
        });

        mProgressBar = findViewById(R.id.progress_bar);
        mSearchContainer = findViewById(R.id.search_container);

        mImageListView = (GridView)findViewById(R.id.images);
        mImageListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Intent intent = new Intent();
                intent.putExtra(KEY_IMAGE_URL, mImageAdapter.getItem(position));
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        mImageListEmptyView = findViewById(R.id.image_list_empty);
        mErrorView = findViewById(R.id.error);

        View searchButton = findViewById(R.id.search_button);
        searchButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                search(mSearchTextView.getText().toString());
            }
        });

        Button retryButton = (Button)findViewById(R.id.retry_button);
        retryButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                search(mSearchTextView.getText().toString());
            }
        });

        FragmentManager fm = getSupportFragmentManager();

        // Add sign in fragment to fragment manager.
        mImageSearchFragment = (ImageSearchFragment)fm.findFragmentByTag(TAG_IMAGE_SEARCH_FRAGMENT);

        if (mImageSearchFragment == null)
        {
            mImageSearchFragment = new ImageSearchFragment();
            fm.beginTransaction().add(mImageSearchFragment, TAG_IMAGE_SEARCH_FRAGMENT).commit();
        }

        if (savedInstanceState != null)
        {
            mUrls = savedInstanceState.getStringArrayList(KEY_URLS);
            mFirstEntry = savedInstanceState.getBoolean(KEY_FIRST_ENTRY);
            mErrorOccurred = savedInstanceState.getBoolean(KEY_ERROR_OCCURRED);

            if (mUrls != null && !mImageSearchFragment.isSearching())
            {
                // Restore scroll position.
                Parcelable state = savedInstanceState.getParcelable(KEY_IMAGE_LIST_VIEW_STATE);
                mImageListView.onRestoreInstanceState(state);
            }

            if (mErrorOccurred)
            {
                mProgressBar.setVisibility(View.GONE);
                mErrorView.setVisibility(View.VISIBLE);
            }

            if (mImageSearchFragment.isSearching())
                mProgressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // Placing this here rather than onCreate so that onTextChanged doesn't get triggered upon configuration change.
        mSearchTextView.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(final CharSequence s, int start, int before, int count)
            {
                // Uncomment the following to allow instant search.
                /*
                if (mHandler != null)
                    mHandler.removeCallbacks(mRunnable);
                else
                    mHandler = new Handler();

                mRunnable = new Runnable()
                {
                    public void run()
                    {
                        search(s.toString());
                    }
                };

                mHandler.postDelayed(mRunnable, SEARCH_DELAY);
                */
            }
        });

        if (mFirstEntry)
        {
            mFirstEntry = false;

            // Pop up keyboard upon initial launch of activity.
            mSearchTextView.requestFocus();
        }
        else
        {
            // Request focus to the parent of the search box so that the keyboard doesn't pop up upon subsequent resumes.
            mSearchContainer.requestFocus();
        }

        if (mUrls != null && !mImageSearchFragment.isSearching())
            initializeImageAdapter(mUrls);
    }

    @Override
    public void onPause()
    {
        super.onPause();

        if (mHandler != null)
            mHandler.removeCallbacks(mRunnable);

        GameFunctions.hideKeyboard(this);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putStringArrayList(KEY_URLS, mUrls);
        savedInstanceState.putParcelable(KEY_IMAGE_LIST_VIEW_STATE, mImageListView.onSaveInstanceState());
        savedInstanceState.putBoolean(KEY_FIRST_ENTRY, mFirstEntry);
        savedInstanceState.putBoolean(KEY_ERROR_OCCURRED, mErrorOccurred);
    }

    public void initializeImageAdapter(ArrayList<String> urls)
    {
        mUrls = urls;
        mImageAdapter = new ImageAdapter(this, mUrls);
        mImageListView.setAdapter(mImageAdapter);
        mProgressBar.setVisibility(View.GONE);

        if (!mErrorOccurred && mUrls.isEmpty() && !mSearchTextView.getText().toString().trim().isEmpty())
            mImageListEmptyView.setVisibility(View.VISIBLE);
        else
            mImageListEmptyView.setVisibility(View.GONE);
    }

    public void search(String text)
    {
        text = text.trim();

        mErrorView.setVisibility(View.GONE);
        mErrorOccurred = false;

        if (text.isEmpty())
        {
            ArrayList<String> urls = new ArrayList<String>();
            mImageSearchFragment.incrementSearchId();
            onImageSearchSuccess(urls, mImageSearchFragment.getSearchId());
        }
        else
        {
            mImageListEmptyView.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
            mImageSearchFragment.search(text);
        }

        GameFunctions.hideKeyboard(ImageSearchActivity.this);
    }

    public void onImageSearchSuccess(ArrayList<String> urls, int searchId)
    {
        if (searchId == mImageSearchFragment.getSearchId())
        {
            ToggleLog.d(TAG, "Successfully found images.");

            initializeImageAdapter(urls);
            mImageSearchFragment.setSearching(false);
        }
    }

    public void onImageSearchFailure(int searchId)
    {
        if (searchId == mImageSearchFragment.getSearchId())
        {
            ToggleLog.d(TAG, "Failed to find images.");

            if (mImageAdapter != null)
            {
                mUrls.clear();
                mImageAdapter.notifyDataSetChanged();
            }

            mImageSearchFragment.setSearching(false);
            mProgressBar.setVisibility(View.GONE);
            mErrorView.setVisibility(View.VISIBLE);
            mErrorOccurred = true;
        }
    }
}
