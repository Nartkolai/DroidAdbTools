package com.nartkolai.droidadbtools.Utils;

import java.lang.reflect.Method;

public interface AlterDialogSelector {

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

     /**
      * Minimum parameters to build dialog box with a field for entering values
      * @param tilts window title of the selected item
      * @param text default text in the input field
      * @param inputType change the way you enter values
      */
     void toAlterDialogInputValues(String tilts, String text, int inputType);

     /**
      * Minimum parameters to build dialog to select a value from a list
      * @param tilts window title of the selected item
      * @param subTilts window title of the selected item
      * @param list list of processed items
      */
     void toAlterDialogListItem(String tilts, String subTilts, String[] list);

     /**
      * Minimum parameters to build confirmation dialog
      * @param tilts window title of the selected item
      * @param text action description text
      */
     void toAlterDialogNoItem(String tilts, String text);
}
