package com.sjw.sensor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements SensorEventListener{
    //加速度
    private EditText et1;
    private TextView t1;
    private Button btn_start,btn_end,btn_ensure;
    private SensorManager sm;
    private Sensor sensor;
    private float acc[]=new float[3];
    private File fi =null;
    private File fidir =null;
    private FileOutputStream fos =null;
    private Calendar mycalendar;
    private Timer updateTime;
    private TimerTask tt =null;

    //陀螺仪
    private float angle[] = new float[3];
    private Sensor magneticSensor;
    private SensorManager sensorManager;
    private Sensor gyroscopeSensor;
    private Sensor accelerometerSensor;

    //磁场
    private float mag[] = new float[3];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //加速度
        sm=(SensorManager) this.getSystemService(SENSOR_SERVICE);
        sensor=sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        btn_start=(Button) findViewById(R.id.Start);
        btn_end=(Button) findViewById(R.id.End);
        btn_ensure=(Button) findViewById(R.id.Ensure);
        et1=(EditText) findViewById(R.id.et1);
        t1=(TextView) findViewById(R.id.t1);
        btn_start.setOnClickListener(new StartClassListener());
        btn_start.setVisibility(View.INVISIBLE);
        btn_end.setOnClickListener(new EndClassListener());
        btn_end.setVisibility(View.INVISIBLE);
        btn_ensure.setOnClickListener(new EnsureClassListener());
        mycalendar=Calendar.getInstance();

        //陀螺仪 、磁场
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        magneticSensor = sensorManager
                .getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometerSensor = sensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        //注册陀螺仪传感器，并设定传感器向应用中输出的时间间隔类型是SensorManager.SENSOR_DELAY_GAME(20000微秒)
        //SensorManager.SENSOR_DELAY_FASTEST(0微秒)：最快。最低延迟，一般不是特别敏感的处理不推荐使用，该模式可能在成手机电力大量消耗，由于传递的为原始数据，诉法不处理好会影响游戏逻辑和UI的性能
        //SensorManager.SENSOR_DELAY_GAME(20000微秒)：游戏。游戏延迟，一般绝大多数的实时性较高的游戏都是用该级别
        //SensorManager.SENSOR_DELAY_NORMAL(200000微秒):普通。标准延时，对于一般的益智类或EASY级别的游戏可以使用，但过低的采样率可能对一些赛车类游戏有跳帧现象
        //SensorManager.SENSOR_DELAY_UI(60000微秒):用户界面。一般对于屏幕方向自动旋转使用，相对节省电能和逻辑处理，一般游戏开发中不使用
        sensorManager.registerListener(this, gyroscopeSensor,
                SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magneticSensor,
                SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, accelerometerSensor,
                SensorManager.SENSOR_DELAY_GAME);

    }
    //更新显示在屏幕上的信息，并且写到文件中
    public void updateGUI(){
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                SimpleDateFormat formatter    =   new    SimpleDateFormat    ("HH:mm:ss");
                Date curDate    =   new    Date(System.currentTimeMillis());//获取当前时间
                String    string    =    formatter.format(curDate);
                //更新显示在屏幕上的信息
                t1.setText(string + "\n"
                        + "加速度：" + "x=" + acc[0] + ",y=" + acc[1] + ",z=" + acc[2] + "\n"
                        + "陀螺仪：" + "x=" + angle[0] + ",y=" + angle[1] + ",y=" + angle[2] + "\n"
                        +  "磁场：" + "x=" + mag[0] + ",y=" + mag[1] + ",y=" + mag[2]);
                //写到文件中
                try {
                    fos.write((((string + "\n"
                            + "加速度：" + "x=" + acc[0] + ",y=" + acc[1] + ",z=" + acc[2] + "\n"
                            + "陀螺仪：" + "x=" + angle[0] + ",y=" + angle[1] + ",y=" + angle[2] + "\n"
                            +  "磁场：" + "x=" + mag[0] + ",y=" + mag[1] + ",y=" + mag[2])).getBytes()));
                    byte []newLine="\r\n".getBytes();
                    fos.write(newLine);
                    fos.flush();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                System.out.println("写入成功！");

            }
        });
    }
    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
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

        if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){  //加速度
            for(int i=0;i<3;i++){
                acc[i]=event.values[i];
            }
        }
        else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){  //磁场
            for (int i=0; i<3; i++)
                mag[i] = event.values[i];
        }
        else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE){  //陀螺仪
            for (int i=0; i<3; i++)
                angle[i] = event.values[i];
        }
    }

    //开始键触发事件
    class StartClassListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            updateTime = new Timer("Acc");

            tt =new TimerTask() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    updateGUI();
                }
            };
            updateTime.scheduleAtFixedRate(tt, 0, 10);

            try {
                fos = new FileOutputStream(fi.getAbsolutePath(),true);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            btn_start.setVisibility(View.INVISIBLE);
            btn_end.setVisibility(View.VISIBLE);
        }

    }
    //储存键触发事件
    class EnsureClassListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            //接受客户端传来的文件名
            String etacc=et1.getText().toString();
            //判断文件名是否为空，为空则不符合要求，提示用户输入不合法
            if(!etacc.equals("")){
                String filestr ="";
                filestr +="/sdcard/"+et1.getText().toString();
                if(!filestr.contains(".")){
                    filestr+=".txt";
                    fi=new File(filestr);
                    if(!fi.exists()){
                        System.out.println("creating it now!");
                        try {
                            fi.createNewFile();
                            Toast.makeText(MainActivity.this, "File isn't exits.Now,create it.",
                                    Toast.LENGTH_LONG).show();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            Toast.makeText(MainActivity.this, "新建文件失败", Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                            System.out.println(e.toString());
                            finish();
                        }
                    }else{
                        Toast.makeText(MainActivity.this, "文件存在!", Toast.LENGTH_LONG).show();
                    }
                    btn_start.setVisibility(View.VISIBLE);
                }else{
                    et1.setText("文件名不正确，有非法符号或者错误信息");
                }
            }else{
                Toast.makeText(MainActivity.this, "输入文件名空，请重新输入！", Toast.LENGTH_LONG).show();
            }
        }

    }
    //结束键触发事件
    class EndClassListener implements View.OnClickListener {
        @Override
        public void onClick(View arg0) {
            // TODO Auto-generated method stub
            try {
                fos.write((mycalendar.getTime().toString()).getBytes());
                byte []newLine="\r\n".getBytes();
                fos.write(newLine);
                fos.close();
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
}