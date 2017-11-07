package com.example.android.whereru;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static com.example.android.whereru.MainActivity.MY_API_KEY;

/**
 * Helper methods related to requesting and receiving place data from Google Places API.
 */
public class Utils {

    /** Url for place data from Google Places API */
    private static final String PLACES_REQUEST_URL_DETAIL =
            "https://maps.googleapis.com/maps/api/place/details/json?";
    /**
     * Create a private empty constructor.
     */
    private Utils() {}

    public static SinglePlace extractPlaceFromId (String id) {
        // If the JSON string is empty or null - return early.
        if (TextUtils.isEmpty(id)) {
            return null;
        }
        SinglePlace placeFromId = null;

        Uri baseUri = Uri.parse(PLACES_REQUEST_URL_DETAIL);
        Uri.Builder uriBuilder = baseUri.buildUpon();
        uriBuilder.appendQueryParameter("key", MY_API_KEY);
        uriBuilder.appendQueryParameter("placeid", id);

        URL newUrl = createUrl(uriBuilder.toString());
        String placeJSON = null;
        try {
            placeJSON = makeHttpRequest(newUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create a JSONObject from the JSON response string.
        try {
            // Create a JSONObject from the JSON response string.
            JSONObject baseJsonResponse = new JSONObject(placeJSON);
            JSONObject placeDetails = baseJsonResponse.getJSONObject("result");

            JSONObject properties = placeDetails.getJSONObject("geometry");
            JSONObject locationProperties = properties.getJSONObject("location");

            // Extract name, vicinity, latitude and longitude from JSON.
            String name = placeDetails.getString("name");
            String vicinity = placeDetails.getString("vicinity");
            Double lat = locationProperties.getDouble("lat");
            Double lng = locationProperties.getDouble("lng");

            // Create a new {@link SinglePlace} object with the name, latitude and longitude
            // from the JSON response.
            placeFromId = new SinglePlace(name, vicinity, lat, lng);

        }catch (JSONException e) {
            // If an error is thrown when executing any of the above statements in the "try" block,
            // catch the exception here, so the app doesn't crash. Print a log message
            // with the message from the exception.
            Log.e("Utils", "Problem parsing the place JSON results", e);
        }
        return placeFromId;
    }

    /**
     * Return a list of {@link SinglePlace} objects built from parsing a JSON response.
     */
    public static ArrayList<SinglePlace> extractFeatureFromJson(String placeJSON) {
        // If the JSON string is empty or null - return early.
        if (TextUtils.isEmpty(placeJSON)) {
            return null;
        }

        // Create an empty ArrayList for collecting places.
        ArrayList<SinglePlace> places = new ArrayList<>();

        // Try to parse the JSON response string. If there's a problem with the way the JSON
        // is formatted, a JSONException exception object will be thrown.
        // Catch the exception so the app doesn't crash, and print the error message to the logs.
        try {
            // Create a JSONObject from the JSON response string
            JSONObject baseJsonResponse = new JSONObject(placeJSON);

            // Extract the JSONArray associated with the key called "predictions"
            JSONArray placeArray = baseJsonResponse.getJSONArray("predictions");

            // Add all places to the list.
            // If you want only 3 places on the list just uncomment the line below.
            // for (int i = 0; i < 3; i++) {
            // And comment this one:
            for (int i = 0; i < placeArray.length(); i++) {
                // Get a single place at position i within the list of places.
                JSONObject currentPlace = placeArray.getJSONObject(i);
                // Extract place ID.
                String id = currentPlace.getString("place_id");
                // Create new {@link SinglePlace} using extractPlaceFromId method.
                SinglePlace place = extractPlaceFromId(id);
                // Add the new {@link SinglePlace} to the list of places.
                places.add(place);
            }
        } catch (JSONException e) {
            // If an error is thrown when executing any of the above statements in the "try" block,
            // catch the exception here, so the app doesn't crash. Print a log message
            // with the message from the exception.
            Log.e("Utils", "Problem parsing the place JSON results", e);
        }

        // Return the list of earthquakes
        return places;
    }

    /**
     * Returns new URL object from the given string URL.
     */
    private static URL createUrl(String stringUrl) {
        URL url = null;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException e) {
            Log.e("Utils", "Problem building the URL ", e);
        }
        return url;
    }

    /**
     * Make an HTTP request to the given URL and return a String as the response.
     */
    private static String makeHttpRequest(URL url) throws IOException {
        String jsonResponse = "";

        // If the URL is null - return early.
        if (url == null) {return jsonResponse;}

        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(10000 /* milliseconds */);
            urlConnection.setConnectTimeout(15000 /* milliseconds */);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // If the request was successful (response code 200),
            // then read the input stream and parse the response.
            if (urlConnection.getResponseCode() == 200) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            } else {
                Log.e("Utils", "Error response code: " + urlConnection.getResponseCode());
            }
        } catch (IOException e) {
            Log.e("Utils", "Problem retrieving the place JSON results.", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                // Closing the input stream could throw an IOException, which is why
                // the makeHttpRequest(URL url) method signature specifies than an IOException
                // could be thrown.
                inputStream.close();
            }
        }

        Log.d("JSON RESPONSE: ", jsonResponse);
        return jsonResponse;
    }

    /**
     * Convert the {@link InputStream} into a String which contains the
     * whole JSON response from the server.
     */
    private static String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

    /**
     * Query the Google Places API dataset and return a list of {@link SinglePlace} objects.
     */
    public static List<SinglePlace> fetchPlaceData(String requestUrl) {
        // Create URL object
        URL url = createUrl(requestUrl);

        // Perform HTTP request to the URL and receive a JSON response back
        String jsonResponse = null;
        try {
            jsonResponse = makeHttpRequest(url);
        } catch (IOException e) {
            Log.e("Utils", "Problem making the HTTP request.", e);
        }

        // Extract relevant fields from the JSON response and create a list of places.
        List<SinglePlace> places = extractFeatureFromJson(jsonResponse);

        // Return the list of places.
        return places;
    }
}
