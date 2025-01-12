package com.etdofresh.cline4rider.tasks

import com.intellij.openapi.diagnostic.Logger
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory
import java.io.StringReader
import org.xml.sax.InputSource

class ToolParser {
    private val logger = Logger.getInstance(ToolParser::class.java)

    fun parseToolFromResponse(response: String): Tool? {
        try {
            // Extract the tool XML from the response
            val toolXml = extractToolXml(response) ?: return null
            
            // Parse the XML
            val document = parseXml(toolXml)
            val rootElement = document.documentElement

            // Get tool name (the root element's tag name)
            val toolName = rootElement.tagName

            // Extract parameters
            val params = mutableMapOf<String, String>()
            val childNodes = rootElement.childNodes
            for (i in 0 until childNodes.length) {
                val node = childNodes.item(i)
                if (node.nodeType == Node.ELEMENT_NODE) {
                    val element = node as Element
                    params[element.tagName] = element.textContent.trim()
                }
            }

            return Tool(toolName, params)
        } catch (e: Exception) {
            logger.error("Failed to parse tool from response", e)
            return null
        }
    }

    fun extractToolXml(response: String): String? {
        // Find the first occurrence of a tool tag
        val toolTags = listOf(
            "execute_command",
            "read_file",
            "write_to_file",
            "replace_in_file",
            "search_files",
            "list_files",
            "list_code_definition_names",
            "ask_followup_question",
            "attempt_completion"
        )

        for (tag in toolTags) {
            // Match tag with any amount of whitespace before it
            val startTagRegex = "\\s*<$tag>"
            val endTagRegex = "\\s*</$tag>"
            val startMatch = startTagRegex.toRegex().find(response)
            val endMatch = endTagRegex.toRegex().find(response)

            if (startMatch != null && endMatch != null) {
                val startIndex = startMatch.range.first
                val endIndex = endMatch.range.last + 1
                return response.substring(startIndex, endIndex)
            }
        }

        return null
    }

    private fun parseXml(xml: String): Document {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            isValidating = false
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
        }
        val builder = factory.newDocumentBuilder()
        val input = InputSource(StringReader(xml))
        try {
            return builder.parse(input)
        } catch (e: Exception) {
            logger.error("Failed to parse XML: $xml", e)
            throw e
        }
    }
}

data class Tool(
    val name: String,
    val parameters: Map<String, String>
)
