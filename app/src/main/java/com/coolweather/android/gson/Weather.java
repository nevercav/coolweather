package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Weather {
    @SerializedName("basic")
    public Basic basic;
    @SerializedName("status")
    public String statues;
    @SerializedName("aqi")
    public AQI aqi;
    @SerializedName("now")
    public Now now;
    @SerializedName("suggestion")
    public Suggestion suggestion;

    @SerializedName("daily_forecast")
    public List<Forecast> forecastList;

}
