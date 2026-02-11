package plugins.podcastindex

import plugins.Category
import plugins.FormattedText
import plugins.FormattedText.Plain
import plugins.Items
import plugins.PlaylistType
import plugins.SearchResult
import plugins.Summary
import plugins.Thumbnail
import plugins.toPlainText
import java.sql.ResultSet

fun String?.asThumbnailUrl() =
    this?.let { Thumbnail(url = this, width = null, height = null) }

fun String?.asPlainText() =
    this?.let {
        Plain(FormattedText.HTML(this).toPlainText())
    }


fun ResultSet.toSummary() : Summary =
    Summary.PlaylistSummary(
        name = getString("title"),
        url = getString("url"),
        thumbnails = listOfNotNull(getString("imageUrl").asThumbnailUrl()),
        service = pluginName,
        categories = listOf(Category.PODCAST),
        related = emptyList(),
        description = FormattedText.HTML(getString("description")),
        uploader = null,
        numberOfItems = null,
        playlistType = PlaylistType.NORMAL,
    )


fun Items.toSearchResult() = SearchResult(this,null,null)
