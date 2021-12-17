/*
 * Copyright (C) 2021 The PixelDust Project
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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settings.R;

import java.util.List;
import java.util.ArrayList;

@SearchIndexable
public class About extends SettingsPreferenceFragment {

    public static final String TAG = "About";

    private String KEY_PIXELDUST_DONATE = "pixeldust_donate";
    private String KEY_PIXELDUST_SOURCE = "pixeldust_source";
    private String KEY_PIXELDUST_TELEGRAM = "pixeldust_telegram";
    private String KEY_PIXELDUST_TELEGRAM_CHANNEL = "pixeldust_telegram_channel";
    private String KEY_PIXELDUST_WEBSITE = "pixeldust_website";

    private Preference mDonate;
    private Preference mSourceUrl;
    private Preference mTelegramUrl;
    private Preference mTelegramChannelUrl;
    private Preference mWebsite;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.about);

        mDonate = findPreference(KEY_PIXELDUST_DONATE);
        mSourceUrl = findPreference(KEY_PIXELDUST_SOURCE);
        mTelegramUrl = findPreference(KEY_PIXELDUST_TELEGRAM);
        mTelegramChannelUrl = findPreference(KEY_PIXELDUST_TELEGRAM_CHANNEL);
        mWebsite = findPreference(KEY_PIXELDUST_WEBSITE);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mDonate) {
            launchUrl("https://www.paypal.me/spezi77");
        } else if (preference == mSourceUrl) {
            launchUrl("https://github.com/pixeldust-project-caf");
        } else if (preference == mTelegramUrl) {
            launchUrl("https://t.me/pixeldustcommunity");
        } else if (preference == mTelegramChannelUrl) {
            launchUrl("https://t.me/pixeldustproject");
        } else if (preference == mWebsite) {
            launchUrl("https://sourceforge.net/projects/pixeldustproject/files/ota/");
        }
        return super.onPreferenceTreeClick(preference);
    }

    private void launchUrl(String url) {
        Uri uriUrl = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uriUrl);
        getActivity().startActivity(intent);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.PIXELDUST;
    }

    /**
     * For search
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.about);

}
