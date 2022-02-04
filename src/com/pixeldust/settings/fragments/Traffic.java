/*
 * Copyright (C) 2021-2022 The PixelDust Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pixeldust.settings.fragments;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.ContentResolver;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.IWindowManager;
import android.view.View;
import android.view.WindowManagerGlobal;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.util.pixeldust.PixeldustUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.pixeldust.support.preference.CustomSeekBarPreference;
import com.pixeldust.support.preference.SystemSettingSeekBarPreference;
import com.pixeldust.support.preference.SystemSettingSwitchPreference;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Traffic extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String DATA_ACTIVITY_ARROW  = "data_activity_arrow";
    private static final String NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD = "network_traffic_autohide_threshold";
    private static final String NETWORK_TRAFFIC_LOCATION = "network_traffic_location";
    private static final String NETWORK_TRAFFIC_REFRESH_INTERVAL = "network_traffic_refresh_interval";
    private static final String NETWORK_TRAFFIC_FONT_SIZE  = "network_traffic_font_size";

    private SwitchPreference mShowCAFArrows;
    private CustomSeekBarPreference mThreshold;
    private ListPreference mNetTrafficLocation;
    private SystemSettingSeekBarPreference mInterval;
    private CustomSeekBarPreference mNetTrafficSize;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.traffic);

        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefSet = getPreferenceScreen();

        int cafValue = Settings.System.getInt(resolver,
                Settings.System.DATA_ACTIVITY_ARROW, 1);
        mShowCAFArrows = (SwitchPreference) findPreference(DATA_ACTIVITY_ARROW);
        mShowCAFArrows.setChecked(cafValue != 0);
        mShowCAFArrows.setOnPreferenceChangeListener(this);

        // Network traffic location
        mNetTrafficLocation = (ListPreference) findPreference(NETWORK_TRAFFIC_LOCATION);
        int location = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_LOCATION, 0, UserHandle.USER_CURRENT);
        mNetTrafficLocation.setOnPreferenceChangeListener(this);

        int trafvalue = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD, 1, UserHandle.USER_CURRENT);
        mThreshold = (CustomSeekBarPreference) findPreference("network_traffic_autohide_threshold");
        mThreshold.setValue(trafvalue);
        mThreshold.setOnPreferenceChangeListener(this);

        int val = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_REFRESH_INTERVAL, 1, UserHandle.USER_CURRENT);
        mInterval = (SystemSettingSeekBarPreference) findPreference(NETWORK_TRAFFIC_REFRESH_INTERVAL);
        mInterval.setValue(val);
        mInterval.setOnPreferenceChangeListener(this);

        int fontSize = Settings.System.getInt(resolver,
                Settings.System.NETWORK_TRAFFIC_FONT_SIZE, 18);
        mNetTrafficSize = (CustomSeekBarPreference) findPreference(NETWORK_TRAFFIC_FONT_SIZE);
        mNetTrafficSize.setValue(fontSize / 1);
        mNetTrafficSize.setOnPreferenceChangeListener(this);

        int netMonitorEnabled = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_STATE, 0, UserHandle.USER_CURRENT);
        if (netMonitorEnabled == 1) {
            mNetTrafficLocation.setValue(String.valueOf(location+1));
            updateTrafficLocation(location+1);
        } else {
            mNetTrafficLocation.setValue("0");
            updateTrafficLocation(0);
        }
        mNetTrafficLocation.setSummary(mNetTrafficLocation.getEntry());
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.PIXELDUST;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mShowCAFArrows) {
            int val = ((Boolean) objValue) ? 1 : 0;
            Settings.System.putInt(resolver,
                    Settings.System.DATA_ACTIVITY_ARROW, val);
            PixeldustUtils.showSystemUiRestartDialog(getContext());
            return true;
        } else if (preference == mNetTrafficLocation) {
            int location = Integer.valueOf((String) objValue);
            int index = mNetTrafficLocation.findIndexOfValue((String) objValue);
            mNetTrafficLocation.setSummary(mNetTrafficLocation.getEntries()[index]);
            if (location > 0) {
                // Convert the selected location mode from our list {0,1,2} and store it to "view location" setting: 0=sb; 1=expanded sb
                Settings.System.putIntForUser(resolver,
                        Settings.System.NETWORK_TRAFFIC_LOCATION, location-1, UserHandle.USER_CURRENT);
                // And also enable the net monitor
                Settings.System.putIntForUser(resolver,
                        Settings.System.NETWORK_TRAFFIC_STATE, 1, UserHandle.USER_CURRENT);
            } else { // Disable net monitor completely
                Settings.System.putIntForUser(resolver,
                        Settings.System.NETWORK_TRAFFIC_STATE, 0, UserHandle.USER_CURRENT);
            }
            updateTrafficLocation(location);
            return true;
        } else if (preference == mThreshold) {
            int val = (Integer) objValue;
            Settings.System.putIntForUser(resolver,
                    Settings.System.NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD, val,
                    UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mInterval) {
            int val = (Integer) objValue;
            Settings.System.putIntForUser(resolver,
                    Settings.System.NETWORK_TRAFFIC_REFRESH_INTERVAL, val,
                    UserHandle.USER_CURRENT);
            return true;
        }  else if (preference == mNetTrafficSize) {
            int fontSize = ((Integer)objValue).intValue();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NETWORK_TRAFFIC_FONT_SIZE, fontSize);
            return true;
        }
        return false;
    }

    public void updateTrafficLocation(int location) {
        switch(location){
            case 0:
                mThreshold.setEnabled(false);
                mInterval.setEnabled(false);
                mNetTrafficSize.setEnabled(false);
                break;
            case 1:
                mNetTrafficSize.setEnabled(true);
                break;
            case 2:
                mThreshold.setEnabled(true);
                mInterval.setEnabled(true);
                mNetTrafficSize.setEnabled(false);
                break;
            default:
                break;
        }
    }
}
