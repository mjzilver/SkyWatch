package com.silversky.core

import com.silversky.core.model.EpisodeInfo
import com.silversky.core.parser.FilenameParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FilenameParserPathTest {

  private val parser = FilenameParser()

  @Test
  fun `extract title from parent folder when filename title is missing`() {
    val filename = "S1 e1.mkv"
    val path = "Alien Earth/S1 e1.mkv"

    val results = parser.parse(filename, path)

    assertEquals(1, results.size)
    val episode = assertIs<EpisodeInfo>(results.first())
    assertEquals("Alien Earth", episode.title)
    assertEquals(1, episode.season)
    assertEquals(1, episode.episode)
  }

  @Test
  fun `extract title from grandparent folder when parent is season folder`() {
    val filename = "S01E02.mkv"
    val path = "Breaking Bad/Season 1/S01E02.mkv"

    val results = parser.parse(filename, path)

    assertEquals(1, results.size)
    val episode = assertIs<EpisodeInfo>(results.first())
    assertEquals("Breaking Bad", episode.title)
    assertEquals(1, episode.season)
    assertEquals(2, episode.episode)
  }

  @Test
  fun `ignore files in noise folders`() {
    val noisePaths =
        listOf(
            "Movies/Inception/Extras/Behind the scenes.mkv",
            "TV/The Boys/Trailers/Season 4 Teaser.mp4",
            "Some/Path/featurettes/making_of.mp4",
        )

    for (path in noisePaths) {
      val filename = path.substringAfterLast('/')
      val results = parser.parse(filename, path)
      assertTrue(results.isEmpty(), "Should be empty for noise path: $path")
    }
  }

  @Test
  fun `ignore files with noise filenames`() {
    val noiseFilenames =
        listOf(
            "sample.mkv",
            "trailer.mp4",
        )

    for (filename in noiseFilenames) {
      val path = "Some/Path/$filename"
      val results = parser.parse(filename, path)
      assertTrue(results.isEmpty(), "Should be empty for noise filename: $filename")
    }
  }
}
