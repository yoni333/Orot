package com.example

import com.example.data.repository.OrotData
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ExampleUnitTest {
  @Test
  fun testOrotDataJsonParsing() {
    val jsonFile = File("src/main/assets/orot_data.json")
    assertTrue("orot_data.json must exist", jsonFile.exists())
    val jsonString = jsonFile.readText()

    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val adapter = moshi.adapter(OrotData::class.java)
    val orotData = adapter.fromJson(jsonString)

    assertNotNull("Parsed OrotData must not be null", orotData)
    assertEquals("orot", orotData!!.book.id)
    assertEquals(28, orotData.chapters.size)
    assertEquals(343, orotData.paragraphs.size)

    val firstP = orotData.paragraphs.first()
    assertEquals("eretz_israel_p_1", firstP.id)
    assertEquals("eretz_israel", firstP.chapterId)
    assertEquals("א׳", firstP.paragraphLetter)
    assertTrue("First paragraph must have content", firstP.textContent.isNotBlank())

    // Every chapter numbers its paragraphs from alef, with no gaps or repeats.
    val chapterIds = orotData.chapters.map { it.id }
    assertEquals("Every paragraph belongs to a known chapter",
        emptyList<String>(), orotData.paragraphs.map { it.chapterId }.distinct() - chapterIds.toSet())
    orotData.paragraphs.groupBy { it.chapterId }.forEach { (chapterId, paragraphs) ->
        val ordered = paragraphs.sortedBy { it.orderIndex }
        assertEquals("$chapterId must be indexed from 0",
            ordered.indices.toList(), ordered.map { it.orderIndex })
        assertEquals("$chapterId must letter each paragraph once",
            ordered.size, ordered.map { it.paragraphLetter }.distinct().size)
        assertTrue("$chapterId must have no blank paragraphs",
            ordered.all { it.textContent.isNotBlank() })
    }
  }
}
