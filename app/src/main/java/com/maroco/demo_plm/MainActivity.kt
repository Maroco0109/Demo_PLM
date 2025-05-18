package com.maroco.demo_plm

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    lateinit var sessionKoBert: OrtSession
    lateinit var sessionKoElectra: OrtSession
    lateinit var inboxMessages: List<Pair<String, Boolean>>
    lateinit var spamMessages: List<Pair<String, Boolean>>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Load ONNX models
        val ortEnv = OrtEnvironment.getEnvironment()
        sessionKoBert = ortEnv.createSession(assets.open("kobert_spam.onnx").readBytes())
        sessionKoElectra = ortEnv.createSession(assets.open("koelectra_spam.onnx").readBytes())

        // Receive classified SMS results from SplashActivity
        inboxMessages = SplashActivity.inboxMessages
        spamMessages = SplashActivity.spamMessages

        bottomNav = findViewById(R.id.bottom_navigation)

        // 초기 Fragment
        loadFragment(InputFragment())

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_input -> {
                    loadFragment(InputFragment())
                    true
                }
                R.id.nav_sms -> {
                    loadFragment(SmsResultFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    fun switchToPage(index: Int) {
        val nav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        nav.selectedItemId = when (index) {
            0 -> R.id.nav_input
            1 -> R.id.nav_sms
            else -> R.id.nav_input
        }
    }
}