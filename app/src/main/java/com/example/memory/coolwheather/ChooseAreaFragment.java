package com.example.memory.coolwheather;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.memory.coolwheather.db.City;
import com.example.memory.coolwheather.db.County;
import com.example.memory.coolwheather.db.Province;
import com.example.memory.coolwheather.gson.Weather;
import com.example.memory.coolwheather.util.HttpUtil;
import com.example.memory.coolwheather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by memory on 2017/12/26.
 */

public class ChooseAreaFragment extends Fragment {

    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY =2;

    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButoon;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();
    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;
    private Province selectProvince;
    private City selectCity;
    private int currentLevel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area,null);
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButoon = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.listView);
        adapter = new ArrayAdapter<String>(getContext(),android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE){
                    selectProvince = provinceList.get(position);
                    queryCity();
                }else if(currentLevel == LEVEL_CITY){
                    selectCity = cityList.get(position);
                    queryConty();
                }else if(currentLevel == LEVEL_COUNTY){
                    String weatherid = countyList.get(position).getWeatherId();
                    if (getActivity() instanceof MainActivity){
                        Intent intent  = new Intent(getActivity(),WeatherActivity.class);
                        intent.putExtra("weather_id",weatherid);
                        Log.d("xxx",weatherid);
                        startActivity(intent);
                        getActivity().finish();
                    }else if (getActivity() instanceof WeatherActivity){
                        WeatherActivity weatherActivity = (WeatherActivity) getActivity();
                        weatherActivity.drawerLayout.closeDrawers();
                        weatherActivity.swipeRefreshLayout.setRefreshing(true);
                        weatherActivity.requestWeather(weatherid);
                    }

                }
            }
        });

        backButoon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel == LEVEL_COUNTY){
                    queryCity();
                }else if(currentLevel == LEVEL_CITY){
                    queryProvice();
                }
            }
        });
        queryProvice();
    }

    public void queryProvice(){
        titleText.setText("中国");
        backButoon.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);
        if(provinceList.size() > 0){
            dataList.clear();
            for (Province province : provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        }else{
            String address = "http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }
    }

    public void queryCity(){
        titleText.setText(selectProvince.getProvinceName());
        backButoon.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid = ?",String.valueOf(selectProvince.getId())).find(City.class);
        if (cityList.size()>0){
            dataList.clear();
            for (City city:cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        }else{
            int provinceCode = selectProvince.getPrivinceCode();
            String address = "http://guolin.tech/api/china/"+provinceCode;
            queryFromServer(address,"city");
        }
    }

    public void queryConty(){
        titleText.setText(selectCity.getCityName());
        backButoon.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid = ?",String.valueOf(selectCity.getId())).find(County.class);
        if (countyList.size()>0){
            dataList.clear();
            for (County county:countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        }else {
            int cityCode = selectCity.getCityCode();
            String address = "http://guolin.tech/api/china/"+selectProvince.getPrivinceCode()+"/"+cityCode;
            queryFromServer(address,"county");
        }


    }

   private void queryFromServer(String address,final String type){
       showProgressDialog();
       HttpUtil.SendOkHttpRequest(address, new Callback() {
           @Override
           public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                       closeProgressDialog();
                        Toast.makeText(getActivity(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
           }

           @Override
           public void onResponse(Call call, Response response) throws IOException {
               String responseText =  response.body().string();
               boolean result = false;
               if ("province".equals(type)){
                   result = Utility.handleProvinceResponse(responseText);
               }else if("city".equals(type)){
                   result = Utility.handleCityResponse(responseText,selectProvince.getId());
               }else if("county".equals(type)){
                   result = Utility.handleCountyResponse(responseText,selectCity.getId());
               }
               if (result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)){
                                queryProvice();
                            }else if("city".equals(type)){
                                queryCity();
                            }else if("county".equals(type)){
                                queryConty();
                            }
                        }
                    });
               }
           }
       });



   }

    private void showProgressDialog(){
        if(progressDialog==null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    private void closeProgressDialog(){
        if (progressDialog!=null){
            progressDialog.dismiss();
        }

    }



}
