package com.quintal.androidtouchcloud.gcm;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

/**
 * Created by Filipe on 28/02/2015.
 */
public class GCMMessageBroadcastReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ComponentName comp = new ComponentName(context.getPackageName(), GCMIntentService.class.getName());
     //   Log.i("GCMBroadcast", " aqui");
        startWakefulService(context, (intent.setComponent(comp)));
        Log.i("gcm receiver ",context.getApplicationInfo()+"");
        setResultCode(Activity.RESULT_OK);
    }
}
