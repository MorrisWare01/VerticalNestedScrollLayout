package com.morrisware.nestedscrolllayout.test;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.app.AppCompatActivity;

import com.morrisware.nestedscrolllayout.test.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
//        mBinding.viewPager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));
//        mBinding.tabLayout.setupWithViewPager(mBinding.viewPager);
    }

    private class MyPagerAdapter extends FragmentStatePagerAdapter {

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return new MainFragment();
            } else {
                return new ErrorFragment();
            }
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return "TAB:" + position;
        }
    }

}
