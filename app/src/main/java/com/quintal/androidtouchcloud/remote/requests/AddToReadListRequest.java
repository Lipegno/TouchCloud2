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

/**
 * Created by Filipe on 15/05/2015.
 */
public class AddToReadListRequest extends Thread {

    private static final String MODULE = "AddToReadListRequest";

    private String _uid;
    private String _dropbox_link;
    private String _file_name;
    private String _opened;
    private String _date;
    private String _tag_id;
    private String _file_size;
    private String _result;

    public AddToReadListRequest(String uid, String dropbox_link, String file_name,String opened, String date, String tag_id, String filesize){

       _uid           = uid;
        _dropbox_link = dropbox_link;
        _file_name    = file_name;
        _opened       = opened;
        _date         = date;
        _tag_id       = tag_id;
        _file_size    = filesize;
    }

    public void run(){
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("uid",_uid));
        params.add(new BasicNameValuePair("dp_link",_dropbox_link));
        params.add(new BasicNameValuePair("file_name",_file_name));
        params.add(new BasicNameValuePair("opened",_opened));
        params.add(new BasicNameValuePair("date",_date));
        params.add(new BasicNameValuePair("tag_id",_tag_id));
        params.add(new BasicNameValuePair("filesize",_file_size));

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost("http://aveiro.m-iti.org/touchCloudProxy/add_to_read_list.php");
        try {
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
            Log.i(MODULE, "->" + _result);

        }
        catch (IOException e) {
            e.printStackTrace();
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
