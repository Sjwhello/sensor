package com.sjw.sensor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.core.app.ActivityCompat;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.*;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.MyLocationStyle;
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity implements SensorEventListener, LocationSource, AMapLocationListener {
    //加速度相关属性
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

    //获取存储权限
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};

    /**
     * todo: 申请打开手机存储权限
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {

        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * todo: 申请打开手机定位权限
     *
     * @param activity
     */
    public static void verifyLocationPermissions(Activity activity) {
        try {
            //检查是否有定位权限
            int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                //没有权限，去申请定位权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //**************************************************************

    //*****************有关图表的属性和方法********************************
    int constNum = 100;
    private GraphicalView chart;
    private float addY = -1;
    private long addX;
    private float addZZ = -1;
    private float addXX = -1;
    private TimeSeries series;
    private TimeSeries series2;
    private TimeSeries series3;
    private XYMultipleSeriesDataset dataset;
    private Handler handler;
    Date[] xcacheX = new Date[constNum];
    Date[] xcacheY = new Date[constNum];
    Date[] xcacheZ = new Date[constNum];
    float[] ycacheX = new float[constNum];
    float[] ycacheY = new float[constNum];
    float[] ycacheZ = new float[constNum];
    private String title[] = {"Y方向加速度","X方向加速度","Z方向加速度"};

    //更新z轴加速度
    private void updateChart() {
        //设定长度为20
        int length = series.getItemCount();
        if (length >= constNum) length = constNum;
        addXX = (float)acc[0];
        addY = (float) (acc[1]+16); //获取z方向上的加速度值，并赋值给坐标轴的y轴
        addZZ = (float) (acc[2]-28);
        addX = System.currentTimeMillis();

        //将前面的点放入缓存
        for (int i = 0; i < length; i++) {
            xcacheX[i] = new Date((long) series.getX(i));
            ycacheX[i] = (float) series.getY(i);
            xcacheY[i] = new Date((long) series2.getX(i));
            ycacheY[i] = (float) series2.getY(i);
            xcacheZ[i] = new Date((long) series3.getX(i));
            ycacheZ[i] = (float) series3.getY(i);
        }

        series.clear();
        series2.clear();
        series3.clear();
        //将新产生的点首先加入到点集中，然后在循环体中将坐标变换后的一系列点都重新加入到点集中
        series.add(new Date(addX), addY);
        series2.add(new Date(addX), addXX);
        series3.add(new Date(addX), addZZ);
        for (int k = 0; k < length; k++) {
            series.add(xcacheX[k], ycacheX[k]);
            series2.add(xcacheY[k], ycacheY[k]);
            series3.add(xcacheZ[k], ycacheZ[k]);
        }
        //在数据集中添加新的点集
        dataset.removeSeries(series);
        dataset.removeSeries(series2);
        dataset.removeSeries(series3);
        dataset.addSeries(series);
        dataset.addSeries(series2);
        dataset.addSeries(series3);

        //曲线更新
        chart.invalidate();
    }

    private XYMultipleSeriesRenderer getDemoRenderer() {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setChartTitle("加速度变化图");//标题
        renderer.setChartTitleTextSize(20);
        renderer.setXTitle("时间");    //x轴说明
        renderer.setYTitle("加速度");
        renderer.setAxisTitleTextSize(16);
        renderer.setAxesColor(Color.BLACK);
        renderer.setLabelsTextSize(18);    //数轴刻度字体大小
        renderer.setLabelsColor(Color.BLACK);
        renderer.setLegendTextSize(18);    //曲线说明
        renderer.setXLabelsColor(Color.BLACK);
        renderer.setYLabelsColor(0, Color.BLACK);
        renderer.setMargins(new int[]{5, 30, 10, 2});//上左下右{ 20, 30, 100, 0 })
        XYSeriesRenderer r = new XYSeriesRenderer(); // 第1条曲线
        r.setColor(Color.RED);
        r.setChartValuesSpacing(3);
        r.setPointStyle(PointStyle.POINT);
        r.setFillPoints(true);
        renderer.addSeriesRenderer(r);
        r = new XYSeriesRenderer(); //第二条曲线
        r.setColor(Color.BLUE);
        r.setChartValuesSpacing(3);
        r.setPointStyle(PointStyle.POINT);
        r.setFillPoints(true);
        renderer.addSeriesRenderer(r);
        r = new XYSeriesRenderer(); //第三条曲线
        r.setColor(Color.GREEN);
        r.setChartValuesSpacing(3);
        r.setPointStyle(PointStyle.POINT);
        r.setFillPoints(true);
        renderer.addSeriesRenderer(r);
        renderer.setMarginsColor(Color.WHITE);
        renderer.setPanEnabled(false, false);
        renderer.setShowGrid(true);
        renderer.setYAxisMax(50);//纵坐标最大值
        renderer.setYAxisMin(-50);//纵坐标最小值
        renderer.setInScroll(true);
        return renderer;
    }

    private XYMultipleSeriesDataset getDateDemoDataset() {//初始化的数据
        dataset = new XYMultipleSeriesDataset();
        final int nr = 10;
        long value = System.currentTimeMillis();
        series = new TimeSeries(title[0]);
        series2 = new TimeSeries(title[1]);
        series3 = new TimeSeries(title[2]);
        for (int k = 0; k < nr; k++) {
            series.add(new Date(value + k * 100), 25);//初值Y轴以0为中心，X轴初值范围再次定义
            series2.add(new Date(value + k * 100), 0);
            series3.add(new Date(value + k * 100), -25);
        }
        dataset.addSeries(series);
        dataset.addSeries(series2);
        dataset.addSeries(series3);
        return dataset;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //获取图表窗口
        LinearLayout layout1 = (LinearLayout) findViewById(R.id.linearlayout1);
        //生成z，x方向的图表
        chart = ChartFactory.getTimeChartView(this, getDateDemoDataset(), getDemoRenderer(), "mm:ss:SSS");
        layout1.addView(chart, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 380));


        //获取手机定位权限
        verifyLocationPermissions(this);

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
        //必须要写，2D界面，暂时不需要
        //mapView.onCreate(savedInstanceState);
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


        //定位的小图标 默认是蓝点
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
                String values = string + "\n" + "加速度：" + "x=" + acc[0] + ",y=" + acc[1] + ",z=" + acc[2] + "\n"
                        + "位置：" + buffer + "\n";
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
            handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    //刷新图表
                    updateChart();
                    super.handleMessage(msg);
                }
            };
            // TODO Auto-generated method stub
            updateTime = new Timer("Acc");

            tt = new TimerTask() {
                @Override
                public void run() {
                    Message message = new Message();
                    message.what = 200;
                    handler.sendMessage(message);
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
            //获取手机存储权限
            verifyStoragePermissions(MainActivity.this);
            // TODO Auto-generated method stub
            //接受客户端传来的文件名
            String etacc = et1.getText().toString();
            //判断文件名是否为空，为空则不符合要求，提示用户输入不合法
            if (!etacc.equals("")) {
                String filestr = "";
                filestr += "/sdcard/Android/" + et1.getText().toString();
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

    /**
     * todo: 开启定位的一些配置信息
     */
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
        //获取最近3s内精度最高的一次定位结果：必须开，否则无法更新位置信息
        //设置setOnceLocationLatest(boolean b)接口为true，启动定位时SDK会返回最近3s内精度最高的一次定位结果。如果设置其为true，setOnceLocation(boolean b)接口也会被设置为true，反之不会，默认为false。
        mLocationOption.setOnceLocationLatest(true);
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

    /**
     * todo： 回调位置信息
     *
     * @param amapLocation
     */
    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (amapLocation != null) {
            if (amapLocation.getErrorCode() == 0) {
                //定位成功回调信息，设置相关消息
                /*回调可以获取的信息如下：
                amapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见官方定位类型表
                amapLocation.getLatitude();//获取纬度
                amapLocation.getLongitude();//获取经度
                amapLocation.getAccuracy();//获取精度信息
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = new Date(amapLocation.getTime());
                df.format(date);//定位时间
                amapLocation.getAddress();//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
                amapLocation.getCountry();//国家信息
                amapLocation.getProvince();//省信息
                amapLocation.getCity();//城市信息
                amapLocation.getDistrict();//城区信息
                amapLocation.getStreet();//街道信息
                amapLocation.getStreetNum();//街道门牌号信息
                amapLocation.getCityCode();//城市编码
                amapLocation.getAdCode();//地区编码*/
                buffer = amapLocation.getCountry() + "" + amapLocation.getProvince() + "" + amapLocation.getCity() + "" + amapLocation.getProvince() + "" + amapLocation.getDistrict() + "" + amapLocation.getStreet() + "" + amapLocation.getStreetNum();
                //弹窗显示
                //Toast.makeText(getApplicationContext(), buffer, Toast.LENGTH_SHORT).show();

            } else {
                //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                Log.e("AmapError", "location Error, ErrCode:"
                        + amapLocation.getErrorCode() + ", errInfo:"
                        + amapLocation.getErrorInfo());

                Toast.makeText(getApplicationContext(), "定位失败", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * todo: 激活监听器
     *
     * @param listener
     */
    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
    }

    /**
     * todo: 销毁监听器
     */
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
        //暂停地图定位
        mapView.onPause();
    }
}