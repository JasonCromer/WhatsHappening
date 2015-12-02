package com.dev.cromer.jason.whatshappening.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.dev.cromer.jason.whatshappening.R;
import com.dev.cromer.jason.whatshappening.logic.ShareMarkerHandler;
import com.dev.cromer.jason.whatshappening.networking.VolleyPostRequest;
import com.dev.cromer.jason.whatshappening.networking.VolleyQueueSingleton;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MarkerDescriptionActivity extends AppCompatActivity implements View.OnClickListener,
                                EditText.OnEditorActionListener, AbsListView.OnScrollListener, View.OnFocusChangeListener, RequestQueue.RequestFinishedListener<Object> {

    private TextView markerDescriptionTextView;
    private TextView markerLikesTextView;
    private ImageButton likeButton;
    private String markerDescription = "";
    private String markerLikes = "";
    private ListView commentsListView;
    private String markerID;
    private LatLng markerPosition;
    private SharedPreferences preferences;
    private FloatingActionButton floatingActionButton;
    private LayoutInflater layoutInflater;
    private EditText userComment;
    private VolleyPostRequest volleyPostRequest;
    private RequestQueue queue;
    private PopupWindow popupWindow;
    private boolean hasLiked = false;

    //constants
    private static final String DEFAULT_LIKES = "0";
    private static final String UPDATE_LIKES_TAG = "UPDATE_LIKES";
    private static final String UPDATE_COMMENTS_TAG = "UPDATE_COMMENTS";
    private static final String LIKED_STRING = "like";
    private static final String DISLIKED_STRING = "dislike";
    private static final String GET_COMMENTS_ENDPOINT = "http://whatsappeningapi.elasticbeanstalk.com/api/get_comments/";
    private static final String POST_COMMENT_ENDPOINT = "http://whatsappeningapi.elasticbeanstalk.com/api/post_comment/";
    private static final String GET_LIKES_ENDPOINT = "http://whatsappeningapi.elasticbeanstalk.com/api/get_marker_likes/";
    private static final String GET_DESCRIPTION_ENDPOINT = "http://whatsappeningapi.elasticbeanstalk.com/api/get_marker_description/";
    private static final String UPDATE_LIKES_ENDPOINT = "http://whatsappeningapi.elasticbeanstalk.com/api/update_marker_likes/";
    private static final boolean DEFAULT_HAS_LIKED = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marker_description);

        //Instantiate our view objects
        instantiateViewObjects();

        //Instantiate our volley objects for http requests
        instantiateVolleyRequestObjects();

        //Get the ID and position from the marker thats been clicked on, on the map
        markerID = getIntent().getStringExtra("MARKER_ID");
        Bundle bundle = getIntent().getParcelableExtra("MARKER_LOCATION");
        markerPosition = bundle.getParcelable("LATLNG");

        //Get the shared preference to see if user has liked or disliked the post
        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        checkUserLike();

        //getMarkerDescription and likes from database via passed in Marker ID
        getMarkerDescription();
        getMarkerLikes();

        //Display our comments
        getAndDisplayComments(commentsListView);
    }


    private void instantiateViewObjects(){
        markerLikesTextView = (TextView) findViewById(R.id.markerLikesTextView);
        markerDescriptionTextView = (TextView) findViewById(R.id.markerDescriptionTextView);
        likeButton = (ImageButton) findViewById(R.id.likeButton);
        commentsListView = (ListView) findViewById(R.id.commentsListView);
        floatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        likeButton.setOnClickListener(this);
        floatingActionButton.setOnClickListener(this);
        commentsListView.setOnScrollListener(this);
    }


    private void instantiateVolleyRequestObjects(){
        //Start our volley request queue to persists in application lifetime
        queue = VolleyQueueSingleton.getInstance(this.getApplicationContext()).
                getRequestQueue();

        queue.addRequestFinishedListener(this);

        volleyPostRequest = new VolleyPostRequest(getApplicationContext());
    }


    private void getMarkerDescription(){
        final String url = GET_DESCRIPTION_ENDPOINT + markerID;

        StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                markerDescription = response;
                markerDescription = markerDescription.replace("\"", "");

                //Display description in the textView
                displayMarkerDescription();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                displayErrorMessage(error);
            }
        });

        queue.add(request);
    }


    //Display
    private void displayMarkerDescription(){
        if(markerDescription != null){
            markerDescriptionTextView.setText(markerDescription);
        }
        else{
            markerDescriptionTextView.setText("");
        }
    }


    private void getMarkerLikes() {
        //This endpoint points to the markerLikes of the specific post
        final String url = GET_LIKES_ENDPOINT + markerID;

        StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                markerLikes = response;

                //Display the likes in the textView
                displayMarkerLikes();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                displayErrorMessage(error);
            }
        });

        //Add our request to the queue
        queue.add(request);
    }


    private void displayErrorMessage(VolleyError error){
        //Convert error to NetworkResponse to get details
        NetworkResponse errorResponse = error.networkResponse;
        String errorResponseString = "Sorry, we ran into some network issues";

        if(errorResponse != null && errorResponse.data != null){
            Toast.makeText(getApplicationContext(), errorResponseString, Toast.LENGTH_LONG).show();
        }
    }


    private void displayMarkerLikes(){
        if(markerLikes != null){
            //Set our view
            markerLikesTextView.setText(markerLikes);
        }
        else{
            //Set our view
            markerLikesTextView.setText(DEFAULT_LIKES);
        }
    }


    private void updateMarkerLikes(String likeType){
        final String url = UPDATE_LIKES_ENDPOINT + markerID;

        //Create parameter HashMap to hold likeType key and the data (likes)
        HashMap<String, String> paramsMap = new HashMap<>();
        paramsMap.put("likeType", likeType);

        //Create Json request object using our params
        JsonObjectRequest request = volleyPostRequest.getRequestObject(url, paramsMap);

        //Set our tag to identify this Post request
        request.setTag(UPDATE_LIKES_TAG);

        //Add our request object to the Singleton Volley queue
        queue.add(request);
    }


    @Override
    public void onClick(View v) {
        if(v == floatingActionButton){
            //Inflate our custom layout
            inflatePopUpWindow(v);
        }
        if(v == likeButton && !hasLiked) {
            likeButton.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_heart_red));
            updateMarkerLikes(LIKED_STRING);
            saveUserLike();
            hasLiked = true;
        }
        else if(v == likeButton){
            likeButton.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_heart_outline_grey));
            updateMarkerLikes(DISLIKED_STRING);
            saveUserDislike();
            hasLiked = false;
        }
    }

    private void inflatePopUpWindow(View v){

        final int popupMarginX = 0;
        final int popupMarginY = 0;
        final ViewGroup viewGroup = (ViewGroup) findViewById(R.id.popUpLayout);
        final View inflatedView = layoutInflater.inflate(R.layout.pop_up_comment, viewGroup, false);

        //Hide the floating action button
        floatingActionButton.hide();

        //Add listener to our EditText to allow window to close after the "done" button is pressed
        userComment = (EditText) inflatedView.findViewById(R.id.commentEditText);
        userComment.setOnEditorActionListener(this);
        userComment.setOnFocusChangeListener(this);

        //Get device screen size and make a new point that corresponds to it
        Display display = getWindowManager().getDefaultDisplay();
        final Point screenSize = new Point();

        //Assign screensize to our point
        display.getSize(screenSize);

        //Set height depending on screen size
        popupWindow = new PopupWindow(inflatedView, screenSize.x, v.getMeasuredHeight(), true);

        //Set background (Round edges), and make focusable (keyboard on window press)
        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.pop_up_background));
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);


        //Set custom animation (found in values/styles.xml folder
        popupWindow.setAnimationStyle(R.style.Animation);

        //Show the popup at bottom of screen with margin.
        //Params are (View parent, gravity, int x, int y);
        popupWindow.showAtLocation(v, Gravity.BOTTOM, popupMarginX, popupMarginY);
    }

    private void saveUserLike(){
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(markerID, true);
        editor.apply();
    }

    private void saveUserDislike(){
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(markerID, false);
        editor.apply();
    }

    private void checkUserLike(){
        final boolean userHasLiked = preferences.getBoolean(markerID, DEFAULT_HAS_LIKED);
        if(userHasLiked){

            //Change image to the filled-in heart image
            likeButton.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_heart_red));
            hasLiked = true;
        }
    }


    private void postNewComment(){
        final String url = POST_COMMENT_ENDPOINT + markerID;

        //Convert our EditText input to a String
        //Replace comma with tilde for GET response processing later
        final String comment = userComment.getText().toString().replaceAll(",","~");

        //Create comment HashMap to hold data
        HashMap<String, String> paramsMap = new HashMap<>();
        paramsMap.put("comment", comment);

        //Create a new request object using our hashmap and url
        JsonObjectRequest request = volleyPostRequest.getRequestObject(url, paramsMap);

        //Tag our request for post-processing
        request.setTag(UPDATE_COMMENTS_TAG);

        //Add our request object to the Singleton Volley queue
        queue.add(request);
    }


    private void getAndDisplayComments(final ListView listView){

        //Retrieve comments from database and assign result to our ArrayList
        final String url = GET_COMMENTS_ENDPOINT + markerID;

        StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                //Format our comment string into a list
                List<String> commentsList = formatCommentList(response);

                //Create an adapter for our listView
                ArrayAdapter adapter = new ArrayAdapter<>(MarkerDescriptionActivity.this,
                        R.layout.comment_item, R.id.commentTextView, commentsList);

                //set our adapter with our list of comments to the listView
                listView.setAdapter(adapter);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                displayErrorMessage(error);
            }
        });

        queue.add(request);
    }

    private List<String> formatCommentList(String input){
        List<String> commentsList = new ArrayList<>();

        if(input != null){
            //We remove the brackets and quotations
            input = input.replace("[", "").replace("]", "").replace("\"", "");

            //Remove any additional white space with the regex \\s*
            commentsList = Arrays.asList(input.split("\\s*, \\s*"));


            //We replaced comma's with tilde's in the POST request, and now we reverse it
            for(int i = 0; i < commentsList.size(); i++){
                if(commentsList.get(i).contains("~")){
                    commentsList.set(i, commentsList.get(i).replace("~", ","));
                }
            }
        }

        return commentsList;
    }

    private void openShareService(){
        //Default text
        final String defaultMessage = "This is Whats Happening: ";

        //Default zoom level for map
        final int zoomLevel = 18;

        //Instatiate new object for creating the Share service
        ShareMarkerHandler shareMarkerHandler = new ShareMarkerHandler(this);

        //Pass in our parameters to share the marker of interest
        shareMarkerHandler.shareMarkerLocation(markerPosition, zoomLevel, defaultMessage +
                "\n" + markerDescription + "\n");
    }

    @Override
    protected void onStop() {
        super.onStop();

        //Destroy all items in our Volley request queue to prevent any crashes from View changes
        VolleyQueueSingleton.getInstance(this).destroyRequestQueue();
    }


    @Override
    public boolean onNavigateUp(){
        //Destroy our activity and return to top of the activity stack
        finish();
        return true;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if(actionId == EditorInfo.IME_ACTION_DONE && !userComment.getText().toString().isEmpty()){

            //Post a new comment if our input isn't empty
            postNewComment();

            //Close our popupWindow
            popupWindow.dismiss();

            return true;
        }
        return false;
    }


    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if(scrollState == SCROLL_STATE_FLING || scrollState == SCROLL_STATE_TOUCH_SCROLL){
            //Hide our floating action bar when scrolling
            floatingActionButton.setVisibility(View.GONE);
        }
        else{
            floatingActionButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.socialMediaShare){
            //open the share service so user can share their marker of interest
            openShareService();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_marker_description, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if(!hasFocus){
            //Show our floating action button (even though it won't be seen until keyboard hides)
            floatingActionButton.show();
        }
    }

    @Override
    public void onRequestFinished(Request<Object> request) {
        if(request.getTag() == UPDATE_LIKES_TAG){

            //Update our likes here to ensure post has finished
            getMarkerLikes();
        }
        if(request.getTag() == UPDATE_COMMENTS_TAG){

            //Update our comments list to refresh newly added comments
            getAndDisplayComments(commentsListView);
        }
    }
}
