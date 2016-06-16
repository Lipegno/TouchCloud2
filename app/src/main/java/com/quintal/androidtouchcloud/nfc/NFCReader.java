package com.quintal.androidtouchcloud.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;
import com.quintal.androidtouchcloud.R;
import com.quintal.androidtouchcloud.mainActivities.EndOfTheRoad;
import com.quintal.androidtouchcloud.mainActivities.FailedUpdateActivity;
import com.quintal.androidtouchcloud.mainActivities.FileOpeningActivity;
import com.quintal.androidtouchcloud.remote.WebServerHandler;
import com.quintal.androidtouchcloud.storage.DBManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NFCReader extends Activity {
    private static final String MODULE = "NFCReader";
    private static final String MIME_TYPE = "application/com.quintal.androidtouchcloud.nfc";
    final static private String SHARE_LINK_KEY = "share_link";
    final static private String USER_KEY = "user";
    final static private String EMAIL_KEY = "email";
    final static private String USER_ID_KEY = "user_id";
    final static private String ACCOUNT_PREFS_NAME = "prefs";
    final static private String DEVICE_KEY = "device";
    //	private static final String DEBUG_MESSAGE_KEY = "debug_text";
    final static private String NFC_KEY = "nfc_id";
    final static private String TAG_TIMESTAMP_KEY = "event_time";

    final static private String APP_KEY = "96dw8siqo14c9rj";
    final static private String APP_SECRET = "t5ljqzpu2o6rwf0";
    final static private String ACCESS_KEY_NAME = "ACCESS_KEY";
    final static private String ACCESS_SECRET_NAME = "ACCESS_SECRET";
    final static private AccessType ACCESS_TYPE = AccessType.DROPBOX;
    private static boolean _isRunning = false;
    private static boolean _i_wasCalled = false;
    private DropboxAPI<AndroidAuthSession> mDBApi;

    private String _filename ="";

    private NfcAdapter _mNfcAdapter;
    private IntentFilter[] _mNdefExchangeFilters;
    private PendingIntent _mNfcPendingIntent;
    private String _nfc_id;
    //	private TextView _debugField;
    private boolean _oneAtTheTime=true; // stupid
    private UI_handler ui_handler;
    private String _debugText;
    private Button _cancelDownloadBtn;
    private ProgressBar _progressBar;
    private TextView _progressPercent;
    private LinearLayout _progressLayout;
    private LinearLayout _openOptionLayout;
    private TextView _fileNameLabel;
    private int _progress=0;
    private boolean downloading = true;
    private TextView _actTitleLabel;
    private boolean isFolder=false;
    private String _originalLink="";
    private String _dropboxLink="";
    private String _tagHash;
    private boolean isFirstTime=true;
    private Context _ctx;
    private File _tagged_file;

    LinearLayout _option_device;
    LinearLayout _option_remote;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getActionBar().setDisplayShowHomeEnabled(false);
        getActionBar().setDisplayShowTitleEnabled(false);
        getActionBar().hide();
        setContentView(R.layout.readtag_layout);
        _progressBar = (ProgressBar)findViewById(R.id.progressBar);
        _progressPercent = (TextView)findViewById(R.id.progress_percent);
        _progressBar.getProgressDrawable().setColorFilter(Color.RED, Mode.SRC_IN);
        _progressLayout = (LinearLayout)findViewById(R.id.progress_layout);
        _progressLayout.setVisibility(View.GONE);
        _fileNameLabel = (TextView)findViewById(R.id.file_name_label);
        _actTitleLabel =(TextView)findViewById(R.id.reading_tag_label);
        _mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        _mNfcPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this,getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP| Intent.FLAG_ACTIVITY_CLEAR_TOP)
                , 0);

        Intent intent = getIntent();
        String action = intent.getAction();
        //Log.i(MODULE, "->"+action+" "+isFirstTime);
        //android.nfc.action.NDEF_DISCOVERED

        //.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP| Intent.FLAG_ACTIVITY_CLEAR_TOP)
        AndroidAuthSession session = buildSession();
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);
        if(!mDBApi.getSession().isLinked()){
            //Log.i(MODULE, " no dropbox linked.. starting process");
            mDBApi.getSession().startAuthentication(NFCReader.this);
        }
        else{
            //Log.i(MODULE, "  user alredy linked ");
        }

        _ctx = getApplicationContext();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    @Override
    protected void onResume() {
        super.onResume();
        _isRunning=true;
        if(_mNfcAdapter != null) {

            if (!_mNfcAdapter.isEnabled()){

                //Log.e(MODULE, "NFC DISABLED");


            } else if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(getIntent().getAction())
                    || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
                processReadTag(getIntent());
            }

        } else {
            Toast.makeText(getApplicationContext(), "Sorry, No NFC Adapter found.", Toast.LENGTH_SHORT).show();
        }

        initView();
    }
    @Override
    public void onBackPressed(){
        Toast.makeText(getApplicationContext(), getString(R.string.back_pressed),
                Toast.LENGTH_SHORT).show();

    }
    @Override
    protected void onPause() {
        super.onPause();
        //downloading = false;
        //Log.e(MODULE, "onPause called");
        try{
            if(_mNfcAdapter != null) _mNfcAdapter.disableForegroundDispatch(this);
        }catch(Exception e){
            e.printStackTrace();
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onStop(){
        super.onStop();
        //Log.e(MODULE, "onStop called");
        //_isRunning=false;
//		if(_progress==100){
//			//Log.i(MODULE, "killing this");
        //	finish();
        //return;
//			}
    }
    public static boolean isRunning(){
        Log.d(MODULE, "i was called "+_isRunning);
        _i_wasCalled =true;
        return _isRunning;
    }
    private boolean isOnline(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null) {
            // There are no active networks.
            return false;
        }
        return true;
    }
    private void initView(){
        ui_handler = new UI_handler();
        _cancelDownloadBtn = (Button)findViewById(R.id.cancel_download_btn);
        _openOptionLayout = (LinearLayout)findViewById(R.id.option_picker_layout);
        _openOptionLayout.setVisibility(View.INVISIBLE);

        if(!_originalLink.equals("") && _progress<=0)
            _openOptionLayout.setVisibility(View.VISIBLE);

        setOptionListeners();

        if(_progress<=0)
            _cancelDownloadBtn.setVisibility(View.GONE);
        else
            _cancelDownloadBtn.setVisibility(View.VISIBLE);

        if(!isFolder){
            _cancelDownloadBtn.setOnTouchListener(new View.OnTouchListener() {
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
                            //Log.i(MODULE, "canceling download");
                            downloading = false;
                            cancelAndClose();
                            break;
                    }
                    return true;
                }
            });
        }
    }
    private void setOptionListeners(){
        _option_device = (LinearLayout)findViewById(R.id.option_device_btn);
        _option_remote = (LinearLayout)findViewById(R.id.option_remote_btn);

        _option_device.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction()==MotionEvent.ACTION_DOWN){
                    _option_device.setBackgroundColor(Color.parseColor("#FFCCCC"));
                }else if(motionEvent.getAction()==MotionEvent.ACTION_UP){
                 //   view.setBackgroundColor(Color.parseColor("#80000000"));
                    Log.i(MODULE,"aqui mudando back");
                    new FileOpenerHandler().execute(1);
                    _option_device.setBackgroundColor(Color.argb(0,255,255,255));

                }
                return true;
            }
        });
        _option_remote.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction()==MotionEvent.ACTION_DOWN){
                    _option_remote.setBackgroundColor(Color.parseColor("#FFCCCC"));
                }else if(motionEvent.getAction()==MotionEvent.ACTION_UP){
                    _option_remote.setBackgroundColor(Color.argb(0,255,255,255));
                    new SendtoPCHandler().execute(1);
                    Toast.makeText(getApplicationContext(),"You can know open this tag on you computer",Toast.LENGTH_LONG).show();
                    cancelAndClose();
                }
                return true;
            }
        });

    }
    // dropbox stuff
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
    //dropbox stuff
    private String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("0x");
        if (src == null || src.length <= 0) {
            return null;
        }

        char[] buffer = new char[2];
        for (int i = 0; i < src.length; i++) {
            buffer[0] = Character.forDigit((src[i] >>> 4) & 0x0F, 16);
            buffer[1] = Character.forDigit(src[i] & 0x0F, 16);
            stringBuilder.append(buffer);
        }

        return stringBuilder.toString();
    }
    @SuppressWarnings("unchecked")
    private synchronized void processReadTag(Intent intent){

        // UCJ1PoWIwERf4xl4ZNYyXdMXQkcWd2ro

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            NdefMessage[] messages = null;
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            byte[] tagId = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
            _nfc_id = bytesToHexString(tagId);
            Log.e(MODULE, "TAG ID->"+_nfc_id);
            if (rawMsgs != null) {
                messages = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    messages[i] = (NdefMessage) rawMsgs[i];
                }
            }
            if(messages[0] != null) {
                _tagHash="";
                byte[] payload = messages[0].getRecords()[0].getPayload();
                // this assumes that we get back am SOH followed by host/code
                for (int b = 1; b<payload.length; b++) { // skip SOH
                    _tagHash += (char) payload[b];
                }
                //Toast.makeText(getApplicationContext(), "Loading your Dropbox contents...", Toast.LENGTH_SHORT).show();
                //((TextView)findViewById(R.id.tag_content)).setText(_tagLink);
                if(!_tagHash.equals("") && _oneAtTheTime){
                    _oneAtTheTime=false;  //stupid hack
                    //Log.i(MODULE, _tagHash);
                    SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0); // get shared prefs to get the user data
                    int user_id=-1;
                    try{
                        user_id = Integer.parseInt(prefs.getString(USER_ID_KEY, ""));
                    }
                    catch(NumberFormatException e){
                        //Log.i(MODULE, "erro de number format ");
                    }
                    DBManager.getDBManager().insertReadEvents(_nfc_id
                            ,user_id,android.os.Build.MODEL, System.currentTimeMillis()); //insert locally

                    _originalLink = _tagHash;
                    _tagHash 	  =_tagHash.split("=")[1].split("&")[0];
                    //	_tagHash = "https://www."+_tagHash;
                    new FileLookUpHandler().execute();

                }
            }
        }
    }
    //01-22 21:08:15.767: I/NFCWriter(30496): result dropbox.com/s/awo8luiocpzmsst/regulation.docx

    private void updateDebugText(String debug){
        _debugText = _debugText+"\n\n"+debug;
    }

    private String[] checkFileNameSimple(){
        String[] result = new String[2];
        String[] tag_tokens = _dropboxLink.split("/");
        String filename = tag_tokens[tag_tokens.length-1];
        Log.i(MODULE,"file name antes "+filename);
        filename = (filename.split("\\?"))[0];
        result[0] = filename;
        result[1] = _dropboxLink;
        Log.i(MODULE,"file name simple: "+filename+ " dropbox link "+_dropboxLink);
        return result;
    }
    private void notifyBrokenLink(){
        //Log.e(MODULE, "Broken Linlk!!");
        Message msg = Message.obtain();
        msg.arg1 = 2;
        ui_handler.sendMessage(msg);
    }
    private void notifyNoConnection(){
        Message msg = Message.obtain();
        msg.arg1 = 7;
        ui_handler.sendMessage(msg);
    }
    private void notifyPrivateTag(){
        Message msg = Message.obtain();
        msg.arg1 = 5;
        ui_handler.sendMessage(msg);
    }
    private void alertNoApp(){
        Message msg = Message.obtain();
        msg.arg1 = 6;
        ui_handler.sendMessage(msg);
    }
    private String buildDownloadLink(String path){
        String url = path.replace("www.dropbox.com", "dl.dropboxusercontent.com");
        return url;
    }
    private HttpURLConnection buildConnectionToFile(String path, String filename) throws IOException{
        filename = URLEncoder.encode(filename, "UTF-8");
        filename = filename.replace("+", "%20");
        path = path.substring(0,path.lastIndexOf("/"));
        URL http_url = new URL(buildDownloadLink(path+"/"+filename));
        HttpURLConnection urlConnection = (HttpURLConnection) http_url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setDoOutput(true);
        urlConnection.setRequestProperty("accept-charset", "UTF-8");
        urlConnection.setRequestProperty("content-type", "application/x-www-form-urlencoded");
        return urlConnection;
    }
    private void cancelAndClose(){
        _isRunning = false;
        Intent i = new Intent(getApplicationContext(),EndOfTheRoad.class);
        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP| Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra("action", 2);
        startActivity(i);			//make sure there arent any loose ends
        finish();
    }

