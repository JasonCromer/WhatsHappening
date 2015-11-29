package com.dev.cromer.jason.whatshappening.networking;


import android.content.Context;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.dev.cromer.jason.whatshappening.objects.MarkerLikesPostRequestParams;

import org.json.JSONException;
import org.json.JSONObject;


public class VolleyPostRequest implements Response.Listener<JSONObject>, Response.ErrorListener{

    //Application Context to persist RequestQueue and perform Toast messages
    private Context applicationContext;


    //A TAG for our String request object
    private static final String STRING_REQUEST_TAG = "PostRequest";

    //String request object
    private JsonObjectRequest jsonRequest;

    //Optional object for updating marker likes
    private MarkerLikesPostRequestParams markerLikesObject;

    //Booleans to determine POST type
    private boolean isMarkerUpdate = false;


    public VolleyPostRequest(Context appContext){
        this.applicationContext = appContext;
        isMarkerUpdate = true;
    }


    @Override
    public void onErrorResponse(VolleyError error) {
        //Convert our error to a NetworkResponse to get more details
        NetworkResponse errorResponse = error.networkResponse;
        String errorResponseString = "Sorry, we ran into some network issues";

        if(errorResponse != null && errorResponse.data != null){
            Toast.makeText(applicationContext, errorResponseString, Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onResponse(JSONObject response) {
    }


    public JsonObjectRequest getRequestObject(MarkerLikesPostRequestParams params){
        markerLikesObject = params;
        //Create our request object (Json object)
        createRequestObject();

        return jsonRequest;
    }

    private void createRequestObject(){

        //Create a new json object
        JSONObject jsonObj = new JSONObject();
        String url = null;

        //Edit json object accordingly if we're updating likes
        if(isMarkerUpdate){
            url = markerLikesObject.getUrl();
            String likeType = markerLikesObject.getLikeType();

            try {
                jsonObj.put("likeType", likeType);
            }
            catch (JSONException e){
                e.printStackTrace();
            }
        }

        //If we've supplied a non-null url, create the jsonRequest object
        if(url != null) {
            jsonRequest = new JsonObjectRequest(Request.Method.POST, url, jsonObj, this, this);
            jsonRequest.setTag(STRING_REQUEST_TAG);
        }
    }

}