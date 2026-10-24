package com.kurobello.tallybook.core.data

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertTrue
import org.w3c.dom.Element
import org.w3c.dom.NodeList

class DataExtractionRulesTest {

  @Test
  fun `cloud backup and device transfer exclude the device preferences file and its temp file`() {
    val rules = File("../androidApp/src/main/res/xml/data_extraction_rules.xml")
    assertTrue(rules.exists(), "rules not found at ${rules.absolutePath}")
    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(rules)
    val device = PreferencesFile.DEVICE.fileName

    for (section in listOf("cloud-backup", "device-transfer")) {
      val excluded =
          document
              .getElementsByTagName(section)
              .elements()
              .flatMap { it.getElementsByTagName("exclude").elements() }
              .map { it.getAttribute("domain") to it.getAttribute("path") }
              .toSet()

      for (path in listOf(device, "$device.tmp")) {
        assertTrue("file" to path in excluded, "$section must exclude $path: $excluded")
      }
    }
  }

  private fun NodeList.elements(): List<Element> =
      (0 until length).map { item(it) }.filterIsInstance<Element>()
}
