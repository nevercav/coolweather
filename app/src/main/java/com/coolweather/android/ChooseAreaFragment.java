package com.coolweather.android;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;
import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.db.WeatherCity;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.LitePal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment implements View.OnClickListener {
    public static final int LEVEL_PROVINCE=0;
    public static final int LEVEL_CITY=1;
    public static final int LEVEL_COUNTY=2;
    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ImageView locImage;
    private ArrayAdapter<String> adapter;
    private List<String> dataList=new ArrayList<>();

    private List<Province> provinceList;
    private List<City>cityList;
    private List<County> countyList;

    private Province selectedProvince;

    private City selectedCity;

    private int currentLevel;

    public LocationClient mLocationClient;
    private String localCity="";
    public static Context mContext;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.choose_area,container,false);
        titleText=(TextView)view.findViewById(R.id.title_text);
        backButton=(Button)view.findViewById(R.id.back_button);
        listView=(ListView)view.findViewById(R.id.list_view);
        locImage=(ImageView)view.findViewById(R.id.location);
        locImage.setOnClickListener(this);
        adapter=new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
//        if(Build.VERSION.SDK_INT>=21){
//            view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|
//            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
//        }
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(currentLevel==LEVEL_PROVINCE){
                    selectedProvince=provinceList.get(i);
                    queryCities();
                }else if(currentLevel==LEVEL_CITY){
                    selectedCity=cityList.get(i);
                    queryCounties();
                }else if(currentLevel==LEVEL_COUNTY){
                    String weatherId=countyList.get(i).getWeatherId();
                    String countyName=countyList.get(i).getCountyName();
                    List<WeatherCity> weatherCities=LitePal.where("weatherId = ?",weatherId).find(WeatherCity.class);
                    if(weatherCities.isEmpty()) {
                        WeatherCity weatherCity = new WeatherCity();
                        weatherCity.setCountyName(countyName);
                        weatherCity.setWeatherId(weatherId);
                        weatherCity.save();
                    }
                    if(getActivity() instanceof MainActivity) {
                        //List<WeatherCity> weatherCitie=LitePal.findAll(WeatherCity.class);
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id", weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }
                    else if(getActivity()instanceof WeatherActivity){
                        WeatherActivity activity=(WeatherActivity)getActivity();
                        activity.drawerLayout.closeDrawers();
                        activity.swipeRefresh.setRefreshing(true);
                        activity.requestWeather(weatherId);
                        if(weatherCities.isEmpty()) {
                            activity.mName.add(countyName);
                            activity.mWeatherId.add(weatherId);
                            activity.listadapter.notifyDataSetChanged();
                        }
                        //activity.spinner.setSelection(activity.mWeatherId.indexOf(weatherId));
                    }
                }
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(currentLevel==LEVEL_COUNTY){
                    final int k=i;
                    final View v=view;
                    AlertDialog.Builder dialog=new AlertDialog.Builder(view.getContext());
                    dialog.setTitle("添加城市");
                    dialog.setMessage("确认添加该城市？");
                    dialog.setCancelable(false);
                    dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int q) {
                            String weatherId=countyList.get(k).getWeatherId();
                            String countyName=countyList.get(k).getCountyName();
                            WeatherCity weatherCity=new WeatherCity();
                            weatherCity.setCountyName(countyName);
                            weatherCity.setWeatherId(weatherId);
                            weatherCity.save();
//                            SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(v.getContext());
//                            Set<String> weatherStringSet= prefs.getStringSet("weatherset",null);
//                            weatherStringSet.add(weatherId);
//                            SharedPreferences.Editor editor= PreferenceManager.getDefaultSharedPreferences(v.getContext()).edit();
//                            editor.putStringSet("weatherset",weatherStringSet);
//                            editor.apply();
                            WeatherActivity activity=(WeatherActivity)getActivity();
                            activity.mName.add(countyName);
                            activity.mWeatherId.add(weatherId);
                            activity.listadapter.notifyDataSetChanged();
                            Toast.makeText(v.getContext(),"添加成功",Toast.LENGTH_SHORT).show();
                        }
                    });
                    dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Toast.makeText(v.getContext(),"添加失败",Toast.LENGTH_SHORT).show();
                        }
                    });
                    dialog.show();
                    return true;
                }
                return false;
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentLevel==LEVEL_COUNTY){
                    queryCities();
                }else if(currentLevel==LEVEL_CITY){
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }

    private void queryProvinces() {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList= LitePal.findAll(Province.class);
        if(provinceList.size()>0){
            dataList.clear();
            for(Province province:provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_PROVINCE;
        }else{
            String address="http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }
    }

    private void queryFromServer(String address, final String type) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText=response.body().string();
                boolean result=false;
                if("province".equals(type)){
                    result= Utility.handleProvinceResponse(responseText);
                }else if("city".equals(type)){
                    result=Utility.handleCityResponse(responseText,selectedProvince.getId());
                }else if("county".equals(type)){
                    result=Utility.handleCountyResponse(responseText,selectedCity.getId());
                }
                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type)){
                                queryProvinces();
                            }else if("city".equals(type)){
                                queryCities();
                            }else if("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }
        });
    }

    private void closeProgressDialog() {
        if(progressDialog!=null)
            progressDialog.dismiss();
    }

    private void showProgressDialog() {
        if(progressDialog==null){
            progressDialog=new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList=LitePal.where("cityid=?",String.valueOf(selectedCity.getId())).find(County.class);
        if(countyList.size()>0){
            dataList.clear();
            for(County county:countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_COUNTY;
        }else{
            int provinceCode=selectedProvince.getProvinceCode();
            int cityCode=selectedCity.getCityCode();
            String address="http://guolin.tech/api/china/"+provinceCode+"/"+cityCode;
            queryFromServer(address,"county");
        }
    }

    private void queryCities(){
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList=LitePal.where("provinceid=?",String.valueOf(selectedProvince.getId())).find(City.class);
        if(cityList.size()>0){
            dataList.clear();
            for(City city:cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_CITY;
        }else{
            int provinceCode=selectedProvince.getProvinceCode();
            String address="http://guolin.tech/api/china/"+provinceCode;
            queryFromServer(address,"city");
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.location:{
                SDKInitializer.initialize(mContext);
                SDKInitializer.setCoordType(CoordType.GCJ02);
                mLocationClient=new LocationClient(mContext);
                mLocationClient.registerLocationListener(new MyLocationListener());
                List<String> permissionList=new ArrayList<>();
                if(ContextCompat.checkSelfPermission(view.getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED){
                    permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
                }
                if(ContextCompat.checkSelfPermission(view.getContext(),Manifest.permission.READ_PHONE_STATE)
                        !=PackageManager.PERMISSION_GRANTED){
                    permissionList.add(Manifest.permission.READ_PHONE_STATE);
                }
                if(ContextCompat.checkSelfPermission(view.getContext(),Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        !=PackageManager.PERMISSION_GRANTED){
                    permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
                if(!permissionList.isEmpty()){
                    String[] permissions=permissionList.toArray(new String[permissionList.size()]);
                    ChooseAreaFragment.this.requestPermissions(permissions,1);
                }
                else {
                    requestLocation();
                }

            }
            default:
                break;
        }
    }

    private synchronized void requestLocation() {
        LocationClientOption option=new LocationClientOption();
        option.setIsNeedAddress(true);
        mLocationClient.setLocOption(option);
        mLocationClient.start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                readFromCsv();
            }
        }).start();
    }
    private void readFromCsv(){
        InputStream abpath=null ;
       // File file=new File(abpath.toString());
       // FileInputStream fileInputStream;
        Scanner in=null;
        try{
            //fileInputStream=new FileInputStream(file);
            abpath = mContext.getAssets().open("chinacitylist.csv");
            in =new Scanner(abpath,"UTF-8");
            in.nextLine();
            while(in.hasNextLine()){
                String[] lines=in.nextLine().split(",");
                if(lines.length>=3){
                    if (lines[2].equals(localCity)){
                        final String weatherId=lines[0];
                        if(getActivity() instanceof MainActivity) {
                            //List<WeatherCity> weatherCitie=LitePal.findAll(WeatherCity.class);
                            Intent intent = new Intent(getActivity(), WeatherActivity.class);
                            intent.putExtra("weather_id", weatherId);
                            startActivity(intent);
                            getActivity().finish();
                        }
                        else if(getActivity()instanceof WeatherActivity){
                            final WeatherActivity activity=(WeatherActivity)getActivity();
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activity.drawerLayout.closeDrawers();
                                    activity.swipeRefresh.setRefreshing(true);
                                    activity.requestWeather(weatherId);
                                }
                            });
                        }
                        break;
                    }
                }
            }
            Toast.makeText(getContext(),"定位失败，未知地区!",Toast.LENGTH_SHORT).show();
        }catch (Exception e){
            e.printStackTrace();
        }
        finally {
            try {
                if (in != null) in.close();
                if (abpath != null) abpath.close();
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if(grantResults.length>0){
                    for(int result:grantResults){
                        if(result!=PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(getContext(),"定位失败！必须同意所有权限才能定位",Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    requestLocation();
                }
                else {
                    Toast.makeText(getContext(),"发生未知错误",Toast.LENGTH_SHORT).show();
                    return;
                }
                break;
                default:
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mLocationClient.stop();
    }

    private class MyLocationListener extends BDAbstractLocationListener{
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            if(bdLocation.getLocType()==BDLocation.TypeGpsLocation||bdLocation.getLocType()==
                    BDLocation.TypeNetWorkLocation)localCity=bdLocation.getDistrict();
            localCity=localCity.replace("市","");
            localCity=localCity.replace("区","");
            Log.d("ChooseAreaFragment", "onReceiveLocation: "+bdLocation.getLocType()+bdLocation.getDistrict());

        }
    }
}
