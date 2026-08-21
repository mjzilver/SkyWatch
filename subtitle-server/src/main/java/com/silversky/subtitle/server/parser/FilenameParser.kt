package com.silversky.subtitle.server.parser

import com.silversky.subtitle.server.model.MediaInfo

class FilenameParser(
    private val classifier: TokenClassifier,
) {
    fun parse(filename: String): MediaInfo {
        val name = filename.substringBeforeLast('.', filename)

        val tokens = name
            .split(".", " ", "_")
            .filter { it.isNotBlank() }
            .map(classifier::classify)

        val seasonEpisode =
            tokens.filterIsInstance<Token.SeasonEpisode>().firstOrNull()

        val year =
            tokens.filterIsInstance<Token.Year>().firstOrNull()

        val titleTokens =
            tokens
                .takeWhile { it is Token.Text }
                .filterIsInstance<Token.Text>()

        return MediaInfo(
            title = titleTokens.joinToString(" ") { it.value },
            year = year?.value,
            season = seasonEpisode?.season,
            episode = seasonEpisode?.episode,
        )
    }
}