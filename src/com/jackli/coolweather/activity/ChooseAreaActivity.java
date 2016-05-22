package com.jackli.coolweather.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.example.coolweather.R;
import com.jackli.coolweather.DB.CoolWeatherDB;
import com.jackli.coolweather.model.City;
import com.jackli.coolweather.model.County;
import com.jackli.coolweather.model.Province;
import com.jackli.coolweather.util.HttpCallbackListener;
import com.jackli.coolweather.util.HttpUtil;
import com.jackli.coolweather.util.Utility;

import java.util.ArrayList;
import java.util.List;

public class ChooseAreaActivity extends Activity {

    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;
    private TextView titleText;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private CoolWeatherDB coolWeatherDB;
    private List<String> dataList = new ArrayList<String>();

    //省列表
    private List<Province>provinceList;
    //市列表
    private List<City>cityList;
    //县列表
    private List<County>countyList;
    //选中的省份
    private Province selectedProvince;
    //选中的城市
    private City selectedCity;
    //当前选中的级别
    private int currentLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("main", "creat1111");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Log.d("main", "creat2222");
        setContentView(R.layout.choose_area);
        Log.d("main", "creat3333");
        listView = (ListView)findViewById(R.id.list_view);
        titleText = (TextView)findViewById(R.id.title_text);
        
        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        Log.d("main", "creat4444");
        coolWeatherDB = coolWeatherDB.getInstance(this);
        Log.d("main", "creat5555");
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE){
                    selectedProvince = provinceList.get(position);
                    queryCities();
                }else if(currentLevel == LEVEL_CITY){
                    selectedCity = cityList.get(position);
                    queryCounties();
                }
            }
        });
        queryProvinces();   //加载省级数据
        
        
    }

    //查询全国所有省，优先从数据库查询，如果没有查询到再去服务器上查询
    private void queryProvinces(){
    	Log.d("main", "queryProvince");
        provinceList = coolWeatherDB.loadProvinces();
        if(provinceList.size() > 0){
        	
        	Log.d("main", "queryProvince11+size"+provinceList.size());
            dataList.clear();
            for (Province province : provinceList){
                dataList.add(province.getProvinceName());
                Log.d("main", "queryProvince  name :"+ province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText("中国");
            currentLevel = LEVEL_PROVINCE;
        }else {
        	Log.d("main", "queryProvince11 else");
            queryFromServer(null,"province");
        }
    }


    //查询选中省内所有市，优先从数据库查询，如果没有查询到再去服务器上查询
    private void queryCities(){
    	Log.d("main", "query cities");
        cityList = coolWeatherDB.loadCities(selectedProvince.getId());
        if(cityList.size() > 0){
        	
            dataList.clear();
            for (City city : cityList){
                dataList.add(city.getCityName());
                Log.d("main", "query cities  if");
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectedProvince.getProvinceName());
            currentLevel = LEVEL_CITY;
        }else {
        	Log.d("main", "query cities else");
            queryFromServer(selectedProvince.getProvinceCode(),"city");
        }
    }


    //查询选中市内所有县，优先从数据库查询，如果没有查询到再去服务器上查询
    private void queryCounties(){
        countyList = coolWeatherDB.loadcounties(selectedCity.getId());
        if(countyList.size() > 0){
            dataList.clear();
            for (County county : countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectedCity.getCityName());
            currentLevel = LEVEL_COUNTY;
        }else {
            queryFromServer(selectedCity.getCityCode(),"county");
        }
    }

    //根据传入的代号和类型从服务器上查询省市县数据
    private void queryFromServer(final String code,final String type){
        String address;
        Log.d("main", "query server");
        if (!TextUtils.isEmpty(code)){
        	Log.d("main", "query server if ");
            address = "http://www.weather.com.cn/data/list3/city"+code+".xml";
        }else {
        	Log.d("main", "query server  else");
            address = "http://www.weather.com.cn/data/list3/city.xml";
        }
        showProgressDialog();
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                boolean result = false;
                if ("province".equals(type)) {
                    result = Utility.handleProvincesResponse(coolWeatherDB, response);
                } else if ("city".equals(type)) {
                    result = Utility.handleCitiesResponse(coolWeatherDB, response,
                            selectedProvince.getId());
                } else if ("county".equals(type)) {
                    result = Utility.handleCountiesResponse(coolWeatherDB, response,
                            selectedCity.getId());
                }
                if (result) {
                    //通过runOnUiThread()方法回到主线程处理逻辑
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)) {
                                queryProvinces();
                            } else if ("city".equals(type)) {
                                queryCities();
                            }
                            if ("county".equals(type)) {
                                queryCounties();
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                //通过runOnUiThread方法回到主线程处理逻辑
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(ChooseAreaActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

    }

    //显示进度对话框
    private void showProgressDialog(){
        if (progressDialog == null){
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    //关闭对话框
    private void closeProgressDialog(){
        if (progressDialog!=null){
            progressDialog.dismiss();
        }
    }


    //捕获Back按键，根据当前的级别来判断，此时应该返回市级表，省级表，还是直接退出。
    public void onBackPressed(){
        if (currentLevel == LEVEL_COUNTY){
            queryCities();
        }else if (currentLevel == LEVEL_CITY){
            queryProvinces();
        }else{
            finish();
        }

    }
}
