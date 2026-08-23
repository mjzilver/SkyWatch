package com.silversky.subtitle.server.parser

import kotlin.test.Test
import kotlin.test.assertEquals

class FilenameParserTest {

  private val parser = FilenameParser(TokenClassifier())

  private data class TestCase(
      val filename: String,
      val title: String,
      val year: Int? = null,
      val season: Int? = null,
      val episode: Int? = null,
      val edition: String? = null,
  )

  @Test
  fun `parse filenames`() {
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
                filename = "The.Get.Out.2026.1080p.WEBRip.10Bit.DDP5.1.x265-NeoNoir.mkv",
                title = "The Get Out",
                year = 2026,
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
                filename =
                    "The Assassination of Jesse James by the Coward Robert Ford (2007) (1080p BluRay x265 10bit Tigole).mkv",
                title = "The Assassination of Jesse James by the Coward Robert Ford",
                year = 2007,
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
                filename = "Parasite.2019.1080p.BluRay.x264-[YTS.LT].mp4",
                title = "Parasite",
                year = 2019,
            ),
            TestCase(
                filename = "Donnie.Brasco.1997.1080p.BluRay.x264.YIFY.mp4",
                title = "Donnie Brasco",
                year = 1997,
            ),
            TestCase(
                filename = "Once.Upon.A.Time.....In.Hollywood.2019.1080p.BluRay.x264-[YTS.LT].mp4",
                title = "Once Upon A Time In Hollywood",
                year = 2019,
            ),
            TestCase(
                filename = "Blow.2001.1080p.BrRip.x264.BOKUTOX.YIFY.mp4",
                title = "Blow",
                year = 2001,
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
                filename = "Unforgiven.1992.1080p.BrRip.x264.YIFY.mp4",
                title = "Unforgiven",
                year = 1992,
            ),
            TestCase(
                filename = "Smiling Friends S03E09 1080p AMZN WEB-DL DDP5 1 H 264 DUAL-BiOMA.mkv",
                title = "Smiling Friends",
                season = 3,
                episode = 9,
            ),
            TestCase(
                filename = "They.Live.1988.REMASTERED.1080p.BluRay.H264.AAC-RARBG.mp4",
                title = "They Live",
                year = 1988,
                edition = "remastered",
            ),
            TestCase(
                filename = "Strange.Days.1995.1080p.BluRay.DDP5.1.x265.10bit-GalaxyRG265.mkv",
                title = "Strange Days",
                year = 1995,
            ),
            TestCase(
                filename = "Backrooms.2026.1080p.WEBRip.x264.AAC5.1-[YTS.GG - YTS.BZ].mp4",
                title = "Backrooms",
                year = 2026,
            ),
            TestCase(
                filename = "The.Invisible.Man.2020.1080p.BluRay.x264.AAC5.1-[YTS.MX].mp4",
                title = "The Invisible Man",
                year = 2020,
            ),
            TestCase(
                filename = "In.the.Mouth.of.Madness.1994.1080p.BluRay.x264.YIFY.mp4",
                title = "In the Mouth of Madness",
                year = 1994,
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
            TestCase(
                filename =
                    "Black Mirror S04E01 USS Callister 1080p NF WEB-DL DDP5 1 H 264-FLUX.mkv",
                title = "Black Mirror",
                season = 4,
                episode = 1,
            ),
            TestCase(
                filename =
                    "Twin.Peaks.S02E04.Laura's.Secret.Diary.1080p.10bit.BluRay.6CH.x265.HEVC-PSA.mkv",
                title = "Twin Peaks",
                season = 2,
                episode = 4,
            ),
            TestCase(
                filename = "Cyberpunk - Edgerunners - S01E01 - Let You Down.mkv",
                title = "Cyberpunk Edgerunners",
                season = 1,
                episode = 1,
            ),
        )

    for (case in testCases) {
      val result = parser.parse(case.filename)

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
          case.season,
          result.season,
          "Season mismatch for: ${case.filename}",
      )
      assertEquals(
          case.episode,
          result.episode,
          "Episode mismatch for: ${case.filename}",
      )
      assertEquals(
          case.edition,
          result.edition,
          "Edition mismatch for: ${case.filename}",
      )
    }
  }
}
