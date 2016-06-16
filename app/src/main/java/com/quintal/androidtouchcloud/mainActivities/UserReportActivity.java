package com.quintal.androidtouchcloud.mainActivities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.quintal.androidtouchcloud.R;
import com.quintal.androidtouchcloud.remote.WebServerHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class UserReportActivity extends Activity {
	private static final String MODULE = "UserReportActivity";
	final static private String TAG_KEY = "nfc_id";
	final static private String PRIVACY_KEY = "privacy";
	final static private String USER_ID_KEY = "user";
	final static private String TAG_DATE = "tag_date";

	private Button _nextButton;
	private Button _backButton;
	private AnswerHandler  _nextHandler = new AnswerHandler();
	private LinearLayout _question1;
	private LinearLayout _question1b;
	private LinearLayout _question2;
	private LinearLayout _question3;
	private LinearLayout _question4;
	private LinearLayout _question5;
	private TextView _question5Label;
	private TextView _errorLabel;
	private EditText _answer1b;
	private EditText _answer2;
	private EditText _answer3;
	private EditText _answer5;
	private RadioGroup _answer4;
	private ProgressBar _survey_progress;
	private int _progress=0;
	private int _progressQuestions=1;
	private TextView _progress_label;
	private boolean _isReTag;
	private static final int REQUEST_IMAGE_CAPTURE = 1888;

	int question = 0;
	private ImageView _photo_view;
	private Bitmap tag_photo;
	private File picture_file;
	private String _uid;
	private String _nfcId;
	private String _privacy;
	private String _tag_date;
	private int total_questions=5;
	private Button _cameraButton;
	private Button _closeButton;
	private String _report_id;
	private String _picturePath;
	private boolean _is_new_picture = false;
	private boolean _uploaded = false;
	private Button _db_button;
	private String temp_file_name;
	private boolean _isTakingPic=false;
	KeyboarNextHandler handler = new KeyboarNextHandler();
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayShowHomeEnabled(false);
		getActionBar().setDisplayShowTitleEnabled(false);
		getActionBar().hide();
		setContentView(R.layout.user_report_layout);
		_uid = getIntent().getStringExtra(USER_ID_KEY);
		_nfcId = getIntent().getStringExtra(TAG_KEY); 
		_privacy = getIntent().getStringExtra(PRIVACY_KEY);
		_tag_date = getIntent().getStringExtra(TAG_DATE);
		Log.i(MODULE," user: "+_uid+" , nfc id: "+_nfcId+" , privacy: "+_privacy+" "+_tag_date);
		initView();
	}
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event){
		switch(keyCode){

		case KeyEvent.KEYCODE_BACK:
			goBack();
			break;
		case KeyEvent.KEYCODE_HOME:
			finishApplication();
			break;
			
		}
		return false;
	}
	@Override
	public void onStop(){
		super.onStop();
 		if(!_isTakingPic)
			finishApplication();
	}
	private void finishApplication(){
		Intent i = new Intent(getApplicationContext(),EndOfTheRoad.class);
		i.putExtra("action", 2);
		startActivity(i);			//make sure there arent any loose ends
		finish();
	}
	private void goBack(){
		if(question>=6 && question>=0){
			super.onBackPressed();
			finish();
			return;
		}else if(question>=1){
			handleBackClick();
		}
	}
	private void hideKeyboard(){
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(_question1.getWindowToken(), 0);
	}
	private void initView(){
		_nextButton = (Button)findViewById(R.id.report_next);
		_backButton = (Button)findViewById(R.id.report_back);
		_question1  = (LinearLayout)findViewById(R.id.question1);
		_question1b  = (LinearLayout)findViewById(R.id.question1b);
		_question2  = (LinearLayout)findViewById(R.id.question2);
		_question3  = (LinearLayout)findViewById(R.id.question3);
		_question4  = (LinearLayout)findViewById(R.id.question4);
		_question5  = (LinearLayout)findViewById(R.id.question5);
		//		_question5_hint_private = (TextView)findViewById(R.id.question5_hint_private);
		//		_question5_hint_public =(TextView)findViewById(R.id.question5_hint_public);
		_question5Label = (TextView)findViewById(R.id.question5_label);
		_answer1b	= (EditText)findViewById(R.id.answer1b);
		_answer2    = (EditText)findViewById(R.id.answer2);
		_answer3    = (EditText)findViewById(R.id.answer3);
		_answer4	= (RadioGroup)findViewById(R.id.answer4);
		_answer5    = (EditText)findViewById(R.id.answer5);
		_photo_view = (ImageView)findViewById(R.id.photo_view);
		_progress_label = (TextView)findViewById(R.id.survey_progress_label);
		_errorLabel = (TextView)findViewById(R.id.problem_label);
		_errorLabel.setVisibility(View.INVISIBLE);
		_survey_progress = (ProgressBar)findViewById(R.id.survey_progress);
		_survey_progress.getProgressDrawable().setColorFilter(Color.RED, Mode.SRC_IN);
		_question1b.setVisibility(View.GONE);
		_photo_view.setVisibility(View.GONE);
		_backButton.setOnTouchListener(_nextHandler);
		_backButton.setVisibility(View.GONE);
		_nextButton.setOnTouchListener(_nextHandler);
		
		_answer1b.setOnEditorActionListener(handler);
		_answer2.setOnEditorActionListener(handler);
		_answer3.setOnEditorActionListener(handler);
		_answer5.setOnEditorActionListener(handler);
		
		if(_privacy.equals("private")){
			_question5Label.setText(R.string.question_5_b);
			//_question5_hint_public.setVisibility(View.GONE);
		}
		_isReTag = verifyPicture();
		if(_isReTag)
			total_questions=6;

		_progress_label.setText(1+"/"+total_questions);
		initCameraButton();
	}
	private void initCameraButton(){
		_cameraButton = (Button)findViewById(R.id.take_photo_btn);
		_cameraButton.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction()){
				case MotionEvent.ACTION_DOWN:
					v.setBackgroundResource(R.drawable.button_background_pressed);
					((Button)v).setTextColor(Color.parseColor("#FF0000"));
					break;
				case MotionEvent.ACTION_UP:
					v.setBackgroundResource(R.drawable.button_background);
					((Button)v).setTextColor(Color.parseColor("#FFFFFF"));
					handlePhotoButton(v);
					break;
				}
				return false;
			}
		});
	}
	private void initCloseButton(){
		_closeButton = (Button)findViewById(R.id.report_close);
		_closeButton.setOnTouchListener(new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction()){
				case MotionEvent.ACTION_DOWN:
					v.setBackgroundResource(R.drawable.button_background_pressed);
					((Button)v).setTextColor(Color.parseColor("#FF0000"));
					break;
				case MotionEvent.ACTION_UP:
					v.setBackgroundResource(R.drawable.button_background);
					((Button)v).setTextColor(Color.parseColor("#FFFFFF"));
					finishApplication();
					break;
				}
				return false;
				}
		});
	}
	private boolean verifyPicture(){
		File SDCardRoot = Environment.getExternalStorageDirectory();
		File file = new File(SDCardRoot+"/touchCloud/temp_files/",_uid+"_"+_nfcId+ ".jpg");
		if(file.exists()){
			String path = file.getAbsolutePath();
			picture_file = new File(path);
			tag_photo = decodeFile(picture_file);
			//Bitmap tag_photo_small = Bitmap.createScaledBitmap(tag_photo,tag_photo.getWidth()/2, tag_photo.getHeight()/2, false);
			//Matrix matrix = new Matrix();
			//matrix.postRotate(90);
			//tag_photo_small = Bitmap.createBitmap(tag_photo_small, 0, 0, 340, 200, matrix, false);
			_photo_view.setVisibility(View.VISIBLE);
			_photo_view.setImageBitmap(tag_photo);
			_photo_view.invalidate();
			((TextView)findViewById(R.id.photo_label)).setText(getString(R.string.update_photo));
			return true;
		}else
			return false;
	}
	private Bitmap decodeFile(File f){
	    try {
	        //Decode image size
	        BitmapFactory.Options o = new BitmapFactory.Options();
	        o.inJustDecodeBounds = true;
	        BitmapFactory.decodeStream(new FileInputStream(f),null,o);
	        //The new size we want to scale to
	        final int REQUIRED_SIZE=300;
	        //Find the correct scale value. It should be the power of 2.
	        int scale=1;
	        while(o.outWidth/scale/2>=REQUIRED_SIZE && o.outHeight/scale/2>=REQUIRED_SIZE)
	            scale*=2;

	        //Decode with inSampleSize
	        BitmapFactory.Options o2 = new BitmapFactory.Options();
	        o2.inSampleSize=scale;
	        return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
	    } catch (FileNotFoundException e) {}
	    return null;
	}
	private void handleNextClick(){
		question++;
		_backButton.setVisibility(View.VISIBLE);
		if(question==1){

			Log.i(MODULE, _isReTag+"  "+_is_new_picture);
			if(_isReTag || _is_new_picture){
				
				updateProgressBarNext();
				_answer2.requestFocus();
				_answer1b.requestFocus();

				if(_report_id==null){
					AnswerWorker answer_worker = new AnswerWorker();
					answer_worker.execute(_uid,1+"",picture_file.getName(),"",_tag_date,_nfcId,"");
				}else if(!_uploaded)
					uploadPicture();

				_question1.setVisibility(View.GONE);
				_question3.setVisibility(View.GONE);
				_question4.setVisibility(View.GONE);
				_question5.setVisibility(View.GONE);
				if(_isReTag)
					_question1b.setVisibility(View.VISIBLE);
				else{
					_question2.setVisibility(View.VISIBLE);
					_question2.requestFocus();
					question++;
				}
			}else{
				question=0;
				_errorLabel.setText(R.string.missing_photo_warning);
				_errorLabel.setVisibility(View.VISIBLE);
				_backButton.setVisibility(View.GONE);
			}
				


		}else if(question==2){
			if(verifyValidAnswer(_answer1b.getText().toString())){  	// verify if there is any text otherwise there is no need to upload
				AnswerWorker worker = new AnswerWorker();
				worker.execute(_uid,1+"b",_answer1b.getText().toString(),_report_id,"","","");
				_question1.setVisibility(View.GONE);
				_question2.setVisibility(View.VISIBLE);
				_question3.setVisibility(View.GONE);
				_question1b.setVisibility(View.GONE);
				_question4.setVisibility(View.GONE);
				_question5.setVisibility(View.GONE);
			}else
				question=1;
		}
		else if(question==3){
			if(verifyValidAnswer(_answer2.getText().toString())){  	// verify if there is any text otherwise there is no need to upload
				AnswerWorker worker = new AnswerWorker();
				worker.execute(_uid,2+"",_answer2.getText().toString(),_report_id,"","","");
				_answer3.requestFocus();
				_question1.setVisibility(View.GONE);
				_question2.setVisibility(View.GONE);
				_question3.setVisibility(View.VISIBLE);
				_question1b.setVisibility(View.GONE);
				_question4.setVisibility(View.GONE);
				_question5.setVisibility(View.GONE);

			}else{ 
				question = 2;
			}
		}else if(question==4){
			if(verifyValidAnswer(_answer3.getText().toString())){  	// verify if there is any text otherwise there is no need to upload
				AnswerWorker worker = new AnswerWorker();
				worker.execute(_uid,3+"",_answer3.getText().toString(),_report_id,"","","");
				_question1.setVisibility(View.GONE);
				_question2.setVisibility(View.GONE);
				_question3.setVisibility(View.GONE);
				_question4.setVisibility(View.VISIBLE);
				_question5.setVisibility(View.GONE);
			}else 
				question=3;
		}else if(question==5){
			if(_answer4.getCheckedRadioButtonId()>0){  	// verify if there is any text otherwise there is no need to upload
				int checked = _answer4.getCheckedRadioButtonId();
				boolean test = checked == R.id.radio0;
				AnswerWorker worker = new AnswerWorker();
				worker.execute(_uid,4+"",test+"",_report_id,"","","");
				_answer5.requestFocus();
				_question1.setVisibility(View.GONE);
				_question2.setVisibility(View.GONE);
				_question3.setVisibility(View.GONE);
				_question4.setVisibility(View.GONE);
				_question5.setVisibility(View.VISIBLE);
				updateProgressBarNext(); // do it here because the verification here does not go to the verifyValid answer
				_nextButton.setText(getString(R.string.finish));
			}else {
				question = 4;
				_errorLabel.setVisibility(View.VISIBLE);
			}	
		}
		else if(question==6){
			if(verifyValidAnswer(_answer5.getText().toString())){  	// verify if there is any text otherwise there is no need to upload
				AnswerWorker worker = new AnswerWorker();
				worker.execute(_uid,5+"",_answer5.getText().toString(),_report_id,"","",_privacy);
				setContentView(R.layout.user_report_success);
				_db_button = (Button)findViewById(R.id.user_report_gotoDB);
				_db_button.setOnTouchListener(new GoToDropboxHandler());
				initCloseButton();
				new FileBackupHandler().execute();
				
			}else
				question=5;
		}
		Log.i(MODULE, "question n "+question);
		Log.i(MODULE, "answer "+_answer2.getText());
	}
	public void handleBackClick(){
		question--;
		updateProgressBarBack();
		_backButton.setVisibility(View.VISIBLE);
		_nextButton.setText(getString(R.string.next));
		_errorLabel.setVisibility(View.INVISIBLE);

		if(question==0){
			_question1.setVisibility(View.VISIBLE);
			_question2.setVisibility(View.GONE);
			_question3.setVisibility(View.GONE);
			_question4.setVisibility(View.GONE);
			_question5.setVisibility(View.GONE);
			_backButton.setVisibility(View.GONE);
		}
		else if(question==1){
			//uploadPicture();
			if(_isReTag){
				_question1.setVisibility(View.GONE);
				_question2.setVisibility(View.GONE);
				_question3.setVisibility(View.GONE);
				_question4.setVisibility(View.GONE);
				_question5.setVisibility(View.GONE);
				_question1b.setVisibility(View.VISIBLE);
			}else{
				_progress = _progress + Math.round(100f/(total_questions-1));
				_progressQuestions++;
				handleBackClick();
			}
				
		}
		else if(question==2){
		//	uploadPicture();
			_question1.setVisibility(View.GONE);
			_question1b.setVisibility(View.GONE);
			_question2.setVisibility(View.VISIBLE);
			_question3.setVisibility(View.GONE);
			_question4.setVisibility(View.GONE);
			_question5.setVisibility(View.GONE);
		}else if(question==3){
			_question1.setVisibility(View.GONE);
			_question2.setVisibility(View.GONE);
			_question3.setVisibility(View.VISIBLE);
			_question4.setVisibility(View.GONE);
			_question5.setVisibility(View.GONE);
		}else if(question==4){
			_question1.setVisibility(View.GONE);
			_question2.setVisibility(View.GONE);
			_question3.setVisibility(View.GONE);
			_question4.setVisibility(View.VISIBLE);
			_question5.setVisibility(View.GONE);
		}else if(question==5){
			_question1.setVisibility(View.GONE);
			_question2.setVisibility(View.GONE);
			_question3.setVisibility(View.GONE);
			_question4.setVisibility(View.GONE);
			_question5.setVisibility(View.VISIBLE);
			_nextButton.setText(getString(R.string.finish));
		}
		else if(question==5){
		}
	}
	public void handlePhotoButton(View v){
		if(v.getId()==R.id.take_photo_btn){
			File SDCardRoot = Environment.getExternalStorageDirectory();
			File file = new File(SDCardRoot+"/touchCloud/temp_files/",_uid+"_"+_nfcId+ ".jpg");
			_picturePath = file.getAbsolutePath();
			if(file.exists()){
				long current_time = System.currentTimeMillis();
				file.renameTo(new File(SDCardRoot+"/touchCloud/temp_files/","old"+current_time+"_"+_uid+"_"+_nfcId+ ".jpg"));
				temp_file_name = SDCardRoot+"/touchCloud/temp_files/old"+current_time+"_"+_uid+"_"+_nfcId+ ".jpg";
			}
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); 
			String captured_image = _uid+"_"+_nfcId+ ".jpg";
			File file_new = new File(SDCardRoot+"/touchCloud/temp_files/",captured_image);
			captured_image = file.getAbsolutePath();
			Uri outputFileUri = Uri.fromFile(file_new); 
			intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri); 
			intent.putExtra("return-data", true);
			_isTakingPic = true;
			startActivityForResult(intent,REQUEST_IMAGE_CAPTURE); 
		}
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
//			Uri data_path = data.getData();
//			String path = data_path.getPath();
			_isTakingPic = false;
			picture_file = new File(_picturePath);
			tag_photo = decodeFile(picture_file);
            Log.i(MODULE,"width "+tag_photo.getWidth()+" height "+tag_photo.getHeight());
			Bitmap tag_photo_small = Bitmap.createScaledBitmap(tag_photo,tag_photo.getWidth(), tag_photo.getHeight(), false);
		//	Matrix matrix = new Matrix();
		//	matrix.postRotate(90);
		//	tag_photo_small = Bitmap.createBitmap(tag_photo_small, 0, 0, 340, 200, matrix, false);
			_photo_view.setVisibility(View.VISIBLE);
			_photo_view.setImageBitmap(tag_photo);
			_photo_view.invalidate();
			((TextView)findViewById(R.id.photo_label)).setText(getString(R.string.update_photo));
			_is_new_picture=true;
			_uploaded = false;
		}else{
			File SDCardRoot = Environment.getExternalStorageDirectory();
			File file = new File(temp_file_name);
			if(file.exists())
	 			file.renameTo(new File(SDCardRoot+"/touchCloud/temp_files/",_uid+"_"+_nfcId+ ".jpg"));
		}
	}
	private void uploadPicture(){
		if(_is_new_picture){
			PictureUploadWorker worker = new PictureUploadWorker();
			worker.file_to_upload = picture_file;
			worker.report_id =_report_id;
			worker.start();
		}
	}
	private boolean verifyValidAnswer(String answer){
	//Log.i(MODULE, "is empty ->"+answer.equals(""));
		if(answer.equals("")){
			_errorLabel.setText(R.string.missing_field_warning);
			_errorLabel.setVisibility(View.VISIBLE);
			return false;
		}else{
			updateProgressBarNext();
			return true;
		}
	}
	private void updateProgressBarNext(){
		_errorLabel.setVisibility(View.INVISIBLE);
		_progress = _progress + Math.round(100f/(total_questions-1));	
		_survey_progress.setProgress(_progress);
		_progress_label.setText((++_progressQuestions)+"/"+total_questions);
		//Log.d(MODULE,"next " + _progressQuestions+"");
		//Log.d(MODULE,"next " + _progress+"");
	}
	private void updateProgressBarBack(){
		Log.e(MODULE, "progress : "+_progress +" diff " +(Math.round(100f/(total_questions-1))) ); 
		_errorLabel.setVisibility(View.INVISIBLE);
		_progress = _progress - Math.round(100f/(total_questions-1));	
		_survey_progress.setProgress(_progress);
		_progress_label.setText((--_progressQuestions)+"/"+total_questions);
	//	Log.e(MODULE,"back " + _progressQuestions+"");
	//	Log.e(MODULE,"back " + _progress+"");
	}
	private class AnswerHandler implements OnTouchListener{

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if(v.getId()==R.id.report_next){
				switch(event.getAction()){
				case MotionEvent.ACTION_DOWN:
					_nextButton.setBackgroundResource(R.drawable.button_background_pressed);
					_nextButton.setTextColor(Color.parseColor("#FF0000"));
					break;
				case MotionEvent.ACTION_UP:
					_nextButton.setBackgroundResource(R.drawable.button_background);
					_nextButton.setTextColor(Color.parseColor("#FFFFFF"));
					handleNextClick();
					break;
				}
				return true;
			}
			else if(v.getId()==R.id.report_back){
				switch(event.getAction()){
				case MotionEvent.ACTION_DOWN:
					_backButton.setBackgroundResource(R.drawable.button_background_pressed);
					_backButton.setTextColor(Color.parseColor("#FF0000"));
					break;
				case MotionEvent.ACTION_UP:
					_backButton.setBackgroundResource(R.drawable.button_background);
					_backButton.setTextColor(Color.parseColor("#FFFFFF"));
					handleBackClick();
					break;
				}
				return true;
			}else
				return false;
		}
	}
	private class PictureUploadWorker extends Thread{
		File file_to_upload;
		String report_id;
		@Override
		public void run(){
			Log.i(MODULE, "uploading File "+(file_to_upload!=null && file_to_upload.exists()));
			if(file_to_upload!=null && file_to_upload.exists()){
				WebServerHandler.get_WS_Handler().uploadFile(file_to_upload,report_id);
				_uploaded = true;
			}
				
		}
	}
	private class AnswerWorker extends AsyncTask<String,Void,Integer>{
		
		@Override
		protected Integer doInBackground(String... mode) {
			if(mode[1].equals("1")){
				_report_id = WebServerHandler.get_WS_Handler().sendAnswer(mode[0], mode[1], mode[2],mode[3],mode[4],mode[5],mode[6]);
				uploadPicture();
			}
			else
				WebServerHandler.get_WS_Handler().sendAnswer(mode[0], mode[1], mode[2],mode[3],mode[4],mode[5],mode[6]);
			return 1;
		}

	}
	private class FileBackupHandler extends AsyncTask{
	

	@Override
	protected Object doInBackground(Object... params) {
		String filename = System.currentTimeMillis()+_uid+"_"+_nfcId+ ".txt";
	    File SDCardRoot = Environment.getExternalStorageDirectory();
		File file = new File(SDCardRoot+"/touchCloud/answers_backup/"+filename);
		file.getParentFile().mkdirs();
		
		String answer = _answer1b.getText()+"\n"
					  + _answer2.getText()+"\n"
					  + _answer3.getText()+"\n"
					  + (_answer4.getCheckedRadioButtonId()  == R.id.radio0)+"\n"
					  + _answer5.getText();
		
		FileOutputStream f;
		try {
			f = new FileOutputStream(file);

			byte[] buffer = new byte[1024];
			int len1 = 0;
			f.write(answer.getBytes(), 0, answer.getBytes().length);
			f.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	}
	private class GoToDropboxHandler implements View.OnTouchListener{

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if(v.getId()==R.id.user_report_gotoDB){
				switch(event.getAction()){
				case MotionEvent.ACTION_DOWN:
					v.setBackgroundColor(Color.parseColor("#247AE0"));

					break;

				case MotionEvent.ACTION_UP:
					v.setBackgroundColor(Color.TRANSPARENT);
					goToDropbox();
					break;
				}
				return true;
			}
			return false;
		}
		
		private void goToDropbox(){
			try{
				Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage("com.dropbox.android");
				startActivity(LaunchIntent);
			}catch(Exception e){
				finish();
			}
		}
		
	}
	private class KeyboarNextHandler implements OnEditorActionListener{

		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			if(actionId == EditorInfo.IME_ACTION_NEXT){
				hideKeyboard();
				handleNextClick();
			}else if(actionId == EditorInfo.IME_ACTION_DONE){
				hideKeyboard();
				handleNextClick();
			}
			return false;
		}
		
	}
}
