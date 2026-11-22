package io.nekohasekai.sagernet.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.text.util.Linkify
import android.view.View
import androidx.activity.result.component1
import androidx.activity.result.component2
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import java.util.concurrent.TimeUnit
import androidx.recyclerview.widget.RecyclerView
import com.danielstone.materialaboutlibrary.MaterialAboutFragment
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.danielstone.materialaboutlibrary.model.MaterialAboutList
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.databinding.LayoutAboutBinding
import io.nekohasekai.sagernet.ktx.*
import io.nekohasekai.sagernet.plugin.PluginManager.loadString
import io.nekohasekai.sagernet.utils.PackageCache
import io.nekohasekai.sagernet.widget.ListListener
import libcore.Libcore
import moe.matsuri.nb4a.plugin.Plugins
import androidx.core.net.toUri
import io.nekohasekai.sagernet.SagerNet

class AboutFragment : ToolbarFragment(R.layout.layout_about) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = LayoutAboutBinding.bind(view)

        ViewCompat.setOnApplyWindowInsetsListener(view, ListListener)
        toolbar.setTitle(R.string.menu_about)

        parentFragmentManager.beginTransaction()
            .replace(R.id.about_fragment_holder, AboutContent())
            .commitAllowingStateLoss()

        runOnDefaultDispatcher {
            val license = view.context.assets.open("LICENSE").bufferedReader().readText()
            onMainDispatcher {
                binding.license.text = license
                Linkify.addLinks(binding.license, Linkify.EMAIL_ADDRESSES or Linkify.WEB_URLS)
            }
        }
    }

    class AboutContent : MaterialAboutFragment() {

        private var updateCheck: Job? = null

        private fun installedBuild(context: Context): Long = PackageInfoCompat.getLongVersionCode(
            context.packageManager.getPackageInfo(context.packageName, 0)
        )

        private fun checkForUpdates() {
            if (updateCheck?.isActive == true) return
            val context = requireContext()
            val preferences = context.getSharedPreferences("github_updates", Context.MODE_PRIVATE)
            var beta = preferences.getBoolean("beta", false)
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.github_update_channel)
                .setSingleChoiceItems(
                    arrayOf(getString(R.string.github_update_stable), getString(R.string.github_update_beta)),
                    if (beta) 1 else 0
                ) { _, index ->
                    beta = index == 1
                }
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.github_update_check) { _, _ ->
                    preferences.edit().putBoolean("beta", beta).apply()
                    fetchUpdate(beta)
                }
                .show()
        }

        private fun fetchUpdate(beta: Boolean) {
            val context = requireContext()
            val installed = installedBuild(context)
            val installedVersion = SagerNet.appVersionNameForDisplay
            val progress = MaterialAlertDialogBuilder(context)
                .setMessage(R.string.github_update_checking)
                .create()
            progress.setOnCancelListener { updateCheck?.cancel() }
            progress.show()

            updateCheck = viewLifecycleOwner.lifecycleScope.launch {
                val dialog = MaterialAlertDialogBuilder(context).setTitle(R.string.github_update_check)
                try {
                    val release = withContext(Dispatchers.IO) { latestRelease(beta) }
                    if (release == null) {
                        dialog.setMessage(if (beta) R.string.github_update_no_release else R.string.github_update_no_stable)
                    } else if (!ReleaseUpdate.isUpgrade(release.code, installed)) {
                        dialog.setMessage(getString(R.string.github_update_current, installedVersion, release.versionName))
                    } else {
                        dialog.setMessage(getString(R.string.github_update_available, release.versionName, installedVersion, release.notes))
                        if ("arm64-v8a" in Build.SUPPORTED_ABIS) {
                            dialog.setPositiveButton(R.string.github_update_download) { _, _ ->
                                downloadAndInstall(release)
                            }
                        } else {
                            dialog.setMessage(getString(R.string.github_update_unsupported, release.versionName))
                        }
                        dialog.setNeutralButton(R.string.github_update_release_page) { _, _ ->
                            context.launchCustomTab("$PROJECT_URL/releases/tag/${release.tag}")
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    dialog.setMessage(R.string.github_update_failed)
                } finally {
                    progress.dismiss()
                }
                dialog.setNegativeButton(android.R.string.ok, null).show()
            }
        }

        private fun downloadAndInstall(release: Release) {
            if (updateCheck?.isActive == true) return
            val context = requireContext()
            val progress = MaterialAlertDialogBuilder(context)
                .setMessage(R.string.github_update_downloading)
                .setNegativeButton(android.R.string.cancel) { _, _ -> updateCheck?.cancel() }
                .create()
            progress.setOnCancelListener { updateCheck?.cancel() }
            progress.show()

            val main = Handler(Looper.getMainLooper())
            updateCheck = viewLifecycleOwner.lifecycleScope.launch {
                var openingInstaller = false
                val failure = try {
                    val url = PROJECT_URL.toHttpUrl().newBuilder()
                        .addPathSegments("releases/download")
                        .addPathSegment(release.tag)
                        .addPathSegment(release.apkName)
                        .build()
                    val apk = UpdateApk.download(
                        context.applicationContext,
                        url,
                        release.code,
                        onProgress = { percent ->
                            main.post {
                                if (progress.isShowing) {
                                    progress.setMessage(getString(R.string.github_update_download_progress, percent))
                                }
                            }
                        },
                        onVerifying = {
                            main.post {
                                if (progress.isShowing) {
                                    progress.setMessage(getString(R.string.github_update_verifying))
                                }
                            }
                        }
                    )
                    ensureActive()
                    openingInstaller = true
                    UpdateApk.install(context, apk)
                    null
                } catch (e: CancellationException) {
                    throw e
                } catch (_: UpdateApk.VerificationException) {
                    R.string.github_update_verify_failed
                } catch (_: Exception) {
                    if (openingInstaller) R.string.github_update_install_failed
                    else R.string.github_update_download_failed
                } finally {
                    progress.dismiss()
                }
                // Interrupted network reads can surface as I/O errors after the user cancels.
                ensureActive()
                if (failure != null) {
                    MaterialAlertDialogBuilder(context)
                        .setMessage(failure)
                        .setNeutralButton(R.string.github_update_release_page) { _, _ ->
                            context.launchCustomTab("$PROJECT_URL/releases/tag/${release.tag}")
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.github_update_retry) { _, _ ->
                            downloadAndInstall(release)
                        }
                        .show()
                }
            }
        }

        private data class Release(
            val tag: String,
            val versionName: String,
            val code: Long,
            val notes: String,
            val apkName: String,
        )

        private fun latestRelease(beta: Boolean): Release? {
            var latest: Release? = null
            var page = 1
            do {
                val request = Request.Builder()
                    .url("$RELEASES_API?per_page=100&page=$page")
                    .header("Accept", "application/vnd.github+json")
                    .build()
                val releases = updateClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("GitHub HTTP ${response.code}")
                    JSONArray(response.body?.string() ?: throw IOException("Empty GitHub response"))
                }
                for (index in 0 until releases.length()) {
                    val item = releases.getJSONObject(index)
                    val tag = item.optString("tag_name")
                    val candidate = ReleaseUpdate.candidate(
                        tag,
                        item.optBoolean("draft"),
                        item.optBoolean("prerelease"),
                        beta,
                        latest?.code ?: 0
                    ) {
                        val assets = item.getJSONArray("assets")
                        (0 until assets.length())
                            .map(assets::getJSONObject)
                            .filter { it.optString("state") == "uploaded" }
                            .map { it.optString("name") }
                    } ?: continue
                    val versionName = tag.removePrefix("v").substringBeforeLast('.').uppercase()
                    latest = Release(tag, versionName, candidate.code, item.optString("body").take(4000), candidate.apkName)
                }
                page++
            } while (releases.length() == 100)
            return latest
        }

        private companion object {
            const val PROJECT_URL = "https://github.com/LauritzWu/Veyr"
            const val RELEASES_API = "https://api.github.com/repos/LauritzWu/Veyr/releases"
            val updateClient = OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build()
        }

        val requestIgnoreBatteryOptimizations = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { (resultCode, _) ->
            if (resultCode == Activity.RESULT_OK) {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.about_fragment_holder, AboutContent())
                    .commitAllowingStateLoss()
            }
        }

        override fun getMaterialAboutList(activityContext: Context): MaterialAboutList {
            return MaterialAboutList.Builder()
                .addCard(
                    MaterialAboutCard.Builder()
                        .outline(true)
                        .addItem(
                            MaterialAboutActionItem.Builder()
                                .icon(R.drawable.ic_baseline_update_24)
                                .text(R.string.app_version)
                                .subText(SagerNet.appVersionNameForDisplay)
                                .build())
                        .addItem(
                            MaterialAboutActionItem.Builder()
                                .text(R.string.github_update_check)
                                .setOnClickAction { checkForUpdates() }
                                .build())
                        .addItem(
                            MaterialAboutActionItem.Builder()
                                .icon(R.drawable.ic_baseline_layers_24)
                                .text(getString(R.string.version_x, "sing-box"))
                                .subText(Libcore.versionBox())
                                .setOnClickAction { }
                                .build())
                        .apply {
                            PackageCache.awaitLoadSync()
                            for ((_, pkg) in PackageCache.installedPluginPackages) {
                                try {
                                    val pluginId =
                                        pkg.providers?.get(0)?.loadString(Plugins.METADATA_KEY_ID)
                                    if (pluginId.isNullOrBlank()) continue
                                    addItem(
                                        MaterialAboutActionItem.Builder()
                                            .icon(R.drawable.ic_baseline_nfc_24)
                                            .text(
                                                getString(
                                                    R.string.version_x,
                                                    pluginId
                                                ) + " (${Plugins.displayExeProvider(pkg.packageName)})"
                                            )
                                            .subText("v" + pkg.versionName)
                                            .setOnClickAction {
                                                startActivity(Intent().apply {
                                                    action =
                                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                                    data = Uri.fromParts(
                                                        "package", pkg.packageName, null
                                                    )
                                                })
                                            }
                                            .build())
                                } catch (e: Exception) {
                                    Logs.w(e)
                                }
                            }
                        }
                        .apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val pm = app.getSystemService(Context.POWER_SERVICE) as PowerManager
                                if (!pm.isIgnoringBatteryOptimizations(app.packageName)) {
                                    addItem(
                                        MaterialAboutActionItem.Builder()
                                            .icon(R.drawable.ic_baseline_running_with_errors_24)
                                            .text(R.string.ignore_battery_optimizations)
                                            .subText(R.string.ignore_battery_optimizations_sum)
                                            .setOnClickAction {
                                                requestIgnoreBatteryOptimizations.launch(
                                                    Intent(
                                                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                                        "package:${app.packageName}".toUri()
                                                    )
                                                )
                                            }
                                            .build())
                                }
                            }
                        }
                        .build())
                .addCard(
                    MaterialAboutCard.Builder()
                        .outline(true)
                        .title(R.string.project)
                        .addItem(
                            MaterialAboutActionItem.Builder()
                                .icon(R.drawable.ic_baseline_sanitizer_24)
                                .text(R.string.github)
                                .setOnClickAction {
                                    requireContext().launchCustomTab(PROJECT_URL)
                                }
                                .build())
                        .build())
                .build()

        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            view.findViewById<RecyclerView>(R.id.mal_recyclerview).apply {
                overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            }
        }
    }
}
