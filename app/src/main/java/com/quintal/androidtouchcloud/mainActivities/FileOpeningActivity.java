package com.quintal.androidtouchcloud.mainActivities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.quintal.androidtouchcloud.R;

import java.io.File;

public class FileOpeningActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_opening_layout);
        Log.i("elec", "dummy act created");
       
        int action = getIntent().getIntExtra("action",-1);
        
        if(action == 1){
        String file_path = getIntent().getStringExtra("file_path");
        
        File file = new File(file_path);
        Intent myIntent = new Intent(android.content.Intent.ACTION_VIEW);
		
        String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
		extension = extension.toLowerCase();
		String mimetype = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
		myIntent.setDataAndType(Uri.fromFile(file),mimetype);
		if(mimetype!=null) {
            try {
                startActivity(myIntent);
                finish();
            } catch (ActivityNotFoundException e) {
                noAppError();
            }
        }else{
            noAppError();
		}
	    }else if(action == 2){
	    	String url = getIntent().getStringExtra("url");
			Uri uriUrl = Uri.parse(url);
			Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
			startActivity(launchBrowser);
			finish();
	    	}
	    else{
	    	finish();
	    }
    }

    public void noAppError(){
        Intent no_app_int = new Intent(getApplicationContext(),FailedUpdateActivity.class);
        no_app_int.putExtra("ORIGIN", 4);
        startActivity(no_app_int);
        finish();
    }

}