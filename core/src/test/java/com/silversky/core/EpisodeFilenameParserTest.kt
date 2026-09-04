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
      val episode: Int,
      val episodeName: String? = null,
      val year: Int? = null,
      val edition: String? = null,
  )

  @Test
  fun `parse episode filenames`() {
    val testCases =
        listOf(
            // S0XE0X
            TestCase(
                filename = "Smiling Friends S03E09 1080p AMZN WEB-DL.mkv",
                title = "Smiling Friends",
                season = 3,
                episode = 9,
            ),
            TestCase(
                filename = "Cyberpunk - Edgerunners - S01E01 - Let You Down.mkv",
                title = "Cyberpunk Edgerunners",
                season = 1,
                episode = 1,
                episodeName = "Let You Down",
            ),

            // 1x01 notation
            TestCase(
                filename = "Doctor Who 2006 2x03 School Reunion XviD [MM].avi",
                title = "Doctor Who",
                year = 2006,
                season = 2,
                episode = 3,
                episodeName = "School Reunion",
            ),
            TestCase(
                filename = "The.Office.1x01.Pilot.720p.WEB-DL.mkv",
                title = "The Office",
                season = 1,
                episode = 1,
                episodeName = "Pilot",
            ),
            TestCase(
                filename = "Lost.01x04.Walkabout.1080p.BluRay.mkv",
                title = "Lost",
                season = 1,
                episode = 4,
                episodeName = "Walkabout",
            ),

            // Unpadded S1E1
            TestCase(
                filename = "Breaking Bad S1E1 Pilot 720p.mkv",
                title = "Breaking Bad",
                season = 1,
                episode = 1,
                episodeName = "Pilot",
            ),

            // Separated S01.E01 / S01 E01
            TestCase(
                filename = "Futurama S01.E02 The Series Has Landed.mkv",
                title = "Futurama",
                season = 1,
                episode = 2,
                episodeName = "The Series Has Landed",
            ),
            TestCase(
                filename = "Archer S03 E04 The Limited.mkv",
                title = "Archer",
                season = 3,
                episode = 4,
                episodeName = "The Limited",
            ),

            // Explicit season/episode
            TestCase(
                filename = "Game of Thrones Season 01 Episode 01 Winter Is Coming.mkv",
                title = "Game of Thrones",
                season = 1,
                episode = 1,
                episodeName = "Winter Is Coming",
            ),
            TestCase(
                filename = "The Simpsons Season 5 Episode 3 Homer Goes to College.mkv",
                title = "The Simpsons",
                season = 5,
                episode = 3,
                episodeName = "Homer Goes to College",
            ),

            // Noise edge cases
            TestCase(
                filename = "Fictional Show S01E02 Love in Stereo 1080p WEB-DL.mkv",
                title = "Fictional Show",
                season = 1,
                episode = 2,
                episodeName = "Love in Stereo",
            ),
            TestCase(
                filename = "The Web S01E02 Dubbed Mono 1080p WEB-DL.mkv",
                title = "The Web",
                season = 1,
                episode = 2,
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
          case.episode,
          episode.episode,
          "Episode mismatch for: ${case.filename}",
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
  fun `parse multi episode filenames`() {
    val filename = "Mr.Robot.S02E01E02.1080p.BluRay.10bit.DD5.1.x265-POIASD.mkv"
    val results = parser.parse(filename)

    assertEquals(2, results.size, "Expected two results for multi-episode file")

    val ep1 = assertIs<EpisodeInfo>(results[0])
    assertEquals("Mr Robot", ep1.title)
    assertEquals(2, ep1.season)
    assertEquals(1, ep1.episode)

    val ep2 = assertIs<EpisodeInfo>(results[1])
    assertEquals("Mr Robot", ep2.title)
    assertEquals(2, ep2.season)
    assertEquals(2, ep2.episode)
  }
}
