package com.fta.testengine;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateFormat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class BackgroundService extends Service implements TcpClientListener {
    private static final String TAG = "BackgroundService";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private TcpClient tcpClient;

    private PowerManager.WakeLock mWakeLock;

    /**
     * 播放无声音乐
     */
    private MediaPlayer mPlayer;

    private ConnectivityManager.NetworkCallback networkCallback;
    private ConnectivityManager connectivityManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "BackgroundService-onCreate: ");
        sysncWriteLog("BackgroundService-onCreate: ");
        tcpClient = new TcpClient();
        tcpClient.setTcpClentListener(this);
        connectNet();

//        Intent intent = new Intent();
//        intent.setClass(this, BackgroundService.class);
//        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
//        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
//        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 10 * 1000, pendingIntent);//////TTTT   重复

        Observable.interval(0, 10, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new MyObserver());

        if (mWakeLock == null || !mWakeLock.isHeld()) {

            try {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
                mWakeLock.acquire();
                sysncWriteLog("获取唤醒锁成功.");
            } catch (Exception e) {
                e.printStackTrace();
                sysncWriteLog("获取唤醒锁失败." + e.getMessage());
            }
        }


        setForeground();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            networkCallback = new NetworkCallbackImpl();
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            NetworkRequest request = builder.build();
            connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            connectivityManager.registerNetworkCallback(request, networkCallback);
        }
    }

    private void connectNet() {
        if (tcpClient != null) {
            tcpClient.connect("192.168.0.124", 10000);
        }
    }

    private void retryConnectNet() {
        connectNet();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "BackgroundService-onDestroy: ");
        sysncWriteLog("BackgroundService-onDestroy: ");
        tcpClient.disConnect();

        if (mWakeLock != null) {
            mWakeLock.release();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (connectivityManager != null && networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            }
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "BackgroundService-onStartCommand: ");
        sysncWriteLog("BackgroundService-onStartCommand: ");
        sendInfo();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void received(byte[] msg) {
        sysncWriteLog("BackgroundService-received: " + new String(msg));
        Log.i(TAG, "BackgroundService-received: " + new String(msg));
    }

    @Override
    public void sent(byte[] sentData) {
        sysncWriteLog("BackgroundService-sent: " + new String(sentData));
        Log.i(TAG, "BackgroundService-sent: " + new String(sentData));
    }

    @Override
    public void connected() {
        String info = "BackgroundService-connected: ";
        sysncWriteLog(info);
        Log.i(TAG, "BackgroundService-connected: ");
        sendInfo();
    }

    @Override
    public void disConnected(int code, String msg) {
        String info = "BackgroundService-disConnected: code=" + code + "=msg=" + msg;
        sysncWriteLog(info);
        Log.i(TAG, "BackgroundService-disConnected: code=" + code + "=msg=" + msg);
        retryConnectNet();
    }


    private void sysncWriteLog(String message) {
        Observable.create(new SaveSubscribe(message))
                .subscribeOn(Schedulers.newThread())
                .subscribe();
    }


    private class SaveSubscribe implements ObservableOnSubscribe<String> {
        String info;

        SaveSubscribe(String info) {
            this.info = info;
        }

        @Override
        public void subscribe(ObservableEmitter<String> emitter) throws Exception {
            FileOutputStream fos = null;
            try {
                File file = new File(Environment.getExternalStorageDirectory(), "mylogs");
                if (!file.exists()) {
                    file.mkdirs();
                }
                fos = new FileOutputStream(new File(file, "test_log.txt"), true);
                fos.write(String.format(Locale.CHINA, "%s: %s \n", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date()), info).getBytes());
                fos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fos != null) {
                        fos.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private class MyObserver implements Observer<Long> {

        @Override
        public void onSubscribe(Disposable d) {

        }


        @Override
        public void onNext(Long aLong) {
            sendInfo();
        }


        @Override
        public void onError(Throwable e) {

        }


        @Override
        public void onComplete() {

        }
    }

    private void sendInfo() {
        sysncWriteLog("定时发送消息" + tcpClient.isConnected());
        CharSequence time = DateFormat.format("HH:mm:ss", System.currentTimeMillis());
        Log.i(TAG, "MyObserver-onNext: " + time + "=连接=" + tcpClient.isConnected());
        if (tcpClient.isConnected()) {
            tcpClient.send((time + "\r\n").getBytes());
        }
    }

    /**
     * 将服务置为前台服务
     * <p>
     * android 8.0 系统都会对后台应用获取用户当前位置的频率进行限制，
     * 只允许后台应用每小时接收几次位置更新，将应用置为前台服务可以规避此限制。
     */
    private void setForeground() {
        String channel;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            channel = createChannel();
        else {
            channel = "";
        }
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), MainActivity.class), 0);
        final Notification notification = new NotificationCompat.Builder(getApplicationContext(), channel)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.app_name).concat("运行"))
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pi)
                .build();
        startForeground(500, notification);
        // 播放无声音乐
        try {
            if (mPlayer != null) {
                mPlayer.release();
            }
            mPlayer = new MediaPlayer();
            mPlayer.setDataSource(getApplicationContext(), Uri.parse(String.format(Locale.CHINA, "android.resource://%s/%d", getPackageName(), R.raw.silent)));
            mPlayer.prepare();
            mPlayer.setLooping(true);
            mPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @NonNull
    @TargetApi(26)
    private synchronized String createChannel() {
        NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        String name = "com.mdm.service.MdmService";
        int importance = NotificationManager.IMPORTANCE_LOW;

        NotificationChannel mChannel = new NotificationChannel("com.mdm.service.MdmService", name, importance);

        mChannel.enableLights(false);
//        mChannel.setLightColor(Color.BLUE);
        mChannel.enableVibration(false);
        mChannel.setSound(null, null);
        if (mNotificationManager != null) {
            mNotificationManager.createNotificationChannel(mChannel);
        } else {
            stopSelf();
        }
        return "com.mdm.service.MdmService";
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private class NetworkCallbackImpl extends ConnectivityManager.NetworkCallback {
        @Override
        public void onAvailable(Network network) {
            super.onAvailable(network);

            sysncWriteLog("网络可用");
        }

        @Override
        public void onLost(Network network) {
            super.onLost(network);
            sysncWriteLog("网络不可用");
        }
    }
}
