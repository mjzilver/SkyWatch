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
                "without cue index",
                "00:00:01,000 --> 00:00:04,000\nHello",
                listOf(SubtitleCue(1000, 4000, "Hello")),
            ),
            TestCase(
                "variable precision milliseconds",
                "1\n00:00:01,5 --> 00:00:04,55\nHello",
                listOf(SubtitleCue(1500, 4550, "Hello")),
            ),
            TestCase(
                "html tags are preserved",
                """
                1
                00:00:01,000 --> 00:00:04,000
                <i><b>Hello</b></i> <u>World</u>
                """
                    .trimIndent(),
                listOf(SubtitleCue(1000, 4000, "<i><b>Hello</b></i> <u>World</u>")),
            ),
            TestCase(
                "complex html tags",
                """
                3
                00:00:45,964 --> 00:00:47,549
                <i><font color=Lime>♪ May I have ♪
                ♪ your attention please? ♪</i></font>
                """
                    .trimIndent(),
                listOf(
                    SubtitleCue(
                        45964,
                        47549,
                        "<i><font color=Lime>♪ May I have ♪\n♪ your attention please? ♪</i></font>",
                    ),
                ),
            ),
            TestCase(
                "font tags with attributes",
                """
                1
                00:00:02,785 --> 00:00:04,922
                <font color="#ec14bd">Sync & corrections </Font> by <font color="Skyblue" size=10 face=Times New Roman>Blue-Bird™</font>
                """
                    .trimIndent(),
                listOf(
                    SubtitleCue(
                        2785,
                        4922,
                        "<font color=\"#ec14bd\">Sync & corrections </Font> by <font color=\"Skyblue\" size=10 face=Times New Roman>Blue-Bird™</font>",
                    ),
                ),
            ),
        )

    cases.forEach { case ->
      assertEquals(
          case.expected,
          SubtitleParser.parse(case.content),
          "Failed test case: ${case.name}",
      )
    }
  }

  @Test
  fun `parse sami cases`() {
    val sami =
        """
        <SAMI>
        <BODY>
        <SYNC Start=1000><P Class=ENUSCC>Hello World
        <SYNC Start=4000><P Class=ENUSCC>&nbsp;
        <SYNC Start=5000><P Class=ENUSCC>Goodbye <i>World</i>
        <SYNC Start=8000><P Class=ENUSCC>
        </BODY>
        </SAMI>
        """
            .trimIndent()

    val expected =
        listOf(
            SubtitleCue(1000, 4000, "Hello World"),
            SubtitleCue(5000, 8000, "Goodbye <i>World</i>"),
        )

    assertEquals(expected, SubtitleParser.parse(sami))
  }

  private data class TestCase(
      val name: String,
      val content: String,
      val expected: List<SubtitleCue>,
  )
}
