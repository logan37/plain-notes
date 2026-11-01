package com.example

import com.example.crypto.CryptoManager
import com.example.markdown.MarkdownNode
import com.example.markdown.MarkdownParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VaultKeepUnitTest {

    @Test
    fun testEncryptionAndDecryption() {
        val plainText = "Confidential meeting notes: launch Q4 targets"
        val passphrase = "MasterPassphrase123!"

        val cipherPayload = CryptoManager.encrypt(plainText, passphrase)
        assertTrue(cipherPayload.startsWith("VK1:"))
        assertNotEquals(plainText, cipherPayload)

        val decryptResult = CryptoManager.decrypt(cipherPayload, passphrase)
        assertTrue(decryptResult.isSuccess)
        assertEquals(plainText, decryptResult.getOrNull())
    }

    @Test
    fun testDecryptionFailsWithWrongPassphrase() {
        val plainText = "Secret journal entry"
        val correctPass = "CorrectPassword123"
        val wrongPass = "WrongPassword456"

        val cipherPayload = CryptoManager.encrypt(plainText, correctPass)
        val decryptResult = CryptoManager.decrypt(cipherPayload, wrongPass)
        assertTrue(decryptResult.isFailure)
    }

    @Test
    fun testMarkdownParserHeadersAndBullets() {
        val markdown = """
            # Heading 1
            ## Heading 2
            - Bullet 1
            - Bullet 2
            - [ ] Task pending
            - [x] Task done
            > Quote test
        """.trimIndent()

        val nodes = MarkdownParser.parse(markdown)
        assertTrue(nodes.any { it is MarkdownNode.Header && it.level == 1 && it.text == "Heading 1" })
        assertTrue(nodes.any { it is MarkdownNode.Header && it.level == 2 && it.text == "Heading 2" })
        assertTrue(nodes.any { it is MarkdownNode.BulletItem && it.text == "Bullet 1" })
        assertTrue(nodes.any { it is MarkdownNode.TaskItem && it.text == "Task pending" && !it.isChecked })
        assertTrue(nodes.any { it is MarkdownNode.TaskItem && it.text == "Task done" && it.isChecked })
        assertTrue(nodes.any { it is MarkdownNode.Quote && it.text == "Quote test" })
    }
}
