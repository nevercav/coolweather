package com.coolweather.android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.db.WeatherCity;
import com.coolweather.android.gson.Basic;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.service.AutoUpdateService;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.LitePal;
import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    public SwipeRefreshLayout swipeRefresh;
    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private ImageView weatherImage;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    private ImageView bingPicImg;
    public DrawerLayout drawerLayout;
    private Button navButton;
    private String weatherId;
    //protected Spinner spinner;
    public List<String> mName=new ArrayList<>();
    public List<String> mWeatherId=new ArrayList<>();
    //public ArrayAdapter<String> adapter;
    //ListView cityListView;
    ArrayAdapter<String> listadapter;
    AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT>=21){
            View decorview=getWindow().getDecorView();
            decorview.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    |View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        bingPicImg=(ImageView)findViewById(R.id.bing_pic_img);
        weatherLayout=(ScrollView)findViewById(R.id.weather_layout);
        titleCity=(TextView)findViewById(R.id.title_city);
        titleUpdateTime=(TextView)findViewById(R.id.title_update_time);
        degreeText=(TextView)findViewById(R.id.degree_text);
        weatherInfoText=(TextView)findViewById(R.id.weather_info_text);
        weatherImage=(ImageView)findViewById(R.id.weather_image);
        forecastLayout=(LinearLayout)findViewById(R.id.forecast_layout);
        aqiText=(TextView)findViewById(R.id.aqi_text);
        pm25Text=(TextView)findViewById(R.id.pm25_text);
        comfortText=(TextView)findViewById(R.id.comfort_text);
        carWashText=(TextView)findViewById(R.id.car_wash_text);
        sportText=(TextView)findViewById(R.id.sport_text);
        //spinner=(Spinner)findViewById(R.id.spinner_text);
        //cityListView=(ListView)findViewById(R.id.city_list_view);
        List<WeatherCity> weatherCities= LitePal.findAll(WeatherCity.class);
        for(WeatherCity weatherCity:weatherCities){
            mName.add(weatherCity.getCountyName());
            mWeatherId.add(weatherCity.getWeatherId());
        }
        listadapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,mName);
//        cityListView.setAdapter(listadapter);
//        cityListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                //titleCity.setText(mName.get(i));
//                requestWeather(mWeatherId.get(i));
//            }
//        });
//        cityListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//            @Override
//            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
//                final int k=i;
//                final View v=view;
//                AlertDialog.Builder dialog=new AlertDialog.Builder(view.getContext());
//                dialog.setTitle("删除城市");
//                dialog.setMessage("确认删除该城市？");
//                dialog.setCancelable(false);
//                dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        //String countyName=mName.remove(k);
//                        String weatherId=mWeatherId.remove(k);
//                        LitePal.deleteAll(WeatherCity.class,"weatherId=?",weatherId);
//                        listadapter.notifyDataSetChanged();
//                    }
//                });
//                dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        Toast.makeText(v.getContext(),"删除失败",Toast.LENGTH_SHORT).show();
//                    }
//                });
//                return true;
//            }
//        });
        titleCity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //alertDialog=new AlertDialog.Builder(WeatherActivity.this);
                //final AlertDialog alertDialog=dialog.create();
                View layout=LayoutInflater.from(view.getContext()).inflate(R.layout.choose_city_listview,null);
                ListView cityListView=layout.findViewById(R.id.city_list_view);
                alertDialog=new AlertDialog.Builder(WeatherActivity.this)
                        .setView(layout)
                        .setCancelable(true)
                        .create();
                cityListView.setAdapter(listadapter);
                cityListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        //titleCity.setText(mName.get(i));
                        requestWeather(mWeatherId.get(i));
                        alertDialog.dismiss();
                    }
                });
                cityListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(final AdapterView<?> adapterView, View view, int i, long l) {
                        final int k=i;
                        final View v=view;
                        final AlertDialog.Builder dialog1=new AlertDialog.Builder(view.getContext());
                        dialog1.setTitle("删除城市");
                        dialog1.setMessage("确认删除该城市？");
                        dialog1.setCancelable(false);
                        dialog1.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String countyName=mName.remove(k);
                                String weatherId=mWeatherId.remove(k);
                                LitePal.deleteAll(WeatherCity.class,"weatherId=?",weatherId);
                                listadapter.notifyDataSetChanged();
                                Toast.makeText(v.getContext(),"删除成功",Toast.LENGTH_SHORT).show();
                                //alertDialog.dismiss();
                            }
                        });
                        dialog1.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Toast.makeText(v.getContext(),"删除失败",Toast.LENGTH_SHORT).show();
                                //alertDialog.dismiss();
                            }
                        });
                        dialog1.show();
                        return true;
                    }
                });


                alertDialog.show();
            }
        });

