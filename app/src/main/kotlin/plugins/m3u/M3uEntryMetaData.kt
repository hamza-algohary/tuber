package plugins.m3u

import net.bjoernpetersen.m3u.model.M3uEntry
import kotlin.text.isNotBlank


private fun M3uEntry.meta(key: String): String? =
    metadata[key]?.takeIf { it.isNotBlank() }

val M3uEntry.channelName get() = meta("tvg-name")?:title
val M3uEntry.groupTitle get() = meta("group-title")
val M3uEntry.channelId get() = meta("tvg-id")
val M3uEntry.channelNumber get() = meta("tvg-chno")?.toIntOrNull()
val M3uEntry.language get() = meta("tvg-language")
val M3uEntry.country get() = meta("tvg-country")

val M3uEntry.logoUrl get() =
    meta("tvg-logo") ?:
    meta("logo") ?:
    meta("tvg-logo-small")