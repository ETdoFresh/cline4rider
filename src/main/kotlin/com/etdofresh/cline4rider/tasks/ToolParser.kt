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

    private fun extractToolXml(response: String): String? {
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
            val startTag = "<$tag>"
            val endTag = "</$tag>"
            val startIndex = response.indexOf(startTag)
            val endIndex = response.indexOf(endTag)

            if (startIndex != -1 && endIndex != -1) {
                return response.substring(startIndex, endIndex + endTag.length)
            }
        }

        return null
    }

    private fun parseXml(xml: String): Document {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val input = InputSource(StringReader(xml))
        return builder.parse(input)
    }
}

data class Tool(
    val name: String,
    val parameters: Map<String, String>
)
