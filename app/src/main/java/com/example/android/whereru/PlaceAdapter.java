package com.example.android.whereru;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class PlaceAdapter extends ArrayAdapter<SinglePlace>{

    // Adapter constructor
    public PlaceAdapter (Context context, ArrayList<SinglePlace> places) {
        super(context, 0, places);
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Check if there is an existing list item view (convertView).
        // If convertView is null, then set a new list item layout.
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.place_list_item, parent, false);
        }

        // Set tint to a pin icon (for Android API <21).
        Drawable normalDrawable = ContextCompat.getDrawable(getContext(), R.drawable.pin);
        Drawable wrapDrawable = DrawableCompat.wrap(normalDrawable);
        DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(getContext(),R.color.colorAccent));

        // Find the place at the given position in the list of places.
        SinglePlace currentPlace = getItem(position);

        // Find the TextView with view ID and set place name to it.
        TextView placeName = (TextView) listItemView.findViewById(R.id.place_name);
        placeName.setText(currentPlace.getName());

        // Find the TextView with view ID and set vicinity to it.
        TextView placeVicinty = (TextView) listItemView.findViewById(R.id.place_vicinty);
        placeVicinty.setText(currentPlace.getVicinity());

        // Return the list item view.
        return listItemView;
    }
}
