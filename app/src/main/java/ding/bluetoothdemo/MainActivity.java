package ding.bluetoothdemo;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import ding.bluetoothdemo.controller.BluetoothDeviceAdapter;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1001;
    private static final int REQUEST_ACCESS_COARSE_LOCATION = 2001;
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 3001;
    BluetoothHeadset mBluetoothHeadset;
    BluetoothA2dp mBluetoothA2DP;
    private BluetoothAdapter mBluetoothAdapter;
    private List<BluetoothDevice> mBondDevice;
    private TextView tvListening, tvDiscovering, tvSpeechRecognization;
    ProgressDialog progressDialog;
    private BluetoothDeviceAdapter mDeviceAdaptor;
    private RecyclerView mRecycleView;
    private boolean mIsBlueToothProfileConnected;


    View.OnClickListener buttonClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch(view.getId()) {
                case (R.id.tv_listening) :
                    Intent discoverableIntent =
                            new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                    startActivity(discoverableIntent);
                    break;
                case R.id.tv_discovering:
                    resetRecycleView();
                    showProgress();
                    mBondDevice.addAll(mBluetoothAdapter.getBondedDevices());
                    // Register for broadcasts when a device is discovered.
                    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
                    filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
                    filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                    filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
                    filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
                    filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
                    filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
                    registerReceiver(mReceiver, filter);
                    mBluetoothAdapter.startDiscovery();
                    break;
                case R.id.tv_stt:
                    PackageManager pm = getPackageManager();
                    List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(
                            RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);

                    if (activities.size() == 0) {
                        Toast.makeText(MainActivity.this,"This device does not support speech recognition", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecycleView = findViewById(R.id.rv_device);
        tvListening = findViewById(R.id.tv_listening);
        tvListening.setOnClickListener(buttonClick);
        tvDiscovering = findViewById(R.id.tv_discovering);
        tvDiscovering.setOnClickListener(buttonClick);
        tvSpeechRecognization = findViewById(R.id.tv_stt);
        tvSpeechRecognization.setOnClickListener(buttonClick);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {  // Only ask for these permissions on runtime when running Android 6.0 or higher
            switch (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
                case PackageManager.PERMISSION_DENIED:
                    ((TextView) new AlertDialog.Builder(this)
                            .setTitle("Runtime Permissions up ahead")
                            .setMessage(Html.fromHtml("<p>To find nearby bluetooth devices please click \"Allow\" on the runtime permissions popup.</p>" +
                                    "<p>For more info see <a href=\"http://developer.android.com/about/versions/marshmallow/android-6.0-changes.html#behavior-hardware-id\">here</a>.</p>"))
                            .setNeutralButton("Okay", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                        ActivityCompat.requestPermissions(MainActivity.this,
                                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                                REQUEST_ACCESS_COARSE_LOCATION);
                                    }
                                }
                            })
                            .show()
                            .findViewById(android.R.id.message))
                            .setMovementMethod(LinkMovementMethod.getInstance());       // Make the link clickable. Needs to be called after show(), in order to generate hyperlinks
                    break;
                case PackageManager.PERMISSION_GRANTED:
                    break;
            }
        }

        // Get the default adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            Log.e(MainActivity.class.getSimpleName(), mBluetoothAdapter.getName());
            Log.e(MainActivity.class.getSimpleName(), mBluetoothAdapter.getAddress());
            mBondDevice = new ArrayList<>();
            mBondDevice.addAll(mBluetoothAdapter.getBondedDevices());
            mDeviceAdaptor = new BluetoothDeviceAdapter(this, mBondDevice,
                    new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                            try {

                                int itemPosition = mRecycleView.getChildLayoutPosition(
                                        (View) compoundButton.getParent().getParent());
                                BluetoothDevice item = mBondDevice.get(itemPosition);

                                if(checked) {
                                    // connect
                                    if (mBluetoothA2DP != null) {
                                        Method connect = null;
                                        connect = BluetoothA2dp.class.getDeclaredMethod("connect",
                                                BluetoothDevice.class);
                                        connect.invoke(mBluetoothA2DP, item);
                                    } else if (mBluetoothHeadset != null) {
                                        Method connect = BluetoothHeadset.class.getDeclaredMethod(
                                                "connect",
                                                BluetoothDevice.class);
                                        connect.invoke(mBluetoothHeadset, item);
                                    }
                                } else {
                                    // disconnect
                                    if (mBluetoothA2DP != null) {
                                        Method connect = null;
                                        connect = BluetoothA2dp.class.getDeclaredMethod("disconnect",
                                                BluetoothDevice.class);
                                        connect.invoke(mBluetoothA2DP, item);
                                    } else if (mBluetoothHeadset != null) {
                                        Method connect = BluetoothHeadset.class.getDeclaredMethod(
                                                "disconnect",
                                                BluetoothDevice.class);
                                        connect.invoke(mBluetoothHeadset, item);
                                    }

                                }
                            } catch (NoSuchMethodException e) {
                                e.printStackTrace();
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            } catch (InvocationTargetException e) {
                                e.printStackTrace();
                            }
                        }
                    });
            mDeviceAdaptor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int itemPosition = mRecycleView.getChildLayoutPosition(view);
                    BluetoothDevice item = mBondDevice.get(itemPosition);
                    onClickDevice(itemPosition, item);
                }
            });
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
            mRecycleView.setLayoutManager(layoutManager);
            mRecycleView.setAdapter(mDeviceAdaptor);
        }

        mIsBlueToothProfileConnected = isBluetoothDeviceConnected(BluetoothProfile.A2DP);
        Log.e(MainActivity.class.getSimpleName(), "Blue tooth A2DP profile connected : "+ mIsBlueToothProfileConnected);

        // Establish connection to the proxy.
        mBluetoothAdapter.getProfileProxy(this, mProfileListener, BluetoothProfile.HEADSET);
        mBluetoothAdapter.getProfileProxy(this, mProfileListener, BluetoothProfile.A2DP);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            if(results != null && results.size() >0) {
                Toast.makeText(this, "You speak "+results.get(0),Toast.LENGTH_LONG).show();
            }
            for(String speech : results) {
                Log.e(MainActivity.class.getSimpleName(), speech + "/n");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    private void resetRecycleView() {
        final int size = mBondDevice.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                mBondDevice.remove(0);
            }

            mDeviceAdaptor.notifyItemRangeRemoved(0, size);
        }
    }

    private void showProgress() {
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Discovering ...");
        progressDialog.setCancelable(true);
        progressDialog.show();
    }

    private BluetoothProfile.ServiceListener mProfileListener = new BluetoothProfile.ServiceListener() {
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.HEADSET) {
                mBluetoothHeadset = (BluetoothHeadset) proxy;
            }
            if (profile == BluetoothProfile.A2DP) {
                mBluetoothA2DP= (BluetoothA2dp) proxy;
            }
        }

        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.HEADSET) {
                mBluetoothHeadset = null;
            }
            if (profile == BluetoothProfile.A2DP) {
                mBluetoothA2DP= null;
            }
        }
    };

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.e(MainActivity.class.getSimpleName(), "Action returned "+ action);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mBondDevice.add(device);
                mDeviceAdaptor.notifyItemInserted(mBondDevice.size() - 1);

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            mBluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET ,mBluetoothHeadset);
            mBluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP ,mBluetoothA2DP);
            if (null != mReceiver) {
                unregisterReceiver(mReceiver);
            }
        } catch (IllegalArgumentException e) {

        }
    }

    public boolean removeBond(BluetoothDevice btDevice)
            throws Exception
    {
        Class btClass = Class.forName("android.bluetooth.BluetoothDevice");
        Method removeBondMethod = btClass.getMethod("removeBond");
        Boolean returnValue = (Boolean) removeBondMethod.invoke(btDevice);
        return returnValue.booleanValue();
    }


    public boolean createBond(BluetoothDevice btDevice)
            throws Exception
    {
        Class class1 = Class.forName("android.bluetooth.BluetoothDevice");
        Method createBondMethod = class1.getMethod("createBond");
        Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);
        return returnValue.booleanValue();
    }

    private void onClickDevice(final int itemPosition, final BluetoothDevice item) {
        try {
            boolean isBond = (item.getBondState() == BluetoothDevice.BOND_BONDED);
            if(isBond) {
                final AlertDialog unpairDialog = new AlertDialog.Builder(this)
                        .setTitle("UnPair device")
                        .setMessage("Are you sure to unpair this device?")
                        .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();

                            }
                        })
                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
//                                remove bond
                                boolean isSuccess = false;
                                try {
                                    isSuccess = removeBond(item);
                                    if (isSuccess) {
                                        Toast.makeText(MainActivity.this,
                                                " remove bond to " + item.getName(),
                                                Toast.LENGTH_LONG).show();
                                        mBondDevice.remove(itemPosition);
                                        mDeviceAdaptor.notifyItemRemoved(itemPosition);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            }
                        }).create();

                unpairDialog.show();




            } else {
                boolean isSuccess = createBond(item);
                if(isSuccess) {
                    Toast.makeText(MainActivity.this, " create bond to "+ item.getName(), Toast.LENGTH_LONG).show();
                    mDeviceAdaptor.notifyItemChanged(itemPosition);
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }//connect(bdDevice);
    }

    private boolean isBluetoothDeviceConnected(int bluetoothProfile) {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()
                && mBluetoothAdapter.getProfileConnectionState(bluetoothProfile) == BluetoothHeadset.STATE_CONNECTED;
    }

}
