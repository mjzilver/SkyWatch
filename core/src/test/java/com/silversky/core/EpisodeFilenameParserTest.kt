package com.silversky.core

import com.silversky.core.model.EpisodeInfo
import com.silversky.core.parser.FilenameParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class EpisodeFilenameParserTest {

  private val parser = FilenameParser()

  private data class TestCase(
      val filename: String,
      val title: String,
      val season: Int,
      val episodes: List<Int>,
      val episodeName: String? = null,
      val year: Int? = null,
      val edition: String? = null,
  )

  @Test
  fun `parse episode filenames`() {
    val testCases =
        listOf(
            TestCase(
                filename = "Smiling Friends S03E09 1080p AMZN WEB-DL.mkv",
                title = "Smiling Friends",
                season = 3,
                episodes = listOf(9),
            ),
            TestCase(
                filename = "Cyberpunk - Edgerunners - S01E01 - Let You Down.mkv",
                title = "Cyberpunk Edgerunners",
                season = 1,
                episodes = listOf(1),
                episodeName = "Let You Down",
            ),

            // 1x01 notation
            TestCase(
                filename = "Doctor Who 2006 2x03 School Reunion XviD [MM].avi",
                title = "Doctor Who",
                year = 2006,
                season = 2,
                episodes = listOf(3),
                episodeName = "School Reunion",
            ),
            TestCase(
                filename = "The.Office.1x01.Pilot.720p.WEB-DL.mkv",
                title = "The Office",
                season = 1,
                episodes = listOf(1),
                episodeName = "Pilot",
            ),
            TestCase(
                filename = "Lost.01x04.Walkabout.1080p.BluRay.mkv",
                title = "Lost",
                season = 1,
                episodes = listOf(4),
                episodeName = "Walkabout",
            ),

            // Unpadded S1E1
            TestCase(
                filename = "Breaking Bad S1E1 Pilot 720p.mkv",
                title = "Breaking Bad",
                season = 1,
                episodes = listOf(1),
                episodeName = "Pilot",
            ),

            // Separated S01.E01 / S01 E01
            TestCase(
                filename = "Futurama S01.E02 The Series Has Landed.mkv",
                title = "Futurama",
                season = 1,
                episodes = listOf(2),
                episodeName = "The Series Has Landed",
            ),
            TestCase(
                filename = "Archer S03 E04 The Limited.mkv",
                title = "Archer",
                season = 3,
                episodes = listOf(4),
                episodeName = "The Limited",
            ),

            // Explicit season/episode
            TestCase(
                filename = "Game of Thrones Season 01 Episode 01 Winter Is Coming.mkv",
                title = "Game of Thrones",
                season = 1,
                episodes = listOf(1),
                episodeName = "Winter Is Coming",
            ),
            TestCase(
                filename = "The Simpsons Season 5 Episode 3 Homer Goes to College.mkv",
                title = "The Simpsons",
                season = 5,
                episodes = listOf(3),
                episodeName = "Homer Goes to College",
            ),

            // Noise edge cases
            TestCase(
                filename = "www.UIndex.org.Black.Mirror.S04E01.mkv",
                title = "Black Mirror",
                season = 4,
                episodes = listOf(1),
            ),
            TestCase(
                filename = "Black Mirror S04E01.mkv",
                title = "Black Mirror",
                season = 4,
                episodes = listOf(1),
            ),
            TestCase(
                filename =
                    "www.UIndex.org - Black Mirror S04E01 USS Callister 1080p NF WEB-DL DDP5 1 H 264-FLUX.mkv",
                title = "Black Mirror",
                season = 4,
                episodes = listOf(1),
                episodeName = "USS Callister",
            ),
            TestCase(
                filename = "Fictional Show S01E02 Love in Stereo 1080p WEB-DL.mkv",
                title = "Fictional Show",
                season = 1,
                episodes = listOf(2),
                episodeName = "Love in Stereo",
            ),
            TestCase(
                filename = "The Web S01E02 Dubbed Mono 1080p WEB-DL.mkv",
                title = "The Web",
                season = 1,
                episodes = listOf(2),
                episodeName = "Dubbed Mono",
            ),
        )

    for (case in testCases) {
      val results = parser.parse(case.filename)

      assertEquals(1, results.size, "Expected exactly one result for: ${case.filename}")
      val result = results.first()

      val episode =
          assertIs<EpisodeInfo>(
              result,
              "Expected EpisodeInfo for: ${case.filename}",
          )

      assertEquals(
          case.title,
          episode.title,
          "Title mismatch for: ${case.filename}",
      )
      assertEquals(
          case.year,
          episode.year,
          "Year mismatch for: ${case.filename}",
      )
      assertEquals(
          case.season,
          episode.season,
          "Season mismatch for: ${case.filename}",
      )
      assertEquals(
          case.episodes,
          episode.episodes,
          "Episodes mismatch for: ${case.filename}",
      )
      assertEquals(
          case.episodeName,
          episode.episodeName,
          "Episode name mismatch for: ${case.filename}",
      )
      assertEquals(
          case.edition,
          episode.edition,
          "Edition mismatch for: ${case.filename}",
      )
    }
  }

  @Test
  fun `handle nested structure`() {
    val grandparent = "Smiling Friends"
    val parent = "www.UIndex.org - Smiling Friends S03E09 1080p AMZN WEB-DL DDP5 1 H 264 DUAL-BiOMA"
    val filename = "Smiling Friends S03E09 1080p AMZN WEB-DL DDP5 1 H 264 DUAL-BiOMA.mkv"
    val path = "$grandparent/$parent/$filename"

    val results = parser.parse(filename, path)
    assertEquals(1, results.size)
    val episode = assertIs<EpisodeInfo>(results.first())
    assertEquals("Smiling Friends", episode.title)
    assertEquals(3, episode.season)
    assertEquals(listOf(9), episode.episodes)
  }

  @Test
  fun `parse multi episode filenames with repeating markers`() {
    val filename = "Mr.Robot.S02E01E02.1080p.BluRay.10bit.DD5.1.x265-POIASD.mkv"
    val expectedEpisodes = listOf(1, 2)

    val results = parser.parse(filename)

    assertEquals(1, results.size)
    val episode = assertIs<EpisodeInfo>(results.first())
    assertEquals("Mr Robot", episode.title)
    assertEquals(2, episode.season)
    assertEquals(expectedEpisodes, episode.episodes)
  }

  @Test
  fun `clean noisy titles and prefer path`() {
    val messyFolder = "Altered Carbon S01 COMPLETE 720p NF WEBRip x264 GalaxyTV[TGx]"
    val filename = "S01E01.mkv"
    val path = "$messyFolder/$filename"

    // When parsing with path, it should clean the folder name used as title
    val results = parser.parse(filename, path)
    assertEquals(1, results.size)
    val episode = assertIs<EpisodeInfo>(results.first())
    assertEquals("Altered Carbon", episode.title)
    assertEquals(1, episode.season)
  }

  @Test
  fun `extract title from common words in path and filename`() {
    val filename = "untouchables-south.park.s14e01.1080p.mkv"
    val path = "South Park/Season 14/$filename"

    val results = parser.parse(filename, path)
    assertEquals(1, results.size)
    val episode = assertIs<EpisodeInfo>(results.first())
    assertEquals("South Park", episode.title)
  }

  @Test
  fun `parse multi episode filenames with special delimiters`() {
    val testCases =
        listOf(
            "Avatar - The Last Airbender - S02E12&E13 - The Serpent's Pass.mkv" to listOf(12, 13),
            "Avatar - The Last Airbender - S03E14-E15 - The Boiling Rock.mkv" to listOf(14, 15),
        )

    for ((filename, expectedEpisodes) in testCases) {
      val results = parser.parse(filename)

      assertEquals(
          1,
          results.size,
          "Expected exactly one result for: $filename",
      )

      val episode =
          assertIs<EpisodeInfo>(
              results.first(),
              "Expected EpisodeInfo for: $filename",
          )

      assertEquals("Avatar The Last Airbender", episode.title)
      val expectedSeason = if (filename.contains("S02")) 2 else 3
      assertEquals(expectedSeason, episode.season)
      assertEquals(expectedEpisodes, episode.episodes)
    }
  }
}
