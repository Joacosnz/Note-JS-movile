package com.example.util

import com.example.model.NoteBlock
import com.example.model.PdfBlock
import com.example.model.AudioBlock
import com.example.model.TableCell
import com.example.model.TextStyleType
import com.example.model.PdfDrawingStroke
import com.example.model.SignatureStamp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object NoteBlockSerializer {
    fun serialize(blocks: List<NoteBlock>): String {
        val jsonArray = JSONArray()
        for (block in blocks) {
            val obj = JSONObject()
            when (block) {
                is NoteBlock.Text -> {
                    obj.put("type", "text")
                    obj.put("id", block.id)
                    obj.put("content", block.content)
                    obj.put("style", block.style.name)
                }
                is NoteBlock.Checklist -> {
                    obj.put("type", "checklist")
                    obj.put("id", block.id)
                    obj.put("content", block.content)
                    obj.put("isChecked", block.isChecked)
                }
                is NoteBlock.SimpleList -> {
                    obj.put("type", "simpleList")
                    obj.put("id", block.id)
                    obj.put("content", block.content)
                    obj.put("isNumbered", block.isNumbered)
                    obj.put("number", block.number)
                }
                is NoteBlock.CalloutBox -> {
                    obj.put("type", "callout")
                    obj.put("id", block.id)
                    obj.put("content", block.content)
                    obj.put("bg", block.backgroundColorHex ?: JSONObject.NULL)
                }
                is NoteBlock.Drawing -> {
                    obj.put("type", "drawing")
                    obj.put("id", block.id)
                    obj.put("pathData", block.pathData)
                }
                is NoteBlock.Table -> {
                    obj.put("type", "table")
                    obj.put("id", block.id)
                    obj.put("rows", block.rows)
                    obj.put("cols", block.cols)
                    obj.put("hasHeaderRow", block.hasHeaderRow)
                    val matrixArray = JSONArray()
                    for (row in block.matrix) {
                        val rowArray = JSONArray()
                        for (cell in row) {
                            val cellObj = JSONObject()
                            cellObj.put("text", cell.text)
                            cellObj.put("bg", cell.backgroundColorHex ?: JSONObject.NULL)
                            rowArray.put(cellObj)
                        }
                        matrixArray.put(rowArray)
                    }
                    obj.put("matrix", matrixArray)
                }
                is PdfBlock -> {
                    obj.put("type", "pdf")
                    obj.put("id", block.id)
                    obj.put("filePath", block.filePath)
                    obj.put("fileName", block.fileName)
                    obj.put("fileSizeFormatted", block.fileSizeFormatted)
                    obj.put("pageCount", block.pageCount)
                    obj.put("currentPageIndex", block.currentPageIndex)
                    obj.put("isExpanded", block.isExpanded)
                    obj.put("indexedText", block.indexedText)

                    val annotationsObj = JSONObject()
                    for ((page, strokes) in block.annotationsPerPage) {
                        val strokesArr = JSONArray()
                        for (stroke in strokes) {
                            val strokeObj = JSONObject()
                            strokeObj.put("colorHex", stroke.colorHex)
                            strokeObj.put("strokeWidth", stroke.strokeWidth.toDouble())
                            strokeObj.put("isHighlighter", stroke.isHighlighter)
                            val pointsArr = JSONArray()
                            for (p in stroke.points) {
                                val pObj = JSONObject()
                                pObj.put("x", p.first.toDouble())
                                pObj.put("y", p.second.toDouble())
                                pointsArr.put(pObj)
                            }
                            strokeObj.put("points", pointsArr)
                            strokesArr.put(strokeObj)
                        }
                        annotationsObj.put(page.toString(), strokesArr)
                    }
                    obj.put("annotationsPerPage", annotationsObj)

                    val signaturesObj = JSONObject()
                    for ((page, stamps) in block.signaturesPerPage) {
                        val stampsArr = JSONArray()
                        for (stamp in stamps) {
                            val stampObj = JSONObject()
                            stampObj.put("id", stamp.id)
                            stampObj.put("normalizedX", stamp.normalizedX.toDouble())
                            stampObj.put("normalizedY", stamp.normalizedY.toDouble())
                            stampObj.put("widthRatio", stamp.widthRatio.toDouble())
                            stampObj.put("heightRatio", stamp.heightRatio.toDouble())
                            val pointsArr = JSONArray()
                            for (p in stamp.points) {
                                val pObj = JSONObject()
                                pObj.put("x", p.first.toDouble())
                                pObj.put("y", p.second.toDouble())
                                pointsArr.put(pObj)
                            }
                            stampObj.put("points", pointsArr)
                            stampsArr.put(stampObj)
                        }
                        signaturesObj.put(page.toString(), stampsArr)
                    }
                    obj.put("signaturesPerPage", signaturesObj)
                }
                is AudioBlock -> {
                    obj.put("type", "audio")
                    obj.put("id", block.id)
                    obj.put("filePath", block.filePath)
                    obj.put("durationMs", block.durationMs)
                    obj.put("transcription", block.transcription)
                    obj.put("isTranscribing", block.isTranscribing)
                    obj.put("isTranscriptionVisible", block.isTranscriptionVisible)
                }
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    fun deserialize(json: String): List<NoteBlock> {
        if (json.isBlank()) {
            return listOf(NoteBlock.Text(content = ""))
        }
        val trimmed = json.trim()
        if (!trimmed.startsWith("[")) {
            // Backward compatibility: legacy plain text
            return listOf(NoteBlock.Text(content = json))
        }
        return try {
            val jsonArray = JSONArray(json)
            val list = mutableListOf<NoteBlock>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                val type = obj.optString("type", "text")
                val id = obj.optString("id", UUID.randomUUID().toString())
                when (type) {
                    "text" -> {
                        val content = obj.optString("content", "")
                        val styleName = obj.optString("style", "BODY")
                        val style = runCatching { TextStyleType.valueOf(styleName) }.getOrDefault(TextStyleType.BODY)
                        list.add(NoteBlock.Text(id = id, content = content, style = style))
                    }
                    "checklist" -> {
                        val content = obj.optString("content", "")
                        val isChecked = obj.optBoolean("isChecked", false)
                        list.add(NoteBlock.Checklist(id = id, content = content, isChecked = isChecked))
                    }
                    "simpleList" -> {
                        val content = obj.optString("content", "")
                        val isNumbered = obj.optBoolean("isNumbered", false)
                        val number = obj.optInt("number", 1)
                        list.add(NoteBlock.SimpleList(id = id, content = content, isNumbered = isNumbered, number = number))
                    }
                    "callout" -> {
                        val content = obj.optString("content", "")
                        val bg = obj.optString("bg", null).takeIf { !it.isNullOrBlank() && it != "null" }
                        list.add(NoteBlock.CalloutBox(id = id, content = content, backgroundColorHex = bg))
                    }
                    "drawing" -> {
                        val pathData = obj.optString("pathData", "")
                        list.add(NoteBlock.Drawing(id = id, pathData = pathData))
                    }
                    "table" -> {
                        val rows = obj.optInt("rows", 2)
                        val cols = obj.optInt("cols", 2)
                        val matrixArray = obj.optJSONArray("matrix")
                        val matrix = mutableListOf<MutableList<TableCell>>()
                        if (matrixArray != null) {
                            for (r in 0 until matrixArray.length()) {
                                val rowArray = matrixArray.optJSONArray(r)
                                val rowList = mutableListOf<TableCell>()
                                if (rowArray != null) {
                                    for (c in 0 until rowArray.length()) {
                                        val cellObj = rowArray.optJSONObject(c)
                                        val text = cellObj?.optString("text", "") ?: ""
                                        val bg = cellObj?.optString("bg", null).takeIf { !it.isNullOrBlank() && it != "null" }
                                        rowList.add(TableCell(text = text, backgroundColorHex = bg))
                                    }
                                }
                                while (rowList.size < cols) {
                                    rowList.add(TableCell())
                                }
                                matrix.add(rowList)
                            }
                        }
                        while (matrix.size < rows) {
                            val rowList = mutableListOf<TableCell>()
                            for (c in 0 until cols) {
                                rowList.add(TableCell())
                            }
                            matrix.add(rowList)
                        }
                        val hasHeaderRow = obj.optBoolean("hasHeaderRow", false)
                        list.add(NoteBlock.Table(id = id, rows = rows, cols = cols, matrix = matrix, hasHeaderRow = hasHeaderRow))
                    }
                    "pdf" -> {
                        val filePath = obj.optString("filePath", "")
                        val fileName = obj.optString("fileName", "Documento.pdf")
                        val fileSizeFormatted = obj.optString("fileSizeFormatted", "")
                        val pageCount = obj.optInt("pageCount", 1)
                        val currentPageIndex = obj.optInt("currentPageIndex", 0)
                        val isExpanded = obj.optBoolean("isExpanded", true)
                        val indexedText = obj.optString("indexedText", "")
                        
                        val annotationsPerPage = mutableMapOf<Int, List<PdfDrawingStroke>>()
                        val annotationsObj = obj.optJSONObject("annotationsPerPage")
                        if (annotationsObj != null) {
                            for (key in annotationsObj.keys()) {
                                val pageIndex = key.toIntOrNull() ?: continue
                                val strokesArr = annotationsObj.optJSONArray(key) ?: continue
                                val strokes = mutableListOf<PdfDrawingStroke>()
                                for (i in 0 until strokesArr.length()) {
                                    val strokeObj = strokesArr.optJSONObject(i) ?: continue
                                    val colorHex = strokeObj.optString("colorHex", "#FF000000")
                                    val strokeWidth = strokeObj.optDouble("strokeWidth", 3.0).toFloat()
                                    val isHighlighter = strokeObj.optBoolean("isHighlighter", false)
                                    val pointsArr = strokeObj.optJSONArray("points") ?: continue
                                    val points = mutableListOf<Pair<Float, Float>>()
                                    for (j in 0 until pointsArr.length()) {
                                        val pObj = pointsArr.optJSONObject(j) ?: continue
                                        points.add(Pair(pObj.optDouble("x", 0.0).toFloat(), pObj.optDouble("y", 0.0).toFloat()))
                                    }
                                    strokes.add(PdfDrawingStroke(points, colorHex, strokeWidth, isHighlighter))
                                }
                                annotationsPerPage[pageIndex] = strokes
                            }
                        }

                        val signaturesPerPage = mutableMapOf<Int, List<SignatureStamp>>()
                        val signaturesObj = obj.optJSONObject("signaturesPerPage")
                        if (signaturesObj != null) {
                            for (key in signaturesObj.keys()) {
                                val pageIndex = key.toIntOrNull() ?: continue
                                val stampsArr = signaturesObj.optJSONArray(key) ?: continue
                                val stamps = mutableListOf<SignatureStamp>()
                                for (i in 0 until stampsArr.length()) {
                                    val stampObj = stampsArr.optJSONObject(i) ?: continue
                                    val stampId = stampObj.optString("id", UUID.randomUUID().toString())
                                    val normalizedX = stampObj.optDouble("normalizedX", 0.0).toFloat()
                                    val normalizedY = stampObj.optDouble("normalizedY", 0.0).toFloat()
                                    val widthRatio = stampObj.optDouble("widthRatio", 0.35).toFloat()
                                    val heightRatio = stampObj.optDouble("heightRatio", 0.15).toFloat()
                                    val pointsArr = stampObj.optJSONArray("points") ?: continue
                                    val points = mutableListOf<Pair<Float, Float>>()
                                    for (j in 0 until pointsArr.length()) {
                                        val pObj = pointsArr.optJSONObject(j) ?: continue
                                        points.add(Pair(pObj.optDouble("x", 0.0).toFloat(), pObj.optDouble("y", 0.0).toFloat()))
                                    }
                                    stamps.add(SignatureStamp(stampId, points, normalizedX, normalizedY, widthRatio, heightRatio))
                                }
                                signaturesPerPage[pageIndex] = stamps
                            }
                        }

                        list.add(PdfBlock(id = id, filePath = filePath, fileName = fileName, fileSizeFormatted = fileSizeFormatted, pageCount = pageCount, currentPageIndex = currentPageIndex, isExpanded = isExpanded, annotationsPerPage = annotationsPerPage, signaturesPerPage = signaturesPerPage, indexedText = indexedText))
                    }
                    "audio" -> {
                        val filePath = obj.optString("filePath", "")
                        val durationMs = obj.optLong("durationMs", 0L)
                        val transcription = obj.optString("transcription", "")
                        val isTranscribing = obj.optBoolean("isTranscribing", false)
                        val isTranscriptionVisible = obj.optBoolean("isTranscriptionVisible", false)
                        list.add(AudioBlock(id = id, filePath = filePath, durationMs = durationMs, transcription = transcription, isTranscribing = isTranscribing, isTranscriptionVisible = isTranscriptionVisible))
                    }
                    else -> {
                        list.add(NoteBlock.Text(id = id, content = obj.optString("content", ""), style = TextStyleType.BODY))
                    }
                }
            }
            val parsedList = if (list.isEmpty()) listOf(NoteBlock.Text(content = "")) else list
            mergeConsecutiveTextBlocks(parsedList)
        } catch (e: Exception) {
            listOf(NoteBlock.Text(content = json))
        }
    }

    private fun mergeConsecutiveTextBlocks(blocks: List<NoteBlock>): List<NoteBlock> {
        if (blocks.isEmpty()) return blocks
        val result = mutableListOf<NoteBlock>()
        for (block in blocks) {
            val last = result.lastOrNull()
            if (last is NoteBlock.Text && block is NoteBlock.Text && last.style == block.style) {
                val combinedText = when {
                    last.content.isEmpty() -> block.content
                    block.content.isEmpty() -> last.content
                    else -> "${last.content}\n${block.content}"
                }
                result[result.lastIndex] = last.copy(content = combinedText)
            } else {
                result.add(block)
            }
        }
        return result
    }
}
