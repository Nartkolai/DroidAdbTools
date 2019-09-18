package com.google.nartkolai.droidadbtools.Utils;


import java.lang.reflect.Method;

public class MySelectorImpl implements MySelector{
    private String[] l = null;
    private String tilts = "";
    private String subTilts = "";
    private String text = "";
    private int inputType;
    private boolean input = false;
    private Object object;
    private Method method;


    public MySelectorImpl(Object object, Method method){
        this.object = object;
        this.method = method;
    }

    @Override
    public void setItemList(String[] list) {
        this.l = list;
    }

    @Override
    public String[] getItemList() {
        return l;
    }

    @Override
    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public void setTilts(String tilts) {
        this.tilts = tilts;
    }

    @Override
    public String getTilts() {
        return tilts;
    }

    @Override
    public void setSubTilts(String subTilts) {
        this.subTilts = subTilts;
    }

    public String getSubTilts() {
        return subTilts;
    }

    @Override
    public void setInputType(int inputType) {this.inputType = inputType;}

    @Override
    public int getInputType() {
        return inputType;
    }

    @Override
    public Method getMethod(){
        return method;
    }
    @Override
    public Object getObject(){
        return object;
    }

    @Override
    public void setNeedInput(boolean input){
        this.input = input;
    }
    @Override
    public boolean getNeedInput(){
        return input;
    }



}