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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.R;
import com.android.settingslib.search.SearchIndexable;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class Crew extends SettingsPreferenceFragment {

    public static final String TAG = "Crew";

    private String[] crewAvatars;
    private String[] crewTitles;
    private String[] crewDescriptions;
    private String[] crewLinks;

    private int crewCount = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.crew);

        crewAvatars = getResources().getStringArray(R.array.crew_avatars);
        crewTitles = getResources().getStringArray(R.array.crew_titles);
        crewDescriptions = getResources().getStringArray(R.array.crew_descriptions);
        crewLinks = getResources().getStringArray(R.array.crew_links);
        crewCount = crewTitles.length;

        // Update the prefs for each crew member
        for (int i = 0; i < crewCount; i++) {
            final Preference mPref = (Preference) findPreference(Integer.toString(i));
            mPref.setTitle(crewTitles[i]);
            mPref.setSummary(crewDescriptions[i]);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Load avatars from github
        for (int i = 0; i < crewCount; i++) {
            final Preference mPref = (Preference) findPreference(Integer.toString(i));
            new Handler().postDelayed(() -> {
                new RetrieveImageTask().execute(mPref);
            }, 100);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference.getKey() != null) {
            if (preference.getKey().equals("join_crew")) {
                Uri uriUrl = Uri.parse("https://github.com/pixeldust-project-caf/Documentations#readme");
                Intent intent = new Intent(Intent.ACTION_VIEW, uriUrl);
                getActivity().startActivity(intent);
            } else {
                int value = (int) Integer.parseInt(preference.getKey());
                if (value < crewCount) {
                    String sUrl = crewLinks[value];
                    if (sUrl != null) {
                        Uri uriUrl = Uri.parse(sUrl);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uriUrl);
                        getActivity().startActivity(intent);
                    }
                }
            }
        }
        return super.onPreferenceTreeClick(preference);
    }

    private class RetrieveImageTask extends AsyncTask<Preference, Void, Void> {

        protected Void doInBackground(Preference... pref) {
            InputStream is = null;
            try {
                Preference currentPref = (pref[0]);
                int value = (int) Integer.parseInt(currentPref.getKey());
                URL url = new URL(crewAvatars[value]);
                is = url.openStream();
                RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(), BitmapFactory.decodeStream(is));
                drawable.setCircular(true);
                currentPref.setIcon(drawable);
                is.close();
            } catch (Exception e) {
                Log.e(TAG, "Exception in RetrieveImageTask", e);
            }
            return null;
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.PIXELDUST;
    }

    /**
     * For search
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.crew);

}
