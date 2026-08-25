package com.retimebox.lite.util

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.retimebox.lite.data.local.entity.ContentReference
import com.retimebox.lite.data.local.entity.RefType
import com.retimebox.lite.data.local.entity.SpaceType

/**
 * 短代码工具类
 *
 * 短代码格式（统一中括号）：
 * - 图片：[image][id][fileRelativePath][fileName]
 * - 视频：[video][id][fileRelativePath][fileName]
 * - 语音：[voice][id][fileRelativePath][fileName]
 * - 空间链接：[spacelink][id][type][webUrl][name][thumbnailUrl]
 *
 * 第一个中括号 = 类型，第二个中括号 = ID，之后的中括号 = 必要信息
 */
object RichEditorHelper {

    private val IMAGE_PATTERN = Regex("""\[image]\[(\d+)\]\[([^\]]*)\]\[([^\]]*)\]""")
    private val VIDEO_PATTERN = Regex("""\[video]\[(\d+)\]\[([^\]]*)\]\[([^\]]*)\]""")
    private val VOICE_PATTERN = Regex("""\[voice]\[(\d+)\]\[([^\]]*)\]\[([^\]]*)\]""")
    private val SPACE_LINK_PATTERN = Regex("""\[spacelink]\[(\d+)\]\[([^\]]*)\]\[([^\]]*)\]\[([^\]]*)\]\[([^\]]*)\]""")

    private val ALL_SHORTCODE_PATTERN = Regex("""\[(?:image|video|voice|spacelink)\]\[[^\]]+\](?:\[[^\]]+\])+""")

    /**
     * 从 Markdown 中提取所有内容引用
     */
    fun extractReferences(markdown: String): List<ContentReference> {
        val references = mutableListOf<ContentReference>()

        IMAGE_PATTERN.findAll(markdown).forEach { match ->
            val id = match.groupValues[1].toLongOrNull()
            if (id != null) {
                references.add(ContentReference(refType = RefType.IMAGE, targetId = id))
            }
        }

        VIDEO_PATTERN.findAll(markdown).forEach { match ->
            val id = match.groupValues[1].toLongOrNull()
            if (id != null) {
                references.add(ContentReference(refType = RefType.VIDEO, targetId = id))
            }
        }

        VOICE_PATTERN.findAll(markdown).forEach { match ->
            val id = match.groupValues[1].toLongOrNull()
            if (id != null) {
                references.add(ContentReference(refType = RefType.VOICE, targetId = id))
            }
        }

        SPACE_LINK_PATTERN.findAll(markdown).forEach { match ->
            val id = match.groupValues[1].toLongOrNull()
            if (id != null) {
                references.add(ContentReference(refType = RefType.SPACE_LINK, targetId = id))
            }
        }

        return references
    }

    /**
     * 为图片生成短代码
     */
    fun createImageShortcode(id: Long, fileRelativePath: String): String {
        val fileName = fileRelativePath.substringAfterLast('/', fileRelativePath)
        return "[image][$id][$fileRelativePath][$fileName]"
    }

    /**
     * 为视频生成短代码
     */
    fun createVideoShortcode(id: Long, fileRelativePath: String): String {
        val fileName = fileRelativePath.substringAfterLast('/', fileRelativePath)
        return "[video][$id][$fileRelativePath][$fileName]"
    }

    /**
     * 为语音生成短代码
     */
    fun createVoiceShortcode(id: Long, fileRelativePath: String): String {
        val fileName = fileRelativePath.substringAfterLast('/', fileRelativePath)
        return "[voice][$id][$fileRelativePath][$fileName]"
    }

    /**
     * 为空间链接生成短代码
     */
    fun createSpaceLinkShortcode(
        id: Long,
        type: SpaceType,
        webUrl: String,
        name: String,
        thumbnailUrl: String?
    ): String {
        val typeStr = when (type) {
            SpaceType.PANORAMA_IMAGE -> "panorama_image"
            SpaceType.PANORAMA_VIDEO -> "panorama_video"
            SpaceType.GSPLAT -> "gsplat"
        }
        val thumbnail = thumbnailUrl ?: ""
        return "[spacelink][$id][$typeStr][$webUrl][$name][$thumbnail]"
    }

