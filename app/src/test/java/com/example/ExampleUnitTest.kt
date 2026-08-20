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
    assertEquals(146, orotData.chapters.size)
    assertEquals(323, orotData.paragraphs.size)
    
    // Check first and last paragraphs
    val firstP = orotData.paragraphs.first()
    assertEquals("eretz_israel_1_p_1", firstP.id)
    assertEquals("eretz_israel_1", firstP.chapterId)
    assertTrue("First paragraph must have content", firstP.textContent.isNotBlank())
    assertNotNull("Paragraph letter must not be null", firstP.paragraphLetter)
  }
}
