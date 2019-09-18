package com.google.nartkolai.droidadbtools.Utils;

import java.lang.reflect.Method;

public interface MySelector {

     /**
      * @param list list of processed items
      */
     void setItemList(String[] list);
     String[] getItemList();

     /**
      * @param input displays a dialog box with a field for entering values.
      */
     void setNeedInput(boolean input);
     boolean getNeedInput();

     /**
      * @param text default text in the input field
      */
     void setText(String text);
     String getText();

     /**
      * @param tilts window title
      */
     void setTilts(String tilts);
     String getTilts();

     /**
      * @param subTilts window title of the selected item
      */
     void setSubTilts(String subTilts);
     String getSubTilts();

     /**
      * @param inputType change the way you enter values
      */
     void setInputType(int inputType);
     int getInputType();

     /**
      * @return method called
      */
     Method getMethod();

     /**
      * @return object containing the called method
      */
     Object getObject();
}
