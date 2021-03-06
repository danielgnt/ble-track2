package de.dguenther.bletrack;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.text.InputType;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;


public class MainActivity extends AppCompatActivity {

    private static final int M = 23;
    private RecyclerView recyclerView;
    private MyAdapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<RecyclerData> myDataset = new ArrayList<RecyclerData>();
    private ArrayList<String> alreadyScanned = new ArrayList<String>();
    BluetoothManager bluetoothManager;
    SensorManager sManager;
    private int which_mode = 0;
    private int which_ad_mode = 0;

    @RequiresApi(M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions(new String[]{"android.permission.ACCESS_FINE_LOCATION"},1);
        recyclerView = findViewById(R.id.list);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new MyAdapter(myDataset);
        recyclerView.setAdapter(mAdapter);
        handler = new Handler();

        getPowerMode(new Runnable() {
             @Override
             public void run() {
                 getADMode(new Runnable() {
                     @Override
                     public void run() {
                         sManager = (SensorManager) getSystemService(SENSOR_SERVICE);
                         sManager.registerListener(mySensorEventListener, sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL);
                         sManager.registerListener(mySensorEventListener, sManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),SensorManager.SENSOR_DELAY_NORMAL);

                         advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
                     }
                 });
             }
        });

    }

    // Gravity rotational data
    private float gravity[];
    // Magnetic rotational data
    private float magnetic[]; //for magnetic rotational data
    private float accels[] = new float[3];
    private float mags[] = new float[3];
    private float[] values = new float[3];

    // azimuth, pitch and roll
    private float azimuth;
    private float pitch;
    private float roll;

    private SensorEventListener mySensorEventListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @RequiresApi(M)
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_MAGNETIC_FIELD:
                    mags = event.values.clone();
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    accels = event.values.clone();
                    break;
            }

            if (mags != null && accels != null) {
                gravity = new float[9];
                magnetic = new float[9];
                SensorManager.getRotationMatrix(gravity, magnetic, accels, mags);
                float[] outGravity = new float[9];
                SensorManager.remapCoordinateSystem(gravity, SensorManager.AXIS_X,SensorManager.AXIS_Z, outGravity);
                SensorManager.getOrientation(outGravity, values);

                /*azimuth = values[0] * 57.2957795f;
                pitch =values[1] * 57.2957795f;
                roll = values[2] * 57.2957795f;*/
                azimuth = (float) (values[0]/Math.PI * 180);
                pitch = (float) (values[1]/Math.PI * 180);
                roll = (float) (values[2]/Math.PI * 180);
                mags = null;
                accels = null;
                advertise2(getUUIDString((int)azimuth, (int)pitch, (int)roll));
            }
        }
    };



    void getPowerMode(final Runnable callback){
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Choose power mode");
        String[] types = {"ADVERTISE_TX_POWER_ULTRA_LOW", "ADVERTISE_TX_POWER_LOW", "ADVERTISE_TX_POWER_MEDIUM", "ADVERTISE_TX_POWER_HIGH"};
        b.setItems(types, new DialogInterface.OnClickListener() {

            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
                which_mode = which;
                callback.run();
            }

        });

        b.show();
    }

    void getADMode(final Runnable callback){
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Choose power mode");
        String[] types = {"ADVERTISE_MODE_LOW_POWER", "ADVERTISE_MODE_BALANCED", "ADVERTISE_MODE_LOW_LATENCY"};
        b.setItems(types, new DialogInterface.OnClickListener() {

            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
                which_ad_mode = which;
                callback.run();
            }

        });

        b.show();
    }

    private BluetoothAdapter bluetoothAdapter;
    private Handler handler;

    String leadingZ(int in){
        if(in < 0)
            in = 360 - Math.abs(in) ;
        if(in < 10)
            return "00" + in;
        if(in < 100)
            return "0" + in;
        return String.valueOf(in);
    } //btcommon.eir_ad.entry && !btcommon.eir_ad.entry.company_id

    String getUUIDString(int azi, int pitch, int roll){
        return "7823C5DE-BFC9-4BC6-8E60-228" + leadingZ(azi) + leadingZ(pitch) + leadingZ(roll);
    }


    BluetoothLeAdvertiser advertiser;
    @RequiresApi(M)
    AdvertiseCallback callback = new AdvertiseCallback() {};
    boolean isAdvertising = false;
    @RequiresApi(M)
    void advertise2(String uuidS){
        myDataset.add(new RecyclerData().withDescription(Calendar.getInstance().getTime().toString()).withTitle(uuidS));
        mAdapter.notifyData(myDataset);
        if(isAdvertising) {
            advertiser.stopAdvertising(callback);
            isAdvertising = false;
        }

        ParcelUuid uuid = ParcelUuid.fromString(uuidS);
        AdvertiseData data = (new AdvertiseData.Builder()).setIncludeDeviceName(false).addServiceUuid(uuid).build();
        AdvertiseSettings settings = (new AdvertiseSettings.Builder()).setAdvertiseMode(which_ad_mode).setConnectable(true).setTxPowerLevel(which_mode).build();


        advertiser.startAdvertising(settings, data, callback);
        isAdvertising = true;
    }

}
