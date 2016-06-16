package com.quintal.androidtouchcloud.mainActivities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Account;
import com.dropbox.client2.DropboxAPI.DropboxLink;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.quintal.androidtouchcloud.R;
import com.quintal.androidtouchcloud.nfc.NFCReader;
import com.quintal.androidtouchcloud.nfc.NFCWriter;
import com.quintal.androidtouchcloud.storage.DBManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
//import android.annotation.SuppressLint;

public class MainActivity extends Activity {

	final static private String MODULE = "TouchCloud MainActivity";

    //dropbox variables
	final static private String USER_KEY = "user";
	final static private String EMAIL_KEY = "email";
	final static private String USER_ID_KEY = "user_id";
	final static private String SHARE_LINK_KEY = "share_link";
	final static private String INSERTED_KEY = "is_inserted";
	final static private String TAG_KEY = "nfc_id";
    final static private String FILE_SIZE_KEY = "size";
    final static private String DESKTOP_PATH_KEY = "desktop_path";
    final static private String DEVICE_KEY = "device";
    final static private String PRIVACY_KEY = "privacy";
	final static private String APP_KEY = "j6tx3t0y6gjllse";
	final static private String APP_SECRET = "1e4sr3buip2rxe0";
	final static private String ACCOUNT_PREFS_NAME = "prefs";
	final static private String ACCESS_KEY_NAME = "ACCESS_KEY";
	final static private String ACCESS_SECRET_NAME = "ACCESS_SECRET";
	final static private AccessType ACCESS_TYPE = AccessType.DROPBOX;
    private static final String MESSAGE_KEY = "message";
    private boolean _fromDropbox=false;
	private DropboxAPI<AndroidAuthSession> mDBApi;
    //
    //gcm variables
    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private final static String SENDER_ID = "744839324308";
    private GoogleCloudMessaging gcm;
    private AtomicInteger msgId = new AtomicInteger();
    private SharedPreferences prefs;
    private String regid;
    private Context _context;
    //
    //class general variables
	private final GUI_Handler gui_handler = new GUI_Handler();
	private AccessTokenPair tokens;
	private Button _manageLinks;
	private Intent _intent;
	private Bundle _extras;
	private String _action;
    //private ProgressDialog mDialog;
	//public static Intent openFileIntent;  aveiro.m-iti.org/touchcloud/gcm_server/send_message.php?desktop_path="C:\Users\Filipe\Dropbox\SINAIS Papers\WIP\RM\Pereira & Quintal - A long-term study of energy eco-feedback - final"
    //aveiro.m-iti.org/touchcloud/gcm_server/send_message.php?uid=2037841&device_id=Windows-8&desktop_path=D:\Dropbox\20150222025718.png&dp_link=https://www.dropbox.com/s/oq8qgw7elm03hcb/20150222025718.png?dl=0&reg_id=APA91bHPZPtOW4WV5tHDQ5cG2Xr3Xkc6l3IVprVdc4YAoJ59vpzhbrhIO9cK9bem8mS-o-9b_Mino1OLmU9zeus2YgMuKkyOjviqlbtPdtFzKaHfpEV1zX42ksFBOnFHmv-Uw5od2byt0Jm8GCOwJhHiGohvog9lhg
	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		getActionBar().setDisplayShowHomeEnabled(false);
		getActionBar().setDisplayShowTitleEnabled(false);
		getActionBar().hide();
		setContentView(R.layout.activity_main);

		_intent = getIntent();
		_extras = _intent.getExtras();
		_action = _intent.getAction();
        _context = getApplicationContext();

       	// Need to create database if it doesn't exist yet
		if (! DBManager.databaseExists())
			DBManager.initDatabase();
		else
			Log.i(MODULE, "Database already exists!!");

		File SDCardRoot = Environment.getExternalStorageDirectory();
		File file = new File(SDCardRoot+"/touchCloud/temp_files/");  // folder to store the temp files
		file.mkdir();

		AndroidAuthSession session = buildSession();
		mDBApi = new DropboxAPI<AndroidAuthSession>(session);
		if(!mDBApi.getSession().isLinked()){
			//Log.i(MODULE, " no dropbox linked.. starting process");
			mDBApi.getSession().startAuthentication(MainActivity.this);
		}
		else{
			if(isNewUser()){
				GatherUserInfo test = new GatherUserInfo();
				test.execute();
			}else{
				SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
				((TextView)findViewById(R.id.user_id_info)).setText("Hi "+prefs.getString(USER_KEY, ""));
			}
		}

