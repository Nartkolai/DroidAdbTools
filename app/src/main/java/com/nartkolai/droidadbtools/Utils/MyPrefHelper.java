package com.nartkolai.droidadbtools.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class MyPrefHelper {
    public static String getPref(String key, String defValue, Context context){
        SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(key, " ");
    }
    public static boolean getPref(String key, boolean defValue, Context context){
        SharedPreferences preferences=PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(key,defValue);
    }
    public static int getPref(String key, int defValue, Context context){
        SharedPreferences preferences=PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getInt(key,defValue);
    }
    public static void putPref(String key,String value,Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.commit();
    }
    public static void putPref(String key,boolean value,Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }
    public static void putPref(String key,int value,Context context){
        SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor=prefs.edit();
        editor.putInt(key,value);
        editor.commit();
    }
}
