package com.example.usbhost1;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final boolean FORCE_CLAIM = true;

    private final Integer VENDOR_ID = 4071;
    private final Integer PRODUCT_ID = 16385;

    private TextView mTxtValue;
    private UsbManager mUsbManager;
    private UsbDevice mDevice;
    private UsbInterface mInterface;
    private UsbEndpoint mEndpointIn;
    private UsbDeviceConnection mConnection;

    private PendingIntent mPermissionIntent;

    private boolean mContinue = true;

    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                            //call method to set up device communication
                            Log.d(TAG, "permission allowed for device " + device);
                            UsbDeviceConnection connection = mUsbManager.openDevice(device);
                            String data = "usb conn serial = " + connection.getSerial();
                            Log.d(TAG, data);
                        }
                    }
                    else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTxtValue = (TextView) findViewById(R.id.txtValue);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        // IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        // registerReceiver(mUsbReceiver, filter);

        /* get device */
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void usbDevices(View v) {
        Log.d(TAG, "usbDevices");

        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        mDevice = null;

        while(deviceIterator.hasNext()){
            UsbDevice usb = deviceIterator.next();

            mDevice = usb;
            //your code
            usbDiscover();
        }

        usbConnect();
    }

    public void connect(View v) {

        Intent intent = getIntent();

        // extract usb device
        if (intent.hasExtra(UsbManager.EXTRA_DEVICE)) {
            Log.d(TAG, "extra found");
            mDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        } else {
            Log.d(TAG, "extra not found");
        }

        if (mDevice != null) {
            // usbDiscover();
            // usbConnect();
            new Thread(new UsbCommunciation(mDevice)).start();
        } else {
            Log.e(TAG, "usb was null");
        }


    }

    public void usbDiscover() {
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
        dlgAlert.setMessage("No device");
        dlgAlert.setTitle("Usb Device");
        dlgAlert.setPositiveButton("OK", null);
        dlgAlert.setCancelable(true);

        String name = mDevice.getDeviceName();
        Integer vendorId  = mDevice.getVendorId();
        Integer productId = mDevice.getProductId();
        Integer classId = mDevice.getDeviceClass();
        Integer subClassId = mDevice.getDeviceSubclass();
        Integer protocolId = mDevice.getDeviceProtocol();

        String data = "device name = " + name + "; vendorId = " + vendorId + "; productId = " + productId +
                "; class = " + classId + "; subClass = " + subClassId + "; protocol = " + protocolId;

        Log.d(TAG, data);
        dlgAlert.setMessage(data);
        dlgAlert.create().show();

        // store device for Mitutoyo
        if (vendorId.equals(VENDOR_ID) && productId.equals(PRODUCT_ID)) {

            for (int i = 0; i < mDevice.getInterfaceCount(); i++) {
                UsbInterface inter = mDevice.getInterface(i);
                Integer interfaceId = inter.getId();
                data = "interfaceId = " + interfaceId;
                Log.d(TAG, data);
                dlgAlert.setMessage(data);
                dlgAlert.create().show();

                if (interfaceId.equals(0)) {

                    mInterface = inter;

                    for (int j = 0; j < inter.getEndpointCount(); j++) {
                        UsbEndpoint endPoint = inter.getEndpoint(i);
                        data = "endpointNumber = " + endPoint.getEndpointNumber() + "; direction = " + endPoint.getDirection() +
                                "; type = " + endPoint.getType() + "; max packet = " + endPoint.getMaxPacketSize();
                        Log.d(TAG, data);
                        dlgAlert.setMessage(data);
                        dlgAlert.create().show();

                        if (endPoint.getDirection() == UsbConstants.USB_DIR_IN) {
                            mEndpointIn = endPoint;
                        }
                    }
                }


            }

        }

    }

    public void usbConnect() {
        // connect to usb device
        if (mDevice != null) {
            // request crashes
            // mUsbManager.requestPermission(mDevice, mPermissionIntent);
            mConnection = mUsbManager.openDevice(mDevice);
            if (mConnection != null) {
                String data = "usb conn serial = " + mConnection.getSerial();
                Log.d(TAG, data);

                boolean claimOk = mConnection.claimInterface(mInterface, FORCE_CLAIM);
                Log.d(TAG, "claimOk = " + claimOk);

                if (claimOk) {
                    // usb control codes
                    mConnection.controlTransfer(33, 34, 0, 0, null, 0, 0);
                    byte[] buffer = new byte[]{ (byte) 0x80,0x25, 0x00, 0x00, 0x00, 0x00, 0x08 };
                    mConnection.controlTransfer(33, 32, 0, 0, buffer, 7, 0);  //8N1, 9600 baud
                }

            }
        }
    }

    public void disconnect(View v) {
        mConnection.close();
    }

    public void startRead(View v) {
        if (mContinue) {
            Log.e(TAG, "startRead: continue = " + mContinue);
            return;
        }

        startDataRecieve();

//        int length = mEndpointIn.getMaxPacketSize();
//        byte[] buf = new byte[length];
//
//        Log.d(TAG, "queueRead = " + queueRead(buf, 0, length));

/*
        if(queueRead(buf, 0, length) > 0) {
            Log.d("usb", new String(buf).substring(0, length));
        }
*/

//        mContinue = true;
//        Thread thread = new Thread() {
//            @Override
//            public void run() {
//                // byte[] buf = new byte[512];
//
//                Log.d(TAG, "run is going");
//                while (mContinue) {
//                    // int length = mConnection.bulkTransfer(mEndpointIn, buf, 8, 100);
//                    int length = mConnection.controlTransfer(0x00000080, 0x03, 0x4138, 0, null, 0, 0);
//                    if (length > 0) {
//                        byte[] received = new byte[length];
//                        System.arraycopy(buf, 0, received, 0, length);
//                        // receivedHandler.incoming(received);
//                        Log.d("usb", new String(buf).substring(0, length));
//                    }
//                }
//            }
//        };
//        thread.setName("usb read loop");
//        thread.setDaemon(true);
//        thread.start();
    }

    public int queueRead(byte[] buffer, int offset, int length){
        int PacketSize = mEndpointIn.getMaxPacketSize();
        if ((length > PacketSize) || (offset < 0) || (length < 0) || ((offset + length) > buffer.length)) throw new IndexOutOfBoundsException();
        ByteBuffer readBuffer = ByteBuffer.allocate(PacketSize);
        UsbRequest request = new UsbRequest();
        request.initialize(mConnection, mEndpointIn);
        request.queue(readBuffer, length);
        if(mConnection == null){return -1;}
        UsbRequest retRequest = mConnection.requestWait();
        if(mConnection == null){return -1;}
        if (mEndpointIn.equals(retRequest.getEndpoint())){
            // System.arraycopy(readBuffer, 0, buffer, offset, length);
            Log.d(TAG, "readBuffer = " + readBuffer);
            return length;
        }
        return -1;
    }

    public void startDataRecieve() {
        new Thread(new Runnable() {

            @Override
            public void run() {

//                UsbEndpoint endpoint = null;
//
//                for (int i = 0; i < intf.getEndpointCount(); i++) {
//                    if (intf.getEndpoint(i).getDirection() == UsbConstants.USB_DIR_IN) {
//                        endpoint = intf.getEndpoint(i);
//                        break;
//                    }
//                }

                UsbRequest request = new UsbRequest(); // create an URB
                boolean initialized = request.initialize(mConnection, mEndpointIn);

                if (!initialized) {
                    Log.e(TAG, "USB CONNECTION failed");
                    return;
                }
                while (mContinue) {
                    int bufferMaxLength = mEndpointIn.getMaxPacketSize();
                    ByteBuffer buffer = ByteBuffer.allocate(bufferMaxLength);

                    if (request.queue(buffer, bufferMaxLength) == true) {
                        UsbRequest retRequest = mConnection.requestWait();
                        Log.d(TAG, "Returned request = " + retRequest);
                        if (mEndpointIn.equals(retRequest.getEndpoint())) {
                            String result = new String(buffer.array());
                            Log.i(TAG, "new data : " + result);
                            // listener.readData(result);
                        }
                    } else {
                        Log.e(TAG, "USB REQUEST failed");
                    }
                    // wait(1000);
                }

                // exit the data receive loop
                if (!mContinue) {
                    return;
                }
            }
        }).start();
    }

    public void stopRead(View v) {
        mContinue = false;
    }

    public interface ReceivedHandler {
        public void incoming(byte[] data);
    }

