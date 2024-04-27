package phss.feelsapp.utils

import phss.ytmusicwrapper.response.models.SongItem

fun SongItem.getSongArtists(): String {
    var artists = ""
    val authorsList = authors?.filter { it != null && it.name != null } ?: listOf()

    for (author in authorsList) {
        if (authorsList.getOrNull(authorsList.indexOf(author) + 1) == null) {
            if (artists == "") artists = author.name!!
            else artists += " and ${author.name}"
            break
        }

        artists += if (artists == "") author.name!! else ", $author"
    }

    if (artists == "") artists = "Unknown"

    return artists
}

fun SongItem.getOnlySongName(): String? {
    return info?.name
        ?.replace("(Official Video)", "", true)
        ?.replace("(Official Music Video)", "", true)
        ?.replace("(Lyric Video)", "", true)
        ?.replace("(PERFORMANCE VIDEO)", "", true)
        ?.replace("M/V", "", true)
        ?.replace("MV", "", true)
        ?.replace("music video", "", true)

}