package com.gunsoft.facerecognitionopencv.service;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.gunsoft.facerecognitionopencv.LockActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by gunanto on 06/05/15.
 */
public class BlockingService extends Service {
    private static long UPDATE_INTERVAL = 1000;  //default
    private static BlockingService myService;
    private static Timer timer;
    private ArrayList<String> apps = new ArrayList<String>();
    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    public BlockingService()
    {
        myService = this;
    }

    public static BlockingService getMyService() {
        return myService;
    }

    @Override
    public void onCreate(){
        super.onCreate();
        PackageManager packageManager = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> appList = packageManager.queryIntentActivities(mainIntent, 0);
        Collections.sort(appList, new ResolveInfo.DisplayNameComparator(packageManager));
        List<PackageInfo> packs = packageManager.getInstalledPackages(0);
        for(int i=0; i < packs.size(); i++) {
            PackageInfo p = packs.get(i);
            ApplicationInfo a = p.applicationInfo;

            // skip system apps if they shall not be included
            if((a.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                continue;
            }

            apps.add(p.packageName);
        }
        startService();
    }

    public void startService()
    {
        timer = new Timer();
        timer.scheduleAtFixedRate(

                new TimerTask() {

                    public void run() {

                        doServiceWork();

                    }
                }, 1000, UPDATE_INTERVAL);
        Log.i(getClass().getSimpleName(), "MyServiceService Timer started....");
    }

    private void doServiceWork()
    {
        //do something wotever you want
        //like reading file or getting data from network
        try {
            ActivityManager mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> RunningTask = mActivityManager.getRunningTasks(1);
            ActivityManager.RunningTaskInfo ar = RunningTask.get(0);
            String activityOnTop = ar.topActivity.getPackageName();

            Log.i(getClass().getSimpleName(), activityOnTop);
            Log.i("openapp" ,LockActivity.app);
            if(LockActivity.app.equals(activityOnTop))
            {
                return;
            }
            else
            {
                LockActivity.app = "";
            }
            if(activityOnTop.equals("com.bbm") && !activityOnTop.equals("com.gunsoft.facerecognitionopencv"))
            {
                Intent lockIntent = new Intent(this, LockActivity.class);
                lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                this.startActivity(lockIntent);
            }
            Toast.makeText(this, "My Service Running", Toast.LENGTH_LONG).show();
        }
        catch (Exception e) {
        }

    }

    public void shutdownService()
    {
        if (timer != null) timer.cancel();
        Log.i(getClass().getSimpleName(), "Timer stopped...");
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        shutdownService();

        // if (MAIN_ACTIVITY != null)  Log.d(getClass().getSimpleName(), "FileScannerService stopped");
    }

}
