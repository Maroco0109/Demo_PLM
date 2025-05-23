package com.maroco.demo_plm

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.maroco.demo_plm.databinding.ActivityMainBinding
import com.maroco.demo_plm.ui.IndicatorFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ai.onnxruntime.OrtEnvironment

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNav: BottomNavigationView

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_READ_SMS = 100
        var inboxMessages: List<Pair<String, Boolean>> = listOf()
        var spamMessages: List<Pair<String, Boolean>> = listOf()
        var isSmsClassified: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ─── ONNX 모델 세션 미리 로드 ───────────────────────────────
        // ModelManager 에서 sessionKoBert / sessionKoElectra / sessionKoRoberta 를 초기화
        ModelManager.loadModels(applicationContext)
        Log.d(TAG, "ONNX models loaded into ModelManager")
        // ───────────────────────────────────────────────────────────

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

        // 권한 확인 및 요청
        if (hasReadSmsPermission()) {
            startClassification()
        } else {
            requestReadSmsPermission()
        }
    }

    // 분류 시작
    private fun startClassification() {
        lifecycleScope.launch(Dispatchers.IO) {
            classifySms(applicationContext)
            withContext(Dispatchers.Main) {
                isSmsClassified = true
                // SMS 분류 완료 후 UI 갱신이 필요하면 여기에 추가
            }
        }
    }

    // 권한 보유 여부 확인
    private fun hasReadSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    // 권한 요청
    private fun requestReadSmsPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_SMS),
            REQUEST_READ_SMS
        )
    }

    // 권한 요청 결과 처리
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_READ_SMS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startClassification()
            } else {
                Toast.makeText(
                    this,
                    "SMS 읽기 권한이 필요합니다.",
                    Toast.LENGTH_LONG
                ).show()
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

                Log.d(TAG, "SMS $processed/$total: " +
                        "BERT - Spam: ${"%.2f".format(s1)}% Ham: ${"%.2f".format(h1)}% / " +
                        "ELECTRA - Spam: ${"%.2f".format(s2)}% Ham: ${"%.2f".format(h2)}% / " +
                        "ROBERTA - Spam: ${"%.2f".format(s3)}% Ham: ${"%.2f".format(h3)}%")

                // 가중 평균 앙상블
                val ensembleProb = 0.2f * s1 + 0.6f * s2 + 0.2f * s3
                val isSpam = ensembleProb >= 50f

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
