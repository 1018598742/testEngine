package com.fta.testengine;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, TcpClientListener {

    private static final String TAG = "test_engine";

    private EditText edtMain;
    private Button btnConnect;
    private TcpClient tcpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        edtMain = (EditText) findViewById(R.id.edt_main);
        btnConnect = (Button) findViewById(R.id.btn_Connect);
        tcpClient = new TcpClient();
        edtMain.append("onCreate<<<<<<<<<<<<<<<<<");
        //Heart beat
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                final CharSequence time = DateFormat.format("hh:mm:ss", System.currentTimeMillis());
                XLog.w1(TAG, "MainActivity-run: " + time);
                if (tcpClient.isConnected()) {
                    tcpClient.send((time + "\r\n").toString().getBytes());
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        edtMain.append(time + "\n");
                    }
                });
            }
        }, 0, 2000);
        btnConnect.setOnClickListener(this);
        tcpClient.setTcpClentListener(this);
    }


    @Override
    protected void onDestroy() {
        edtMain.append("onDestroy>>>>>>>>>>>>>>>>");
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        Log.i(TAG, "MainActivity-onClick: ");
        tcpClient.connect("192.168.0.124", 10000);
    }

    @Override
    public void received(final byte[] msg) {
        Log.i(TAG, "MainActivity-received: ");
        runOnUiThread(() -> {
            String s = "received ============= " + new String(msg);
            edtMain.append(s);
            XLog.w1(TAG, "MainActivity-run: " + s);
        });
    }


    @Override
    public void sent(byte[] sentData) {
        Log.i(TAG, "MainActivity-sent: ");
    }

    @Override
    public void connected() {
        Log.i(TAG, "MainActivity-connected: ");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnConnect.setText("Connected");
            }
        });
    }

    @Override
    public void disConnected(int code, String msg) {
        Log.i(TAG, "MainActivity-disConnected: code=" + code + "=msg=" + msg);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnConnect.setText("Connect");
            }
        });
    }
}
