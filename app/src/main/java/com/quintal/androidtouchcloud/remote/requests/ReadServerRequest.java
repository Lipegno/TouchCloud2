package com.quintal.androidtouchcloud.remote.requests;

import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ReadServerRequest extends Thread {
	private static final String MODULE = "ReadTagRequestServer";

	private String _code;
	private String _uid;
	private String _result;
	public ReadServerRequest(String uid, String code ){
		_uid  = uid;
		_code = code;
	}

	public void run(){
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("tcode",_code));
		params.add(new BasicNameValuePair("uid",""+_uid));
		params.add(new BasicNameValuePair("op","link"));
		params.add(new BasicNameValuePair("dev","app"));
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost("http://aveiro.m-iti.org/touchcloud/read.py");
		try {
			Log.i(MODULE, _code+" "+_uid);
			httpPost.setEntity(new UrlEncodedFormEntity(params));
			HttpResponse httpResponse = httpClient.execute(httpPost);
			HttpEntity httpEntity = httpResponse.getEntity();
			InputStream is = httpEntity.getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					is, "utf-8"), 8);
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			is.close();
			String test = sb.toString();
			_result = test.replace("\n","");
			Log.i(MODULE, "->"+test);
			
		}
		catch (IOException e) {
			e.printStackTrace();
			_result = "IOERROR";
		} 
		catch (IllegalStateException e) {
			e.printStackTrace();
		} 
	}

	public String get_result() {
		return _result;
	}

	public void set_result(String _result) {
		this._result = _result;
	}

}