        if(checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(_context);

            if (regid.isEmpty()) {
                registerInBackground();
            }else{
                Log.i(MODULE,"device registed id "+regid);
            }
        }else{
            Log.i(MODULE,"No valid Google Play Services APK found");
        }
	}
	//	
	@Override 
	public void onResume(){
		super.onResume();
		Log.e(MODULE, "is reader running "+isActivityRunning());
		if(isActivityRunning()){
			Intent i = new Intent(getApplicationContext(), NFCReader.class);
			startActivity(i);
			finish();
		}
		else if (_action !=null && Intent.ACTION_SEND.equals(_action)) {                // CAME FROM DROPBOX
          //  Toast.makeText(getApplicationContext(), "Got intent from drobox starting writer", Toast.LENGTH_SHORT).show();
            _fromDropbox = true;
			String shareLink = _extras.getString("android.intent.extra.TEXT");// Get resource path
			Log.i(MODULE, shareLink);
            String device = android.os.Build.MODEL;
            startNFCWriter(shareLink,"no file size",device,"NULL");
		}
        else if(_intent.getStringExtra("message")!=null) {                           // CAME FROM PC
          //  Toast.makeText(getApplicationContext(), "Got intent from GCM starting writer", Toast.LENGTH_SHORT).show();
            Log.i(MODULE, _intent.getExtras().toString());
            String shareLink = _intent.getStringExtra(MESSAGE_KEY);
            String file_size = _intent.getStringExtra(FILE_SIZE_KEY);
            String device    = _intent.getStringExtra(DEVICE_KEY);
            String desk_path = _intent.getStringExtra(DESKTOP_PATH_KEY);
            Log.i(MODULE,"desktop path "+desk_path);
            startNFCWriter(shareLink,file_size,device,desk_path);
        }
		else{
			if (mDBApi.getSession().authenticationSuccessful()) {
				try {
					// Required to complete auth, sets the access token on the session
					mDBApi.getSession().finishAuthentication();
					tokens = mDBApi.getSession().getAccessTokenPair();
					storeKeys(tokens.key, tokens.secret);
				} catch (IllegalStateException e) {
					//Log.e("DbAuth//Log", "Error authenticating", e);
				}
			}
			if(isNewUser()){
				GatherUserInfo test = new GatherUserInfo();
				test.execute();
			}else{
				SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
				TextView textView = (TextView) findViewById(R.id.app_description);
				SpannableString content = new SpannableString(getString(R.string.start_message));
				content.setSpan(new UnderlineSpan(), 58, 63, 0);
				textView.setText(content);
				((TextView)findViewById(R.id.user_id_info)).setText(getString(R.string.hi)+" "+prefs.getString(USER_KEY, "")+"!");
			}
			initView();
			updateCountViews();
			initDPButton();
		}
        checkPlayServices();

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if (requestCode == 1) {
			if(resultCode == RESULT_OK){
				this.onStop();
			}
			if (resultCode == RESULT_CANCELED) {
				//Write your code if there's no result
			}
		}
	}
	@Override
	public void onStop(){
		super.onStop();
	//	if(_fromDropbox) // if the app was open through the share command on dropbox we want to destroy it once the share starts.
		//	finish();
	}
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the _action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    private void startNFCWriter(String share_link, String file_size, String device, String desktop_path){
        Intent i = new Intent(getApplicationContext(), NFCWriter.class);
        i.putExtra(SHARE_LINK_KEY, share_link);
        i.putExtra(FILE_SIZE_KEY, file_size);
        i.putExtra(DESKTOP_PATH_KEY,desktop_path);
        i.putExtra(DEVICE_KEY,device);

        if(!share_link.equals("") && !file_size.equals("") && !desktop_path.equals("") && !device.equals(""))
           // Toast.makeText(getApplicationContext(), "Starting NFC Writer, got all the fields", Toast.LENGTH_SHORT).show();

        startActivityForResult(i, 0);
        finish();
    }

	public boolean isActivityRunning() { 
		return NFCReader.isRunning();
	}
	private void initView(){
		_manageLinks = (Button)findViewById(R.id.manage_share_btn);
		_manageLinks.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction()){
				case MotionEvent.ACTION_DOWN:
					v.setBackgroundResource(R.drawable.button_background_pressed);
					((TextView)v).setTextColor(Color.parseColor("#FF0000"));
					break;
				case MotionEvent.ACTION_UP:
					v.setBackgroundResource(R.drawable.button_background);
					((TextView)v).setTextColor(Color.parseColor("#FFFFFF"));
					Uri uriUrl = Uri.parse("https://www.dropbox.com/links");
					Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
					startActivity(launchBrowser);
//                    saveUserInfoInFile();
					break;
				}
				return true;
			}
		});
	}
	private void initDPButton(){
		Button DPButton = (Button)findViewById(R.id.openDP);
		DPButton.setOnTouchListener(new OnTouchListener(){

			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				switch (arg1.getAction()){
				case MotionEvent.ACTION_DOWN:
					arg0.setBackgroundColor(Color.parseColor("#247AE0"));

					break;

				case MotionEvent.ACTION_UP:
					arg0.setBackgroundColor(Color.TRANSPARENT);
					goToDropbox();
					break;
				}
				return true;
			}
		});
	}
	private void goToDropbox(){
		try{
			Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage("com.dropbox.android");
			startActivity(LaunchIntent);
		}catch(Exception e){
			finish();
		}
	}
	private void updateCountViews(){
		SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
		int user_id = Integer.parseInt(prefs.getString(USER_ID_KEY, "0"));
		int[] count_tags = DBManager.getDBManager().countTags(user_id);
		((TextView)findViewById(R.id.tags_created)).setText(count_tags[0]+"");
		((TextView)findViewById(R.id.tags_read)).setText(count_tags[1]+"");
		//Log.e(MODULE, "got tags count");
	}
	private boolean isNewUser(){
		SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
		return !prefs.getBoolean(INSERTED_KEY, false);
	}
	private String[] getKeys() {
		SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
		String key = prefs.getString(ACCESS_KEY_NAME, null);
		String secret = prefs.getString(ACCESS_SECRET_NAME, null);
		if (key != null && secret != null) {
			String[] ret = new String[2];
			ret[0] = key;
			ret[1] = secret;
			return ret;
		} else {
			return null;
		}
	}
	private void storeKeys(String key, String secret) {
		// Save the access key for later
		SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
		Editor edit = prefs.edit();
		edit.putString(ACCESS_KEY_NAME, key);
		edit.putString(ACCESS_SECRET_NAME, secret);
		edit.commit();
	}
	private AndroidAuthSession buildSession() {
		AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
		AndroidAuthSession session;
		String[] stored = getKeys();
		if (stored != null) {
			AccessTokenPair accessToken = new AccessTokenPair(stored[0], stored[1]);
			session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE, accessToken);
		} else {
			session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE);
		}
		return session;
	}
	private void testGatherLink(String path){
		try {
			DropboxLink response;
			response = mDBApi.share(path);
			//Log.i("teste", "share link created "+response.url);
		} catch (DropboxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void storeUserInfo(String name,String id,String email){
		//Log.e(MODULE, "storing user data");
		((TextView)findViewById(R.id.user_id_info)).setText(getString(R.string.hi)+" "+name+"!");
		TextView textView = (TextView) findViewById(R.id.app_description);
		SpannableString content = new SpannableString(getString(R.string.start_message));
		content.setSpan(new UnderlineSpan(), 66, 71, 0);
		textView.setText(content);
		SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
		Editor edit = prefs.edit();
		edit.putString(USER_KEY, name);
		edit.putString(USER_ID_KEY, id);
		edit.putString(EMAIL_KEY, email);
		edit.commit();

        saveUserInfoInFile(); // saves this info along with the gcm info in a local file;
	}
	public void handleWriteNfc(View v){
		Intent i = new Intent(getApplicationContext(),NFCWriter.class); 
		startActivity(i);
	}
	public void handleReadNfc(View v){
		Intent i = new Intent(getApplicationContext(),NFCReader.class); 
		startActivity(i);
	}
	public void handleDropBoxClick(View v){
		v.setBackgroundColor(Color.parseColor("#247AE0"));
		Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage("com.dropbox.android");
		startActivity(LaunchIntent);
		this.finish();
	}
//
//	public void handleManageSharesClick(View v){
//		if(v.getId()==R.id.manage_share_btn){
//			Uri uriUrl = Uri.parse("https://www.dropbox.com/links");
//			Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
//			startActivity(launchBrowser);
//		}
//	}
    //
    //
    //GCM STUFF
    public boolean checkPlayServices(){
        int result_code = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(result_code != ConnectionResult.SUCCESS){
            if(GooglePlayServicesUtil.isUserRecoverableError(result_code)){
                GooglePlayServicesUtil.getErrorDialog(result_code,this,PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }else{
                Log.i(MODULE,"This device is not Supported");
                finish();
            }
            return false;
        }

        return true;
    }
    private String getRegistrationId(Context context){
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID,"");
        if(registrationId.isEmpty()){
            Log.i(MODULE, "Registration Not Found");
            return "";
        }else
            Log.i(MODULE,"found registration id  "+registrationId);

        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MAX_VALUE);
        int currentVersion    = getAppVersion(context);
        if(registeredVersion != currentVersion){
            Log.i(MODULE,"App version changed");
            return "";
        }
        return registrationId;
    }

    private SharedPreferences getGCMPreferences(Context c){
        return getSharedPreferences(MainActivity.ACCOUNT_PREFS_NAME,c.MODE_PRIVATE);
    }

    private static int getAppVersion(Context c){
        try{
            PackageInfo packageInfo = c.getPackageManager().getPackageInfo(c.getPackageName(),0);
            return packageInfo.versionCode;
        }catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not get package name: " + e);
        }

    }
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(MODULE, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }
    private void registerInBackground(){
        new AsyncTask(){
            @Override
            protected String doInBackground(Object[] params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(_context);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID = " + regid;
                    // sendRegistrationIdToBackEnd();
                    storeRegistrationId(_context, regid);
                }catch(IOException e){
                    msg = "Error: "+e.getMessage();
                }
                return msg;
            }
            @Override
            protected void onPostExecute(Object msg){Log.e(MODULE,msg + "\n");
            }
        }.execute(null,null,null);

    }
    private void saveUserInfoInFile(){

        File user_info_file  = new File(Environment.getExternalStorageDirectory(), "/touchCloud");
        user_info_file       = new File(user_info_file, "user.info");
        try {
            FileOutputStream fo = new FileOutputStream(user_info_file);
            SharedPreferences sp = getSharedPreferences(MainActivity.ACCOUNT_PREFS_NAME, MODE_PRIVATE);

            Log.i(MODULE, " uid: " + sp.getString(USER_KEY, "no dp user found") + "  regId: " + sp.getString(PROPERTY_REG_ID, "no gcm user found") + "  access key: " +
                    " " + sp.getString(ACCESS_KEY_NAME, " no access key found") + " access secret: " + sp.getString(ACCESS_SECRET_NAME, "no user secret found"));

            String file_content = sp.getString(USER_ID_KEY, "no dp user found") + "\n" +
                    sp.getString(PROPERTY_REG_ID, "no gcm user found") + "\n" +
                    sp.getString(ACCESS_KEY_NAME, " no access key found") + "|" + sp.getString(ACCESS_SECRET_NAME, "no user secret found");

            fo.write(file_content.getBytes());
            fo.close();
            uploadUserInfoFile(user_info_file);

        }catch (Exception e){
            Log.e(MODULE, "problem writing user file");
            e.printStackTrace();
        }
    }
    private void uploadUserInfoFile(File file){

        new AsyncTask(){
            @Override
            protected String doInBackground(Object[] params) {
                File file = (File)params[0];
                try {
                    FileInputStream inputStream = new FileInputStream(file);
                    DropboxAPI.Entry response = mDBApi.putFileOverwrite("Apps/Touchcloud/user.info", inputStream,
                            file.length(), null);
                    return "success";
                }catch(Exception e){
                    Log.e(MODULE," problem uploading file");
                    e.printStackTrace();
                    return "error";
                }

            }

        }.execute(file,null,null);
    }
    //
    //
    //
    //DEBUG MODAFACA
    public void handleDebugButtonClick(View v){

        new AsyncTask(){
            @Override
            protected Void doInBackground(Object[] params) {
                try {
                    Log.i(MODULE, "performing request");
                    URL url = new URL("https://api.pushbullet.com/v2/pushes");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    String api_key = " Bearer UCJ1PoWIwERf4xl4ZNYyXdMXQkcWd2ro";
                    connection.setRequestProperty ("Authorization", api_key);
                    connection.setRequestProperty ("Content-Type", "application/json");
                    connection.setUseCaches(true);
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setRequestMethod("POST");
                    OutputStream os = connection.getOutputStream();
                    String request = "{'type': 'note', 'title': 'Note Title', 'body': 'Note Body'}";
                    byte[] outputInBytes = request.getBytes("UTF-8");
                    os.write( outputInBytes );
                    os.close();

                    Map<String, List<String>> hdrs = connection.getHeaderFields();
                    Set<String> hdrKeys = hdrs.keySet();

                    for (String k : hdrKeys)
                        System.out.println("Key: " + k + "  Value: " + hdrs.get(k));


                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        Log.i(MODULE, "OK");
                    } else {
                        Log.i(MODULE, "PROBLEM "+connection.getResponseCode());
                        Log.i(MODULE, "PROBLEM "+connection.getResponseMessage());
                    }

                }catch(Exception e){
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();

    }
	/*
	 * 
    PRIVATE CLASSES
	 *
	 */
	private class  GatherUserInfo extends AsyncTask{

		private Bundle getUserInfo(){ // POSSO VERIFICAR PRIMEIRO SE JA EXISTE NAS SP
			try {
				//Log.i(MODULE, " no dropbox linked.. getting user info");
				Message msg = new Message();
				Account usr;
				usr = mDBApi.accountInfo();
				//Log.i("Test Gather Info", usr.displayName+" "+usr.country+" "+usr.uid);
				msg.arg1=1;
				Bundle b = new Bundle();
				b.putString(USER_KEY,usr.displayName);
				b.putString(USER_ID_KEY,usr.uid+"");
				msg.setData(b);
				gui_handler.sendMessage(msg);
				return b;
			} catch (DropboxException e) {
				e.printStackTrace();
				return null;
			}
		}
		//	@SuppressLint("CommitPrefEdits")
		private void flagAsInserted(){
			SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
			Editor edit = prefs.edit();
			edit.putBoolean(INSERTED_KEY, true);
			edit.commit();
			//Log.i(MODULE, "flagged");
		}
		@Override
		protected Object doInBackground(Object... arg0) {
			Bundle b = getUserInfo();	// get user info from dropbox.
			if(b!=null){
				String name =b.getString(USER_KEY);
				String id = b.getString(USER_ID_KEY);
				// Building Parameters
				List<NameValuePair> params = new ArrayList<NameValuePair>();
				params.add(new BasicNameValuePair(USER_KEY, name));
				params.add(new BasicNameValuePair(USER_ID_KEY, id));
				params.add(new BasicNameValuePair(PROPERTY_REG_ID, regid));

				DefaultHttpClient httpClient = new DefaultHttpClient();
				HttpPost httpPost = new HttpPost("http://aveiro.m-iti.org/touchCloudProxy/insert_user.php");
				try {
					httpPost.setEntity(new UrlEncodedFormEntity(params));
					HttpResponse httpResponse = httpClient.execute(httpPost);
					HttpEntity httpEntity = httpResponse.getEntity();
					InputStream is = httpEntity.getContent();
					BufferedReader reader = new BufferedReader(new InputStreamReader(
							is, "iso-8859-1"), 8);
					StringBuilder sb = new StringBuilder();
					String line = null;
					while ((line = reader.readLine()) != null) {
						sb.append(line + "\n");
					}
					is.close();
					JSONObject result = new JSONObject(sb.toString());
					int isInserted = result.getInt("success");
                    Log.i(MODULE, result.toString());
					if((isInserted==1 || isInserted==2) && !regid.equals("")) // inserted or already exists in the server
						flagAsInserted();
				}
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return null;	
		}
	}

	// @SuppressLint("HandlerLeak")
	private class GUI_Handler extends Handler{
		@Override
		public void handleMessage(Message msg)
		{
			switch(msg.arg1){
			case 1:
				storeUserInfo(msg.getData().getString(USER_KEY),msg.getData().getString(USER_ID_KEY),"");
				break;
			default:
				break;
			}
		}
	}
}