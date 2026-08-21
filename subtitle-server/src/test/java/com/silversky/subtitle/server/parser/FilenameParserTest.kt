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
    )

    @Test
    fun `parse filenames`() {
        val testCases =
            listOf(
                TestCase(
                    filename = "Parasite.2019.1080p.BluRay.x264-[YTS.LT]",
                    title = "Parasite",
                    year = 2019,
                ),
                TestCase(
                    filename = "21.Jump.Street.2012.1080p.BluRay.x264.YIFY",
                    title = "21 Jump Street",
                    year = 2012,
                ),
                TestCase(
                    filename = "Cyberpunk - Edgerunners - S01E01 - Let You Down",
                    title = "Cyberpunk Edgerunners",
                    season = 1,
                    episode = 1,
                ),
                TestCase(
                    filename =
                        "Once Upon a Time in America 1984 EXTENDED REMASTERED 1080p BluRay HEVC x265 5.1 BONE",
                    title = "Once Upon a Time in America",
                    year = 1984,
                ),
                TestCase(
                    filename =
                        "Nirvanna the Band the Show the Movie 2026 1080p WEB-DL HEVC x265-RMTeam",
                    title = "Nirvanna the Band the Show the Movie",
                    year = 2026,
                ),
                TestCase(
                    filename =
                        "Smiling Friends S03E09 1080p AMZN WEB-DL DDP5 1 H 264 DUAL-BiOMA",
                    title = "Smiling Friends",
                    season = 3,
                    episode = 9,
                ),

                // Number is part of the title, not the year.
                TestCase(
                    filename = "Blade.Runner.2049.2017.1080p.BluRay",
                    title = "Blade Runner 2049",
                    year = 2017,
                ),
                TestCase(
                    filename = "2001.A.Space.Odyssey.1968.1080p.BluRay",
                    title = "2001 A Space Odyssey",
                    year = 1968,
                ),
                TestCase(
                    filename = "District.9.2009.1080p.BluRay",
                    title = "District 9",
                    year = 2009,
                ),
                TestCase(
                    filename = "Apollo.13.1995.1080p.BluRay",
                    title = "Apollo 13",
                    year = 1995,
                ),

                // Multiple plausible years: use the last one.
                TestCase(
                    filename = "1917.2019.1080p.BluRay",
                    title = "1917",
                    year = 2019,
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
        }
    }
}