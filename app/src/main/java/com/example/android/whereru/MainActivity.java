package com.example.android.whereru;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.R.attr.radius;
import static android.os.Build.VERSION_CODES.M;
import static android.view.View.GONE;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LoaderManager.LoaderCallbacks<List<SinglePlace>> {

    /** Tag for the log messages */
    private final String TAG = getClass().getSimpleName();

    /** Constant value for the place loader ID. */
    private static final int PLACE_LOADER_ID = 1;

    private static final int PLACE_PICKER_REQUEST = 1;
    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    private static final int MY_PERMISSION_FINE_LOCATION = 1;
    private static final int ASK_MULTIPLE_PERMISSION_REQUEST_CODE = 1;

    /** The minimum distance to change Updates in meter */
    private static final long MIN_UPDATE_DISTANCE = 10;

    /** The minimum time between updates in milliseconds */
    private static final long MIN_UPDATE_TIME = 1000 * 60 * 1; // 1 minute

    private static final String PLACES_REQUEST_URL_INPUT =
            "https://maps.googleapis.com/maps/api/place/autocomplete/json?";

    /** Key API for Google Places API */
    public static final String MY_API_KEY = "AIzaSyB_RNWd5dunahIbuRnJ4C-A4aIuv2dZ29w";

    /** EditText to search places. */
    private EditText searchPlace;

    private GoogleMap mMap;
    LocationManager mLocationManager;

    /** Location variables. */
    private double latitude;
    private double longitude;

    /** Adapter for the list of places */
    private PlaceAdapter mAdapter;

    /** TextView that is displayed when the list is empty */
    private TextView mEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();

        ListView placeListView = (ListView) findViewById(R.id.listView);

        mEmptyState = (TextView) findViewById(R.id.empty_view);
        placeListView.setEmptyView(mEmptyState);

        // Create an adapter.
        mAdapter = new PlaceAdapter(this, new ArrayList<SinglePlace>());
        // Set adapter to the listview.
        placeListView.setAdapter(mAdapter);

        searchPlace = (EditText) findViewById(R.id.place_search);
        searchPlace.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Restart Loader to refresh searching places.
                getLoaderManager().restartLoader(PLACE_LOADER_ID, null, MainActivity.this).forceLoad();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapG);
        mapFragment.getMapAsync(MainActivity.this);

        // Get system service for location.
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Check the nerwork/gps provider is enable.
        if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            updateLocation(LocationManager.NETWORK_PROVIDER);
        } else if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            updateLocation(LocationManager.GPS_PROVIDER);
        }

        // Get a reference to the ConnectivityManager to check state of network connectivity
        ConnectivityManager cm =
                (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        // Get details on the currently active default data network
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        // If there is a network connection, fetch data
        if (!isConnected) {
            // Update empty state with no connection error message.
            mEmptyState.setText(R.string.no_internet_connection);
        }
    }

    private void updateLocation(final String locationProvider) {
        checkPermission();
        mLocationManager.requestLocationUpdates(locationProvider,
                MIN_UPDATE_TIME, MIN_UPDATE_DISTANCE, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        // Check if there is some provider enabled
                        // and get location from.
                        if (mLocationManager != null) {
                            // Get the last known location.
                            checkPermission();
                            location = mLocationManager
                                    .getLastKnownLocation(locationProvider);
                            // Get the latitude and the longitude.
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                        }

                        LatLng latLng = new LatLng(latitude, longitude);
                        Geocoder geocoder = new Geocoder(getApplicationContext());
                        try {
                            List<android.location.Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);
                            // Make string containing location description (City, Country).
                            String here = addressList.get(0).getLocality() + ", ";
                            here += addressList.get(0).getCountryName();

                            // Add marker to the map - users location.
                            mMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title(here)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                    }

                    @Override
                    public void onProviderEnabled(String provider) {
                    }

                    @Override
                    public void onProviderDisabled(String provider) {
                    }
                });
    }

    // Request permissions for Android Marshmallow.
    @TargetApi(M)
    private void checkPermission(){
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= M) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION},
                        ASK_MULTIPLE_PERMISSION_REQUEST_CODE);
            }
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case ASK_MULTIPLE_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0
                        && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "This app requires location permissions to be granted",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    /*
    * Triggered when map is ready to use.
    * We can add markers, move camera etc.
    */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        try {
            // Style map using a JSON object defined in a raw resource file.
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.style_json));
            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        checkPermission();
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {}
    @Override
    public void onConnectionSuspended(int i) {}
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}

    // LoadManager.LoaderCallbacks methods
    @Override
    public Loader<List<SinglePlace>> onCreateLoader(int i, Bundle args) {
        // Get user's input from edit text.
        String input = searchPlace.getText().toString();
        // Get user's location.
        String location = String.valueOf(latitude) + "," + String.valueOf(longitude);

        // Initialize {@link baseUri} as null for the begining.
        Uri baseUri = null;
        Uri.Builder uriBuilder = null;

        // If there editText isn't empty make {@link Uri}
        // based on {@link PLACES_REQUEST_URL_INPUT}
        if (input.trim().length() != 0) {
            baseUri = Uri.parse(PLACES_REQUEST_URL_INPUT);
            uriBuilder = baseUri.buildUpon();
            // Add parameters to the query.
            uriBuilder.appendQueryParameter("input", input);
            uriBuilder.appendQueryParameter("key", MY_API_KEY);
            uriBuilder.appendQueryParameter("location", location);
            uriBuilder.appendQueryParameter("radius", "10000");
        } else {
            String noInputUtl = PLACES_REQUEST_URL_INPUT + "key=" + MY_API_KEY;
            return new PlaceLoader(this, noInputUtl);
        }

        Log.d("URI STRING:", uriBuilder.toString());
        return new PlaceLoader(this, uriBuilder.toString());
    }

    @Override
    public void onLoadFinished(Loader<List<SinglePlace>> loader, List<SinglePlace> places) {
        // Set empty state text to display there is no places to show.
        mEmptyState.setText(R.string.no_places);

        // Clear the adapter of previous place data
        // and clear map (remove markers).
        mAdapter.clear();
        mMap.clear();

        // If there is a valid list of {@link SinglePlace}, then add them to the adapter's
        // data set. This will trigger the ListView to update.
        // Also add markers to the {@link GoogleMap}
        if (places != null && !places.isEmpty()) {
            mAdapter.addAll(places);

            // Add markers to the map.
            for (int i = 0; i < places.size(); i++) {
                mMap.addMarker(new MarkerOptions()
                        .title(places.get(i).getName())
                        .position(
                                new LatLng(places.get(i).getLatitude(),
                                        places.get(i).getLongitude())));
            }

        }
    }

    @Override
    public void onLoaderReset(Loader<List<SinglePlace>> loader) {
        // Loader reset, so we can clear out our existing data.
        mAdapter.clear();
        mMap.clear();
    }

    /**
     * Loads a list of places by using an AsyncTask to perform the
     * network request to the given URL.
     */
    public static class PlaceLoader extends AsyncTaskLoader<List<SinglePlace>> {
        /** Query URL */
        private String mUrl;

        /**
         * Constructs a new {@link PlaceLoader}.
         *
         * @param context of the activity
         * @param url to load data from
         */
        public PlaceLoader(Context context, String url) {
            super(context);
            mUrl = url;
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }

        /**
         * This is on a background thread.
         */
        @Override
        public List<SinglePlace> loadInBackground() {
            if (mUrl == null) {
                return null;
            }
            // Perform the network request, parse the response, and extract a list of places.
            List<SinglePlace> places = Utils.fetchPlaceData(mUrl);

            return places;
        }
    }
}