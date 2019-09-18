package com.google.nartkolai.droidadbtools.Utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.lang.reflect.InvocationTargetException;


public class DialogAlter {
    private Context context;

    public DialogAlter(Context context){
       this.context = context;
    }

    public void displayDialog(final MySelectorImpl mySel) {
        final String[] list = mySel.getItemList();
        final EditText input = new EditText(context);
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(mySel.getTilts());
        if (mySel.getNeedInput()) {
            input.setInputType(mySel.getInputType());
            input.setText(mySel.getText());
            input.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            builder.setView(input);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()  {
                @Override
                public void onClick(DialogInterface dialog, int which)  {
                    String act = String.valueOf(input.getText());
                    Object[] parameters = new Object[1];
                    parameters[0] = act;
                    try {
                        mySel.getMethod().invoke(mySel.getObject(), parameters);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            });
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
        }else {
            builder.setPositiveButton(context.getResources().getString(android.R.string.cancel), null);
            builder.setItems(list, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int position) {
                    Object[] parameters = new Object[2];
                    parameters[0] = list[position];
                    parameters[1] = false;
                    try {
                        mySel.getMethod().invoke(mySel.getObject(), parameters);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        builder.create();
        final AlertDialog alertDialog = builder.create();
        if (!mySel.getNeedInput()){
            alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    ListView lv = alertDialog.getListView();
                    lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                        @Override
                        public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                            builder.setTitle(mySel.getSubTilts() + "\n" + list[position]);
                            builder.setItems(null, null);
                            builder.setPositiveButton(context.getResources().getString(android.R.string.ok),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Object[] parameters = new Object[2];
                                            parameters[0] = list[position];
                                            parameters[1] = true;
                                            try {
                                                mySel.getMethod().invoke(mySel.getObject(), parameters);
                                            } catch (IllegalAccessException e) {
                                                e.printStackTrace();
                                            } catch (InvocationTargetException e) {
                                                e.printStackTrace();
                                            }
                                            dialog.cancel();
                                            alertDialog.cancel();
                                        }
                                    }
                            );
                            builder.setNegativeButton(context.getResources().getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                        }
                                    }
                            );
                            builder.show();
                            return true;
                        }
                    });
                }
            });
        }
        alertDialog.show();
    }
}