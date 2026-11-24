package io.nekohasekai.sagernet.group

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.GroupManager
import io.nekohasekai.sagernet.database.ProxyGroup
import io.nekohasekai.sagernet.ktx.onMainDispatcher
import io.nekohasekai.sagernet.ktx.runOnMainDispatcher
import io.nekohasekai.sagernet.ui.ThemedActivity
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GroupInterfaceAdapter(val context: ThemedActivity) : GroupManager.Interface {

    override suspend fun confirm(message: String): Boolean {
        return suspendCoroutine {
            runOnMainDispatcher {
                MaterialAlertDialogBuilder(context).setTitle(R.string.confirm)
                    .setMessage(message)
                    .setPositiveButton(R.string.yes) { _, _ -> it.resume(true) }
                    .setNegativeButton(R.string.no) { _, _ -> it.resume(false) }
                    .setOnCancelListener { _ -> it.resume(false) }
                    .show()
            }
        }
    }

    override suspend fun confirmSubscriptionRemoval(
        group: ProxyGroup,
        nodes: List<String>,
        selectedIndex: Int,
        affectedRules: List<GroupManager.AffectedRoute>,
        replacementNames: List<String>
    ): Map<Long, String>? = onMainDispatcher {
        if (context.isFinishing || context.isDestroyed) return@onMainDispatcher null
        suspendCancellableCoroutine { continuation ->
            val names = nodes.mapIndexed { index, name ->
                if (index == selectedIndex) {
                    context.getString(R.string.subscription_refresh_current_node, name)
                } else {
                    name
                }
            }.joinToString("\n")
            val replacements = mutableMapOf<Long, String>()
            var approved: Map<Long, String>? = null
            var picker: androidx.appcompat.app.AlertDialog? = null
            val builder = MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.group_diff, group.displayName()))
                .setPositiveButton(R.string.subscription_refresh_apply) { _, _ -> approved = replacements.toMap() }
                .setNegativeButton(android.R.string.cancel, null)
            val summary = context.getString(R.string.subscription_refresh_remove_preview, names)
            if (affectedRules.isEmpty()) {
                builder.setMessage(summary)
            } else {
                val list = android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    val pad = (24 * resources.displayMetrics.density).toInt()
                    setPadding(pad, pad / 2, pad, 0)
                }
                list.addView(android.widget.TextView(context).apply {
                    text = summary + "\n\n" + context.getString(R.string.subscription_route_repair_hint)
                })
                for (rule in affectedRules) {
                    val button = android.widget.Button(context, null, android.R.attr.borderlessButtonStyle)
                    button.isAllCaps = false
                    button.gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
                    fun updateLabel() {
                        button.text = listOfNotNull(rule.name, rule.oldNode, replacements[rule.id]).joinToString(" → ")
                    }
                    updateLabel()
                    button.setOnClickListener {
                        val options = listOf(context.getString(R.string.subscription_route_keep_reference)) + replacementNames
                        val checked = replacements[rule.id]?.let { replacementNames.indexOf(it) + 1 } ?: 0
                        picker = MaterialAlertDialogBuilder(context)
                            .setTitle(rule.name)
                            .setSingleChoiceItems(options.toTypedArray(), checked) { dialog, index ->
                                if (index == 0) replacements.remove(rule.id)
                                else replacements[rule.id] = replacementNames[index - 1]
                                updateLabel()
                                dialog.dismiss()
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                    }
                    list.addView(button)
                }
                builder.setView(android.widget.ScrollView(context).apply { addView(list) })
            }
            val dialog = builder.create()
            val observer = object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    dialog.dismiss()
                }
            }
            dialog.setOnDismissListener {
                picker?.dismiss()
                context.lifecycle.removeObserver(observer)
                if (continuation.isActive) continuation.resume(approved)
            }
            context.lifecycle.addObserver(observer)
            continuation.invokeOnCancellation { runOnMainDispatcher { dialog.dismiss() } }
            dialog.show()
        }
    }

    override suspend fun onUpdateSuccess(
        group: ProxyGroup,
        added: List<String>,
        updated: Map<String, String>,
        deleted: List<String>,
        duplicate: List<String>,
        byUser: Boolean,
        selectedRemoved: Boolean,
        nodeCount: Int
    ) {
        if (!byUser && !selectedRemoved && added.isEmpty() && updated.isEmpty() && deleted.isEmpty() && duplicate.isEmpty()) {
            return
        }
        onMainDispatcher {
            if (context.isFinishing || context.isDestroyed) return@onMainDispatcher
            val summary = context.getString(
                R.string.subscription_refresh_summary, group.displayName(), added.size, updated.size, deleted.size
            )
            if (!selectedRemoved && added.isEmpty() && updated.isEmpty() && deleted.isEmpty() && duplicate.isEmpty()) {
                context.snackbar(context.getString(R.string.subscription_refresh_unchanged_count, group.displayName(), nodeCount)).show()
                return@onMainDispatcher
            }

            val details = buildList {
                add(summary)
                if (selectedRemoved) add(context.getString(R.string.subscription_refresh_selected_removed))
                if (added.isNotEmpty()) add(context.getString(R.string.group_added, added.joinToString("\n")))
                if (updated.isNotEmpty()) {
                    val names = updated.entries.joinToString("\n") { (before, after) ->
                        if (before == after) before else "$before → $after"
                    }
                    add(context.getString(R.string.group_changed, names))
                }
                if (deleted.isNotEmpty()) add(context.getString(R.string.group_deleted, deleted.joinToString("\n")))
                if (duplicate.isNotEmpty()) add(context.getString(R.string.group_duplicate, duplicate.joinToString("\n")))
            }
            fun showDetails() {
                if (context.isFinishing || context.isDestroyed) return
                MaterialAlertDialogBuilder(context)
                    .setTitle(context.getString(R.string.group_diff, group.displayName()))
                    .setMessage(details.joinToString("\n\n"))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
            if (selectedRemoved || duplicate.isNotEmpty()) {
                showDetails()
            } else {
                context.snackbar(summary)
                    .setAction(R.string.subscription_refresh_details) { showDetails() }
                    .show()
            }
        }
    }

    override suspend fun onUpdateFailure(group: ProxyGroup, message: String) {
        onMainDispatcher {
            if (!context.isFinishing && !context.isDestroyed) {
                context.snackbar(context.getString(R.string.subscription_refresh_failure, group.displayName(), message)).show()
            }
        }
    }

    override suspend fun alert(message: String) {
        return suspendCoroutine {
            runOnMainDispatcher {
                MaterialAlertDialogBuilder(context).setTitle(R.string.ooc_warning)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok) { _, _ -> it.resume(Unit) }
                    .setOnCancelListener { _ -> it.resume(Unit) }
                    .show()
            }
        }
    }

}