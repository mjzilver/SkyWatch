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
    assertEquals(listOf(1), episode.episodes)
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
    assertEquals(listOf(2), episode.episodes)
  }

  @Test
  fun `prefer series folder over release prefix`() {
    val filename = "flhd-sps13e01.mkv"
    val path = "South Park/Season 13/flhd-sps13e01.mkv"

    val results = parser.parse(filename, path)

    assertEquals(1, results.size)
    val episode = assertIs<EpisodeInfo>(results.first())
    assertEquals("South Park", episode.title)
    assertEquals(13, episode.season)
    assertEquals(listOf(1), episode.episodes)
  }

  @Test
  fun `prefer series folder over extra words in filename`() {
    val filename = "untouchables-south.park.s14e01.1080p.mkv"
    val path = "South Park/Season 14/untouchables-south.park.s14e01.1080p.mkv"

    val results = parser.parse(filename, path)

    assertEquals(1, results.size)
    val episode = assertIs<EpisodeInfo>(results.first())
    assertEquals("South Park", episode.title)
    assertEquals(14, episode.season)
    assertEquals(listOf(1), episode.episodes)
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
