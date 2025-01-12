package com.etdofresh.cline4rider.tasks

import com.intellij.openapi.diagnostic.Logger

class DiffProcessor {
    private val logger = Logger.getInstance(DiffProcessor::class.java)

    data class DiffResult(
        val success: Boolean,
        val content: String,
        val error: String? = null
    )

    fun processDiff(originalContent: String, diff: String): DiffResult {
        var result = originalContent
        val blocks = extractDiffBlocks(diff)
        
        for (block in blocks) {
            val blockResult = applyDiffBlock(result, block)
            if (!blockResult.success) {
                logger.warn("Failed to apply diff block: ${blockResult.error}")
                return blockResult
            }
            result = blockResult.content
        }
        
        return DiffResult(true, result)
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

    private fun applyDiffBlock(content: String, block: DiffBlock): DiffResult {
        val searchContent = block.search
        val replaceContent = block.replace
        
        // Find the first occurrence of the search content
        val index = content.indexOf(searchContent)
        if (index == -1) {
            logger.warn("Search content not found: $searchContent")
            return DiffResult(false, content, "Could not find the text to replace. The file may have been modified.")
        }

        // Replace the content
        val newContent = content.substring(0, index) + 
                        replaceContent + 
                        content.substring(index + searchContent.length)
        return DiffResult(true, newContent)
    }

    data class DiffBlock(
        val search: String,
        val replace: String
    )
}
