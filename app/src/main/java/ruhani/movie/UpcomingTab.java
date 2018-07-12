package ruhani.movie;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class UpcomingTab extends Fragment {

    private MyHandler mHandler;
    List<DataList> mList = new ArrayList<DataList>();
    List<DataList> mViewed = new ArrayList<DataList>();
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private RequestQueue mRequestQueue;
    private final String API_KEY = "api_key=5ba3ea53a95300013b3082080d1039dd";
    private final String IMAGE_URL = "http://image.tmdb.org/t/p/w500/";
    private static final String MOVIE_URL = "http://api.themoviedb.org/3/movie/";
    private static final String EXTRA_CREDITS = "&append_to_response=credits";
    private int mPageno;
    private int mTotalPage;

    public UpcomingTab() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.upcoming_movie, container, false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == getActivity().RESULT_OK) {
            Log.e("Ruhani", "Adding");
            DataList item = mList.get(requestCode);
            if(mViewed.contains(item)){
                mViewed.remove(item);
            }
            mViewed.add(0, item);
            if (mViewed.size() > 10) {
                mViewed.remove(10);
            }
            ((MainActivity)getActivity()).setMyList(mViewed);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Log.i("Upcoming Tab", "+onViewCreated");
        super.onViewCreated(view, savedInstanceState);

        mRequestQueue = Volley.newRequestQueue(getActivity().getApplicationContext());
        DiskBasedCache cache = new DiskBasedCache(getActivity().getApplicationContext().getCacheDir(), 16 * 1024 * 1024);
        mRequestQueue = new RequestQueue(cache, new BasicNetwork(new HurlStack()));
        mRequestQueue.start();
        mHandler = new MyHandler();

        mRecyclerView = (RecyclerView) view.findViewById(R.id.my_recycler_view);


        final LinearLayoutManager manager = new LinearLayoutManager(getActivity().getApplicationContext());
        mLayoutManager = manager;
        mRecyclerView.setLayoutManager(manager);
        mAdapter = new MyAdapter(mList);
        mRecyclerView.setAdapter(mAdapter);
        fetchMovieList(MOVIE_URL + "upcoming?" + API_KEY);

        mRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(getActivity().getApplicationContext(),
                        new RecyclerItemClickListener.OnItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {
                                if(position < 0)
                                    return;
                                DataList item = mList.get(position);
                                fetchMovieDetails(item.mId, position);
                            }
                        })
        );
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int visibleItems = mLayoutManager.getChildCount();
                int totalItems = mLayoutManager.getItemCount();
                int firstVisiblePos = manager.findFirstVisibleItemPosition();
                if ( (visibleItems + firstVisiblePos) >= totalItems) {
                    Log.i("Upcoming Tab", "Last fetched page" + (mPageno));
                    fetchMovieList(MOVIE_URL + "upcoming?" + API_KEY + "&page=" +(mPageno+1));
                }
            }
        });
    }


    void fetchMovieList(String url) {
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject json) {
                        try {
                            JSONArray jsonArray = json.getJSONArray("results");
                            int length = jsonArray.length();
                            mTotalPage = json.getInt("total_pages");
                            mPageno = json.getInt("page");
                            Log.i("Upcoming Tab", "Total pages: " + mTotalPage);
                            Log.i("Upcoming Tab", "Current page: " + mPageno);
                            for (int i = 0; i < length; i++) {
                                JSONObject jObj = (JSONObject) jsonArray.get(i);
                                Message msg = mHandler.obtainMessage();
                                msg.obj = jObj;
                                mHandler.sendMessage(msg);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                });

        mRequestQueue.add(jsObjRequest);
    }

    void fetchMovieDetails(int id, final int position) {
        String movieurl = MOVIE_URL + id + "?" + API_KEY + EXTRA_CREDITS;

        JsonObjectRequest movieRequest = new JsonObjectRequest
                (Request.Method.GET, movieurl, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject json) {
                        try {
                            String genres = "";
                            JSONArray genre = json.getJSONArray("genres");

                            for (int i = 0; i < genre.length(); i++) {
                                JSONObject gen = genre.getJSONObject(i);
                                genres = genres + gen.getString("name") + ", ";
                            }
                            if (!genres.isEmpty()) {
                                genres = genres.substring(0, genres.length() - 2);
                            }

                            JSONObject credits = json.getJSONObject("credits");

                            String stars = "";
                            int starslength = 0;
                            JSONArray cast = credits.getJSONArray("cast");
                            if (cast != null) {
                                if (cast.length() > 3) {
                                    starslength = 3;
                                } else {
                                    starslength = cast.length();
                                }
                            }
                            for (int i = 0; i < starslength; i++) {
                                JSONObject star = cast.getJSONObject(i);
                                stars = stars + star.getString("name") + ", ";
                            }
                            if (!stars.isEmpty()) {
                                stars = stars.substring(0, stars.length() - 2);
                            }

                            String director = "";
                            JSONArray crews = credits.getJSONArray("crew");
                            for (int i = 0; i < crews.length(); i++) {
                                JSONObject crew = crews.getJSONObject(i);
                                if (crew.getString("job").equalsIgnoreCase("director")) {
                                    director = crew.getString("name");
                                    break;
                                }
                            }



                            DataList item = mList.get(position);
                            String filename = "bitmap.png";
                            try {
                                FileOutputStream stream = getActivity().getApplicationContext().
                                        openFileOutput(filename, Context.MODE_PRIVATE);
                                item.mIcon.compress(Bitmap.CompressFormat.PNG, 100, stream);
                                stream.close();
                            }catch (IOException e){

                            }
                            Intent intent = new Intent(getActivity().getApplicationContext(), MovieDetails.class);
                            intent.putExtra("Title", item.mTitle);
                            intent.putExtra("Desc", item.mOverView);
                            intent.putExtra("Poster", filename);
                            intent.putExtra("Release", item.mReleaseDate);
                            intent.putExtra("Genre", genres);
                            intent.putExtra("Director", director);
                            intent.putExtra("Stars", stars);
                            startActivityForResult(intent, position);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                });

        mRequestQueue.add(movieRequest);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
    }

    class MyHandler extends android.os.Handler {

        @Override
        public void handleMessage(Message msg) {
            try {
                JSONObject jObj = (JSONObject) msg.obj;
                String image = jObj.getString("poster_path");
                final String imageurl = IMAGE_URL + image;
                final  String release_date =  jObj.getString("release_date");
                final String title = jObj.getString("original_title");
                final String overview =  jObj.getString("overview");
                final int rating = jObj.getInt("popularity");
                final int id = jObj.getInt("id");

                ImageRequest imgRequest = new ImageRequest(imageurl, new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap bitmap) {
                        bitmap = Bitmap.createScaledBitmap(bitmap, (bitmap.getWidth() * 3) / 4, (bitmap.getHeight() * 3) / 4, false);
                        DataList item = new DataList();
                        item.mId = id;
                        item.mTitle = title;
                        item.mReleaseDate = release_date;
                        item.mOverView = overview;
                        item.mRating = rating;
                        item.mIcon = bitmap;
                        mList.add(item);
                        mAdapter.notifyDataSetChanged();
                    }
                }, 0, 0, null, null, new Response.ErrorListener() {
                    public void onErrorResponse(VolleyError error) {
                    }
                });

                mRequestQueue.add(imgRequest);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private byte[] getByteFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }


    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

        private Context mContext;
        private List<DataList> mList;
        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            TextView mTitle;
            TextView mReleaseDate;
            ImageView mImageView;

            public ViewHolder(View v) {
                super(v);
                mImageView = (ImageView)v.findViewById(R.id.poster);
                mTitle = (TextView)v.findViewById(R.id.title);
                mReleaseDate = (TextView)v.findViewById(R.id.releaseDate);
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public MyAdapter(List<DataList> list) {
            mList = list;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent,
                                                       int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.listitem, parent, false);
            // set the view's size, margins, paddings and layout parameter
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            if(position < 0)
                return;
            Log.e("Ruhani", "elements"+ mList.size());
            DataList feedItem = mList.get(position);
            if(feedItem != null) {
                Log.e("Ruhani", "going inside");
                holder.mTitle.setText(feedItem.mTitle);
                holder.mReleaseDate.setText(feedItem.mReleaseDate);
                holder.mImageView.setImageBitmap(feedItem.mIcon);
            }
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return (null != mList ? mList.size() : 0);
        }
    }
}