//    public void handleOpenFileClick(View v){
//        if(v.getId()==R.id.open_file_device_btn){
//            _openOptionLayout.setVisibility(View.INVISIBLE);
//            new FileOpenerHandler().execute(1);
//        }else if(v.getId()==R.id.open_file_remote_btn){
//
//        }
//    }


    //
    //  DOWNLOAD AND OPENING STUFF
    //

    private synchronized void  doDownloadFileDirectly(String path,String name){
        try {

            _isRunning=true;

            HttpURLConnection urlConnection = buildConnectionToFile(path,name);
            Log.i(MODULE, "Downloading file "+name+" at "+urlConnection.getURL());
            urlConnection.connect();

            File SDCardRoot = Environment.getExternalStorageDirectory();
            File file = new File(SDCardRoot+"/touchCloud/temp_files/",name);
            FileOutputStream fileOutput = new FileOutputStream(file);
            InputStream inputStream = urlConnection.getInputStream();
            int totalSize = urlConnection.getContentLength();
            int downloadedSize = 0;
            double temp=0;
            byte[] buffer = new byte[1024];
            int bufferLength = 0; //used to store a temporary size of the buffer

            Message msg2 = new Message();
            msg2.arg1 = 3;
            ui_handler.sendMessage(msg2);

            msg2 = Message.obtain();
            msg2.arg1 = 4;
            ui_handler.sendMessage(msg2);

            msg2 = Message.obtain();
            msg2.arg1 = 9;
            ui_handler.sendMessage(msg2);

            Message msg = new Message();
            msg.arg1 = 1;

            while ( downloading && ((bufferLength = inputStream.read(buffer)) > 0)  ) {
                fileOutput.write(buffer, 0, bufferLength);
                downloadedSize += bufferLength;
                temp = (double)downloadedSize/(double)totalSize;
                msg = Message.obtain();
                msg.arg1 = 1;
                _progress = (int) Math.round(temp*100);
                ui_handler.sendMessage(msg);
                Log.i(MODULE, "aqui downloading "+downloading);
                //   //Log.i(MODULE,"Downloading "+downloadedSize+" "+totalSize+" "+temp+" "+_progress);
            }
            fileOutput.close();
            //Log.i(MODULE,"Download complete");
            msg = Message.obtain();
            msg.arg1 = 1;
            Log.i(MODULE,"opening File");
            updateDebugText("opening File");
            //ui_handler.sendMessage(msg);
            if(downloading)
                openFile(file);
            else{
                //Log.i(MODULE,"download canceled");
                onPause();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (javax.net.ssl.SSLException e) {
            e.printStackTrace();
            notifyNoConnection();
        } catch (IOException e) {
            e.printStackTrace();
            notifyBrokenLink();

        }
    }
    private int getRemoteFileSize(String url, String name){
        try {

            HttpURLConnection urlConnection = buildConnectionToFile(url,name);

            urlConnection.connect();
            int totalSize = urlConnection.getContentLength();
            //Log.i(MODULE, "checking remote file size "+totalSize);
            return totalSize;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return -1;
        }
    }
    private void openFile(File file){
        _isRunning=false;
        Log.i(MODULE, "not created from main");
        Intent myIntent2 = new Intent(_ctx,FileOpeningActivity.class);
        myIntent2.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        Log.i(MODULE,getApplicationContext().toString());
        myIntent2.putExtra("file_path", file.getAbsolutePath());
        myIntent2.putExtra("action", 1);
        startActivity(myIntent2);
        finish();
    }
    private void downloadAndOpen(){
       String []file_meta = checkFileNameSimple();
        _filename = file_meta[0];
//        Message msg2 = Message.obtain();
//        msg2.arg1 = 9;
//        ui_handler.sendMessage(msg2);

        String share_link = file_meta[1].split("/")[3];
        if(!file_meta[0].equals("") && share_link.equals("sh")){            // verify if its a folder if yes open directly
            Intent myIntent2 = new Intent(_ctx,FileOpeningActivity.class);
            myIntent2.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            Log.i(MODULE,getApplicationContext().toString());
            myIntent2.putExtra("url", file_meta[1]);
            myIntent2.putExtra("action", 2);
            startActivity(myIntent2);
            finish();
        }else{
            //Log.i(MODULE," itsa file");
            if(isOnline())
                doDownloadFileDirectly(file_meta[1],file_meta[0]);
            else
                notifyNoConnection();

        }
    }
    private String getLinkFromServer(String hash){
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        int uid = Integer.parseInt(prefs.getString(USER_ID_KEY, "1"));
        String dropbox_link = WebServerHandler.get_WS_Handler().getTagRead(uid+"",hash);
        Log.i(MODULE, "Getting link from server dawg");
        return dropbox_link;
    }
    private String getLinkFromHash(String hash){
        hash = _tagHash.replace("https://www.dropbox.com/", "");
        String dropbox_link  = DBManager.getDBManager().getMetadataFromHash(hash)[0];

        if(dropbox_link.equals("")) {
            dropbox_link = getLinkFromServer(hash);
        }else{
            Log.i(MODULE, "found file info locally hash: "+hash);
        }

        return dropbox_link;
    }
    private List<File> getListFiles(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                inFiles.addAll(getListFiles(file));
            } else {
                inFiles.add(file);
            }
        }
        return inFiles;
    }
    private File isFavorite(String filename, List<File> files){
        ArrayList<File> selectedFiles = new ArrayList<File>();
        for(File file: files){
            if(file.getName().equals(filename)){ // verify size to check if is the same size
                selectedFiles.add(file);
                //Log.d(MODULE,"found file with the same name");
            }
        }
        if(selectedFiles.size()>0){ // no need to check the remote size if there arent any local files that match the tag's file name
            if(isOnline()){			// if te system is not online return the default one
                long link_size = getRemoteFileSize(_dropboxLink, filename);
                for(File file: selectedFiles){
                    //Log.d(MODULE," file size "+file.length());
                    if(file.length()==link_size){ // verify size to check if is the same size
                        //Log.d(MODULE,"found file with the same size");
                        return file;
                    }
                }
            }else
                return selectedFiles.get(0);
        }
        return null;
    }
    /*
     *
     * PRIVATE CLASSES YO
     *
     */
    private class  InsertTagRead extends AsyncTask{

        private Bundle getUserInfo(){ // POSSO VERIFICAR PRIMEIRO SE JA EXISTE NAS SP
            SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
            Message msg = new Message();
            Bundle b = new Bundle();
            b.putString(USER_KEY,prefs.getString(USER_KEY,"0"));
            b.putString(USER_ID_KEY,prefs.getString(USER_ID_KEY,"0"));
            b.putString(EMAIL_KEY, prefs.getString(EMAIL_KEY,""));
            msg.setData(b);
            // g//ui_handler.sendMessage(msg);
            return b;
        }
        @Override
        protected Object doInBackground(Object... arg0) {
            Bundle b = getUserInfo();	// get user info from dropbox.
            if(b!=null){
                String id = b.getString(USER_ID_KEY);
                SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String date = s.format(new Date());
                // Building Parameters
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair(NFC_KEY, _nfc_id));
                params.add(new BasicNameValuePair(USER_ID_KEY, id));
                params.add(new BasicNameValuePair(TAG_TIMESTAMP_KEY, date));
                params.add(new BasicNameValuePair(DEVICE_KEY,android.os.Build.MODEL));
                params.add(new BasicNameValuePair(SHARE_LINK_KEY,_dropboxLink));
                //Log.i(MODULE+" insert", " ->"+_dropboxLink);
                DefaultHttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost("http://aveiro.m-iti.org/touchCloudProxy/insert_tag_read.php");
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
                    @SuppressWarnings("unused")
                    JSONObject result = new JSONObject(sb.toString());
                    //Log.i(MODULE, "-> "+sb);

                }
                catch (Exception e) {
                    // TODO Auto-generated catch block
                    //e.printStackTrace();
                    //notifyBrokenLink();
                }



            }
            return null;
        }
    }
    private class GotoDropBox extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... arg0) {
            Uri uri = Uri.parse(_tagHash);
            Intent i = new Intent(Intent.ACTION_VIEW, uri);
            startActivityForResult(i,1);
            return null;
        }

    }

    private class FileLookUpHandler extends AsyncTask{


        @Override
        protected Object doInBackground(Object... objs) {
            try{
                String data[];
                _dropboxLink = getLinkFromHash(_tagHash);
                new InsertTagRead().execute(); // now that we have the original link we can start the insertion of the read event in the server
                if(_dropboxLink.equals("IOERROR")){
                    notifyBrokenLink();
                    return null;
                }
                else if(_dropboxLink.equals("error:PRIVATE_TAG")){
                    //Log.i(MODULE, "private tag dawg");
                    notifyPrivateTag();
                    return null;
                }
                else{

                    String decodedTag = URLDecoder.decode(_dropboxLink);
                    _dropboxLink = decodedTag;
                    //sends a message with to the ui handler to put the file name
                    Log.i(MODULE,"dropbox link "+_dropboxLink);
                    Log.i(MODULE,"decoded link "+decodedTag);
                    //starts the verification if its file/folder/favourite/...
                    //Log.i(MODULE," checking if file info exists in the database ");
                    data = DBManager.getDBManager().getMetadataFromHash(decodedTag);
                    String filename="";
                    //Log.i(MODULE," file data "+data[0]+","+data[1]+","+data[2]);
                    if(data[2].equals("")){
                        //Log.i(MODULE, "no file info locally");
                        data = checkFileNameSimple();
                        filename = data[0];
                    }else{
                        filename = data[2];
                        Log.i(MODULE, "found file info locally");
                    }
                    File dropbox_root = new File(Environment.getExternalStorageDirectory()+"/Android/data/com.dropbox.android/files/");
                    if(!dropbox_root.exists())
                        dropbox_root = new File(Environment.getExternalStorageDirectory()+"/Android/data/com.dropbox.android/");

                    List<File> files =  getListFiles(dropbox_root);
                    _tagged_file = isFavorite(filename,files);

                    Message msg2;
                    msg2 = Message.obtain();
                    msg2.arg1 = 4;
                    ui_handler.sendMessage(msg2);
                    msg2 = Message.obtain();
                    msg2.arg1 = 8;
                    ui_handler.sendMessage(msg2);

                    return null;
                }
            }catch(Exception e){
                //notifyBrokenLink();
                return null;
            }
        }
    }
    private class FileOpenerHandler extends AsyncTask<Integer,Void,Void>{

        @Override
        protected Void doInBackground(Integer... args) {
            if(args[0]==1) {
                if (_tagged_file != null) {
                    Message msg2 = new Message();
                    msg2.arg1 = 4;
                    ui_handler.sendMessage(msg2);
                    openFile(_tagged_file);
                } else {
                    //Log.i(MODULE, "its not fav");
                    downloadAndOpen();
                }
            }else{
                // open in PC CODE
            }
            return null;
        }
    }

    private class UI_handler extends Handler{

        private void formatFileFolderInfo(){
            String[] file_meta = checkFileNameSimple();
            String share_link = file_meta[1].split("/")[3];
            //Log.i(MODULE, " aqui checking file type 1");
            if(!file_meta[0].equals("") && share_link.equals("sh")){
                //Log.i(MODULE," itsa folder");
                _actTitleLabel.setText(getString(R.string.opening_folder));
                _fileNameLabel.setText(getString(R.string.chose_device_question)+"  folder?");
                _progressLayout.removeAllViewsInLayout();
                isFolder=true;
            }else{
                //Log.i(MODULE," itsa file");
                _actTitleLabel.setText(getString(R.string.opening_file));
                _fileNameLabel.setText(getString(R.string.chose_device_question)+" "+file_meta[0]+" file?");
            }
        }

        @Override
        public void handleMessage(Message msg)
        {
            switch(msg.arg1){
                case 1:
                    ////Log.i(MODULE, " "+progress);
                    _progressBar.setProgress(_progress);
                    _progressPercent.setText(_progress+"%");
                    break;
                case 2:
                    //Log.i(MODULE, "-> 0 aqui");
                    Intent novo = new Intent(getApplicationContext(),FailedUpdateActivity.class);
                    novo.putExtra("ORIGIN", 2);
                    startActivity(novo);
                    finish();
                    break;
                case 3:
                    _progressBar.setVisibility(View.VISIBLE);
                    _progressLayout.setVisibility(View.VISIBLE);
                    _cancelDownloadBtn.setVisibility(View.VISIBLE);
                    _progressLayout.invalidate();
                    break;
                case 4:
                    formatFileFolderInfo();
                    _openOptionLayout.setVisibility(View.GONE);
                    break;
                case 5:
                    Intent private_tag_int = new Intent(getApplicationContext(),FailedUpdateActivity.class);
                    private_tag_int.putExtra("ORIGIN", 3);
                    startActivity(private_tag_int);
                    finish();
                    break;
                case 6:
                    Intent no_app_int = new Intent(getApplicationContext(),FailedUpdateActivity.class);
                    no_app_int.putExtra("ORIGIN", 4);
                    startActivity(no_app_int);
                    finish();
                    break;
                case 7:
                    Intent no_connection = new Intent(getApplicationContext(),FailedUpdateActivity.class);
                    no_connection.putExtra("ORIGIN", 5);
                    startActivity(no_connection);
                    finish();
                    break;
                case 8:
                   _openOptionLayout.setVisibility(View.VISIBLE);
                    break;
                case 9:
                    _fileNameLabel.setText(_filename);
                    break;
                default:
                    break;

            }

        }
    }

    private class SendtoPCHandler extends AsyncTask <Integer,Void,Void>{

        @Override
        protected Void doInBackground(Integer... integers) {

        //   String u54id, String dropbox_link, String file_name,String opened, String date, String tag_id, String filesize
            SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);

            String[] data = DBManager.getDBManager().getMetadataFromHash(_tagHash);
            String filename="";
           if(data[2].equals("")){
                //Log.i(MODULE, "no file info locally");
                data = checkFileNameSimple();
                filename = data[0];
            }else{
                filename = data[2];
                Log.i(MODULE, "found file info locally");
            }

            if(data[1].contains("/sh/")){  // one last check to verify if its folder
                filename = "folder";
            }

            String dropbox_link = null;
            dropbox_link = getLinkFromHash(_tagHash);
            String tag_code     = _nfc_id;
            String uid          = prefs.getString(USER_ID_KEY,"0");
            String opened       = "no";
            SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String date = s.format(new Date(System.currentTimeMillis()));
            String filesize     = getRemoteFileSize(dropbox_link,filename)+"";


            Log.i(MODULE,"deixa ver se tenho tudo");

            try {
                dropbox_link = URLEncoder.encode(getLinkFromHash(_tagHash), "UTF-8");
                filename            = URLEncoder.encode(filename,"UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            String result = WebServerHandler.get_WS_Handler().addToReadList(uid,dropbox_link,filename,opened,date,tag_code,filesize);

            Log.i(MODULE,result);

            return null;
        }
    }
    //		private String testSlashPath(String path){
    //			String path_slashed = path.substring(0,path.lastIndexOf('/')+1);
    //			//Log.i(MODULE, path_slashed);
    //			return path_slashed;
    //		}

    //		private boolean existsLocally(String path){
    //			return false;
    //		}

    //		private void doDownloadFileDummy(String file_name,String dp_filepath, String file_rev){
    //
    //
    //			//Log.i(MODULE,"name "+file_name+" path "+dp_filepath);
    //			String total_path = "/Android/data/com.dropbox.android/files/scratch"+dp_filepath;
    //			File file = new File(Environment.getExternalStorageDirectory()+testSlashPath(total_path));
    //			//Log.i(MODULE, "file path local "+file.getAbsolutePath());
    //			file.mkdirs();
    //			File file2 = new File(Environment.getExternalStorageDirectory()+"/Android/data/com.dropbox.android/files/scratch"+dp_filepath);
    //
    //			FileOutputStream outputStream;
    //			try {
    //
    //				Message msg = Message.obtain();
    //				msg.arg1 = 1;
    //				updateDebugText("Reaching to Dropbox!");
    //				//ui_handler.sendMessage(msg);
    //
    //				outputStream = new FileOutputStream(file2);
    //				DropboxFileInfo info = mDBApi.getFile(dp_filepath, null, outputStream, null);
    //				file_rev = info.getMetadata().rev;
    //				outputStream.close();
    //
    //				msg = Message.obtain();
    //				msg.arg1 = 1;
    //				updateDebugText("File Downloaded");
    //				//ui_handler.sendMessage(msg);
    //
    //				msg = Message.obtain();
    //				msg.arg1 = 1;
    //				updateDebugText("Updating local file metadata");
    //				//ui_handler.sendMessage(msg);
    //				if (!DBManager.getDBManager().updateLocalFileInfo(dp_filepath, file_rev)){
    //					//Log.w(MODULE, "Can't update.. will insert new");
    //					DBManager.getDBManager().insertLocalFileInfo(file_name,dp_filepath, file_rev,_tagLink);
    //				}
    //				msg = Message.obtain();
    //				msg.arg1 = 1;
    //				updateDebugText("Opening File");
    //				//ui_handler.sendMessage(msg);
    //				openFile(file2);
    //
    //			} catch (FileNotFoundException e) {
    //				// TODO Auto-generated catch block
    //				e.printStackTrace();
    //			} catch (DropboxException e) {
    //				// TODO Auto-generated catch block
    //				e.printStackTrace();
    //			} catch (IOException e) {
    //				// TODO Auto-generated catch block
    //				e.printStackTrace();
    //			}
    //		}

    //		private String checkFileRevision(String path){
    //			Entry existingEntry;
    //			try {
    //				//Log.i(MODULE,"getting file revision "+path);
    //				existingEntry = mDBApi.metadata(path, 1, null, false, null);
    //				return existingEntry.rev;
    //			} catch (Exception e) {
    //				e.printStackTrace();
    //				return "";
    //			}
    //		}

    //		private String checkFilePath(String name){
    //			try {
    //				List<Entry> exists = mDBApi.search("/", name, 100, false);
    //				//Log.i(MODULE, "aqui");
    //
    //				return exists.get(0).path;
    //			} catch (Exception e) {
    //				e.printStackTrace();
    //				return "";
    //			}
    //		}

    //		private String[] checkFileName(){
    //			String file_data [] = new String[2];
    //			List<NameValuePair> params = new ArrayList<NameValuePair>();
    //			params.add(new BasicNameValuePair(SHARE_LINK_KEY,_tagLink));
    //
    //			DefaultHttpClient httpClient = new DefaultHttpClient();
    //			HttpPost httpPost = new HttpPost("http://aveiro.m-iti.org/touchCloudProxy/file_look_up.php");
    //			try {
    //				httpPost.setEntity(new UrlEncodedFormEntity(params));
    //				HttpResponse httpResponse = httpClient.execute(httpPost);
    //				HttpEntity httpEntity = httpResponse.getEntity();
    //				InputStream is = httpEntity.getContent();
    //				BufferedReader reader = new BufferedReader(new InputStreamReader(
    //						is, "utf-8"), 8);
    //				StringBuilder sb = new StringBuilder();
    //				String line = null;
    //				while ((line = reader.readLine()) != null) {
    //					sb.append(line + "\n");
    //				}
    //				is.close();
    //				JSONObject result = new JSONObject(sb.toString());
    //				if(!result.getBoolean("success"))
    //					new GotoDropBox().execute();
    //
    //				file_data[0] = result.getString("message");
    //				file_data[1] = result.getString("file_path");
    //				return file_data;
    //			}
    //			catch (IOException e) {
    //				e.printStackTrace();
    //				return file_data;
    //			}
    //			catch (IllegalStateException e) {
    //				e.printStackTrace();
    //				return file_data;
    //			} catch (JSONException e) {
    //				e.printStackTrace();
    //				return file_data;
    //			}
    //		}
    //		private void lookUpLocalCopy(){
    //
    //			String []file_meta = checkFileName();
    //			String file_name = file_meta[0];
    //			//Log.i(MODULE, "->"+file_name+"*");
    //			//check file path
    //			Message msg = Message.obtain();
    //			msg.arg1 = 1;
    //			updateDebugText("Checking share link, file path");
    //			//ui_handler.sendMessage(msg);
    //			String file_path = checkFilePath(file_name);
    //			// check file revision
    //			msg = Message.obtain();
    //			msg.arg1 = 1;
    //			updateDebugText("Checking file revision");
    //			//ui_handler.sendMessage(msg);
    //			String revision = checkFileRevision(file_path);
    //			//check local revision
    //			String[] local_file = DBManager.getDBManager().getFileMetaData(file_name);
    //			//Log.e(MODULE,"dropbox file path "+file_path+" last revised: "+revision+"local file last revised: "+local_file[1]);
    //
    //			if(local_file[1]!=null){
    //				if(local_file[1].equals(revision)){
    //					//Log.i(MODULE, "wadup dawg you got the same file up in this shit");
    //					File file = new File(Environment.getExternalStorageDirectory()+"/Android/data/com.dropbox.android/files/scratch/"+file_path);
    //					openFile(file);
    //				}else{
    //
    //					//Log.i(MODULE, "wadup dawg you got a different version up in this shit");
    //					doDownloadFileDummy(file_name,file_path,revision);
    //				}
    //			}else{
    //				doDownloadFileDirectly(file_meta[1],file_name);
    //			}
    //		}

    //		private void oldDoinbk(){
    //			String dumm = _tagLink;
    //			Message msg = Message.obtain();
    //			msg.arg1 = 1;
    //			updateDebugText("Checking file name associated with share link in the DB");
    //			//ui_handler.sendMessage(msg);
    //
    //			String[] file_data = DBManager.getDBManager().getMetadataFromShareLink(dumm);
    //			String saved_db_path = file_data[0];
    //			if(!(saved_db_path.equals(""))){//first check if the share link was created here and there is a direct path to the file saved in the local db
    //
    //				msg = Message.obtain();
    //				msg.arg1 = 1;
    //				updateDebugText("share link found!!");
    //				//ui_handler.sendMessage(msg);
    //
    //				msg = Message.obtain();
    //				msg.arg1 = 1;
    //				updateDebugText("Checking if it's my file");
    //				//ui_handler.sendMessage(msg);
    //
    //				if(saved_db_path.equals("not_mine")){
    //					try{
    //
    //						msg = Message.obtain();
    //						msg.arg1 = 1;
    //						updateDebugText("It's not my file - opening");
    //						//ui_handler.sendMessage(msg);
    //
    //						File file = new File(Environment.getExternalStorageDirectory()+"/touchCloud/temp_files/"+file_data[2]);
    //						openFile(file);
    //					}catch(Exception e){
    //						e.printStackTrace();
    //						lookUpLocalCopy();
    //					}
    //				}else{
    //					msg = Message.obtain();
    //					msg.arg1 = 1;
    //					updateDebugText("It's my File .. checking revision");
    //					//ui_handler.sendMessage(msg);
    //
    //					try{
    //						if(file_data[1].equals(checkFileRevision(file_data[0]))){
    //
    //							msg = Message.obtain();
    //							msg.arg1 = 1;
    //							updateDebugText("File is up to date!!");
    //							//ui_handler.sendMessage(msg);
    //
    //							File file = new File(Environment.getExternalStorageDirectory()+"/Android/data/com.dropbox.android/files/scratch/"+file_data[0]);
    //							openFile(file);
    //						}else{
    //
    //							msg = Message.obtain();
    //							msg.arg1 = 1;
    //							updateDebugText("File is not the most recent revision... will download");
    //							//ui_handler.sendMessage(msg);
    //
    //							doDownloadFileDummy(file_data[2],file_data[0],file_data[1]);
    //						}
    //					}catch(Exception e){
    //						e.printStackTrace();
    //						lookUpLocalCopy();
    //					}
    //				}
    //			}else{
    //
    //				msg = Message.obtain();
    //				msg.arg1 = 1;
    //				updateDebugText("File Does not exists locally.. it was not created or ever read here");
    //				////ui_handler.sendMessage(msg);
    //
    //				msg = Message.obtain();
    //				msg.arg1 = 1;
    //				updateDebugText("Checking if the file can be downloaded directly");
    //				//ui_handler.sendMessage(msg);
    //
    //				if(_tagLink.contains("www.dropbox.com")){  // can download directly
    //					msg = Message.obtain();
    //					msg.arg1 = 1;
    //					updateDebugText("File can be downloaded directly");
    //					//ui_handler.sendMessage(msg);
    //					String filename = _tagLink.split("/")[_tagLink.split("/").length-1];
    //					doDownloadFileDirectly("h"+_tagLink,filename);
    //				}else{
    //					msg = Message.obtain();
    //					msg.arg1 = 1;
    //					updateDebugText("File cannot be downliaded directly");
    //					//ui_handler.sendMessage(msg);
    //					lookUpLocalCopy();
    //				}
    //			}
    //
    //		}

} // is favorite I:\Android\data\com.dropbox.android\files\scratch
