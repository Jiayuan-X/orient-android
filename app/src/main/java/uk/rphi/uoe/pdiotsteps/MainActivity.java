package uk.rphi.uoe.pdiotsteps;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import io.reactivex.disposables.Disposable;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends Activity implements AdapterView.OnItemSelectedListener {

    // test device - replace with the real BLE address of your sensor, which you can find
    // by scanning for devices with the NRF Connect App

    private static final String ORIENT_BLE_ADDRESS = "F6:48:11:77:88:4C";
    private static final String ORIENT_RAW_CHARACTERISTIC = "00002A58-0000-1000-8000-00805F9B34FB";

    private static final boolean raw = true;
    private RxBleDevice orient_device;
    private Disposable scanSubscription;
    private RxBleClient rxBleClient;

    boolean connected = false;

    private Context ctx;
    private TextView captureTimetextView;

    private final int RC_LOCATION_AND_STORAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ctx = this;

        getPermissions();
    }

    @AfterPermissionGranted(RC_LOCATION_AND_STORAGE)
    private void getPermissions() {
        String[] perms = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(this, perms)) {
            // Already have permission, do the thing
            // ...
            runApp();

        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, getString(R.string.location_and_storage_rationale),
                    RC_LOCATION_AND_STORAGE, perms);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    private void runApp() {

        captureTimetextView = findViewById(R.id.captureTimetextView);

        rxBleClient = RxBleClient.create(this);

        scanSubscription = rxBleClient.scanBleDevices(
                new ScanSettings.Builder()
                        // .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // change if needed
                        // .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES) // change if needed
                        .build()
                // add filters if needed
        )
                .subscribe(
                        scanResult -> {
                            Log.i("OrientAndroid", "FOUND: " + scanResult.getBleDevice().getName() + ", " +
                                    scanResult.getBleDevice().getMacAddress());
                            // Process scan result here.
                            if (scanResult.getBleDevice().getMacAddress().equals(ORIENT_BLE_ADDRESS)) {
                                runOnUiThread(() -> {
                                    Toast.makeText(ctx, "Found " + scanResult.getBleDevice().getName() + ", " +
                                                    scanResult.getBleDevice().getMacAddress(),
                                            Toast.LENGTH_SHORT).show();
                                });
                                connectToOrient(ORIENT_BLE_ADDRESS);
                                scanSubscription.dispose();
                            }
                        },
                        throwable -> {
                            // Handle an error here.
                            runOnUiThread(() -> {
                                Toast.makeText(ctx, "BLE scanning error",
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                );
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position,
                               long id) {
        // TODO Auto-generated method stub
        if (parent.getItemAtPosition(position).toString().compareTo("---") == 0) return;

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // TODO Auto-generated method stub
    }

    private void connectToOrient(String addr) {
        orient_device = rxBleClient.getBleDevice(addr);
        String characteristic;
        if (raw) characteristic = ORIENT_RAW_CHARACTERISTIC;

        orient_device.establishConnection(false)
                .flatMap(rxBleConnection -> rxBleConnection.setupNotification(UUID.fromString(characteristic)))
                .doOnNext(notificationObservable -> {
                    // Notification has been set up
                })
                .flatMap(notificationObservable -> notificationObservable) // <-- Notification has been set up, now observe value changes.
                .subscribe(
                        bytes -> {
                            //n += 1;
                            // Given characteristic has been changes, here is the value.

                            //Log.i("OrientAndroid", "Received " + bytes.length + " bytes");
                            if (!connected) {
                                connected = true;
                                runOnUiThread(() -> {
                                                               Toast.makeText(ctx, "Receiving sensor data",
                                                                       Toast.LENGTH_SHORT).show();
                                    ConstraintLayout c = findViewById(R.id.layout);
                                    c.setBackgroundColor(Color.parseColor("#C7DCE4"));
                                                           });
                            }
                            if (raw) handleRawPacket(bytes);
                        },
                        throwable -> {
                            // Handle an error here.
                            Log.e("OrientAndroid", "Error: " + throwable.toString());
                            runOnUiThread(() -> {
                                Toast.makeText(ctx, "Disconnected:" + throwable.toString(),
                                        Toast.LENGTH_LONG).show();
                                ConstraintLayout c = findViewById(R.id.layout);
                                c.setBackgroundColor(Color.parseColor("#f7300c"));
                            });
                        }
                );
    }

    private void handleRawPacket(final byte[] bytes) {

        int word = (bytes[0] & 0xff) | ((bytes[1] & 0xff) << 8);

        runOnUiThread(() -> {
                    captureTimetextView.setText(String.valueOf(word));
                });

    }

}