//    private class UsbCommunciationArduino implements Runnable {
//        private final UsbDevice mDevice;
//        UsbCommunciationArduino(UsbDevice dev) {
//            mDevice = dev;
//        }
//        @Override
//        public void run() {
//            UsbDeviceConnection usbConnection
//                    = mUsbManager.openDevice(mDevice);
//            if (!usbConnection.claimInterface(mDevice.getInterface(0),
//                    true)) {
//                return;
//            }
//            // Arduino USB serial converter setup
//            usbConnection.controlTransfer(0x21, 34, 0, 0, null, 0, 0);
//            usbConnection.controlTransfer(0x21, 32, 0, 0,
//                    new byte[]{(byte) 0x80, 0x25, 0x00,
//                            0x00, 0x00, 0x00, 0x08},
//                    7, 0);
//            UsbEndpoint output = null;
//            UsbEndpoint input = null;
//            UsbInterface usbInterface = mDevice.getInterface(1);
//            for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
//                if (usbInterface.getEndpoint(i).getType() ==
//                        UsbConstants.USB_ENDPOINT_XFER_BULK) {
//                    if (usbInterface.getEndpoint(i).getDirection() ==
//                            UsbConstants.USB_DIR_IN) {
//                        input = usbInterface.getEndpoint(i);
//                    }
//                    if (usbInterface.getEndpoint(i).getDirection() ==
//                            UsbConstants.USB_DIR_OUT) {
//                        output = usbInterface.getEndpoint(i);
//                    }
//                }
//            }
//            // byte[] readBuffer = new byte[MAX_MESSAGE_SIZE];
//            while (mContinue) {
//                // usbConnection.bulkTransfer(output, TEST_MESSAGE, TEST_MESSAGE.length, 0);
//                // int read = usbConnection.bulkTransfer(input, readBuffer, 0, readBuffer.length, 0);
//                // handleResponse(readBuffer, read);
//                SystemClock.sleep(1000);
//            }
//            usbConnection.close();
//            usbConnection.releaseInterface(usbInterface);
//        }
//    }

    private class UsbCommunciation implements Runnable {
        private final UsbDevice mDevice;
        private final int mPause = 0;   // was 100

        UsbCommunciation(UsbDevice dev) {
            mDevice = dev;
        }
        @Override
        public void run() {
            final StringBuffer sbValue = new StringBuffer("");
            boolean readValue = false;  // skips first read after connection is made

            // get device
            UsbDeviceConnection usbConnection
                    = mUsbManager.openDevice(mDevice);
            if (usbConnection == null) {
                Log.i(TAG, "usb device not connected");
                setTextValue("usb device not connected");
                return;
            }

            // claim interface
            if (!usbConnection.claimInterface(mDevice.getInterface(0), true)) {
                Log.e(TAG, "interface claim failed");
                setTextValue("interface claim failed");
                return;
            }

            // send usb control codes
            // set configuration 1 - TODO is this necessary?
            if (usbConnection.controlTransfer(0x40, 9, 1, 0, null, 0, 0) < 0) {
                Log.e(TAG, "control transfer failed");
                setTextValue("control transfer failed");
                return;
            }
            // byte[] bufferCont = new byte[]{ (byte) 0x80,0x25, 0x00, 0x00, 0x00, 0x00, 0x08 };
            // usbConnection.controlTransfer(0x40, 32, 0, 0, bufferCont, 7, 0);  //8N1, 9600 baud
            // usbConnection.controlTransfer(33, 34, 0, 0, null, 0, 0);
            // byte[] bufferCont = new byte[]{ (byte) 0x80,0x25, 0x00, 0x00, 0x00, 0x00, 0x08 };
            // usbConnection.controlTransfer(33, 32, 0, 0, bufferCont, 7, 0);  //8N1, 9600 baud

            // get interface
            UsbInterface usbInterface = mDevice.getInterface(0);
            if (usbInterface == null) {
                Log.e(TAG, "get Interface failed");
                setTextValue("get Interface failed");
                return;
            }

            // get endpoint
            UsbEndpoint output = null;
            UsbEndpoint input = null;
            for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
                if (usbInterface.getEndpoint(i).getType() ==
                        UsbConstants.USB_ENDPOINT_XFER_INT) {
                    if (usbInterface.getEndpoint(i).getDirection() ==
                            UsbConstants.USB_DIR_IN) {
                        input = usbInterface.getEndpoint(i);
                    }
                    if (usbInterface.getEndpoint(i).getDirection() ==
                            UsbConstants.USB_DIR_OUT) {
                        output = usbInterface.getEndpoint(i);
                    }
                }
            }

            // verify endpoint
            if (input == null) {
                Log.e(TAG, "get inbound endpoint failed");
                setTextValue("get inbound endpoint failed");
                return;
            }

            // usb read loop - TODO use Thread Interrupt instead of boolean
            while (mContinue) {
                UsbRequest request = new UsbRequest(); // create an URB
                boolean initialized = request.initialize(usbConnection, input);

                if (!initialized) {
                    Log.e(TAG, "Usb initialize failed");
                    return;
                }

                int bufferMaxLength = input.getMaxPacketSize();
                ByteBuffer buffer = ByteBuffer.allocate(bufferMaxLength);

                if (request.queue(buffer, bufferMaxLength) == true) {
                    UsbRequest retRequest = usbConnection.requestWait();
                    // Log.d(TAG, "Returned request = " + retRequest);
                    if (input.equals(retRequest.getEndpoint())) {

                        // Log.d(TAG, "buffer 0= " + buffer.get(0));
                        // Log.d(TAG, "buffer 1= " + buffer.get(1));
                        // Log.d(TAG, "buffer 2= " + buffer.get(2));
                        // Log.d(TAG, "buffer 3= " + buffer.get(3));

                        Byte keyByte = buffer.get(2);

                        int keyInt = keyByte.intValue();
                        if (keyInt > 0) {
                            // Log.d(TAG, "keyInt = " + keyInt);

                            switch(keyInt) {
                                case 40:    // KEY_ENTER
                                    Log.d(TAG, "sbValue = " + sbValue.toString());
                                    if (readValue && sbValue.length() > 0) {
                                        setTextValue(sbValue.toString());
                                    } else {
                                        readValue = true;
                                    }

                                    break;
                                case 83:    // KEY_NUMLOCK
                                    sbValue.setLength(0);
                                    break;
                                case 86:    // KEY_KPMINUS
                                    sbValue.append('-');
                                    break;
                                case 89:    // KEY_KP1
                                    sbValue.append('1');
                                    break;
                                case 90:    // KEY_KP2
                                    sbValue.append('2');
                                    break;
                                case 91:    // KEY_KP3
                                    sbValue.append('3');
                                    break;
                                case 92:    // KEY_KP4
                                    sbValue.append('4');
                                    break;
                                case 93:    // KEY_KP5
                                    sbValue.append('5');
                                    break;
                                case 94:    // KEY_KP6
                                    sbValue.append('6');
                                    break;
                                case 95:    // KEY_KP7
                                    sbValue.append('7');
                                    break;
                                case 96:    // KEY_KP8
                                    sbValue.append('8');
                                    break;
                                case 97:    // KEY_KP9
                                    sbValue.append('9');
                                    break;
                                case 98:    // KEY_KP0
                                    sbValue.append('0');
                                    break;
                                case 99:    // KEY_KPDOT
                                    sbValue.append('.');
                                    break;
                                default:
                                    break;
                            }
                        }
                        // String result = new String(buffer.array());
                        // Log.i(TAG, "new data : " + result);
                        // listener.readData(result);
                    }
                } else {
                    Log.e(TAG, "USB REQUEST failed");
                    mContinue = false;
                }

                // take a nap
                if (mPause > 0) {
                    SystemClock.sleep(mPause);
                }
            }
            usbConnection.close();
            usbConnection.releaseInterface(usbInterface);
        }

        // sets on-screen text value via ui thread
        private void setTextValue(final String strValue) {
            try {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTxtValue.setText(strValue);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
