package com.ljj.fragplugin.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ljj.fragplugin.R;
import com.ljj.fragplugin.appcomm.BaseFragment;

/**
 * Created by Lijj on 17/2/9.
 */

public class HomeFragment extends BaseFragment{

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle
            savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home,null);

        TextView titleTextView = (TextView) rootView.findViewById(R.id.home_title);
        TextView appcommTextView = (TextView) rootView.findViewById(R.id.home_appcomm);
        TextView resourceTextView = (TextView) rootView.findViewById(R.id.home_resource);

        titleTextView.setText(getString(R.string.home_content));
        appcommTextView.setText(getString(R.string.appcomm_string));
        resourceTextView.setText(getString(R.string.resource_string));

        return rootView;
    }
}
