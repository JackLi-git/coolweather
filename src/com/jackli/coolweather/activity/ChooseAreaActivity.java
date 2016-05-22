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

    //ʡ�б�
    private List<Province>provinceList;
    //���б�
    private List<City>cityList;
    //���б�
    private List<County>countyList;
    //ѡ�е�ʡ��
    private Province selectedProvince;
    //ѡ�еĳ���
    private City selectedCity;
    //��ǰѡ�еļ���
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
        queryProvinces();   //����ʡ������
        
        
    }

    //��ѯȫ������ʡ�����ȴ����ݿ��ѯ�����û�в�ѯ����ȥ�������ϲ�ѯ
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
            titleText.setText("�й�");
            currentLevel = LEVEL_PROVINCE;
        }else {
        	Log.d("main", "queryProvince11 else");
            queryFromServer(null,"province");
        }
    }


    //��ѯѡ��ʡ�������У����ȴ����ݿ��ѯ�����û�в�ѯ����ȥ�������ϲ�ѯ
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


    //��ѯѡ�����������أ����ȴ����ݿ��ѯ�����û�в�ѯ����ȥ�������ϲ�ѯ
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

    //���ݴ���Ĵ��ź����ʹӷ������ϲ�ѯʡ��������
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
                    //ͨ��runOnUiThread()�����ص����̴߳����߼�
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
                //ͨ��runOnUiThread�����ص����̴߳����߼�
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(ChooseAreaActivity.this, "����ʧ��", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

    }

    //��ʾ���ȶԻ���
    private void showProgressDialog(){
        if (progressDialog == null){
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("���ڼ���...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    //�رնԻ���
    private void closeProgressDialog(){
        if (progressDialog!=null){
            progressDialog.dismiss();
        }
    }


    //����Back���������ݵ�ǰ�ļ������жϣ���ʱӦ�÷����м���ʡ��������ֱ���˳���
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
