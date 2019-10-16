package com.nartkolai.droidadbtools.Utils;


import java.lang.reflect.Method;

public class AlterDialogSelectorImpl implements AlterDialogSelector {
    private String[] list = null;
    private String tilts = "";
    private String subTilts = "";
    private String text = "";
    private int inputType;
    private boolean input = false;
    private Object object;
    private Method method;


    public AlterDialogSelectorImpl(Object object, Method method){
        this.object = object;
        this.method = method;
    }

    @Override
    public void setItemList(String[] list) {
        this.list = list;
    }

    @Override
    public String[] getItemList() {
        return list;
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

    /**
     * @param tilts     window title of the selected item
     * @param text      default text in the input field
     * @param inputType change the way you enter values
     */
    @Override
    public void toAlterDialogInputValues(String tilts, String text, int inputType){
        this.tilts = tilts;
        this.text = text;
        this.inputType = inputType;
        this.input = true;
    }

    /**
     * @param tilts    window title of the selected item
     * @param subTilts window title of the selected item
     * @param list     list of processed items
     */
    @Override
    public void toAlterDialogListItem(String tilts, String subTilts, String[] list){
        this.tilts = tilts;
        this.subTilts = subTilts;
        this.list = list;
        this.input = false;
    }

    /**
     * @param tilts window title of the selected item
     * @param text  action description text
     */
    @Override
    public void toAlterDialogNoItem(String tilts, String text){
        this.list = null;
        this.tilts = tilts;
        this.text = text;
        this.input = false;
    }
}