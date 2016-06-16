package com.quintal.androidtouchcloud.gcm;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.quintal.androidtouchcloud.R;
import com.quintal.androidtouchcloud.mainActivities.MainActivity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by Filipe on 28/02/2015.
 */
public class GCMIntentService extends IntentService {

    public static final int NOTIFICATION_ID = 1;
    private NotificationManager _notificationManager;
    NotificationCompat.Builder builder;
    static final String TAG = "GCM IntentService";
    public GCMIntentService(){
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);

        if(!extras.isEmpty()){

            if(GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)){
                //  sendNotification("Send error: "+ extras.toString());
            }else if(GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)){
                //  sendNotification("Deleted messages on server: "+extras.toString());
            }else if(GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)){

                Log.i(TAG, "Completed work @ " + SystemClock.elapsedRealtime());
                // Post notification of received message.
                sendNotification(extras);
                Log.i(TAG, "Received: " + extras.toString());

                Log.e(TAG,extras.getString("desktop_path"));
//                Log.e(TAG,extras.getString("desktop_path2"));
//                Log.e(TAG,extras.getString("desktop_path3"));
//                Log.e(TAG,extras.getString("desktop_path4"));

                String message = extras.getString("message");

            }
        }
        GCMMessageBroadcastReceiver.completeWakefulIntent(intent);
    }

    private String checkFileNameSimple(String share_link){

        if(share_link.contains("/sh/")){
            return "folder";
        }else {
            String[] result = new String[2];
            String[] tag_tokens = share_link.split("/");
            String filename = tag_tokens[tag_tokens.length - 1];
            filename = (filename.split("\\?"))[0];
            return filename;
        }
    }

    private String decodeDropboxLink(String share_link){
        String[] tag_tokens = share_link.split("/");
        String filename = tag_tokens[tag_tokens.length-1];
        filename=filename.substring(0,filename.lastIndexOf('?'));
        String nova = null;
        try {

            filename = URLEncoder.encode(filename, "UTF-8");
            filename = filename.replace("+", "%20");

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String decodedLink = share_link.substring(0,share_link.lastIndexOf('/'))+"/"+filename+"?dl=0";
        return decodedLink;
    }

    private void sendNotification(Bundle msg){
        _notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.putExtra("message",decodeDropboxLink(msg.getString("message")));
        resultIntent.putExtra("size",msg.getString("size"));
        resultIntent.putExtra("desktop_path",msg.getString("desktop_path"));
        resultIntent.putExtra("device",msg.getString("device"));

        PendingIntent contentIntent = PendingIntent.getActivity(this,0,resultIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder _builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("New files ready to be tagged")
                .setStyle(new NotificationCompat.BigTextStyle()
                .bigText(checkFileNameSimple(msg.getString("message"))))
                .setAutoCancel(true);

        _builder.setContentIntent(contentIntent);
        _notificationManager.notify(NOTIFICATION_ID,_builder.build());
    }

}