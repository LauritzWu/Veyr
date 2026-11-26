package io.nekohasekai.sagernet.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.appcompat.widget.Toolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.view.ViewCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.databinding.LayoutLogcatBinding
import io.nekohasekai.sagernet.ktx.*
import io.nekohasekai.sagernet.widget.ListListener
import libcore.Libcore
import moe.matsuri.nb4a.utils.SendLog

class LogcatFragment : ToolbarFragment(R.layout.layout_logcat),
    Toolbar.OnMenuItemClickListener {

    private lateinit var binding: LayoutLogcatBinding
    private val logAccess = Mutex()
    private var displayedLog: String? = null
    private var logSnapshot = ""
    private var searchQuery = ""
    private var viewingHistory = false
    private val colorCodes = Regex("""(?:\u001B\[|\[(?=[0-9;]+m))[0-9;]*m""")

    @SuppressLint("RestrictedApi", "WrongConstant")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setTitle(R.string.menu_log)

        toolbar.inflateMenu(R.menu.logcat_menu)
        toolbar.setOnMenuItemClickListener(this)

        binding = LayoutLogcatBinding.bind(view)
        binding.textview.movementMethod = LinkMovementMethod.getInstance()

        if (Build.VERSION.SDK_INT >= 23) {
            binding.textview.breakStrategy = 0 // simple
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root, ListListener)

        displayedLog = null
        logSnapshot = ""
        viewingHistory = false
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    reloadSession()
                    delay(1000)
                }
            }
        }
    }

    private fun getColorForLine(line: String): ForegroundColorSpan {
        var color = ForegroundColorSpan(Color.GRAY)
        when {
            line.contains("INFO[") || line.contains(" [Info]") -> {
                color = ForegroundColorSpan((0xFF86C166).toInt())
            }

            line.contains("ERROR[") || line.contains(" [Error]") -> {
                color = ForegroundColorSpan(Color.RED)
            }

            line.contains("WARN[") || line.contains(" [Warning]") -> {
                color = ForegroundColorSpan(Color.RED)
            }
        }
        return color
    }

    private suspend fun reloadSession(force: Boolean = false) = logAccess.withLock {
        val current = binding
        if (!force && (viewingHistory || current.scroolview.canScrollVertically(1))) return@withLock
        val previous = displayedLog
        val query = searchQuery
        val history = viewingHistory
        val readLimit = if (history) 256 * 1024L else 50 * 1024L
        val rendered = withContext(Dispatchers.IO) {
            val file = File(current.root.context.cacheDir, "neko.log")
            val raw = try {
                RandomAccessFile(file, "r").use { reader ->
                    val length = reader.length()
                    val start = (length - readLimit).coerceAtLeast(0)
                    val beginsMidLine = start > 0 && run {
                        reader.seek(start - 1)
                        reader.read() != '\n'.code
                    }
                    reader.seek(start)
                    val bytes = ByteArray((length - start).toInt())
                    val count = reader.read(bytes).coerceAtLeast(0)
                    val tail = String(bytes, 0, count, Charsets.UTF_8)
                    if (beginsMidLine) tail.substringAfter('\n', "") else tail
                }
            } catch (_: java.io.IOException) {
                return@withContext null
            }
            val snapshot = colorCodes.replace(raw, "")
            val clean = if (query.isEmpty()) snapshot else snapshot.lineSequence()
                .filter { it.contains(query, ignoreCase = true) }.joinToString("\n")
            if (!force && clean == previous) return@withContext RenderedLog(snapshot, clean, null)

            val span = SpannableString(
                if (query.isNotEmpty() && clean.isEmpty()) current.root.context.getString(R.string.logcat_search_empty)
                else clean
            )
            var offset = 0
            for (line in clean.lineSequence()) {
                if (line.isNotEmpty()) {
                    span.setSpan(getColorForLine(line), offset, offset + line.length, SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                ConnectionLog.idRange(line)?.let { range ->
                    val id = line.substring(range)
                    span.setSpan(object : ClickableSpan() {
                        override fun onClick(widget: View) = showConnectionDetails(id)

                        override fun updateDrawState(paint: TextPaint) {
                            paint.isUnderlineText = true
                        }
                    }, offset + range.first, offset + range.last + 1, SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                if (query.isNotEmpty()) {
                    var match = line.indexOf(query, ignoreCase = true)
                    while (match >= 0) {
                        val start = offset + match
                        val end = start + query.length
                        span.setSpan(BackgroundColorSpan(0xFFFFDB80.toInt()), start, end, SPAN_EXCLUSIVE_EXCLUSIVE)
                        span.setSpan(ForegroundColorSpan(Color.BLACK), start, end, SPAN_EXCLUSIVE_EXCLUSIVE)
                        match = line.indexOf(query, startIndex = match + query.length, ignoreCase = true)
                    }
                }
                offset += line.length + 1
            }
            RenderedLog(snapshot, clean, span)
        } ?: return@withLock
        if (query != searchQuery || history != viewingHistory || (!force && current.scroolview.canScrollVertically(1))) return@withLock

        logSnapshot = rendered.snapshot
        val text = rendered.text ?: return@withLock
        displayedLog = rendered.visible
        current.textview.text = text
        current.textview.doOnLayout {
            current.scroolview.scrollTo(0, current.textview.height)
        }
    }

    private data class RenderedLog(val snapshot: String, val visible: String, val text: SpannableString?)

    private fun showConnectionDetails(id: String) {
        val snapshot = logSnapshot
        viewLifecycleOwner.lifecycleScope.launch {
            val details = withContext(Dispatchers.Default) { ConnectionLog.inspect(snapshot, id) }
            val missing = getString(R.string.logcat_connection_unobserved)
            val result = when {
                details.errors.isNotEmpty() -> getString(R.string.logcat_connection_errors, details.errors.joinToString("\n"))
                details.transferFinished -> getString(R.string.logcat_connection_transfer_finished)
                else -> missing
            }
            val report = buildList {
                details.destination?.let { add(getString(R.string.logcat_connection_destination, it)) }
                details.domain?.let { add(getString(R.string.logcat_connection_domain, it)) }
                details.app?.let { add(getString(R.string.logcat_connection_app, it)) }
                add(getString(R.string.logcat_connection_dns, details.dns.joinToString("\n").ifEmpty { missing }))
                add(getString(R.string.logcat_connection_rules, details.rules.joinToString("\n").ifEmpty { missing }))
                add(getString(R.string.logcat_connection_outbound, details.outbounds.joinToString("\n").ifEmpty { missing }))
                add(getString(R.string.logcat_connection_result, result))
                if (details.rules.isEmpty() || details.dns.isEmpty()) {
                    add(getString(R.string.logcat_connection_debug_hint))
                }
                add(getString(R.string.logcat_connection_excerpt, details.lines.joinToString("\n")))
            }.joinToString("\n\n")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.logcat_connection_title, id))
                .setMessage(report)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    private fun showSearch() {
        val input = EditText(requireContext()).apply {
            setSingleLine()
            setHint(R.string.logcat_search_hint)
            setText(searchQuery)
            selectAll()
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.logcat_search)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                searchQuery = input.text.toString().trim()
                viewLifecycleOwner.lifecycleScope.launch { reloadSession(force = true) }
            }
            .setNeutralButton(R.string.logcat_search_clear) { _, _ ->
                searchQuery = ""
                viewLifecycleOwner.lifecycleScope.launch { reloadSession(force = true) }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search_logcat -> showSearch()

            R.id.action_clear_logcat -> {
                viewLifecycleOwner.lifecycleScope.launch {
                    logAccess.withLock {
                        try {
                            withContext(Dispatchers.IO) {
                                Libcore.nekoLogClear()
                                Runtime.getRuntime().exec("/system/bin/logcat -c")
                            }
                            displayedLog = null
                            logSnapshot = ""
                            binding.textview.text = ""
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            snackbar(e.readableMessage).show()
                        }
                    }
                }

            }

            R.id.action_send_logcat -> {
                val context = requireContext()
                runOnDefaultDispatcher {
                    SendLog.sendLog(context, "NB4A")
                }
            }

            R.id.action_logcat_history -> {
                viewingHistory = true
                viewLifecycleOwner.lifecycleScope.launch { reloadSession(force = true) }
            }

            R.id.action_logcat_latest -> {
                viewingHistory = false
                viewLifecycleOwner.lifecycleScope.launch { reloadSession(force = true) }
            }
        }
        return true
    }

}
