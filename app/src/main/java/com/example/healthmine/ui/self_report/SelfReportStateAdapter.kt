package com.example.healthmine.ui.self_report

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class SelfReportStateAdapter(activity:FragmentActivity, val list:ArrayList<Fragment>):
    FragmentStateAdapter(activity){
    override fun getItemCount(): Int {
        return list.size
    }

    override fun createFragment(position: Int): Fragment {
        return list[position]
    }
}