package com.maroco.demo_plm

import android.content.Context
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.maroco.demo_plm.databinding.ActivityMainBinding
import com.maroco.demo_plm.ui.IndicatorFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ai.onnxruntime.OrtEnvironment
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNav: BottomNavigationView

    companion object {
        private const val TAG = "MainActivity"
        var inboxMessages: List<Pair<String, Boolean>> = listOf()
        var spamMessages: List<Pair<String, Boolean>> = listOf()
        var isSmsClassified: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomNav = binding.bottomNavigation
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_input -> {
                    loadFragment(InputFragment())
                    true
                }
                R.id.nav_sms -> {
                    if (isSmsClassified) loadFragment(SmsFragment())
                    else loadFragment(IndicatorFragment())
                    true
                }
                else -> false
            }
        }
        bottomNav.selectedItemId = R.id.nav_input

        lifecycleScope.launch(Dispatchers.IO) {
            classifySms(applicationContext)
            withContext(Dispatchers.Main) {
                isSmsClassified = true
            }
        }
    }

    private fun classifySms(context: Context) {
        Log.d(TAG, "SMS classification started")
        val ortEnv = OrtEnvironment.getEnvironment()
        val tokB = Tokenizer(context, "vocab_kobert.txt", "tokenizer_config_kobert.json", subwordPrefix = "##")
        val tokE = Tokenizer(context, "vocab_koelectra.txt", "tokenizer_config_koelectra.json", subwordPrefix = "##")
        val tokR = Tokenizer(context, "vocab_koroberta.txt", "tokenizer_config_koroberta.json", subwordPrefix = "Ġ")
        val clsB = ModelClassifier(ortEnv, tokB, ModelManager.sessionKoBert)
        val clsE = ModelClassifier(ortEnv, tokE, ModelManager.sessionKoElectra)
        val clsR = ModelClassifier(ortEnv, tokR, ModelManager.sessionKoRoberta)

        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.BODY), null, null, null
        )?.use { cur ->
            val total = cur.count
            Log.d(TAG, "Found $total SMS messages to classify")
            val idx = cur.getColumnIndex(Telephony.Sms.BODY)
            var processed = 0
            val inList = mutableListOf<Pair<String, Boolean>>()
            val spList = mutableListOf<Pair<String, Boolean>>()

            while (cur.moveToNext()) {
                val text = cur.getString(idx) ?: continue
                processed++

                // 개별 모델 스팸/햄 퍼센티지 계산
                val (s1, _) = clsB.classify(text)
                val (s2, _) = clsE.classify(text)
                val (s3, _) = clsR.classify(text)
                val h1 = 100f - s1
                val h2 = 100f - s2
                val h3 = 100f - s3

                // per-message 로그
                Log.d(TAG, "SMS $processed/$total: " +
                        "BERT - Spam: ${"%.2f".format(s1)}% Ham: ${"%.2f".format(h1)}% / " +
                        "ELECTRA - Spam: ${"%.2f".format(s2)}% Ham: ${"%.2f".format(h2)}% / " +
                        "ROBERTA - Spam: ${"%.2f".format(s3)}% Ham: ${"%.2f".format(h3)}%" )

                val isSpam = (s1 >= 50f && s2 >= 50f && s3 >= 50f)
                if (isSpam) spList.add(text to true) else inList.add(text to false)
            }

            inboxMessages = inList
            spamMessages  = spList
            Log.d(TAG, "SMS classification completed: $processed processed, ${spList.size} spam, ${inList.size} inbox")
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
