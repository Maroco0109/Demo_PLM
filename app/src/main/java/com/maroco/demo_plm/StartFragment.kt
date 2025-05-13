package com.maroco.demo_plm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class StartFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_start, container, false)
        val btnStart = view.findViewById<Button>(R.id.btnStart)
        btnStart.setOnClickListener {
            (activity as? MainActivity)?.switchToPage(1)
        }
        return view
    }
}