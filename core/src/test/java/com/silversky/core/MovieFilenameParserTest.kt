package com.silversky.core

import com.silversky.core.parser.FilenameParser
import com.silversky.core.parser.TokenClassifier
import kotlin.test.Test
import kotlin.test.assertEquals

class MovieFilenameParserTest {

  private val parser = FilenameParser(TokenClassifier())

  private data class TestCase(
      val filename: String,
      val title: String,
      val year: Int? = null,
      val edition: String? = null,
  )

  @Test
  fun `parse movie filenames`() {
    val testCases =
        listOf(
            TestCase(
                filename = "Casino 1995 Remastered 1080p BluRay HEVC x265 5.1 BONE.mkv",
                title = "Casino",
                year = 1995,
                edition = "remastered",
            ),
            TestCase(
                filename =
                    "Once Upon a Time in America 1984 EXTENDED REMASTERED 1080p BluRay HEVC x265 5.1 BONE.mkv",
                title = "Once Upon a Time in America",
                year = 1984,
                edition = "extended",
            ),
            TestCase(
                filename = "One-Eyed Jacks 1961 Criterion 1080p BluRay HEVC x265 BONE.mkv",
                title = "One Eyed Jacks",
                year = 1961,
                edition = "criterion",
            ),
            TestCase(
                filename = "Taxi.Driver.1976.REMASTERED.1080p.BluRay.6CH.2.5GB.MkvCage.mkv",
                title = "Taxi Driver",
                year = 1976,
                edition = "remastered",
            ),
            TestCase(
                filename = "The.Dark.Crystal.1982.REMASTERED.1080p.BluRay.H264.AAC-RBG.mp4",
                title = "The Dark Crystal",
                year = 1982,
                edition = "remastered",
            ),
            TestCase(
                filename = "21.Jump.Street.2012.1080p.BluRay.x264.YIFY.mp4",
                title = "21 Jump Street",
                year = 2012,
            ),
            TestCase(
                filename =
                    "Apocalypse.Now.1979.FINAL.CUT.720p.BluRay.999MB.HQ.x265.10bit-GalaxyRG.mkv",
                title = "Apocalypse Now",
                year = 1979,
                edition = "final",
            ),
            TestCase(
                filename = "Falling.Down.1993.1080p.BluRay.x264.YIFY.mp4",
                title = "Falling Down",
                year = 1993,
            ),
            TestCase(
                filename = "22.Jump.Street.2014.1080p.BluRay.x264.YIFY.mp4",
                title = "22 Jump Street",
                year = 2014,
            ),
            TestCase(
                filename = "Flight.2012.1080p.BrRipx264.YIFY.mp4",
                title = "Flight",
                year = 2012,
            ),
            TestCase(
                filename = "Once.Upon.A.Time.....In.Hollywood.2019.1080p.BluRay.x264-[YTS.LT].mp4",
                title = "Once Upon A Time In Hollywood",
                year = 2019,
            ),
            TestCase(
                filename = "Escape.From.New.York.1981.1080p.BrRip.x264.BOKUTOX.YIFY.mp4",
                title = "Escape From New York",
                year = 1981,
            ),
            TestCase(
                filename = "Obsession.2026.1080p.AMZN.WEB-DL[Ben The Men].mp4",
                title = "Obsession",
                year = 2026,
            ),
            TestCase(
                filename =
                    "Prince Of Darkness - John Carpenter Horror 1987 Eng Subs 1080p [H264-mp4].mp4",
                title = "Prince Of Darkness John Carpenter Horror",
                year = 1987,
            ),
            TestCase(
                filename = "12.Monkeys.1995.1080p.BluRay.x264.AAC5.1.mp4",
                title = "12 Monkeys",
                year = 1995,
            ),
        )

    for (case in testCases) {
      val results = parser.parse(case.filename)

      assertEquals(1, results.size, "Expected exactly one result for: ${case.filename}")
      val result = results.first()

      assertEquals(
          case.title,
          result.title,
          "Title mismatch for: ${case.filename}",
      )
      assertEquals(
          case.year,
          result.year,
          "Year mismatch for: ${case.filename}",
      )
      assertEquals(
          case.edition,
          result.edition,
          "Edition mismatch for: ${case.filename}",
      )
    }
  }
}
