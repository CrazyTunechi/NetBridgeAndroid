package com.example.netbridge

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView

/**
 * NetBridge development build.
 * Telegram mode can validate and hand off a user-supplied remote MTProto proxy to Telegram.
 * Device-wide DPI processing is deliberately marked unavailable until a real engine is bundled.
 */
class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var log: TextView
    private lateinit var systemPanel: LinearLayout
    private lateinit var telegramPanel: LinearLayout
    private lateinit var modeSpinner: Spinner
    private lateinit var telegramServer: EditText
    private lateinit var telegramPort: EditText
    private lateinit var telegramSecret: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(15, 23, 42)
        window.navigationBarColor = Color.rgb(15, 23, 42)
        window.decorView.systemUiVisibility = 0
        setContentView(buildUi())
    }

    private fun buildUi(): View {
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(9, 14, 27)) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(26), dp(22), dp(24))
        }
        scroll.addView(root)

        root.addView(text("NETBRIDGE", 13, Color.rgb(94, 234, 212), true))
        root.addView(text("Сетевой помощник", 29, Color.WHITE, true), margin(top = 8))
        root.addView(text("Локальные сетевые инструменты для Android", 14, MUTED), margin(top = 6, bottom = 22))

        val panel = card()
        root.addView(panel, matchWidth())
        panel.addView(text("СОСТОЯНИЕ", 12, MUTED, true))
        status = text("Движок не подключён", 21, Color.WHITE, true)
        panel.addView(status, margin(top = 10))
        panel.addView(text("Режим Telegram умеет передать заданный MTProto-прокси в Telegram. Системный DPI-движок пока не включён.", 13, MUTED), margin(top = 8))

        root.addView(text("Режим работы", 18, Color.WHITE, true), margin(top = 24, bottom = 8))
        modeSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                listOf("Весь телефон — DPI / VPN", "Только Telegram — MTProto proxy"))
            setSelection(0)
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (::systemPanel.isInitialized && ::telegramPanel.isInitialized) {
                        systemPanel.visibility = if (position == 0) View.VISIBLE else View.GONE
                        telegramPanel.visibility = if (position == 1) View.VISIBLE else View.GONE
                    }
                    if (::status.isInitialized) {
                        status.text = if (position == 0) "Системный движок не подключён" else "Готов к настройке Telegram-прокси"
                    }
                }
            }
        }
        root.addView(modeSpinner, matchWidth())

        systemPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(text("Системный режим", 18, Color.WHITE, true), margin(top = 20, bottom = 8))
            addView(text("Для работы нужен ByeDPI + hev-socks5-tunnel (или совместимый движок), собранный для Android. Эти бинарные компоненты ещё не включены в проект, поэтому трафик сейчас не перенаправляется.", 14, MUTED))
            addView(text("Профиль обработки", 14, Color.WHITE, true), margin(top = 16, bottom = 6))
            addView(Spinner(this@MainActivity).apply {
                adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                    listOf("Автоматический (пока не активен)", "Профиль A — тестовый", "Профиль B — тестовый"))
            }, matchWidth())
            addView(Button(this@MainActivity).apply {
                text = "Проверить системный движок"
                isAllCaps = false
                setOnClickListener {
                    status.text = "Системный движок отсутствует"
                    appendLog("Системный режим: запуск отменён — ByeDPI и tun2socks ещё не интегрированы.")
                }
            }, matchWidth(top = 12))
        }
        root.addView(systemPanel, matchWidth())

        telegramPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            addView(text("Настройка Telegram", 18, Color.WHITE, true), margin(top = 20, bottom = 8))
            addView(text("Введи данные существующего удалённого MTProto-прокси. Это не запускает локальный TgWsProxy-движок — его интеграция остаётся следующим этапом.", 14, MUTED))
            telegramServer = field("Адрес сервера, например 1.2.3.4")
            telegramPort = field("Порт, например 443", "443")
            telegramSecret = field("Secret (hex или формат dd...)")
            addView(telegramServer, matchWidth(top = 12))
            addView(telegramPort, matchWidth(top = 8))
            addView(telegramSecret, matchWidth(top = 8))
            addView(Button(this@MainActivity).apply {
                text = "Открыть прокси в Telegram"
                isAllCaps = false
                setOnClickListener { openTelegramProxy() }
            }, matchWidth(top = 12))
            addView(Button(this@MainActivity).apply {
                text = "Скопировать ссылку"
                isAllCaps = false
                setOnClickListener {
                    val link = makeTelegramProxyUri()
                    if (link != null) {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Telegram proxy", link))
                        appendLog("Ссылка tg://proxy скопирована в буфер обмена.")
                    }
                }
            }, matchWidth(top = 6))
        }
        root.addView(telegramPanel, matchWidth())

        root.addView(text("Журнал событий", 18, Color.WHITE, true), margin(top = 28, bottom = 8))
        log = text("Приложение запущено.\nСетевой движок ещё не подключён.", 13, Color.rgb(188, 199, 218))
        log.setPadding(dp(14), dp(14), dp(14), dp(14))
        log.background = rounded(Color.rgb(20, 29, 48), dp(14))
        root.addView(log, matchWidth())
        root.addView(text("Версия 0.2.0 • Этап интеграции", 12, Color.rgb(111, 125, 151)), margin(top = 24))
        return scroll
    }

    private fun openTelegramProxy() {
        val link = makeTelegramProxyUri() ?: return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
            status.text = "Передано в Telegram"
            appendLog("Передана ссылка MTProto-прокси в Telegram. Успех соединения ещё нужно проверить в самом Telegram.")
        } catch (_: Exception) {
            status.text = "Telegram не найден"
            appendLog("Не удалось открыть ссылку. Можно скопировать её и открыть на устройстве с Telegram.")
        }
    }

    private fun makeTelegramProxyUri(): String? {
        val server = telegramServer.text.toString().trim()
        val portText = telegramPort.text.toString().trim()
        val secret = telegramSecret.text.toString().trim()
        val port = portText.toIntOrNull()
        if (server.isBlank() || server.any { it.isWhitespace() } || server.contains("/") || server.contains("?")) {
            appendLog("Ошибка: введи корректный адрес сервера.")
            return null
        }
        if (port == null || port !in 1..65535) {
            appendLog("Ошибка: порт должен быть числом от 1 до 65535.")
            return null
        }
        if (!secret.matches(Regex("(?i)(dd)?[0-9a-f]{32,128}"))) {
            appendLog("Ошибка: secret должен быть hex-строкой (32–128 символов), необязательно с префиксом dd.")
            return null
        }
        return "tg://proxy?server=${Uri.encode(server)}&port=$port&secret=${Uri.encode(secret)}"
    }

    private fun field(hintText: String, initial: String = "") = EditText(this).apply {
        hint = hintText
        setHintTextColor(Color.rgb(111, 125, 151))
        setTextColor(Color.WHITE)
        textSize = 15f
        setSingleLine(true)
        setPadding(dp(14), dp(12), dp(14), dp(12))
        setBackgroundColor(Color.rgb(20, 29, 48))
        setText(initial)
    }

    private fun appendLog(message: String) { log.append("\n• $message") }
    private fun text(value: String, size: Int, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.START
    }
    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18))
        background = rounded(Color.rgb(20, 29, 48), dp(18))
    }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun matchWidth(top: Int = 0): LinearLayout.LayoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { if (top > 0) topMargin = dp(top) }
    private fun margin(top: Int = 0, bottom: Int = 0): LinearLayout.LayoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(top); bottomMargin = dp(bottom) }
    private fun rounded(color: Int, radius: Int) = android.graphics.drawable.GradientDrawable().apply {
        setColor(color); cornerRadius = radius.toFloat()
    }
    companion object { private val MUTED = Color.rgb(163, 174, 195) }
}
