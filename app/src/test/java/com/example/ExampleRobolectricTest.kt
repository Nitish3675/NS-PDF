package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.BookmarkEntity
import com.example.data.PdfNoteEntity
import com.example.data.RecentPdfEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NS PDF", appName)
  }

  @Test
  fun `verify data entity models`() {
    val recent = RecentPdfEntity(
      filePath = "/sample/test.pdf",
      fileName = "test.pdf",
      pageCount = 5,
      fileSizeBytes = 1024L,
      lastOpenedTimestamp = System.currentTimeMillis(),
      lastPage = 2
    )
    assertEquals("test.pdf", recent.fileName)
    assertEquals(5, recent.pageCount)

    val bookmark = BookmarkEntity(
      filePath = "/sample/test.pdf",
      pageIndex = 3,
      title = "Section 2"
    )
    assertEquals(3, bookmark.pageIndex)

    val note = PdfNoteEntity(
      filePath = "/sample/test.pdf",
      pageIndex = 1,
      noteText = "Important contract note"
    )
    assertNotNull(note.timestamp)
    assertEquals("Important contract note", note.noteText)
  }
}



