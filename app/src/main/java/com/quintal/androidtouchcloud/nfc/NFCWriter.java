package com.quintal.androidtouchcloud.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;
import com.quintal.androidtouchcloud.R;
import com.quintal.androidtouchcloud.mainActivities.TagUpdatedActivity;
import com.quintal.androidtouchcloud.remote.WebServerHandler;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by filipequintal on 5/27/13.
 */
public class NFCWriter extends Activity {
	static private final String[] PREFIXES={"http://www.", "https://www.","http://", "https://"};
	private static final String MODULE = "NFCWriter";
	private static final String MIME_TYPE = "application/com.quintal.androidtouchcloud.nfc";
	final static private String SHARE_LINK_KEY = "share_link";
	final static private String USER_KEY = "user";
	final static private String EMAIL_KEY = "email";
	final static private String USER_ID_KEY = "user_id";
	final static private String ACCOUNT_PREFS_NAME = "prefs";
	final static private String DEVICE_KEY = "device";
	final static private String APP_KEY = "96dw8siqo14c9rj";
	final static private String APP_SECRET = "t5ljqzpu2o6rwf0";
	final static private String ACCESS_KEY_NAME = "ACCESS_KEY";
	final static private String ACCESS_SECRET_NAME = "ACCESS_SECRET";
	final static private String NFC_KEY = "nfc_id";
	final static private String TAG_TIMESTAMP_KEY = "event_time";
	final static private String PRIVACY_KEY = "privacy";
	final static private String TAG_DATE = "tag_date";
	final static private AccessType ACCESS_TYPE = AccessType.DROPBOX;
    final static private String FILE_SIZE_KEY = "size";
    final static private String DESKTOP_PATH_KEY = "desktop_path";

	private NfcAdapter _mNfcAdapter;
	private IntentFilter[] _mWriteTagFilters;
	private PendingIntent _mNfcPendingIntent;
	private boolean _writeProtect = false;
	private Switch _private ;
	private Context _context;
	private String _tagPremission="public";
	private String _dropBoxLink;
    private String _originalLink;
    private String _fileHash;
    private String _tagDropBoxLink;
    private TextView _namefield;
    private String _nfc_id;
    private String _desktopPath;
    private String _device;
    private String _size;
	private long _readTime=0;
	private DropboxAPI<AndroidAuthSession> mDBApi;
	private int isFolder=0;
	private TextView _premissionLabel;
//	private Intent _appIntent;
//	private GUI_Handler gui_handler = new GUI_Handler();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayShowHomeEnabled(false);
		getActionBar().setDisplayShowTitleEnabled(false);
		getActionBar().hide();
		setContentView(R.layout.writetag_layout);

		_context = getApplicationContext();

		_mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

