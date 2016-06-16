package com.quintal.androidtouchcloud.remote;

import com.quintal.androidtouchcloud.remote.requests.AddToReadListRequest;
import com.quintal.androidtouchcloud.remote.requests.AnswerServerRequest;
import com.quintal.androidtouchcloud.remote.requests.HashServerRequest;
import com.quintal.androidtouchcloud.remote.requests.ReadServerRequest;
import com.quintal.androidtouchcloud.remote.requests.TagServerRequest;
import com.quintal.androidtouchcloud.remote.requests.UploadPictureServerRequest;

import java.io.File;

public final class WebServerHandler {

	public static class WebServerHandlerHolder{
		public static WebServerHandler INSTANCE = new WebServerHandler();
	}

	public static WebServerHandler get_WS_Handler(){
		return WebServerHandlerHolder.INSTANCE;
	}
	
	public String getTagHash(String url, String uid, String privacy){
		HashServerRequest request = new HashServerRequest(url,uid,privacy);
		request.start();
		try {
			request.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return request.get_result();
	}
	public String getTagRead(String uid, String code){
		ReadServerRequest request = new ReadServerRequest(uid,code);
		request.start();
		try {
			request.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return request.get_result();
	}
	public void registTag(String url, String uid, String privacy, String hash, String nfcid, String date){
		TagServerRequest request = new TagServerRequest(url,uid,privacy, hash, nfcid, date);
		request.start();
		try {
			request.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void uploadFile(File file, String report_id){
		UploadPictureServerRequest request = new UploadPictureServerRequest(file, report_id);
		request.start();
		try{
		request.join();
		} catch(InterruptedException e){
			e.printStackTrace();
		}
	}
	public String sendAnswer(String uid, String question_id, String answer, String report_id,String date,String nfc_id,String extra){
    	AnswerServerRequest request = new AnswerServerRequest(uid, question_id,  answer, report_id, date, nfc_id,extra);
		request.start();
		try {
			request.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return request.get_result();
	}

    public String addToReadList(String uid, String dropbox_link, String file_name,String opened, String date, String tag_id, String filesize){
        AddToReadListRequest request = new AddToReadListRequest( uid,  dropbox_link,  file_name, opened,  date,  tag_id,  filesize);
        request.start();
        try {
            request.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return request.get_result();
    }
}
