package com.maroco.demo_plm

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import android.os.Bundle

class SmsResultPageAdapter(
    fragment: Fragment,
    private val inboxList: List<String>,
    private val spamList: List<String>
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> InboxFragment().apply {
                arguments = Bundle().apply {
                    putStringArrayList("inbox", ArrayList(inboxList))
                }
            }
            1 -> SpamFragment().apply {
                arguments = Bundle().apply {
                    putStringArrayList("spam", ArrayList(spamList))
                }
            }
            else -> Fragment()
        }
    }
}