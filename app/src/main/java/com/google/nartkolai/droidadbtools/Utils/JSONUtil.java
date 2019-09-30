package com.google.nartkolai.droidadbtools.Utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class JSONUtil {
    private Context context;
    private String TAG = "FsUtil";
    private String fileName;
    public JSONUtil(Context context, String fileName){
        this.context = context;
        this.fileName = fileName;
    }

    public void chkFile(){
        try {
            new JSONArray(readFromFile(fileName));
        } catch (JSONException e) {
            writeToFile(fileName, "[]");
            e.printStackTrace();
        }
    }


     private void writeToFile(String name, String data) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(name, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e(TAG, "File write failed: " + e.toString());
        }
    }


      private String readFromFile(String name) {

        String ret = "";

        try {
            InputStream inputStream = context.openFileInput(name);

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString;
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException ignore) {
            writeToFile(fileName, "[]");
            Log.i(TAG, "Create new file " + fileName);
        } catch (IOException e) {
            Log.e(TAG, "Can not read file: " + e.toString());
        }

        return ret;
    }

    public void jsonHelper(String input) {
        ArrayList<Object> list = new ArrayList<>();
        JSONArray jsonArray;
        try {
            JSONObject main = new JSONObject();
            jsonArray = new JSONArray(readFromFile(fileName));
        for (int i=0;i<jsonArray.length();i++){
                list.add(jsonArray.get(i));
                JSONObject jsonobject = jsonArray.getJSONObject(i);
                if (jsonobject.getString("dev_ip").equals(input)) {
                    list.remove(jsonArray.get(i));
                }
        }
        main.put("dev_ip", input);
        jsonArray = new JSONArray(list);
        jsonArray.put(main);
        writeToFile(fileName, jsonArray.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public void jsonHelper(String input, boolean remove) {
        JSONArray jsonArray;
        ArrayList<Object> list = new ArrayList<>();
        try {
            jsonArray = new JSONArray(readFromFile(fileName));
            JSONObject main = new JSONObject();
            for (int i = 0; i < jsonArray.length(); i++) {
                list.add(jsonArray.get(i));
                JSONObject jsonobject = jsonArray.getJSONObject(i);
                if (jsonobject.getString("dev_ip").equals(input)) {
                       // jsonArray.remove(i);// if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                    list.remove(jsonArray.get(i));
                }
            }
            jsonArray = new JSONArray(list);
            if (!remove) {
                main.put("dev_ip", input);
                jsonArray.put(main);
            }
            writeToFile(fileName, jsonArray.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("Assert")
    public String[] jsonHelperGetItemArr() {
        String[] arrayList = new String[0];
        JSONArray jsonArray;
        try {
            jsonArray = new JSONArray(readFromFile(fileName));
            arrayList = new String[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonobject = jsonArray.getJSONObject(i);
                assert false;
                arrayList[i] = ((jsonobject.getString("dev_ip")));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return arrayList;
    }
}
