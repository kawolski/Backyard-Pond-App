package com.example.btp;


import android.annotation.SuppressLint;
import android.bluetooth.BluetoothSocket;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.ebanx.swipebtn.OnStateChangeListener;
import com.ebanx.swipebtn.SwipeButton;
import java.io.IOException;
import java.io.InputStream;

public class userConsole extends AppCompatActivity {

    boolean  fabOpened = false;
    MediaPlayer btnClicked;
    String readMessage;
    TextView temp_view;
    SwipeButton mLedSwipe,mFlowSwipe;
    SeekBar mRedSeek,mGreenSeek,mBlueSeek;
    FloatingActionButton mFabBtn,mFabInfoBtn;
    BluetoothSocket uSocket = null;

    @SuppressLint("HandlerLeak")
    Handler messageHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (msg.what==0){
                String temperature = (String) msg.obj;
                temp_view.setText(temperature);
            }
            else if (msg.what==1){
                int deviceSettings = Integer.parseInt((String) msg.obj);
                settingUp(deviceSettings);
            }

        }
    };

    private void settingUp( int deviceSetting){
        //Setting Things Top To Bottom    eg: 11 12 34 56
        int swipe = deviceSetting/1000000;

        if (swipe/10 == 1  && !mLedSwipe.isActive())
            mLedSwipe.setEnabled(true);

        if (swipe%10 == 1  && !mFlowSwipe.isActive())
            mFlowSwipe.setEnabled(true);

//        deviceSetting = deviceSetting%1000000;
//        int color = deviceSetting/10000;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_console);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        mLedSwipe = findViewById(R.id.ledSwipe);
        mFlowSwipe = findViewById(R.id.flowSwipe);
        temp_view = findViewById(R.id.temp_view);
        mFabBtn = findViewById(R.id.fabBtn);
        mFabInfoBtn = findViewById(R.id.fabInfoBtn);
        mRedSeek = findViewById(R.id.redSeek);
        mGreenSeek = findViewById(R.id.greenSeek);
        mBlueSeek = findViewById(R.id.blueSeek);
        btnClicked = MediaPlayer.create(userConsole.this,R.raw.btn_click);


        pairing.GlobalBT uskt = ((pairing.GlobalBT) getApplicationContext());
        uSocket = uskt.getSocket();

        sendData("999");   //Requesting Device Settings

        mLedSwipe.setOnStateChangeListener(new OnStateChangeListener() {
            @Override
            public void onStateChange(boolean active) {
                btnClicked.seekTo(0);
                btnClicked.start();
                if (active){
                    sendData("11");
                }
                else {
                    sendData("10");
                }
            }
        });

        mFlowSwipe.setOnStateChangeListener(new OnStateChangeListener() {
            @Override
            public void onStateChange(boolean active) {
                btnClicked.seekTo(0);
                btnClicked.start();
                if (active){
                    sendData("21");
                }
                else {
                    sendData("20");
                }
            }
        });

       mFabBtn.setOnClickListener(new View.OnClickListener(){
           @Override
           public void onClick(View v){
               btnClicked.seekTo(0);
               btnClicked.start();
               if(fabOpened == false){
                   showAllFabs();
                   fabOpened = true;
               }
               else{
                   hideAllFabs();
                   fabOpened = false;
               }

           }
        });

       //Under Development
       mFabInfoBtn.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               btnClicked.seekTo(0);
               btnClicked.start();
               showToast("REFRESHING SETTINGS");
               sendData("9");
           }
       });

       mRedSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
           String value;
           @Override
           public void onStartTrackingTouch(SeekBar seekBar) {
           }
           @Override
           public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
               if (fromUser) {
                   value = String.valueOf(progress);
               }
           }
           @Override
           public void onStopTrackingTouch(SeekBar seekBar) {
               sendColor("R"+value+",");
           }
       });
        mGreenSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            String value;
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    value = String.valueOf(progress);
                }
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                sendColor("G"+value+",");
            }
        });
        mBlueSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            String value;
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    value = String.valueOf(progress);
                }
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                sendColor("B"+value+",");
            }
        });

        //Start Reading Signals
        ReadThread rThread;
        rThread = new ReadThread(uSocket);
        rThread.start();
    }


    public class ReadThread extends Thread{

        private final InputStream mmInStream;

        private ReadThread(BluetoothSocket socket){
            InputStream tmpin = null;
            try {
                tmpin = socket.getInputStream();
            }catch(Exception e){
                e.printStackTrace();
            }
            mmInStream = tmpin;
        }

        public void run(){

            int bytes = 0;
            int result = 0;
            int messageCode=0;

            while(uSocket!=null){
                    try{
                        bytes = mmInStream.read();

                        if(bytes == 35  || bytes == 33){
                            messageCode = (bytes == 33) ? 1 : 0 ;
                            result = 0 ;
                            messageCode = 0;
                            bytes = mmInStream.read();
                            while(bytes != 44){
                                bytes = bytes - 48;
                                result = (result*10) + bytes;
                                bytes = mmInStream.read();
                            }
                            readMessage = Integer.toString(result);
                            messageHandler.obtainMessage(messageCode,bytes,-1,readMessage).sendToTarget();
                        }
                    }
                    catch (IOException e){
                        break;
                    }
                }

        }
        //run ends here
    }


    @Override
    protected void onStop() {
        try {
            uSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (fabOpened == false)
        super.onBackPressed();
        else {
            hideAllFabs();
            fabOpened = false;
        }
    }

    public void showAllFabs(){
        mFabBtn.animate().rotation(180);
        mFabInfoBtn.animate().translationY(-200);
        mFabInfoBtn.show();
    }

    public void hideAllFabs(){
        mFabBtn.animate().rotation(0);
        mFabInfoBtn.animate().translationY(0);
        mFabInfoBtn.hide();
    }

    //Manages the complete format of sending data
    private void sendData(String msg){
        if (uSocket!=null){
            try {
                uSocket.getOutputStream().write("#".getBytes());
                uSocket.getOutputStream().write(msg.getBytes());   //toString()   removed
                uSocket.getOutputStream().write(",".getBytes());
            }catch (Exception e){
                showToast("ERROR SENDING");
            }
        }
    }

    private void sendColor(String msg){
        if (uSocket!=null){
            try {
                uSocket.getOutputStream().write("C".getBytes());
                uSocket.getOutputStream().write(msg.getBytes());   //toString()   removed
                uSocket.getOutputStream().write(",".getBytes());
            }catch (Exception e){
                showToast("ERROR SENDING");
            }
        }
    }

    public void dissconnectDevice(View  view) {
        btnClicked.seekTo(0);
        btnClicked.start();
        try {
            uSocket.close();
            showToast("DISSCONNECTED");
            finish();
        } catch (IOException e) {
            e.printStackTrace();
        }
     }


    private void showToast(String msg){
        Toast.makeText(userConsole.this,msg,Toast.LENGTH_SHORT).show();
    }


}
