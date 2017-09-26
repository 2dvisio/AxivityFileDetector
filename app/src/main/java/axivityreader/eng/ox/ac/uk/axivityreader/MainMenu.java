package axivityreader.eng.ox.ac.uk.axivityreader;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;

public class MainMenu extends AppCompatActivity {

    UsbManager mUsbManager = null;
    TextView mainLabel = null;
    IntentFilter filterAttached_and_Detached = null;
    private static final String ACTION_USB_PERMISSION = "axivityreader.eng.ox.ac.uk.axivityreader.USB_PERMISSION";


    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {


        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if(device != null){
                        log("DETACHED-" + device);
                    }
                }
            }
            //
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {

                        if(device != null){
                            log("ATTACHED-" + device);
                        }
                    }
                    else {
                        PendingIntent mPermissionIntent;
                        mPermissionIntent = PendingIntent.getBroadcast(MainMenu.this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_ONE_SHOT);
                        mUsbManager.requestPermission(device, mPermissionIntent);

                    }

                }
            }
            //
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {

                        if(device != null){
                            log("PERMISSION-" + device);

                        }
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        mainLabel = (TextView) findViewById(R.id.main_label);


        if (true) { //checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {


            //
            mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
            //
            filterAttached_and_Detached = new IntentFilter(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
            filterAttached_and_Detached.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
            filterAttached_and_Detached.addAction(ACTION_USB_PERMISSION);
            //
            registerReceiver(mUsbReceiver, filterAttached_and_Detached);
            //

            HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
            log(deviceList.size() + " USB device(s) found");
            Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();


            int ii = 0;
            String[] dirs = new String[40];

            dirs[ii++] = "/storage/UsbDriveA";
            dirs[ii++] = "/storage/USBstorage1";
            dirs[ii++] = "/storage/usbdisk";
            dirs[ii++] = "/storage/usbotg";
            dirs[ii++] = "/storage/UDiskA";
            dirs[ii++] = "/storage/usb-storage";
            dirs[ii++] = "/storage/usbcard";
            dirs[ii++] = "/storage/usb";
            //        dirs[ii++] = "/storage/0000-924A";
            dirs[ii++] = "/storage/";
            dirs[ii++] = "/mnt/";
            dirs[ii++] = "/";
            dirs[ii++] = "/storage/usbdrivea";
            dirs[ii++] = "/storage/usbstorage1";
            dirs[ii++] = "/storage/udiska";

            while (deviceIterator.hasNext()) {
                UsbDevice device = deviceIterator.next();

                if (device.getProductName().contains("AX3") || device.getDeviceName().contains("AX3")) {


                    log("\n!!!!!!\n" + device + "\n!!!!!!\n\n");

                    int interfaceCount = device.getInterfaceCount();

                    for (int interface_i=0; interface_i < interfaceCount; interface_i++) {

                        UsbInterface usbI = device.getInterface(interface_i);
                        log(" -- I --\n" + usbI.toString());

                        int endpointCount = usbI.getEndpointCount();
                        for (int endpoint_i=0; endpoint_i < endpointCount; endpoint_i++) {

                            UsbEndpoint usbE = usbI.getEndpoint(endpoint_i);

                            log(" -- E --\n" + usbE.toString());

                        }

                    }



                    int DEPTH = 0;



                    for (String i: dirs) {

                        if (i == null || i.isEmpty()) {
                            continue;
                        }

                        File f = new File(i);
                        File CWA = checkDirIsAxivity(f, DEPTH);

                        if (CWA != null && CWA.exists() && CWA.isFile()) {
                            log("This is an axivity");
                            log("Accelerometry file exists");
                            log("File is: " + CWA.getAbsolutePath());
                            log("File size (byte) is: " + CWA.length());

                        }
                    }

                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2000);
                }
            }
        }
    }


    public File checkDirIsAxivity(File f, int DEPTH) {

        if (DEPTH >= 4) {
            return null;
        }

        if (!f.exists()) {
            return null;
        }

        if (checkFileExists(f, "cwa-data.cwa")) {
            for (File a : f.listFiles()) {
                if (a.getName().toLowerCase().equals("cwa-data.cwa")) {
                    return a;
                }
            }
        }

        if (f.isDirectory()) {
            log("O DIR " + f);
            for (File a : f.listFiles()) {
                log("  O F  " + a);
                if (checkAxivityFolder(a)) {
                    if (a.isDirectory() && checkFileExists(a, "cwa-data.cwa")) {
                        for (File aa : a.listFiles()) {
                            if (aa.getName().toLowerCase().equals("cwa-data.cwa")) {
                                return aa;
                            }
                        }

                    }
                }
            }
        }

        return null;
    }



    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                //Log.v(TAG,"Permission is granted");
                return true;
            } else {

                //Log.v(TAG,"Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            //Log.v(TAG,"Permission is granted");
            return true;
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
            //resume tasks needing this permission
        }
    }


    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();

        unregisterReceiver(mUsbReceiver);
    }


    private boolean checkFileExists(File a, String name) {

        boolean exists = false;
        if (a.exists() && a.isDirectory()) {
            Log.e("_____", a.toString());
            for (File aa : a.listFiles()) {
                if (aa.getName().toLowerCase().equals("cwa-data.cwa")) {
                    exists = true;
                }
            }
        }
        return exists;
    }

    private boolean checkAxivityFolder(File a) {
        return (a.getName().contains("0000-") && a.isDirectory());
    }

    private void log(String t) {
        Log.i(" O O O O ", t);

        if (mainLabel != null)
            mainLabel.setText(mainLabel.getText() + "\n" + t);
    }

}
