package com.stonedroid.mpgvertretungsplan;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class ViewPagerAdapter extends FragmentPagerAdapter
{
    private Fragment[] fragments;
    private CharSequence[] titles;

    public ViewPagerAdapter(FragmentManager fm, Fragment[] fragments, CharSequence[] titles)
    {
        super(fm);
        this.fragments = fragments;
        this.titles = titles;
    }

    @Override
    public Fragment getItem(int position)
    {
        return fragments[position];
    }

    @Override
    public int getCount()
    {
        return fragments.length;
    }

    @Override
    public CharSequence getPageTitle(int position)
    {
        return titles[position];
    }
}
