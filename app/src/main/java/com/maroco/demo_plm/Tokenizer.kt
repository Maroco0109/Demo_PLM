package com.maroco.demo_plm

import android.content.Context
import com.google.gson.Gson
import java.io.InputStreamReader

data class TokenizerConfig(
    val unk_token: String = "[UNK]",
    val cls_token: String = "[CLS]",
    val sep_token: String = "[SEP]",
    val pad_token: String = "[PAD]"
)

class Tokenizer(
    private val context: Context,
    vocabFileName: String,
    configFileName: String
) {
    private val vocab: Map<String, Int>
    private val config: TokenizerConfig

    init {
        vocab = loadVocabFromAssets(vocabFileName)
        config = loadTokenizerConfigFromAssets(configFileName)
    }

    // vocab.txt 로딩
    private fun loadVocabFromAssets(fileName: String): Map<String, Int> {
        val vocab = mutableMapOf<String, Int>()
        context.assets.open(fileName).bufferedReader().useLines { lines ->
            lines.forEachIndexed { idx, token -> vocab[token.trim()] = idx }
        }
        return vocab
    }

    // tokenizer_config.json 로딩
    private fun loadTokenizerConfigFromAssets(fileName: String): TokenizerConfig {
        val json = context.assets.open(fileName).bufferedReader().use { it.readText() }
        return Gson().fromJson(json, TokenizerConfig::class.java)
    }

    // 입력 텍스트를 HuggingFace 스타일로 토큰화 + 인덱스 변환
    fun tokenize(text: String, maxLen: Int = 64): Triple<LongArray, LongArray, LongArray> {
        val clsId = vocab[config.cls_token] ?: 101
        val sepId = vocab[config.sep_token] ?: 102
        val padId = vocab[config.pad_token] ?: 0
        val unkId = vocab[config.unk_token] ?: 100

        // 현재는 띄어쓰기 기준 토큰화 (WordPiece 전처리 일부 생략됨)
        val tokens = text.split(" ").map { vocab[it] ?: unkId }

        val inputIds = mutableListOf(clsId)
        inputIds.addAll(tokens.take(maxLen - 2))
        inputIds.add(sepId)

        while (inputIds.size < maxLen) {
            inputIds.add(padId)
        }

        val attentionMask = LongArray(maxLen) { if (it < tokens.size + 2) 1 else 0 }
        val tokenTypeIds = LongArray(maxLen) { 0 }

        return Triple(
            inputIds.map { it.toLong() }.toLongArray(),
            attentionMask,
            tokenTypeIds
        )
    }
}