package tribixbite.cleverkeys.swipe.ctc

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import tribixbite.cleverkeys.swipe.geometric.CkdtDictionaryReader
import java.io.File
import java.security.MessageDigest

/** The APK's offline et source is the released CKDT, not a second generated vocabulary. */
class BundledEstonianDictionaryTest {
    @After fun resetResolver() = CtcImportedPackSupport.installResolver(null)

    @Test fun bundledBytesRemoveOnlyExactSisAndContainEstonianForms() {
        val asset = File("src/main/assets/dictionaries/et_enhanced.bin")
        val bytes = asset.readBytes()
        val sha256 = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
        assertThat(sha256)
            .isEqualTo("e5b6bc41df85a636d6892e51e2e0ef45a1a79990a0e0c9c326244b76b64d51a1")
        val entries = CkdtDictionaryReader.readEntries(bytes)
        assertThat(entries.size).isEqualTo(94_999)
        assertThat(entries.map { it.word }).contains("tere")
        assertThat(entries.map { it.word }).doesNotContain("sis")
        assertThat(entries.map { it.word }).contains("siis")
    }

    @Test fun bundledAttributionAndManifestRetainPublishedMetadata() {
        val manifest = File("src/main/assets/estonian/manifest.json").readText()
        val notice = File("src/main/assets/estonian/NOTICE.txt").readText()
        assertThat(manifest).contains("\"code\": \"et\"")
        assertThat(manifest).contains("\"wordCount\": 94999")
        assertThat(manifest).contains("\"dictionarySha256\": " +
            "\"e5b6bc41df85a636d6892e51e2e0ef45a1a79990a0e0c9c326244b76b64d51a1\"")
        assertThat(manifest).contains("CC-BY-4.0")
        assertThat(notice).contains("Leipzig Corpora Collection")
        assertThat(notice).contains("CC BY 4.0")
    }

    @Test fun freshInstallRoutesEstonianToBundledCkdt() {
        assertThat(CtcLanguageSupport.sourceFor("et-EE"))
            .isEqualTo(CtcLanguageSupport.LexiconSource.CKDT_BIN)
        assertThat(CtcLanguageSupport.assetFor("et"))
            .isEqualTo("dictionaries/et_enhanced.bin")
        assertThat(CtcLanguageSupport.langpackRelativePath("et")).isNull()
        assertThat(CtcScoringParams.presetFor("et").lambda).isEqualTo(2.0)
    }

    @Test fun eligibleImportedEstonianPackTakesPrecedenceWithoutChangingOtherBundledLanguages() {
        CtcImportedPackSupport.installResolver { it == "et" }
        assertThat(CtcLanguageSupport.sourceFor("et"))
            .isEqualTo(CtcLanguageSupport.LexiconSource.CKDT_LANGPACK)
        assertThat(CtcLanguageSupport.assetFor("et")).isNull()
        assertThat(CtcLanguageSupport.langpackRelativePath("et"))
            .isEqualTo("langpacks/et/dictionary.bin")
        assertThat(CtcLanguageSupport.sourceFor("fr"))
            .isEqualTo(CtcLanguageSupport.LexiconSource.CKDT_BIN)
    }
}
