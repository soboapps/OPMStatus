package com.soboapps.opm_status;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by Avi on 1/24/2015.
 */
public class PrefFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);
    }
}
