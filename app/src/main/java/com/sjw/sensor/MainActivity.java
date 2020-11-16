package com.sjw.sensor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.*;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.MyLocationStyle;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements SensorEventListener, LocationSource, AMapLocationListener {
    //加速度
    private EditText et1;
    private TextView t1;
    private Button btn_start, btn_end, btn_ensure;
    private SensorManager sm;
    private Sensor sensor;
    private float acc[] = new float[3];
    private File fi = null;
    private FileOutputStream fos = null;
    private Calendar mycalendar;
    private Timer updateTime;
    private TimerTask tt = null;
    private String buffer = null;


    //传感器管理器
    private SensorManager sensorManager;
    //加速度传感器
    private Sensor accelerometerSensor;


    /**
     * 地图相关的配置
     */
    //显示地图需要的变量
    private MapView mapView;//地图控件
    private AMap aMap;//地图对象

    //定位需要的声明
    private AMapLocationClient mLocationClient = null;//定位发起端
    private AMapLocationClientOption mLocationOption = null;//定位参数
    private OnLocationChangedListener mListener = null;//定位监听器



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //加速度
        sm = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        btn_start = (Button) findViewById(R.id.Start);
        btn_end = (Button) findViewById(R.id.End);
        btn_ensure = (Button) findViewById(R.id.Ensure);
        et1 = (EditText) findViewById(R.id.et1);
        t1 = (TextView) findViewById(R.id.t1);
        btn_start.setOnClickListener(new StartClassListener());
        btn_start.setVisibility(View.INVISIBLE);
        btn_end.setOnClickListener(new EndClassListener());
        btn_end.setVisibility(View.INVISIBLE);
        btn_ensure.setOnClickListener(new EnsureClassListener());
        mycalendar = Calendar.getInstance();

        /**
         * 地图相关的配置
         */
        //显示地图
        mapView = (MapView) findViewById(R.id.map);
        //必须要写
//        mapView.onCreate(savedInstanceState);
        //获取地图对象
        aMap = mapView.getMap();


        //设置显示定位按钮 并且可以点击
        UiSettings settings = aMap.getUiSettings();
        //设置定位监听
        aMap.setLocationSource(this);
        // 是否显示定位按钮
        settings.setMyLocationButtonEnabled(true);
        // 是否可触发定位并显示定位层
        aMap.setMyLocationEnabled(true);


        //定位的小图标 默认是蓝点 这里自定义一团火，其实就是一张图片
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.id.accelerate));
        myLocationStyle.radiusFillColor(android.R.color.transparent);
        myLocationStyle.strokeColor(android.R.color.transparent);
        aMap.setMyLocationStyle(myLocationStyle);

        //开始定位
        initLoc();

        //获取加速度传感器
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //注册陀螺仪传感器，并设定传感器向应用中输出的时间间隔类型是SensorManager.SENSOR_DELAY_GAME(20000微秒)
        //SensorManager.SENSOR_DELAY_FASTEST(0微秒)：最快。最低延迟，一般不是特别敏感的处理不推荐使用，该模式可能在成手机电力大量消耗，由于传递的为原始数据，诉法不处理好会影响游戏逻辑和UI的性能
        //SensorManager.SENSOR_DELAY_GAME(20000微秒)：游戏。游戏延迟，一般绝大多数的实时性较高的游戏都是用该级别
        //SensorManager.SENSOR_DELAY_NORMAL(200000微秒):普通。标准延时，对于一般的益智类或EASY级别的游戏可以使用，但过低的采样率可能对一些赛车类游戏有跳帧现象
        //SensorManager.SENSOR_DELAY_UI(60000微秒):用户界面。一般对于屏幕方向自动旋转使用，相对节省电能和逻辑处理，一般游戏开发中不使用
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);//注册加速度传感器
    }

    /**
     * todo: 更新手机屏幕显示信息，并将从手机获取的数据写入文件中
     */
    public void updateGUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
                Date curDate = new Date(System.currentTimeMillis());//获取当前时间
                String string = formatter.format(curDate);
                String values = string + "\n\n" + "x,y,z三个方向的加速度：\n" + "x=" + acc[0] + ",y=" + acc[1] + ",z=" + acc[2] + "\n"
                        + buffer;
                //更新显示在屏幕上的信息
                t1.setText(values);
                //写到文件中
                try {
                    byte[] bytes = values.getBytes();
                    //将字符数组，写入到文件中
                    fos.write(bytes);
                    byte[] newLine = "\r\n".getBytes();
                    fos.write(newLine);
                    fos.flush();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
        System.out.println("更新信息成功！");
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
        // TODO Auto-generated method stub
    }

    //采集数据
    @SuppressLint("NewApi")
    @Override
    public void onSensorChanged(SensorEvent event) {
        // TODO Auto-generated method stub
        //获取x,y,z三个方向上的加速度
        for (int i = 0; i < 3; i++) {
            acc[i] = event.values[i];
        }
    }

    /**
     * todo: 点击开始按钮，触发获取手机加速度等数据
     */
    class StartClassListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            updateTime = new Timer("Acc");

            tt = new TimerTask() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    updateGUI();
                }
            };
            updateTime.scheduleAtFixedRate(tt, 0, 100);

            try {
                //使用字节流将数据写入缓冲区
                fos = new FileOutputStream(fi.getAbsolutePath(), true);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            btn_start.setVisibility(View.INVISIBLE);
            btn_end.setVisibility(View.VISIBLE);
        }

    }

    /**
     * todo: 点击触发存储事件， 开始存储数据
     */
    class EnsureClassListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            //接受客户端传来的文件名
            String etacc = et1.getText().toString();
            //判断文件名是否为空，为空则不符合要求，提示用户输入不合法
            if (!etacc.equals("")) {
                String filestr = "";
                filestr += "/sdcard/" + et1.getText().toString();
                if (!filestr.contains(".")) {
                    filestr += ".txt";
                    fi = new File(filestr);
                    if (!fi.exists()) {
                        System.out.println("creating it now!");
                        try {
                            fi.createNewFile();
                            Toast.makeText(MainActivity.this, "File isn't exits.Now,create it.",
                                    Toast.LENGTH_LONG).show();
                        } catch (IOException e) {
                            Toast.makeText(MainActivity.this, "新建文件失败", Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                            System.out.println(e.toString());
                            finish();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "文件存在!", Toast.LENGTH_LONG).show();
                    }
                    btn_start.setVisibility(View.VISIBLE);
                } else {
                    et1.setText("文件名不正确，有非法符号或者错误信息");
                }
            } else {
                Toast.makeText(MainActivity.this, "输入文件名空，请重新输入！", Toast.LENGTH_LONG).show();
            }
        }

    }

    /**
     * todo: 点击结束按钮，结束获取手机中的各项数据
     */
    class EndClassListener implements View.OnClickListener {
        @Override
        public void onClick(View arg0) {
            // TODO Auto-generated method stub
            try {
                fos.write((mycalendar.getTime().toString()).getBytes());//将数据添加到字节流中
                byte[] newLine = "\r\n".getBytes();
                fos.write(newLine); // 将数据写入缓存中
                fos.close(); //关闭字节流
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            tt.cancel();
            updateTime.cancel();
            btn_end.setVisibility(View.INVISIBLE);
            btn_start.setVisibility(View.VISIBLE);
            btn_ensure.setOnClickListener(new EnsureClassListener());
        }
    }

// -----------------------获取定位信息--------------------------------------地图相关的方法

    //定位
    private void initLoc() {
        //初始化定位
        mLocationClient = new AMapLocationClient(getApplicationContext());
        //设置定位回调监听
        mLocationClient.setLocationListener(this);
        //初始化定位参数
        mLocationOption = new AMapLocationClientOption();
        //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(true);
        //设置是否只定位一次,默认为false
        mLocationOption.setOnceLocation(false);
        //设置是否强制刷新WIFI，默认为强制刷新
        mLocationOption.setWifiActiveScan(true);
        //设置是否允许模拟位置,默认为false，不允许模拟位置
        mLocationOption.setMockEnable(false);
        //设置定位间隔,单位毫秒,默认为2000ms
        mLocationOption.setInterval(2000);
        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
        //启动定位
        mLocationClient.startLocation();
    }

    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (amapLocation != null) {
            if (amapLocation.getErrorCode() == 0) {
                //定位成功回调信息，设置相关消息
//                amapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见官方定位类型表
//                amapLocation.getLatitude();//获取纬度
//                amapLocation.getLongitude();//获取经度
//                amapLocation.getAccuracy();//获取精度信息
//                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                Date date = new Date(amapLocation.getTime());
//                df.format(date);//定位时间
//                amapLocation.getAddress();//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
//                amapLocation.getCountry();//国家信息
//                amapLocation.getProvince();//省信息
//                amapLocation.getCity();//城市信息
//                amapLocation.getDistrict();//城区信息
//                amapLocation.getStreet();//街道信息
//                amapLocation.getStreetNum();//街道门牌号信息
//                amapLocation.getCityCode();//城市编码
//                amapLocation.getAdCode();//地区编码
                buffer = amapLocation.getCountry() + "" + amapLocation.getProvince() + "" + amapLocation.getCity() + "" + amapLocation.getProvince() + "" + amapLocation.getDistrict() + "" + amapLocation.getStreet() + "" + amapLocation.getStreetNum();
                Toast.makeText(getApplicationContext(), buffer, Toast.LENGTH_SHORT).show();

            } else {
                //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                Log.e("AmapError", "location Error, ErrCode:"
                        + amapLocation.getErrorCode() + ", errInfo:"
                        + amapLocation.getErrorInfo());

                Toast.makeText(getApplicationContext(), "定位失败", Toast.LENGTH_LONG).show();
            }
        }
    }
    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
    }

    @Override
    public void deactivate() {
        mListener = null;
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
}