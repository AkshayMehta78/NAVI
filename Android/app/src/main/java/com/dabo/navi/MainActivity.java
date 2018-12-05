package com.dabo.navi;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener, BleWrapperUiCallbacks {

    public static String TAG = "NAVI";

    private int SIGNAL_DELTA = 10;

    // TODO: change to your device's address
    private String DEVICE_ADDRESS;

    private String SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b";
    private String CHAR_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8";

    private String SIGNAL_LEFT = "0x30";
    private String SIGNAL_RIGHT = "0x31";
    private String SIGNAL_FINAL = "0x32";

    private TextView tv_steps, tv_log;
    private SensorManager sensorManager;
    private BleWrapper bleWrapper;

    private BluetoothGattCharacteristic characteristic = null;

    private boolean running = false;
    private boolean first_signal, second_signal, third_signal, final_signal = false;
    private int initial_count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DEVICE_ADDRESS = getResources().getString(R.string.device_address);

        tv_steps = findViewById(R.id.tv_steps);
        tv_log = findViewById(R.id.tv_log);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        bleWrapper = new BleWrapper(this, this);

        if (bleWrapper.checkBleHardwareAvailable()) {
            if (bleWrapper.isBtEnabled()) {
                if (bleWrapper.initialize()) {
                    if (!bleWrapper.connect(DEVICE_ADDRESS)) {
                        Log.e(TAG, "Conncetion failed");
                        Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "Initialization failed");
                    Toast.makeText(this, "Initialization failed", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "Bluetooth is not enabled");
                Toast.makeText(this, "Bluetooth is not enabled", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "Did not found BT hardware");
            Toast.makeText(this, "Did not found BT hardware", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        running = true;

        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (countSensor != null) {
            sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            Toast.makeText(this, "Sensor not found!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // running = false;
        // sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (running) {
            if (initial_count == 0) {
                initial_count = Math.round(sensorEvent.values[0]);
            }

            int step_count = Math.round(sensorEvent.values[0]) - initial_count;
            tv_steps.setText(String.valueOf(step_count));

            if (step_count >= SIGNAL_DELTA * 4 && !final_signal) {
                tv_log.setText(String.format("%s\nFINAL-SIGNAL", tv_log.getText()));
                send_signal(SIGNAL_FINAL);

                final_signal = !final_signal;
            } else if (step_count >= SIGNAL_DELTA * 3 && !third_signal) {
                tv_log.setText(String.format("%s\nNEW-SIGNAL", tv_log.getText()));
                random_signal();

                third_signal = !third_signal;
            } else if (step_count >= SIGNAL_DELTA * 2 && !second_signal) {
                tv_log.setText(String.format("%s\nNEW-SIGNAL", tv_log.getText()));
                random_signal();

                second_signal = !second_signal;
            } else if ((step_count >= (SIGNAL_DELTA)) && !first_signal) {
                tv_log.setText(String.format("%s\nNEW-SIGNAL", tv_log.getText()));
                random_signal();

                first_signal = !first_signal;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void uiDeviceFound(BluetoothDevice device, int rssi, byte[] record) {

    }

    @Override
    public void uiDeviceConnected(BluetoothGatt gatt, BluetoothDevice device) {
        Log.d(TAG, "Successfully connected with " + device.getName());
        set_connected(true);

        bleWrapper.getSupportedServices();
    }

    @Override
    public void uiDeviceDisconnected(BluetoothGatt gatt, BluetoothDevice device) {
        Log.d(TAG, device.getName() + " disconnected");
        set_connected(false);
    }

    @Override
    public void uiAvailableServices(BluetoothGatt gatt, BluetoothDevice device, List<BluetoothGattService> services) {
        // Log.d(TAG, services.toString());

        for (BluetoothGattService service : services) {
            UUID serviceUUID = service.getUuid();
            // Log.d(TAG, serviceUUID.toString());

            if (serviceUUID.toString().equals(SERVICE_UUID)) {
                bleWrapper.getCharacteristicsForService(service);
            }
        }
    }

    @Override
    public void uiCharacteristicForService(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, List<BluetoothGattCharacteristic> chars) {
        // Log.d(TAG, chars.toString());

        for (BluetoothGattCharacteristic characteristic : chars) {
            UUID charUUID = characteristic.getUuid();
            // Log.d(TAG, charUUID.toString());

            if (charUUID.toString().equals(CHAR_UUID)) {
                this.characteristic = characteristic;
            }
        }
    }

    @Override
    public void uiCharacteristicsDetails(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic characteristic) {

    }

    @Override
    public void uiNewValueForCharacteristic(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic ch, String strValue, int intValue, byte[] rawValue, String timestamp) {

    }

    @Override
    public void uiGotNotification(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic characteristic) {

    }

    @Override
    public void uiSuccessfulWrite(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic ch, String description) {

    }

    @Override
    public void uiFailedWrite(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic ch, String description) {

    }

    @Override
    public void uiNewRssiAvailable(BluetoothGatt gatt, BluetoothDevice device, int rssi) {

    }

    public byte[] parseHexStringToBytes(final String hex) {
        String tmp = hex.substring(2).replaceAll("[^[0-9][a-f]]", "");
        byte[] bytes = new byte[tmp.length() / 2]; // every two letters in the string are one byte finally

        String part;

        for(int i = 0; i < bytes.length; ++i) {
            part = "0x" + tmp.substring(i*2, i*2+2);
            bytes[i] = Long.decode(part).byteValue();
        }

        return bytes;
    }

    private void random_signal() {
        boolean randomBool = Math.random() < 0.5;

        if (randomBool) send_signal(SIGNAL_LEFT);
        else send_signal(SIGNAL_RIGHT);
    }

    private void send_signal(String value) {
        if (characteristic != null) {
            byte[] dataToWrite;
            dataToWrite = parseHexStringToBytes(value);

            bleWrapper.writeDataToCharacteristic(characteristic, dataToWrite);
        }
    }

    private void set_connected(final boolean connected) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                if (connected)  tv_log.setText("Connected..");
                else  tv_log.setText("...");
            }
        });
    }
}
