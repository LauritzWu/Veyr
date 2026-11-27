package io.nekohasekai.sagernet.ui

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.group.ClashImportPreview
import io.nekohasekai.sagernet.ktx.onMainDispatcher
import io.nekohasekai.sagernet.ktx.runOnMainDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal suspend fun Context.confirmClashImport(preview: ClashImportPreview, importCount: Int): Boolean {
    val message = buildList {
        add(getString(R.string.clash_import_count, importCount))
        if (preview.unsupported > 0) add(getString(R.string.clash_import_skipped, preview.unsupported))
        val excluded = preview.supported - importCount
        if (excluded > 0) add(getString(R.string.clash_import_filtered, excluded))
        add(getString(R.string.clash_import_nodes_only))
    }.joinToString("\n\n")
    return awaitImportConfirmation(R.string.clash_import_title, message, R.string.clash_import_apply)
}

internal suspend fun Context.awaitImportConfirmation(title: Int, message: String, action: Int): Boolean =
    onMainDispatcher {
        suspendCancellableCoroutine { continuation ->
            var accepted = false
            val dialog = MaterialAlertDialogBuilder(this@awaitImportConfirmation)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(action) { _, _ -> accepted = true }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
            dialog.setOnDismissListener {
                if (continuation.isActive) continuation.resume(accepted)
            }
            continuation.invokeOnCancellation { runOnMainDispatcher { dialog.dismiss() } }
            if (continuation.isActive) dialog.show()
        }
    }
