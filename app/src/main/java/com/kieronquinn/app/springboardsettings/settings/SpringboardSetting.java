package com.kieronquinn.app.springboardsettings.settings;

import android.graphics.drawable.Drawable;
import android.widget.CompoundButton;

import com.kieronquinn.app.springboardsettings.SpringboardItem;

/**
 * Created by Kieron on 12/02/2018.
 */

public class SpringboardSetting extends SwitchSetting {

    //Hold springboard item for use later
    private SpringboardItem springboardItem;

    //Constructor to be identical to the super but with an item for setting
    public SpringboardSetting(Drawable icon, String title, String subtitle, CompoundButton.OnCheckedChangeListener changeListener, boolean isChecked, SpringboardItem springboardItem) {
        super(icon, title, subtitle, changeListener, isChecked);
        //Set item
        this.springboardItem = springboardItem;
    }

    //Getter for item
    public SpringboardItem getSpringboardItem() {
        return springboardItem;
    }
}