		_mNfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
						| Intent.FLAG_ACTIVITY_CLEAR_TOP), 0);
		IntentFilter discovery    = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
		IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
		IntentFilter techDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);

		AndroidAuthSession session = buildSession();
		mDBApi = new DropboxAPI<AndroidAuthSession>(session);
		if(!mDBApi.getSession().isLinked()){
			Log.i(MODULE, " no dropbox linked.. starting process");
			mDBApi.getSession().startAuthentication(NFCWriter.this);
		}
		else{
			Log.i(MODULE, "  user alredy linked ");
		}


		// Intent filters for writing to a tag
		_mWriteTagFilters = new IntentFilter[] { discovery,ndefDetected };

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if(extras!=null) {
            _dropBoxLink = extras.getString(SHARE_LINK_KEY);
            _size        = extras.getString(FILE_SIZE_KEY);
            _desktopPath = extras.getString(DESKTOP_PATH_KEY);
            _device      = extras.getString(DEVICE_KEY);
        }
		_tagDropBoxLink = "no link";
		_readTime = System.currentTimeMillis();
		new TagHashCodeHandler().execute("QUERY");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		_premissionLabel = (TextView) findViewById(R.id.premission_field);
		if(_mNfcAdapter != null) {
			if (!_mNfcAdapter.isEnabled()){
				Log.e("NFC Writer", "NFC DISABLED");
			}
			_mNfcAdapter.enableForegroundDispatch(this, _mNfcPendingIntent, _mWriteTagFilters, null);
		} else {
			Toast.makeText(_context, "Sorry, No NFC Adapter found.", Toast.LENGTH_SHORT).show();
		}
		setUpPrivacySwitcher();
	}
	@Override
	public void onPause() {
		super.onPause();
		if(_mNfcAdapter != null)
			_mNfcAdapter.disableForegroundDispatch(this);

		if(!_tagDropBoxLink.equals("no link"))
				finish();
		return;
	}
	@Override
	public void onStop(){
		super.onStop();
		if(!_tagDropBoxLink.equals("no link"))
			finish();
		return;
	}
	@Override
	public void onBackPressed(){
		
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
        performTagOperations(intent);

	}

    private void performTagOperations(Intent intent){
        if(!_tagDropBoxLink.equals("no link")){
            if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
                // validate that this tag can be written....
                Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                _nfc_id = bytesToHexString(detectedTag.getId());
                Log.e(MODULE, "tag id -> "+_nfc_id);
                if(supportedTechs(detectedTag.getTechList())) {
                    // check if tag is writable (to the extent that we can
                    if(writableTag(detectedTag)) {
                      //  Toast.makeText(getApplicationContext(),"writing tag",Toast.LENGTH_SHORT).show();
                        //writeTag here
                        WriteResponse wr = writeTag(getTagAsNdef(), detectedTag);
                        //	String message = (wr.getStatus() == 1? "Success: " : "Failed: ") + wr.getMessage();
                        // Toast.makeText(_context,message,Toast.LENGTH_SHORT).show();
                        if(wr.getStatus()==1){

                            SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0); // get shared prefs to get the user data
                            DBManager.getDBManager().
                                    insertAnchorEvents(_nfc_id
                                            ,Integer.parseInt(prefs.getString(USER_ID_KEY, "")),
                                            System.currentTimeMillis()); //insert locally
                        //    Toast.makeText(getApplicationContext(),"Inserting anchor event",Toast.LENGTH_SHORT).show();
                            new InsertTagCreated().execute();       // inserts tag in le server
                        //    Toast.makeText(getApplicationContext(),"getting file information",Toast.LENGTH_SHORT).show();
                            new FileMetaDataHandler().execute();    // gets file metadata (also verifies if is folder)
                        //    Toast.makeText(getApplicationContext(),"registing tag into custom urls",Toast.LENGTH_SHORT).show();
                            new TagHashCodeHandler().execute("REGIST");
                            Intent i = new Intent(getApplicationContext(),TagUpdatedActivity.class);
                            SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            String date = s.format(new Date(_readTime));
                            i.putExtra(SHARE_LINK_KEY, _originalLink);
                            i.putExtra(NFC_KEY, _nfc_id);
                            i.putExtra(USER_KEY, Integer.parseInt(prefs.getString(USER_ID_KEY, "0")));
                            i.putExtra(PRIVACY_KEY, _tagPremission);
                            i.putExtra(TAG_DATE, date);
                            startActivity(i);
                        }

                    } else {
                        Toast.makeText(_context,"This tag is read-only",Toast.LENGTH_SHORT).show();
                        //    Sounds.PlayFailed(context, silent);

                    }
                } else {
                    Toast.makeText(_context,"Only Touchcloud tags are supported",Toast.LENGTH_SHORT).show();
                    //  Sounds.PlayFailed(context, silent);
                }
            }
        }else{
            Log.e(MODULE, "still no have custom url");
            Toast.makeText(_context,"Waitting for the server response",Toast.LENGTH_SHORT).show();
        }
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
	public WriteResponse writeTag(NdefMessage message, Tag tag) {
		int size = message.toByteArray().length;
		String mess = "";

		try {
			Ndef ndef = Ndef.get(tag);
			if (ndef != null) {
				ndef.connect();

				if (!ndef.isWritable()) {
					return new WriteResponse(0,"Tag is read-only");
				}
				if (ndef.getMaxSize() < size) {
					mess = "Tag capacity is " + ndef.getMaxSize() + " bytes, message is " + size
							+ " bytes.";
					return new WriteResponse(0,mess);
				}
				ndef.writeNdefMessage(message);
				Log.e(MODULE, " IS WRITABLE "+ndef.isWritable());
				
				if(_writeProtect){ 
					Log.i(MODULE, "vou trancar");
					ndef.makeReadOnly();
				}
				
				mess = "Your Touchcloud tag has been successfully updated!.";
				return new WriteResponse(1,mess);
			} else {
				NdefFormatable format = NdefFormatable.get(tag);
				if (format != null) {
					try {
						format.connect();
						format.format(message);
						mess = "Formatted tag and wrote message";
						return new WriteResponse(1,mess);
					} catch (IOException e) {
						mess = "Failed to format tag.";
						return new WriteResponse(0,mess);
					}
				} else {
					mess = "Tag doesn't support NDEF.";
					return new WriteResponse(0,mess);
				}
			}
		} catch (Exception e) {
			mess = "Failed to write tag";
			return new WriteResponse(0,mess);
		}
	}

	private class WriteResponse {
		int status;
		String message;
		WriteResponse(int Status, String Message) {
			this.status = Status;
			this.message = Message;
		}
		public int getStatus() {
			return status;
		}
		public String getMessage() {
			return message;
		}
	}

	public static boolean supportedTechs(String[] techs) {
		boolean ultralight=false;
		boolean nfcA=false;
		boolean ndef=false;
		for(String tech:techs) {
			if(tech.equals("android.nfc.tech.MifareUltralight")) {
				ultralight=true;
			}else if(tech.equals("android.nfc.tech.NfcA")) {
				nfcA=true;
			} else if(tech.equals("android.nfc.tech.Ndef") || tech.equals("android.nfc.tech.NdefFormatable")) {
				ndef=true;

			}
		}
		if(ultralight && nfcA && ndef) {
			return true;
		} else {
			return false;
		}
	}

	private boolean writableTag(Tag tag) {

		try {
			Ndef ndef = Ndef.get(tag);
			if (ndef != null) {
				ndef.connect();
				if (!ndef.isWritable()) {
					Toast.makeText(_context,"Tag is read-only.",Toast.LENGTH_SHORT).show();
					//  Sounds.PlayFailed(context, silent);
					ndef.close();
					return false;
				}
				ndef.close();
				return true;
			}
		} catch (Exception e) {
			Toast.makeText(_context,"Failed to read tag",Toast.LENGTH_SHORT).show();
			// Sounds.PlayFailed(context, silent);
		}

		return false;
	}

	private NdefMessage getTagAsNdef() {

		boolean addAAR = false;
		//String uniqueId = MIME_TYPE;
		//byte[] uriField = uniqueId.getBytes(Charset.forName("US-ASCII"));
		//_dropBoxLink = "https://db.tt/8CTNCw4x";
	//	byte[] payload = (_tagDropBoxLink).getBytes(Charset.forName("US-ASCII"));              //add 1 for the URI Prefix
		//payload[0] = 0x01;                                      	//prefixes http://www. to the URI     
		//Log.i(MODULE,_tagDropBoxLink);
		byte[] url=parseURLtoBytes(_tagDropBoxLink);
	
	//	byte[] url=buildUrlBytes("https://www.abola.pt");
		//byte[] mimeBytes = MIME_TYPE.getBytes(Charset.forName("US-ASCII"));
		NdefRecord rtdUriRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI, new byte[0], url);


		if(addAAR) {
			// note:  returns AAR for different app (nfcreadtag)
			return new NdefMessage(new NdefRecord[] {
					rtdUriRecord, NdefRecord.createApplicationRecord("com.quintal.androidtouchcloud.com.quintal.touchcould.mainActivities")
			});
		} else {
			return new NdefMessage(new NdefRecord[] {
					rtdUriRecord});
		} 
	}

    private String buildDownloadLink(String path){
        String url = path.replace("www.dropbox.com", "dl.dropboxusercontent.com");
        return url;
    }

    private HttpURLConnection buildConnectionToFile(String path, String filename) throws IOException{
     //   filename = URLEncoder.encode(filename, "UTF-8");
      //  filename = filename.replace("+", "%20");
      //  path = path.substring(0,path.lastIndexOf("/"));
        URL http_url = new URL(buildDownloadLink(path+"/"+filename));
        HttpURLConnection urlConnection = (HttpURLConnection) http_url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setDoOutput(true);
        urlConnection.setRequestProperty("accept-charset", "UTF-8");
        urlConnection.setRequestProperty("content-type", "application/x-www-form-urlencoded");
        return urlConnection;
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

    private String[] checkFileNameSimple(String share_link){
        String[] result = new String[2];
        String[] tag_tokens = share_link.split("/");
        String filename = tag_tokens[tag_tokens.length-1];
        Log.i(MODULE,"file name antes "+filename);
        filename = (filename.split("\\?"))[0];
        result[0] = filename;
        result[1] = share_link;
        Log.i(MODULE,"file name simple: "+filename+ " dropbox link "+share_link);
        return result;
    }

	  private byte[] parseURLtoBytes(String url) {
		  try{
			  byte pareByte=0;
			  String short_url=url;
			  int prefix_size=0;
			  String prefix="https://www.";
			  if (url.startsWith(prefix) && prefix.length() > prefix_size) {
				  pareByte=(byte)(2);
				  prefix_size=prefix.length();
				  short_url=url.substring(prefix_size); // string with just the dropbox..... thing
			  }
			  final byte[] short_url_bytes = short_url.getBytes();
			  final byte[] result = new byte[short_url_bytes.length+1];
			  result[0]=pareByte;
			  System.arraycopy(short_url_bytes, 0, result, 1, short_url_bytes.length);
			  return(result);
		  }catch(Exception e){
			  e.printStackTrace();
			  return  new byte[0];
		  }
		    
		  }
	//    private NdefMessage getSmartPosterMessage(){
	//    	boolean addAAR = false;
	//        String uniqueId = MIME_TYPE;
	//        byte[] uriField = uniqueId.getBytes(Charset.forName("US-ASCII"));
	//        byte[] payload = (" "+_dropBoxLink).getBytes();              //add 1 for the URI Prefix
	//        //payload[0] = 0x01;                                      	//prefixes http://www. to the URI
	//
	//        byte[] mimeBytes = MIME_TYPE.getBytes(Charset.forName("US-ASCII"));
	//        NdefRecord rtdUriRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], payload);
	//
	//        TextRecord title = new TextRecord();
	//    	title.setText("touchCloud_1");
	//    	title.setLocale(Locale.ENGLISH);
	//    	title.setEncoding(Charset.defaultCharset());
	//    	UriRecord uri = new UriRecord(_dropBoxLink);
	//    	SmartPosterRecord message = new SmartPosterRecord(title,uri,null);
	//    	MimeRecord mimeRecord = new MimeRecord();
	//    	mimeRecord.setMimeType(MIME_TYPE);
	//    	//message.
	//
	//         if(addAAR) {
	//            // note:  returns AAR for different app (nfcreadtag)
	//            return new NdefMessage(new NdefRecord[] {
	//            		rtdUriRecord, NdefRecord.createApplicationRecord("com.quintal.androidtouchcloud.com.quintal.touchcould.mainActivities")
	//            });
	//        } else {
	//            return new NdefMessage(new NdefRecord[] {
	//            		rtdUriRecord});
	//        }
	//    }
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
 
	private boolean isFolder(String filename){
		boolean folder = !filename.contains(".");
		isFolder = folder?1:0;
		return  folder;
	}
	public void handlePremissionClick(View v){
		if(v.getId()==R.id.private_switch){
			if(_private.isChecked()){
				_tagPremission = "private";
				_premissionLabel.setText(R.string.premission_private);
			}else{
				_tagPremission = "public";
				_premissionLabel.setText(R.string.premission_public);
			}
			new TagHashCodeHandler().execute("QUERY");
		}
	}
	private void setUpPrivacySwitcher(){
		_private = (Switch) findViewById(R.id.private_switch);
		_private.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton v, boolean arg1) {
				if(v.getId()==R.id.private_switch){
					if(_private.isChecked())
						_tagPremission = "private";
					else
						_tagPremission = "public";
					
					
					new TagHashCodeHandler().execute("QUERY");
				}				
			}
		});
	}
	/**
	 * 
	 * PRIVATE CLASSES
	 * 
	 * @author Filipe
	 *
	 */
	private class  InsertTagCreated extends AsyncTask{



		private Bundle getUserInfo(){ // POSSO VERIFICAR PRIMEIRO SE JA EXISTE NAS SP
			SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
			Message msg = new Message();
			Bundle b = new Bundle();
			b.putString(USER_KEY,prefs.getString(USER_KEY,"0"));
			b.putString(USER_ID_KEY,prefs.getString(USER_ID_KEY,"0"));
			b.putString(EMAIL_KEY, prefs.getString(EMAIL_KEY,""));
			msg.setData(b);
			return b;
		}
		@Override
		protected Object doInBackground(Object... arg0) {
			Bundle b = getUserInfo();	// get user info from dropbox.
			if(b!=null){

                String[] file_data = checkFileNameSimple(_dropBoxLink);

                if(_size.equals("no file size"))
                    _size = getRemoteFileSize(_dropBoxLink,file_data[0])+"";

				String id = b.getString(USER_ID_KEY);
				SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				String date = s.format(new Date(_readTime));
				// Building Parameters
				List<NameValuePair> params = new ArrayList<NameValuePair>();
				params.add(new BasicNameValuePair(NFC_KEY, _nfc_id));
				params.add(new BasicNameValuePair(USER_ID_KEY, id));
				params.add(new BasicNameValuePair(TAG_TIMESTAMP_KEY, date));
				params.add(new BasicNameValuePair(DEVICE_KEY,_device));
				params.add(new BasicNameValuePair(SHARE_LINK_KEY,_dropBoxLink));
                params.add(new BasicNameValuePair(FILE_SIZE_KEY,_size));
                String temp = null;
                try {
                    temp = URLEncoder.encode(_desktopPath, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                params.add(new BasicNameValuePair(DESKTOP_PATH_KEY,temp));
				Log.i(MODULE+" insert ", _dropBoxLink+ " d path "+temp);
				DefaultHttpClient httpClient = new DefaultHttpClient();
				HttpPost httpPost = new HttpPost("http://aveiro.m-iti.org/touchCloudProxy/insert_tag_created.php");
                Log.i(MODULE,"trying to insert tag into server insert_tag_created.php");
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
                    String temp_string = sb.toString();
                    try {
                        JSONObject result = new JSONObject(sb.toString());
                        Log.i(MODULE, "-> "+result);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Log.i(MODULE, "-> "+sb.toString());
                    Log.i(MODULE,"tag inserted into server");
				}
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
				catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return null;	
		}
	}
	private class FileMetaDataHandler extends AsyncTask{

		private String[] checkFileNameSimple(){
			String[] result = new String[2];
			String[] tag_tokens = URLDecoder.decode(_originalLink).split("/");
			String filename = tag_tokens[tag_tokens.length-1];
            filename = (filename.split("\\?"))[0];
			result[0] = filename;
			result[1] = _originalLink;
			return result;
		}
		
		@Override
		protected Object doInBackground(Object... arg0) {
			//doDownloadFileDummy();
			String file_name = checkFileNameSimple()[0];
			if(isFolder(file_name)){
				Log.e(MODULE, "sharing  Folder");
				}
			else{
                Log.i(MODULE,"inserting local file info into db");
				DBManager.getDBManager().insertLocalFileInfo(file_name, "", "", URLDecoder.decode(_originalLink),_fileHash);
			}
			return null;
		}

	}
	private class TagHashCodeHandler  extends AsyncTask <String,Void,Integer> {

		@Override
		protected Integer doInBackground(String... mode) {
			if(mode[0].equals("QUERY")){
				SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
				int user_id = Integer.parseInt(prefs.getString(USER_ID_KEY, "0"));
				_originalLink = _dropBoxLink;
                Log.i(MODULE,"getting hash from server "+_dropBoxLink+" "+user_id+" "+_tagPremission);
				_fileHash 	  = WebServerHandler.get_WS_Handler().getTagHash(_dropBoxLink, user_id+"", _tagPremission);
				_tagDropBoxLink  = "http://aveiro.m-iti.org/touchcloud/read.py?tcode="+_fileHash+"&op=page";
				Log.i(MODULE, "recorded file "+_tagDropBoxLink);
				return 1;
			}else if(mode[0].equals("REGIST")){
				SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
				int user_id = Integer.parseInt(prefs.getString(USER_ID_KEY, "0"));
				SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				String date = s.format(new Date(_readTime));
				Log.i(MODULE, "registing tag into server "+_dropBoxLink+","+user_id+" "+_tagPremission+" "+_fileHash+" "+_nfc_id+" "+date);
				WebServerHandler.get_WS_Handler().registTag(_dropBoxLink, user_id + "", _tagPremission, _fileHash, _nfc_id, date);
				return 2;
			}else return -1;
		}
	}



//		private String checkFileName(){
//			Log.i(MODULE,_dropBoxLink);
//			List<NameValuePair> params = new ArrayList<NameValuePair>();
//			params.add(new BasicNameValuePair(SHARE_LINK_KEY,_dropBoxLink));
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
//				String test = sb.toString();
//
//				byte[] b = test.getBytes("UTF-8");
//				String s = new String(b);
//				JSONObject result = new JSONObject(s);
//				Log.e(MODULE, test+" //"+b+" // "+s+" //"+result.getString("message") );
//				return result.getString("message");
//			}
//			catch (IOException e) {
//				e.printStackTrace();
//				return "FAIL";
//			} 
//			catch (IllegalStateException e) {
//				e.printStackTrace();
//				return "FAIL";
//			} 
//			catch (JSONException e) {
//				e.printStackTrace();
//				return "FAIL";
//			} 
//		}
//		private String checkFilePath(String name){
//			try {
//				List<Entry> exists = mDBApi.search("/", name, 100, false);
//				Log.i(MODULE, "aqui");
//
//				return exists.get(0).path;
//			} catch (DropboxException e) {
//				e.printStackTrace();
//				return "";
//			}
//		}
//		private void doDownloadFileDummy(){
//			String file_name = checkFileName();
//			String dp_filepath = checkFilePath(file_name);
//			String file_rev="";
//			Log.i(MODULE,"name "+file_name+" path "+dp_filepath);
//			String total_path = "/Android/data/com.dropbox.android/files/scratch"+dp_filepath;
//			File file = new File(Environment.getExternalStorageDirectory()+testSlashPath(total_path));
//			Log.i(MODULE, "file path local "+file.getAbsolutePath());
//			file.mkdirs();
//			File file2 = new File(Environment.getExternalStorageDirectory()+"/Android/data/com.dropbox.android/files/scratch"+dp_filepath);
//			FileOutputStream outputStream;
//			try {
//				outputStream = new FileOutputStream(file2);
//				DropboxFileInfo info = mDBApi.getFile(dp_filepath, null, outputStream, null);
//				file_rev = info.getMetadata().rev;
//				outputStream.close();
//				Log.i(MODULE, "File Downloaded");
//				//Toast.makeText(getApplicationContext(), "File downladed", Toast.LENGTH_LONG).show();
//				DBManager.getDBManager().insertLocalFileInfo(file_name, dp_filepath, file_rev,_dropBoxLink);
//				Message msg = new Message();
//				msg.arg1 = 2;
//				gui_handler.sendMessage(msg);
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
		
//		private String testSlashPath(String path){
//			String path_slashed = path.substring(0,path.lastIndexOf('/')+1);
//			Log.i(MODULE, path_slashed);
//			return path_slashed;
//	}

		
//	private class GUI_Handler extends Handler{
//		@Override
//		public void handleMessage(Message msg)
//		{
//			switch(msg.arg1){
//			case 1:
//				//                    displayUserInfo(msg.getData().getString(USER_KEY),msg.getData().getString(USER_ID_KEY),msg.getData().getString(EMAIL_KEY));
//				break;
//			case 2:
//				notifyDownloadComplete();
//				break;
//			default:
//				break;
//			}
//		}
//	}
	//    public void teste(){
	//    	TextRecord title = new TextRecord();
	//    	title.setText("touchCloud_1");
	//    	UriRecord uri = new UriRecord(_dropBoxLink);
	//    	SmartPosterRecord message = new SmartPosterRecord(title,uri,null);
	//
	//    }

}