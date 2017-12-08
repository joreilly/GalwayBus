package com.surrus.galwaybus.ui

import android.os.Bundle
import android.preference.PreferenceFragment
import android.support.v7.app.AppCompatActivity
import com.surrus.galwaybus.R


class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content, PrefsFragment()).commit()
    }


    class PrefsFragment : PreferenceFragment() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences)

            val packageInfo = activity.getPackageManager().getPackageInfo(activity.packageName, 0)
            findPreference("version").setSummary(packageInfo.versionName)
        }
    }
}
