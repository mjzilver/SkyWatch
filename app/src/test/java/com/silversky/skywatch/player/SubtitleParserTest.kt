package com.silversky.skywatch.player

import kotlin.test.Test
import kotlin.test.assertEquals

class SubtitleParserTest {

  @Test
  fun `parse valid srt cases`() {
    val cases =
        listOf(
            TestCase(
                "LF line endings",
                """
                1
                00:00:01,000 --> 00:00:04,000
                Hello World
                """
                    .trimIndent(),
                listOf(SubtitleCue(1000, 4000, "Hello World")),
            ),
            TestCase(
                "CRLF line endings",
                "1\r\n00:00:01,000 --> 00:00:04,000\r\nHello",
                listOf(SubtitleCue(1000, 4000, "Hello")),
            ),
            TestCase(
                "CR line endings",
                "1\r00:00:01,000 --> 00:00:04,000\rHello",
                listOf(SubtitleCue(1000, 4000, "Hello")),
            ),
            TestCase(
                "without cue index",
                "00:00:01,000 --> 00:00:04,000\nHello",
                listOf(SubtitleCue(1000, 4000, "Hello")),
            ),
            TestCase(
                "arbitrary cue index",
                "foo\n00:00:01,000 --> 00:00:04,000\nHello",
                listOf(SubtitleCue(1000, 4000, "Hello")),
            ),
            TestCase(
                "period milliseconds",
                "1\n00:00:01.500 --> 00:00:04.123\nHello",
                listOf(SubtitleCue(1500, 4123, "Hello")),
            ),
            TestCase(
                "variable precision milliseconds",
                "1\n00:00:01,5 --> 00:00:04,55\nHello",
                listOf(SubtitleCue(1500, 4550, "Hello")),
            ),
            TestCase(
                "no milliseconds",
                "1\n00:00:01 --> 00:00:04\nHello",
                listOf(SubtitleCue(1000, 4000, "Hello")),
            ),
            TestCase(
                "timestamp whitespace and settings",
                "1\n00:00:01,000   -->   00:00:04,000 align:center position:50%\nHello",
                listOf(SubtitleCue(1000, 4000, "Hello")),
            ),
            TestCase(
                "multiline text",
                """
                1
                00:00:01,000 --> 00:00:04,000
                Line One
                Line Two
                Line Three
                """
                    .trimIndent(),
                listOf(
                    SubtitleCue(
                        1000,
                        4000,
                        "Line One\nLine Two\nLine Three",
                    ),
                ),
            ),
            TestCase(
                "html tags",
                """
                1
                00:00:01,000 --> 00:00:04,000
                <i><b>Hello</b></i> <u>World</u>
                """
                    .trimIndent(),
                listOf(SubtitleCue(1000, 4000, "Hello World")),
            ),
            TestCase(
                "html tags across lines",
                """
                1
                00:00:01,000 --> 00:00:04,000
                <i>Hello
                World</i>
                """
                    .trimIndent(),
                listOf(SubtitleCue(1000, 4000, "Hello\nWorld")),
            ),
            TestCase(
                "multiple cues",
                """
                1
                00:00:01,000 --> 00:00:03,000
                First

                2
                00:00:04,000 --> 00:00:06,000
                Second

                3
                00:00:07,000 --> 00:00:10,000
                Third
                """
                    .trimIndent(),
                listOf(
                    SubtitleCue(1000, 3000, "First"),
                    SubtitleCue(4000, 6000, "Second"),
                    SubtitleCue(7000, 10000, "Third"),
                ),
            ),
            TestCase(
                "zero timestamp",
                "1\n00:00:00,000 --> 00:00:01,000\nHello",
                listOf(SubtitleCue(0, 1000, "Hello")),
            ),
            TestCase(
                "long duration",
                "1\n12:34:56,789 --> 23:45:01,123\nHello",
                listOf(
                    SubtitleCue(
                        45_296_789,
                        85_501_123,
                        "Hello",
                    ),
                ),
            ),
        )

    cases.forEach { case ->
      assertEquals(
          case.expected,
          SubtitleParser.parseSrt(case.content),
          "Failed test case: ${case.name}",
      )
    }
  }

  @Test
  fun `parse invalid and empty srt cases`() {
    val cases: List<TestCase> =
        listOf(
            TestCase(
                name = "LF line endings",
                content =
                    """
                    1
                    00:00:01,000 --> 00:00:04,000
                    Hello World
                    """
                        .trimIndent(),
                expected =
                    listOf(
                        SubtitleCue(1000, 4000, "Hello World"),
                    ),
            ),
            TestCase(
                name = "CRLF line endings",
                content = "1\r\n00:00:01,000 --> 00:00:04,000\r\nHello",
                expected =
                    listOf(
                        SubtitleCue(1000, 4000, "Hello"),
                    ),
            ),
        )

    cases.forEach { case ->
      assertEquals(
          case.expected,
          SubtitleParser.parseSrt(case.content),
          "Failed test case: ${case.name}",
      )
    }
  }

  private data class TestCase(
      val name: String,
      val content: String,
      val expected: List<SubtitleCue>,
  )
}
