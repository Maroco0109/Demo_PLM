package com.maroco.demo_plm

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.maroco.demo_plm.R
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession

class MainActivity : AppCompatActivity() {

    lateinit var sessionKoBert: OrtSession
    lateinit var sessionKoElectra: OrtSession
    lateinit var inboxMessages: List<String>
    lateinit var spamMessages: List<String>

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

        // Load MainFragment with results
        val fragment = MainFragment().apply {
            arguments = Bundle().apply {
                putStringArrayList("inbox", ArrayList(inboxMessages))
                putStringArrayList("spam", ArrayList(spamMessages))
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }

    fun switchToPage(index: Int) {
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        viewPager.currentItem = index
    }
}