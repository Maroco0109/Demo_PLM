package com.maroco.demo_plm

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.io.IOException

object TokenLogger {
    private const val LOG_DIR  = "main/logs"
    private const val LOG_FILE = "token_log.txt"
    private var writer: FileWriter? = null

    /** 앱 시작 시 한 번만 호출해 로그 파일 준비 */
    fun init(context: Context) {
        try {
            // 앱 내부 저장소 /main/logs/token_log.txt
            val logDir = File(context.filesDir, LOG_DIR)
            if (!logDir.exists()) logDir.mkdirs()

            val file = File(logDir, LOG_FILE)
            if (!file.exists()) file.createNewFile()

            // append 모드로 FileWriter 오픈
            writer = FileWriter(file, /* append = */ true)
        } catch (e: IOException) {
            e.printStackTrace()
            writer = null
        }
    }

    /** 한 줄씩 로그 쓰기 */
    fun logLine(line: String) {
        try {
            writer?.apply {
                append(line)
                append("\n")
                flush()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /** 앱 종료 시 호출해 자원 정리 */
    fun close() {
        try {
            writer?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