//        adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,mName);
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinner.setAdapter(adapter);
//
//
//        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
//                requestWeather(mWeatherId.get(i));
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> adapterView) {
//
//            }
//        });

        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString= prefs.getString("weather",null);
        if(weatherString!=null){
           Weather weather= Utility.handleWeatherResponse(weatherString);
           weatherId=weather.basic.weatherId;
           showWeatherInfo(weather);
        }else{
            weatherId=getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
        }
//        if(mWeatherId.contains(weatherId)){
//            titleCity.setText(mName.get(mWeatherId.indexOf(weatherId)));
//        }

        String bingPic=prefs.getString("bing_pic",null);
        if(bingPic!=null){
            Glide.with(this).load(bingPic).into(bingPicImg);
        }else{
            loadBingPic();
        }
        swipeRefresh=(SwipeRefreshLayout)findViewById(R.id.swiper_refresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(weatherId);
            }
        });

        drawerLayout=(DrawerLayout)findViewById(R.id.drawer_layout);
        navButton=(Button)findViewById(R.id.nav_button);
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }


    private void loadBingPic(){
        String requestBingPic="http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic=response.body().string();
                SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });
    }

    public void requestWeather(final String mweatherId) {
        String weatherUrl="http://guolin.tech/api/weather?cityid="+
                mweatherId+"&key=fe4ed3f1367d490ca92103ba1533cd92";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText=response.body().string();
                final Weather weather=Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather!=null&&"ok".equals(weather.statues)){
                            weatherId=mweatherId;
                            SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(
                                    WeatherActivity.this).edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                            Intent intent=new Intent(WeatherActivity.this, AutoUpdateService.class);
                            startService(intent);
                        }else{
                            Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                        }
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
        loadBingPic();
    }

    private void showWeatherInfo(Weather weather) {
        String cityName=weather.basic.cityName;
        String updateTime=weather.basic.update.updateTime.split(" ")[1];
        String degree=weather.now.temperature+"℃";
        String weatherInfo=weather.now.more.info;
        String weatherCode="file:///android_asset/"+weather.now.more.code+".png";
        titleCity.setText(cityName);
        //ApplicationInfo appInfo = getApplicationInfo();
        //int resID = getResources().getIdentifier(weatherCode, "assets", appInfo.packageName);
        Glide.with(this).load(weatherCode).into(weatherImage);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        for(Forecast forecast:weather.forecastList){
            View view= LayoutInflater.from(this).inflate(R.layout.forecast_item,forecastLayout,false);
            TextView dateText=(TextView)view.findViewById(R.id.date_text);
            TextView infoText=(TextView)view.findViewById(R.id.info_text);
            TextView maxText=(TextView)view.findViewById(R.id.max_text);
            TextView minText=(TextView)view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }
        if(weather.aqi!=null){
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        String comfort="舒适度："+weather.suggestion.comfort.info;
        String carWash="洗车指数："+weather.suggestion.carWash.info;
        String sport="运动建议："+weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);
    }
}
