package phss.feelsapp.utils

import phss.ytmusicwrapper.response.models.SongItem

fun SongItem.getSongArtists(): String {
    var artists = ""
    val authorsList = authors?.filter { it.name != null } ?: listOf()

    for (author in authorsList) {
        if (authorsList.getOrNull(authorsList.indexOf(author) + 1) == null) {
            if (artists == "") artists = author.name!!
            else artists += " and ${author.name}"
            break
        }

        artists += if (artists == "") author.name!! else ", $author"
    }

    if (artists == "") artists = "Null"

    return artists
}