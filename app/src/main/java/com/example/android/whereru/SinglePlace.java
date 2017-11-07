package com.example.android.whereru;

public class SinglePlace {
    private String mName;
    private String mVicinity;
    private Double mLatitude;
    private Double mLongitude;

    /**
     * Constructs a new {@link SinglePlace} object.
     * @param name is a name of the place
     * @param vicinity is an address info of the place
     * @param latitude is a latitude of the place
     * @param longitude is a longitute of the place
     */
    public SinglePlace(String name, String vicinity,
                       Double latitude, Double longitude) {
        mName = name;
        mVicinity = vicinity;
        mLatitude = latitude;
        mLongitude = longitude;
    }

    /* Getters */
    public Double getLatitude() {return mLatitude;}
    public Double getLongitude() {return mLongitude;}
    public String getName() {return mName;}
    public String getVicinity() {return mVicinity;}
}
