package com.silversky.core

import com.silversky.core.model.EpisodeInfo
import com.silversky.core.parser.FilenameParser
import com.silversky.core.parser.TokenClassifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class EpisodeFilenameParserTest {

  private val parser = FilenameParser(TokenClassifier())

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
            TestCase(
                filename = "Smiling Friends S03E09 1080p AMZN WEB-DL DDP5 1 H 264 DUAL-BiOMA.mkv",
                title = "Smiling Friends",
                season = 3,
                episode = 9,
            ),
            TestCase(
                filename =
                    "Black Mirror S04E01 USS Callister 1080p NF WEB-DL DDP5 1 H 264-FLUX.mkv",
                title = "Black Mirror",
                season = 4,
                episode = 1,
                episodeName = "USS Callister",
            ),
            TestCase(
                filename =
                    "Twin.Peaks.S02E04.Laura's.Secret.Diary.1080p.10bit.BluRay.6CH.x265.HEVC-PSA.mkv",
                title = "Twin Peaks",
                season = 2,
                episode = 4,
                episodeName = "Laura's Secret Diary",
            ),
            TestCase(
                filename = "Cyberpunk - Edgerunners - S01E01 - Let You Down.mkv",
                title = "Cyberpunk Edgerunners",
                season = 1,
                episode = 1,
                episodeName = "Let You Down",
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
