package io.nekohasekai.sagernet.group

internal data class ClashImportPreview(val declared: Int, val supported: Int) {
    val unsupported: Int get() = declared - supported
}
