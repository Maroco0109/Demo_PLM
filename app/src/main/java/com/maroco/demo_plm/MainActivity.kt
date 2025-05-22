package com.maroco.demo_plm

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Intent
import com.maroco.demo_plm.databinding.ActivityMainBinding  // ViewBinding 사용 시
import com.maroco.demo_plm.InputFragment
import com.maroco.demo_plm.SmsFragment
import com.maroco.demo_plm.ui.IndicatorFragment

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    lateinit var sessionKoBert: OrtSession
    lateinit var sessionKoElectra: OrtSession
    lateinit var sessionKoRoberta: OrtSession
    lateinit var inboxMessages: List<Pair<String, Boolean>>
    lateinit var spamMessages: List<Pair<String, Boolean>>

    // 모델 준비 상태 변수
    companion object {
        var isModelReady = false
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Load ONNX models
        val ortEnv = OrtEnvironment.getEnvironment()
        sessionKoBert = ortEnv.createSession(assets.open("kobert_spam.onnx").readBytes())
        sessionKoElectra = ortEnv.createSession(assets.open("koelectra_spam.onnx").readBytes())
        sessionKoRoberta = ortEnv.createSession(assets.open("koroberta_spam.onnx").readBytes())

        // Receive classified SMS results from SplashActivity
        inboxMessages = SplashActivity.inboxMessages
        spamMessages = SplashActivity.spamMessages

        bottomNav = findViewById(R.id.bottom_navigation)

        // 초기 Fragment
        loadFragment(InputFragment())

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_input -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, InputFragment())
                        .commit()
                    true
                }
                R.id.nav_sms -> {
                    val fragment = if (isModelReady) SmsFragment() else IndicatorFragment()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .commit()
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