    /**
     * 从 Markdown 中移除指定引用的短代码
     */
    fun removeReference(markdown: String, refType: RefType, targetId: Long): String {
        val pattern = when (refType) {
            RefType.IMAGE -> Regex("""\[image]\[$targetId]\[[^\]]*]\[[^\]]*]""")
            RefType.VIDEO -> Regex("""\[video]\[$targetId]\[[^\]]*]\[[^\]]*]""")
            RefType.VOICE -> Regex("""\[voice]\[$targetId]\[[^\]]*]\[[^\]]*]""")
            RefType.SPACE_LINK -> Regex("""\[spacelink]\[$targetId]\[[^\]]*]\[[^\]]*]\[[^\]]*]\[[^\]]*]""")
        }
        return pattern.replace(markdown, "")
    }

    /**
     * 从 Markdown 中提取指定类型短代码的信息
     * 返回 map: id -> (fileRelativePath, fileName)
     */
    fun extractMediaInfo(markdown: String, refType: RefType): Map<Long, Pair<String, String>> {
        val result = mutableMapOf<Long, Pair<String, String>>()
        val pattern = when (refType) {
            RefType.IMAGE -> IMAGE_PATTERN
            RefType.VIDEO -> VIDEO_PATTERN
            RefType.VOICE -> VOICE_PATTERN
            RefType.SPACE_LINK -> return emptyMap()
        }
        pattern.findAll(markdown).forEach { match ->
            val id = match.groupValues[1].toLongOrNull()
            val path = match.groupValues[2]
            val name = match.groupValues[3]
            if (id != null) {
                result[id] = path to name
            }
        }
        return result
    }

    /**
     * 从 Markdown 中提取空间链接短代码的信息
     * 返回 map: id -> (type, webUrl, name, thumbnailUrl)
     */
    fun extractSpaceLinkInfo(markdown: String): Map<Long, SpaceLinkInfo> {
        val result = mutableMapOf<Long, SpaceLinkInfo>()
        SPACE_LINK_PATTERN.findAll(markdown).forEach { match ->
            val id = match.groupValues[1].toLongOrNull()
            val type = match.groupValues[2]
            val webUrl = match.groupValues[3]
            val name = match.groupValues[4]
            val thumbnailUrl = match.groupValues[5]
            if (id != null) {
                result[id] = SpaceLinkInfo(type, webUrl, name, thumbnailUrl)
            }
        }
        return result
    }

    /**
     * 安全解析 Markdown，失败时返回空字符串
     */
    fun safeParseMarkdown(markdown: String?): String {
        return try {
            markdown ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 更新空间链接短代码中的信息
     */
    fun updateSpaceLinkShortcode(
        markdown: String,
        id: Long,
        type: SpaceType,
        webUrl: String,
        name: String,
        thumbnailUrl: String?
    ): String {
        val newShortcode = createSpaceLinkShortcode(id, type, webUrl, name, thumbnailUrl)
        val pattern = Regex("""\[spacelink]\[$id]\[[^\]]*]\[[^\]]*]\[[^\]]*]\[[^\]]*]""")
        return if (pattern.containsMatchIn(markdown)) {
            pattern.replace(markdown, newShortcode)
        } else {
            markdown
        }
    }

    data class SpaceLinkInfo(
        val type: String,
        val webUrl: String,
        val name: String,
        val thumbnailUrl: String
    )

    /**
     * 在光标所在行的末尾插入换行加短代码
     * - 光标在行首（行内位置为0）：短代码前加1个换行，后加1个换行
     * - 光标不在行首（行内位置非0）：短代码前加2个换行，后加1个换行
     *
     * @param textFieldValue 文本编辑器的值
     * @param shortcode 要插入的短代码
     * @return 插入后的 TextFieldValue（包含新的光标位置）
     */
    fun insertAtCursor(textFieldValue: TextFieldValue, shortcode: String): TextFieldValue {
        val text = textFieldValue.text
        val cursorPos = textFieldValue.selection.start.coerceIn(0, text.length)

        val isAtLineStart = cursorPos == 0 || text[cursorPos - 1] == '\n'

        val lineEnd = text.indexOf('\n', cursorPos).let { nextNewline ->
            if (nextNewline == -1) text.length else nextNewline
        }

        val before = text.substring(0, lineEnd)
        val after = text.substring(lineEnd)

        val prefixNewlines = if (isAtLineStart) "\n" else "\n\n"
        val newText = if (after.isNotEmpty()) {
            "$before$prefixNewlines$shortcode\n$after"
        } else {
            "$before$prefixNewlines$shortcode\n"
        }

        val newCursor = before.length + prefixNewlines.length + shortcode.length + 1
        return TextFieldValue(
            text = newText,
            selection = TextRange(newCursor, newCursor)
        )
    }
}
