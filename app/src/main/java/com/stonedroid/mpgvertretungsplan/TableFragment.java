package com.stonedroid.mpgvertretungsplan;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class TableFragment extends Fragment
{
    private OnFragmentCreatedListener listener = null;
    private LinearLayout layout;

    public static TableFragment newInstance()
    {
        Bundle args = new Bundle();
        TableFragment fragment = new TableFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnFragmentCreatedListener(OnFragmentCreatedListener listener)
    {
        this.listener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_table, container, false);
        
        layout = view.findViewById(R.id.fragment_layout);
        layout.setId(View.generateViewId());

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        if (listener != null)
        {
            listener.onCreated();
        }
    }

    public LinearLayout getLayout()
    {
        return layout;
    }
}
