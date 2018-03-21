package com.xyz.uninstall;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class LogcatScannerService extends Service implements LogcatObserver {

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        // TODO Auto-generated method stub
        super.onStart(intent, startId);
        new AndroidLogcatScannerThread(this).start();
    }

    @Override
    public void handleLog(String info) {
        // TODO Auto-generated method stub
        if (info.contains("android.intent.action.DELETE")
                && info.contains(getPackageName())) {
            startActivity(new Intent("com.xyz.uninstall.UninstallActivity")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    private class AndroidLogcatScannerThread extends Thread {

        private LogcatObserver mObserver;

        public AndroidLogcatScannerThread(LogcatObserver observer) {
            mObserver = observer;
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            String[] cmds = { "logcat", "-c" };
            String shellCmd = "logcat";
            Process process = null;
            InputStream is = null;
            DataInputStream dis = null;
            String line = "";
            Runtime runtime = Runtime.getRuntime();
            try {
                mObserver.handleLog(line);
                int waitValue;
                waitValue = runtime.exec(cmds).waitFor();
                mObserver.handleLog("waitValue=" + waitValue
                        + "\n Has do Clear logcat cache.");
                process = runtime.exec(shellCmd);
                is = process.getInputStream();
                dis = new DataInputStream(is);
                while ((line = dis.readLine()) != null) {
                    if (mObserver != null)
                        mObserver.handleLog(line);

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException ie) {
            } finally {
                try {
                    if (dis != null) {
                        dis.close();
                    }
                    if (is != null) {
                        is.close();
                    }
                    if (process != null) {
                        process.destroy();
                    }
                } catch (Exception e) {
                }
            }
        }
    }

}
