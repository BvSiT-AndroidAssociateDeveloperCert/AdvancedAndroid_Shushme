package com.example.android.shushme;

/*
* Copyright (C) 2017 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*  	http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.shushme.bvsit.DbUtils;
import com.example.android.shushme.provider.PlaceContract;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.FetchPhotoResponse;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {



    // Constants
    public static final String TAG = MainActivity.class.getSimpleName();

    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 111;
    //BvS PlacePicker is deprecated private static final int PLACE_PICKER_REQUEST = 123;
    private static final int AUTOCOMPLETE_REQUEST_CODE = 1;


    // Member variables
    private PlaceListAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private PlacesClient mPlacesClient;

    /**
     * Called when the activity is starting
     *
     * @param savedInstanceState The Bundle that contains the data supplied in onSaveInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO (1) Implement a method called refreshPlacesData that:
        // - Queries all the locally stored Places IDs
        // - Calls Places.GeoDataApi.getPlaceById with that list of IDs
        // Note: When calling Places.GeoDataApi.getPlaceById use the same GoogleApiClient created
        // in MainActivity's onCreate (you will have to declare it as a private member)

        //TODO (8) Set the getPlaceById callBack so that onResult calls the Adapter's swapPlaces with the result
        //DEPRECATED (2) call refreshPlacesData in GoogleApiClient's onConnected and in the Add New Place button click event
        //TODO (2) call refreshPlacesData in onCreate and in the Add New Place button click event

        // Set up the recycler view
        mRecyclerView = (RecyclerView) findViewById(R.id.places_list_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new PlaceListAdapter(this,null);  //!!
        mRecyclerView.setAdapter(mAdapter);




        //BvS: See https://developers.google.com/places/android-sdk/start
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.api_key));
        }
        mPlacesClient = Places.createClient(this); //!!
        refreshPlacesData();  //updates the mRecyclerView adapter
    }

    private void refreshPlacesData(){


        Cursor cursor  = getContentResolver().query(PlaceContract.PlaceEntry.CONTENT_URI,null,null,null,null);
        //? Check for null or 0?
        if (cursor==null || cursor.getCount()==0) return;

        List<String> placeIds = new ArrayList<>();
        while (cursor.moveToNext()) {
            placeIds.add(cursor.getString(cursor.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_ID)));
        }

        List<Place.Field> placeFields = new ArrayList<>();
        placeFields.add(Place.Field.NAME);
        placeFields.add(Place.Field.ADDRESS);

        final List<Place> places = new ArrayList<>();

        for (String placeId : placeIds){
            FetchPlaceRequest fetchPlaceRequest = FetchPlaceRequest.builder(placeId,placeFields).build();
            //Or FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeID, placeFields);
            Task<FetchPlaceResponse> fetchPlaceResponseTask = mPlacesClient.fetchPlace(fetchPlaceRequest);

            fetchPlaceResponseTask.addOnSuccessListener(new OnSuccessListener<FetchPlaceResponse>() {
                @Override
                public void onSuccess(FetchPlaceResponse fetchPlaceResponse) {
                    places.add(fetchPlaceResponse.getPlace());
                    mAdapter.swapPlaces(places); //Seems not very efficient?
                }
            });
        }
        Log.d("148 : ", "refreshPlacesData: mPlaces.size() = " + places.size());
    }


    public void onLocationPermissionClicked(View view) {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSIONS_REQUEST_FINE_LOCATION);
                // BvS: PERMISSIONS_REQUEST_FINE_LOCATION is an
                // app-defined int constant. The callback method gets the
                // result of the request. See also https://developer.android.com/training/permissions/requesting
    }

    public void onAddPlaceButtonClicked(View view) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, getString(R.string.need_location_permission_message), Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, getString(R.string.location_permissions_granted_message), Toast.LENGTH_LONG).show();

        // Set the fields to specify which types of place data to
        // return after the user has made a selection.
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME);
        Intent intent = new Autocomplete.IntentBuilder(
                AutocompleteActivityMode.FULLSCREEN, fields)
                .build(this);
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                Log.i(TAG, "Place: " + place.getName() + ", " + place.getId());
                String placeName = place.getName();
                String placeID = place.getId();
                //Test with: Place: Cradle of Aviation Museum, ChIJxVoKjZx9wokRQ-lIKmN9lA0
                ContentValues contentValues = new ContentValues();
                contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_ID, placeID);
                //NB NOT: getContentResolver().insert(PlaceContract.BASE_CONTENT_URI,contentValues);
                getContentResolver().insert(PlaceContract.PlaceEntry.CONTENT_URI, contentValues);
                //refreshPlacesData();
                DbUtils.dumpTable(this,PlaceContract.PlaceEntry.CONTENT_URI); //debug

                refreshPlacesData();
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                // TODO: Handle the error.
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.i(TAG, status.getStatusMessage());
            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Initialize location permissions checkbox
        CheckBox locationPermissions = (CheckBox) findViewById(R.id.location_permission_checkbox);
        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissions.setChecked(false);
        } else {
            locationPermissions.setChecked(true);
            locationPermissions.setEnabled(false);
        }
    }



}
