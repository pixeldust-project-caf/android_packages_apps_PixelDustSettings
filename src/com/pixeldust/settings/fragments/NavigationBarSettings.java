/*
 * Copyright (C) 2017-2020 The PixelDust Project
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

import android.content.Context;
import android.content.ContentResolver;
import android.content.om.IOverlayManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.ServiceManager;
import android.os.Vibrator;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import androidx.preference.PreferenceCategory;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.pixeldust.PixeldustUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.gestures.SystemNavigationGestureSettings;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class NavigationBarSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener, Indexable {

    private static final String ENABLE_NAV_BAR = "enable_nav_bar";
    private static final String LAYOUT_SETTINGS = "navbar_layout_views";
    private static final String NAVIGATION_BAR_INVERSE = "navbar_inverse_layout";
    private static final String GESTURE_SYSTEM_NAVIGATION = "gesture_system_navigation";
    private static final String KEY_GESTURE_BAR_SIZE = "navigation_handle_width";
    private static final String PIXEL_NAV_ANIMATION = "pixel_nav_animation";

    private SwitchPreference mEnableNavigationBar;
    private Preference mLayoutSettings;
    private SwitchPreference mNavigationArrows;
    private SwitchPreference mSwapNavButtons;
    private Preference mGestureSystemNavigation;
    private ListPreference mGestureBarSize;
    private SwitchPreference mPixelNavAnimation;

    private boolean mIsNavSwitchingMode = false;
    private Handler mHandler;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.pixeldust_settings_navigation);
        final PreferenceScreen prefScreen = getPreferenceScreen();

        mGestureBarSize = (ListPreference) findPreference(KEY_GESTURE_BAR_SIZE);
        int gesturebarsize = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.NAVIGATION_HANDLE_WIDTH, 2, UserHandle.USER_CURRENT);
        mGestureBarSize.setValue(String.valueOf(gesturebarsize));
        mGestureBarSize.setSummary(mGestureBarSize.getEntry());
        mGestureBarSize.setOnPreferenceChangeListener(this);

        // Navigation bar related options
        mEnableNavigationBar = (SwitchPreference) findPreference(ENABLE_NAV_BAR);

        mLayoutSettings = (Preference) findPreference(LAYOUT_SETTINGS);
        mSwapNavButtons = (SwitchPreference) findPreference(NAVIGATION_BAR_INVERSE);
        if (!PixeldustUtils.isThemeEnabled("com.android.internal.systemui.navbar.threebutton")) {
            if (mLayoutSettings != null) prefScreen.removePreference(mLayoutSettings);
        }

        mGestureSystemNavigation = (Preference) findPreference(GESTURE_SYSTEM_NAVIGATION);
        mPixelNavAnimation = (SwitchPreference) findPreference(PIXEL_NAV_ANIMATION);
        if (PixeldustUtils.isThemeEnabled("com.android.internal.systemui.navbar.threebutton")) {
            mGestureSystemNavigation.setSummary(getString(R.string.legacy_navigation_title));
            prefScreen.removePreference(mGestureBarSize);
        } else if (PixeldustUtils.isThemeEnabled("com.android.internal.systemui.navbar.twobutton")) {
            mGestureSystemNavigation.setSummary(getString(R.string.swipe_up_to_switch_apps_title));
            prefScreen.removePreference(mGestureBarSize);
        } else { // Navbar gestural mode
            mGestureSystemNavigation.setSummary(getString(R.string.edge_to_edge_navigation_title));
            prefScreen.removePreference(mPixelNavAnimation);
            if (mSwapNavButtons != null) prefScreen.removePreference(mSwapNavButtons);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mEnableNavigationBar) {
            if (mIsNavSwitchingMode) {
                return false;
            }
            mIsNavSwitchingMode = true;
            boolean isNavBarChecked = ((Boolean) newValue);
            mEnableNavigationBar.setEnabled(false);
            writeNavBarOption(isNavBarChecked);
            updateNavBarOption();
            mEnableNavigationBar.setEnabled(true);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mIsNavSwitchingMode = false;
                }
            }, 1000);
            return true;
        } else if (preference == mGestureBarSize) {
            int value = Integer.parseInt((String) newValue);
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_HANDLE_WIDTH, value,
                    UserHandle.USER_CURRENT);
            int index = mGestureBarSize.findIndexOfValue((String) newValue);
            mGestureBarSize.setSummary(mGestureBarSize.getEntries()[index]);
            SystemNavigationGestureSettings.setBackSensivityOverlay(true);
            SystemNavigationGestureSettings.setCurrentSystemNavigationMode(getActivity(),
                    getOverlayManager(), SystemNavigationGestureSettings.getCurrentSystemNavigationMode(getActivity()));
            return true;
        }
        return false;
    }

    private void writeNavBarOption(boolean enabled) {
        Settings.System.putIntForUser(getActivity().getContentResolver(),
                Settings.System.FORCE_SHOW_NAVBAR, enabled ? 1 : 0, UserHandle.USER_CURRENT);
    }

    private void updateNavBarOption() {
        boolean defaultToNavigationBar = PixeldustUtils.deviceSupportNavigationBar(getActivity());
        boolean enabled = Settings.System.getIntForUser(getActivity().getContentResolver(),
                Settings.System.FORCE_SHOW_NAVBAR, defaultToNavigationBar ? 1 : 0, UserHandle.USER_CURRENT) != 0;
        mEnableNavigationBar.setChecked(enabled);
    }

    private IOverlayManager getOverlayManager() {
        return IOverlayManager.Stub.asInterface(ServiceManager.getService(Context.OVERLAY_SERVICE));
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.PIXELDUST;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                            boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.pixeldust_settings_navigation;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    return keys;
                }
            };
}
