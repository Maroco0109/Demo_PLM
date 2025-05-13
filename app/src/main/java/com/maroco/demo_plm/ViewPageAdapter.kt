package com.maroco.demo_plm

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import android.os.Bundle

class ViewPageAdapter(
    activity: FragmentActivity,
    private val inboxList: List<String>,
    private val spamList: List<String>
) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> InputFragment()
            1 -> SmsResultFragment().apply {
                arguments = Bundle().apply {
                    putStringArrayList("inbox", ArrayList(inboxList))
                    putStringArrayList("spam", ArrayList(spamList))
                }
            }
            else -> Fragment()
        }
    }
}
