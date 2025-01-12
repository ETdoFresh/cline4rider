package com.etdofresh.cline4rider.tasks

import com.intellij.openapi.diagnostic.Logger

class DiffProcessor {
    private val logger = Logger.getInstance(DiffProcessor::class.java)

    fun processDiff(originalContent: String, diff: String): String {
        var result = originalContent
        val blocks = extractDiffBlocks(diff)
        
        for (block in blocks) {
            try {
                result = applyDiffBlock(result, block)
            } catch (e: Exception) {
                logger.error("Failed to apply diff block", e)
                throw e
            }
        }
        
        return result
    }

    private fun extractDiffBlocks(diff: String): List<DiffBlock> {
        val blocks = mutableListOf<DiffBlock>()
        val lines = diff.lines()
        var currentBlock: MutableList<String>? = null
        var isSearch = false
        var searchContent = StringBuilder()
        var replaceContent = StringBuilder()

        for (line in lines) {
            when {
                line.trim() == "<<<<<<< SEARCH" -> {
                    currentBlock = mutableListOf()
                    isSearch = true
                }
                line.trim() == "=======" && currentBlock != null -> {
                    isSearch = false
                }
                line.trim() == ">>>>>>> REPLACE" && currentBlock != null -> {
                    blocks.add(
                        DiffBlock(
                            searchContent.toString().trimEnd(),
                            replaceContent.toString().trimEnd()
                        )
                    )
                    searchContent = StringBuilder()
                    replaceContent = StringBuilder()
                    currentBlock = null
                }
                currentBlock != null -> {
                    if (isSearch) {
                        searchContent.append(line).append("\n")
                    } else {
                        replaceContent.append(line).append("\n")
                    }
                }
            }
        }

        return blocks
    }

    private fun applyDiffBlock(content: String, block: DiffBlock): String {
        val searchContent = block.search
        val replaceContent = block.replace
        
        // Find the first occurrence of the search content
        val index = content.indexOf(searchContent)
        if (index == -1) {
            logger.error("Search content not found: $searchContent")
            throw IllegalStateException("Search content not found in the original text")
        }

        // Replace the content
        return content.substring(0, index) + 
               replaceContent + 
               content.substring(index + searchContent.length)
    }

    data class DiffBlock(
        val search: String,
        val replace: String
    )
}
