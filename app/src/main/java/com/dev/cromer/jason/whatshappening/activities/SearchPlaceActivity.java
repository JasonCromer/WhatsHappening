package com.dev.cromer.jason.whatshappening.activities;

import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import com.dev.cromer.jason.whatshappening.logic.PlaceArrayAdapter;
import com.dev.cromer.jason.whatshappening.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.io.IOException;
import java.util.List;

public class SearchPlaceActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, GoogleApiClient.ConnectionCallbacks,
                                                                        GoogleApiClient.OnConnectionFailedListener, TextView.OnEditorActionListener {

    //Autocomplete search bar
    static GoogleApiClient mGoogleApiClient;
    private static final int GOOGLE_API_CLIENT_ID = 0;
    static AutoCompleteTextView autocompleteTextView;
    private PlaceArrayAdapter placeArrayAdapter;
    private static final LatLngBounds GLOBAL_BOUNDS = new LatLngBounds(new LatLng(-85.0, -180.0), new LatLng(85.0, 180.0));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_place);

        autocompleteTextView = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView);

        setupGoogleApiClient();
        mGoogleApiClient.connect();
        showKeyboard();
        setUpAutocompleteView();
    }


    private void setupGoogleApiClient(){
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .enableAutoManage(this, GOOGLE_API_CLIENT_ID, this)
                .build();
    }


    private void setUpAutocompleteView() {
        autocompleteTextView.setOnItemClickListener(this);
        autocompleteTextView.setOnEditorActionListener(this);

        //Threshold defines the number of characters the user must enter before location querying starts
        autocompleteTextView.setThreshold(3);

        //Instantiate an adapter with search bounds for global locations
        placeArrayAdapter = new PlaceArrayAdapter(this, android.R.layout.simple_list_item_1,
                GLOBAL_BOUNDS, null);
        autocompleteTextView.setAdapter(placeArrayAdapter);
    }



    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(@NonNull PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                Log.e("TAG", "Place query did not complete. Error: " +
                        places.getStatus().toString());
                return;
            }
            // Selecting the first object buffer.
            final Place place = places.get(0);

            //Get address and return a latlng location
            final String address = String.valueOf(place.getAddress());
            LatLng thisAddress = getLocationFromAddress(address);

            if(thisAddress != null) {
                //move camera to latlng location and clear search box
                autocompleteTextView.setText("");

                //Start intent to pass latlng to map activity
                Intent resultIntent = new Intent();
                resultIntent.putExtra("SEARCHED_LOCATION", thisAddress);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        }
    };



    private LatLng getLocationFromAddress(String strAddress){
        Geocoder geocoder = new Geocoder(this);
        final int MAX_LOCATION_RESULTS = 3;

        if(strAddress != null && !strAddress.isEmpty()){
            try{
                List<Address> addressList = geocoder.getFromLocationName(strAddress, MAX_LOCATION_RESULTS);
                if(addressList != null && addressList.size() > 0) {
                    double lat = addressList.get(0).getLatitude();
                    double lng = addressList.get(0).getLongitude();

                    return new LatLng(lat,lng);
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    private void showKeyboard() {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final PlaceArrayAdapter.PlaceAutocomplete item = placeArrayAdapter.getItem(position);
        final String placeId = String.valueOf(item.placeId);
        PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId);
        placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if(actionId == EditorInfo.IME_ACTION_DONE){
            if(autocompleteTextView.getText().toString().isEmpty()){
                //close intent if user presses done on empty input
                finish();
                return false;
            }
            else{
                //Do nothing and keep keyboard up
                finish();
                return true;
            }
        }
        return false;
    }


    @Override
    public void onConnected(Bundle bundle) {
        //Connect our adapter to the googleApiClient
        placeArrayAdapter.setGoogleApiClient(mGoogleApiClient);
    }

    @Override
    public void onConnectionSuspended(int i) {
        //conserve battery
        placeArrayAdapter.setGoogleApiClient(null);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(getApplicationContext(), "Oh no! Looks like we've got some network issues.", Toast.LENGTH_SHORT).show();
    }


    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

}
