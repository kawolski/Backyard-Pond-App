package com.example.btp;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.RippleDrawable;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.provider.Telephony;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class pairing extends AppCompatActivity {

    ListView mpairedList;
    MediaPlayer btnClicked,error;
    private ProgressDialog progressDialog;

    public BluetoothAdapter mAdapter = null;
    private Set<BluetoothDevice> pairedDevices;
    public static String bubbles_address = "98:D3:31:40:26:0F";

    public static String EXTRA_SOCKET = "deviceSocket";

    public BluetoothSocket mSocket = null;

    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        btnClicked = MediaPlayer.create(pairing.this,R.raw.btn_click);
        error = MediaPlayer.create(pairing.this,R.raw.error);

        mpairedList = findViewById(R.id.pairedList);
        checkBTAdapter();

     Thread searchForBubbles = new Thread(checkBTSwitch);
     searchForBubbles.start();


    }

    private void checkBTAdapter() {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null) {
            showToast("Bluetooth not supported");
        } else {
            if (!mAdapter.isEnabled()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, 0);
            }
        }
    }

    @SuppressLint("HandlerLeak")
    Handler connect = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            selfConnect();
        }
    };

    Runnable checkBTSwitch = new Runnable() {
        @Override
        public void run() {
            while(!mAdapter.isEnabled()){

            }
            connect.sendEmptyMessage(0);
        }


    };

    public void selfConnect(){
        pairing.autoConnectBubbles selfCon = new pairing.autoConnectBubbles();
        selfCon.execute(bubbles_address);
    }

    Runnable unableToConnect = new Runnable() {
        @Override
        public void run() {
            showToast("Unable to connect");
        }
    };

    Runnable jumpOnUserConsole = new Runnable() {
        @Override
        public void run() {

            Intent newConsole = new Intent(pairing.this, userConsole.class);
            newConsole.putExtra(EXTRA_SOCKET, String.valueOf(mSocket));
            startActivity(newConsole);
        }
    };


    public void getPairedDevices(View view) {

        if (mAdapter.isEnabled()) {
            btnClicked.seekTo(0);
            btnClicked.start();
            showToast("Searching Devices");
            pairedDevices = mAdapter.getBondedDevices();
            ArrayList list = new ArrayList();

            if (pairedDevices.size() > 0) {
                for (BluetoothDevice bt : pairedDevices) {
                    list.add(bt.getName() + "\n" + bt.getAddress());
                }
            } else showToast("No paired device found");
            final ArrayAdapter adapter = new ArrayAdapter(this, R.layout.my_list_layout, list);
            mpairedList.setAdapter(adapter);
            mpairedList.setOnItemClickListener(myListClickListener);
        }
        else{
            error.seekTo(0);
            error.start();
            showToast("Enable Bluetooth First");
        }
    }


    public AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            if (mAdapter.isEnabled()) {
                btnClicked.seekTo(0);
                btnClicked.start();

                String info = ((TextView) view).getText().toString();

                final String address = info.substring(info.length() - 17);

                pairing.autoConnectBubbles connectToDevice = new pairing.autoConnectBubbles();
                connectToDevice.execute(address);
            }
            else{
                error.seekTo(0);
                error.start();
            }
        }
    };

    public static class GlobalBT extends Application{
        private static BluetoothSocket socket;
        public BluetoothSocket getSocket(){
            return socket;
        }

        public void setSocket(BluetoothSocket socket){
            if (socket.isConnected())
                this.socket = socket;
        }
    }

    private class autoConnectBubbles extends android.os.AsyncTask<String,Void,Void>{

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(pairing.this,"Connecting","Please Wait...");
            super.onPreExecute();
        }


        @Override
        protected Void doInBackground(String... strings) {

            mSocket = null;

            try {

                    BluetoothDevice dispositivo = mAdapter.getRemoteDevice(strings[0]);
                    mSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);
                    GlobalBT pskt = ((GlobalBT) getApplicationContext());
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    mSocket.connect();
                    pskt.setSocket(mSocket);

            } catch (IOException e) {
                try {
                    mSocket.close();
                    runOnUiThread(unableToConnect);
                } catch (IOException e1) {
                }
            }

            if (mSocket.isConnected()) {

                runOnUiThread(jumpOnUserConsole);
            } else {

            }

            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            progressDialog.dismiss();
            super.onPostExecute(aVoid);
        }
    }


    private  void showToast(String msg){
        Toast.makeText(pairing.this,msg,Toast.LENGTH_SHORT).show();
    }

    //Turn OFF Bluetooth Adapter when application is Destroyed
    @Override
    protected void onDestroy() {
        mAdapter.disable();
        super.onDestroy();
    }

}
