package com.nartkolai.droidadbtools.Utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.nartkolai.droidadbtools.R;

import java.lang.reflect.InvocationTargetException;


public class AlterDialogHelper extends AlertDialog{
    private Context context;
    private AlertDialog alertDialog;

    public AlterDialogHelper(Context context){
        super(context);
        this.context = context;
    }

    public AlertDialog displayDialog(final AlterDialogSelector mySel) {
        final String[] list = mySel.getItemList();
        final EditText input = new EditText(context);
        final Builder builder = new Builder(context);
        int position = mySel.getText().length();
        builder.setTitle(mySel.getTilts());
        if (mySel.getNeedInput()) { // Creating a dialog box with a field for entering values
            input.setInputType(mySel.getInputType());
            if(mySel.getKeyListener() != null) {
                input.setKeyListener(mySel.getKeyListener());
            }
            input.setText(mySel.getText());
            input.setSelection(position);
            input.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            builder.setView(input);
            builder.setPositiveButton(android.R.string.ok, new OnClickListener()  {
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
            builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
        }else if(list != null && !mySel.getNeedInput()){ //Creating a dialog to select a value from a list
            builder.setPositiveButton(context.getResources().getString(R.string.add_ip_dev), new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Object[] parameters = new Object[3];
                    parameters[0] = null;
                    parameters[1] = false;
                    parameters[2] = true;
                    try {
                        mySel.getMethod().invoke(mySel.getObject(), parameters);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }

                }
            });
            builder.setNegativeButton(context.getResources().getString(android.R.string.cancel), null);
            builder.setItems(list, new OnClickListener() {
                public void onClick(DialogInterface dialog, int position) {
                    Object[] parameters = new Object[3];
                    parameters[0] = list[position];
                    parameters[1] = false;
                    parameters[2] = false;
                    try {
                        mySel.getMethod().invoke(mySel.getObject(), parameters);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            });
        }else { //Create confirmation dialog
            builder.setTitle(mySel.getTilts());
            builder.setMessage(mySel.getText());
            builder.setPositiveButton(context.getResources().getString(android.R.string.ok), new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (mySel.getMethod() != null) {
                        Object[] parameters = new Object[1];
                        parameters[0] = "YES";
                        try {
                            mySel.getMethod().invoke(mySel.getObject(), parameters);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                    dialog.cancel();
                }
            });
            if (mySel.getMethod() != null) {
                builder.setNegativeButton(context.getResources().getString(android.R.string.cancel), new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Object[] parameters = new Object[1];
                        parameters[0] = "NO";
                        try {
                            mySel.getMethod().invoke(mySel.getObject(), parameters);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                        dialog.cancel();
                    }
                });
            }
        }
        builder.create();
        alertDialog = builder.create();
        if (!mySel.getNeedInput() && mySel.getItemList() != null){// Creating a confirmation dialog box after a long click on an item
            alertDialog.setOnShowListener(new OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    ListView lv = alertDialog.getListView();
                    lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                        @Override
                        public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                            assert list != null;
                            builder.setTitle(mySel.getSubTilts() + list[position]);
                            builder.setItems(null, null);
                            builder.setPositiveButton(context.getResources().getString(android.R.string.ok),
                                    new OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Object[] parameters = new Object[3];
                                            parameters[0] = list[position];
                                            parameters[1] = true;
                                            parameters[2] = false;
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
                            builder.setNegativeButton(context.getResources().getString(android.R.string.cancel), new OnClickListener() {
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
        return alertDialog;
    }
}
