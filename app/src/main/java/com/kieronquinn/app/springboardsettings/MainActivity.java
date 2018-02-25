package com.kieronquinn.app.springboardsettings;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.kieronquinn.app.springboardsettings.helper.SimpleItemTouchHelperCallback;
import com.kieronquinn.app.springboardsettings.settings.Adapter;
import com.kieronquinn.app.springboardsettings.settings.BaseSetting;
import com.kieronquinn.app.springboardsettings.settings.HeaderSetting;
import com.kieronquinn.app.springboardsettings.settings.SpringboardSetting;
import com.kieronquinn.app.springboardsettings.settings.TextSetting;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    //List to hold all settings
    private ArrayList<BaseSetting> settingList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Get in and out settings. In is the main setting, which defines the order and state of a page, but does not always contain them all. Out contains them all, but no ordering
        String springboard_widget_order_in = Settings.System.getString(getContentResolver(), "springboard_widget_order_in");
        String springboard_widget_order_out = Settings.System.getString(getContentResolver(), "springboard_widget_order_out");
        //Create empty list
        settingList = new ArrayList<>();
        try {
            //Parse JSON
            JSONObject root = new JSONObject(springboard_widget_order_in);
            JSONArray data = root.getJSONArray("data");
            List<String> addedComponents = new ArrayList<>();
            //Data array contains all the elements
            for(int x = 0; x < data.length(); x++){
                //Get item
                JSONObject item = data.getJSONObject(x);
                //srl is the position, stored as a string for some reason
                int srl = Integer.parseInt(item.getString("srl"));
                //State is stored as an integer when it would be better as a boolean so convert it
                boolean enable = item.getInt("enable") == 1;
                //Create springboard item with the package name, class name and state
                final SpringboardItem springboardItem = new SpringboardItem(item.getString("pkg"), item.getString("cls"), enable);
                //Create a setting (extending switch) with the relevant data and a callback
                SpringboardSetting springboardSetting = new SpringboardSetting(null, getTitle(springboardItem.getPackageName()), formatComponentName(springboardItem.getClassName()), new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        //Ignore on create to reduce load
                        if(!compoundButton.isPressed())return;
                        //Update state
                        springboardItem.setEnabled(b);
                        //Save
                        save();
                    }
                }, springboardItem.isEnable(), springboardItem);
                //Store component name for later
                addedComponents.add(springboardItem.getClassName());
                try {
                    //Attempt to add at position, may cause exception
                    settingList.add(srl, springboardSetting);
                }catch (IndexOutOfBoundsException e){
                    //Add at top as position won't work
                    settingList.add(springboardSetting);
                }
            }
            //Parse JSON
            JSONObject rootOut = new JSONObject(springboard_widget_order_out);
            JSONArray dataOut = rootOut.getJSONArray("data");
            //Loop through main data array
            for(int x = 0; x < data.length(); x++){
                //Get item
                JSONObject item = dataOut.getJSONObject(x);
                //Get component name to check list
                String componentName = item.getString("cls");
                if(!addedComponents.contains(componentName)){
                    //Get if item is enabled, this time stored as a string (why?)
                    boolean enable = item.getString("enable").equals("true");
                    //Create item with the package name, class name and state
                    final SpringboardItem springboardItem = new SpringboardItem(item.getString("pkg"), item.getString("cls"), enable);
                    //Create setting with all the relevant data
                    SpringboardSetting springboardSetting = new SpringboardSetting(null, getTitle(springboardItem.getPackageName()), formatComponentName(springboardItem.getClassName()), new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                            if(!compoundButton.isPressed())return;
                            springboardItem.setEnabled(b);
                            save();
                        }
                    }, springboardItem.isEnable(), springboardItem);
                    //Add class name to list to prevent it being adding more than once
                    addedComponents.add(springboardItem.getClassName());
                    //Add setting to main list
                    settingList.add(springboardSetting);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //Empty settings list can be confusing to the user, and is quite common, so we'll add the FAQ to save them having to read the OP (oh the horror)
        if(settingList.size() == 0){
            //Add error message
            settingList.add(new TextSetting(getString(R.string.error_loading), new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Restart
                    startActivity(new Intent(MainActivity.this, MainActivity.class));
                    finish();
                }
            }));
        }
        //Add main header to top (pos 0)
        settingList.add(0, new HeaderSetting(getString(R.string.springboard)));
        //Create adapter with setting list and a change listener to save the settings on move
        Adapter adapter = new Adapter(this, settingList, new Adapter.ChangeListener() {
            @Override
            public void onChange() {
                checkSave();
            }
        });
        //Create recyclerview as layout
        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        //Setup drag to move using the helper
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);
        //Add padding for the watch
        recyclerView.setPadding((int) getResources().getDimension(R.dimen.padding_round_small), 0, (int) getResources().getDimension(R.dimen.padding_round_small), (int) getResources().getDimension(R.dimen.padding_round_large));
        recyclerView.setClipToPadding(false);
        //Set the view
        setContentView(recyclerView);
    }

    //Get an app name from the package name
    private String getTitle(String pkg) {
        PackageManager packageManager = getPackageManager();
        try {
            return String.valueOf(packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)));
        } catch (PackageManager.NameNotFoundException e) {
            return getString(R.string.unknown);
        }
    }

    private void save() {
        //Create a blank array
        JSONArray data = new JSONArray();
        //Hold position for use as srl
        int pos = 0;
        for(BaseSetting springboardSetting : settingList){
            //Ignore if not a springboard setting
            if(!(springboardSetting instanceof SpringboardSetting))continue;
            //Get item
            SpringboardItem springboardItem = ((SpringboardSetting) springboardSetting).getSpringboardItem();
            JSONObject item = new JSONObject();
            //Setup item with data from the item
            try {
                item.put("pkg", springboardItem.getPackageName());
                item.put("cls", springboardItem.getClassName());
                item.put("srl", String.valueOf(pos));
                item.put("enable", springboardItem.isEnable() ? "1" : "0");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //Add to list and increment position
            data.put(item);
            pos++;
        }
        //Add to root object
        JSONObject root = new JSONObject();
        try {
            root.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //Save setting
        Settings.System.putString(getContentResolver(), "springboard_widget_order_in", root.toString());
        //Notify user
        Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_LONG).show();
    }

    //Countdown timer to prevent saving too often
    private CountDownTimer countDownTimer;
    private void checkSave(){
        //Create timer if not already, for 2 seconds. Call save after completion
        if(countDownTimer == null)countDownTimer = new CountDownTimer(2000, 2000) {
            @Override
            public void onTick(long l) {

            }

            @Override
            public void onFinish() {
                save();
            }
        };
        //Cancel and start timer. This means that this method must be called ONCE in 2 seconds before save will be called, it prevents save from being called more than once every 2 seconds (buffers moving)
        countDownTimer.cancel();
        countDownTimer.start();
    }

    //Get last part of component name
    private String formatComponentName(String componentName){
        //Ignore if no . and just return component name
        if(!componentName.contains("."))return componentName;
        //Return just the last section of component name
        return componentName.substring(componentName.lastIndexOf(".") + 1);
    }
}
