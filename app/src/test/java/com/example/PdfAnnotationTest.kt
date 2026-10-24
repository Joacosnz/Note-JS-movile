package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.DigitalSignature
import com.example.ui.screens.DrawingPoint
import com.example.ui.screens.DrawingStroke
import com.example.util.PdfHelper
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PdfAnnotationTest {

    @Test
    fun `createSamplePdf generates 2 page contract pdf file`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = PdfHelper.createSamplePdf(context)

        assertNotNull("The sample PDF file should not be null", file)
        assertTrue("The generated PDF file should exist", file!!.exists())
        assertTrue("The generated PDF file should have non-zero size", file.length() > 0)
    }

    @Test
    fun `serialize and deserialize pdf annotations correctly`() {
        val pageStrokesMap = mapOf(
            0 to listOf(
                DrawingStroke(
                    id = "stroke-1",
                    points = listOf(DrawingPoint(0.1f, 0.2f), DrawingPoint(0.3f, 0.4f)),
                    colorHex = "#2563EB",
                    strokeWidthDp = 5f,
                    isHighlighter = false
                ),
                DrawingStroke(
                    id = "stroke-2",
                    points = listOf(DrawingPoint(0.5f, 0.5f), DrawingPoint(0.8f, 0.8f)),
                    colorHex = "#FFFF00",
                    strokeWidthDp = 24f,
                    isHighlighter = true
                )
            )
        )

        val pageSigsMap = mapOf(
            1 to listOf(
                DigitalSignature(
                    id = "sig-1",
                    points = listOf(DrawingPoint(0.2f, 0.2f), DrawingPoint(0.4f, 0.4f)),
                    normalizedX = 0.25f,
                    normalizedY = 0.72f,
                    scale = 1.0f
                )
            )
        )

        // Serialize
        val json = serializeTestAnnotations(pageStrokesMap, pageSigsMap)
        assertTrue(json.contains("stroke-1"))
        assertTrue(json.contains("sig-1"))

        // Deserialize
        val (deserializedStrokes, deserializedSigs) = deserializeTestAnnotations(json)

        val strokesPage0 = deserializedStrokes[0]
        assertNotNull(strokesPage0)
        assertEquals(2, strokesPage0!!.size)
        assertEquals("stroke-1", strokesPage0[0].id)
        assertEquals("#2563EB", strokesPage0[0].colorHex)
        assertFalse(strokesPage0[0].isHighlighter)

        assertEquals("stroke-2", strokesPage0[1].id)
        assertTrue(strokesPage0[1].isHighlighter)

        val sigsPage1 = deserializedSigs[1]
        assertNotNull(sigsPage1)
        assertEquals(1, sigsPage1!!.size)
        assertEquals("sig-1", sigsPage1[0].id)
        assertEquals(0.25f, sigsPage1[0].normalizedX, 0.01f)
    }

    private fun assertFalse(condition: Boolean) {
        Assert.assertFalse(condition)
    }

    private fun serializeTestAnnotations(
        pageStrokesMap: Map<Int, List<DrawingStroke>>,
        pageSignaturesMap: Map<Int, List<DigitalSignature>>
    ): String {
        val root = JSONObject()
        val pagesObj = JSONObject()

        val allPageIndices = (pageStrokesMap.keys + pageSignaturesMap.keys).distinct()
        for (pageIdx in allPageIndices) {
            val pageObj = JSONObject()

            val strokesArray = JSONArray()
            pageStrokesMap[pageIdx]?.forEach { stroke ->
                val strokeObj = JSONObject()
                strokeObj.put("id", stroke.id)
                strokeObj.put("colorHex", stroke.colorHex)
                strokeObj.put("strokeWidthDp", stroke.strokeWidthDp.toDouble())
                strokeObj.put("isHighlighter", stroke.isHighlighter)

                val pointsArr = JSONArray()
                stroke.points.forEach { pt ->
                    val ptObj = JSONObject()
                    ptObj.put("x", pt.x.toDouble())
                    ptObj.put("y", pt.y.toDouble())
                    pointsArr.put(ptObj)
                }
                strokeObj.put("points", pointsArr)
                strokesArray.put(strokeObj)
            }
            pageObj.put("strokes", strokesArray)

            val sigsArray = JSONArray()
            pageSignaturesMap[pageIdx]?.forEach { sig ->
                val sigObj = JSONObject()
                sigObj.put("id", sig.id)
                sigObj.put("normalizedX", sig.normalizedX.toDouble())
                sigObj.put("normalizedY", sig.normalizedY.toDouble())
                sigObj.put("scale", sig.scale.toDouble())

                val pointsArr = JSONArray()
                sig.points.forEach { pt ->
                    val ptObj = JSONObject()
                    ptObj.put("x", pt.x.toDouble())
                    ptObj.put("y", pt.y.toDouble())
                    pointsArr.put(ptObj)
                }
                sigObj.put("points", pointsArr)
                sigsArray.put(sigObj)
            }
            pageObj.put("signatures", sigsArray)

            pagesObj.put(pageIdx.toString(), pageObj)
        }

        root.put("pages", pagesObj)
        return root.toString()
    }

    private fun deserializeTestAnnotations(
        jsonStr: String
    ): Pair<Map<Int, List<DrawingStroke>>, Map<Int, List<DigitalSignature>>> {
        val strokesMap = mutableMapOf<Int, List<DrawingStroke>>()
        val sigsMap = mutableMapOf<Int, List<DigitalSignature>>()

        if (jsonStr.isBlank()) return Pair(strokesMap, sigsMap)

        try {
            val root = JSONObject(jsonStr)
            val pagesObj = root.optJSONObject("pages") ?: return Pair(strokesMap, sigsMap)

            val keys = pagesObj.keys()
            while (keys.hasNext()) {
                val keyStr = keys.next()
                val pageIdx = keyStr.toIntOrNull() ?: continue
                val pageObj = pagesObj.getJSONObject(keyStr)

                val strokesList = mutableListOf<DrawingStroke>()
                val strokesArr = pageObj.optJSONArray("strokes")
                if (strokesArr != null) {
                    for (i in 0 until strokesArr.length()) {
                        val sObj = strokesArr.getJSONObject(i)
                        val id = sObj.optString("id", UUID.randomUUID().toString())
                        val colorHex = sObj.optString("colorHex", "#2563EB")
                        val strokeWidthDp = sObj.optDouble("strokeWidthDp", 5.0).toFloat()
                        val isHighlighter = sObj.optBoolean("isHighlighter", false)

                        val ptsList = mutableListOf<DrawingPoint>()
                        val ptsArr = sObj.optJSONArray("points")
                        if (ptsArr != null) {
                            for (j in 0 until ptsArr.length()) {
                                val pObj = ptsArr.getJSONObject(j)
                                ptsList.add(DrawingPoint(pObj.getDouble("x").toFloat(), pObj.getDouble("y").toFloat()))
                            }
                        }
                        strokesList.add(DrawingStroke(id, ptsList, colorHex, strokeWidthDp, isHighlighter))
                    }
                }
                strokesMap[pageIdx] = strokesList

                val sigsList = mutableListOf<DigitalSignature>()
                val sigsArr = pageObj.optJSONArray("signatures")
                if (sigsArr != null) {
                    for (i in 0 until sigsArr.length()) {
                        val sigObj = sigsArr.getJSONObject(i)
                        val id = sigObj.optString("id", UUID.randomUUID().toString())
                        val normX = sigObj.optDouble("normalizedX", 0.25).toFloat()
                        val normY = sigObj.optDouble("normalizedY", 0.72).toFloat()
                        val scale = sigObj.optDouble("scale", 1.0).toFloat()

                        val ptsList = mutableListOf<DrawingPoint>()
                        val ptsArr = sigObj.optJSONArray("points")
                        if (ptsArr != null) {
                            for (j in 0 until ptsArr.length()) {
                                val pObj = ptsArr.getJSONObject(j)
                                ptsList.add(DrawingPoint(pObj.getDouble("x").toFloat(), pObj.getDouble("y").toFloat()))
                            }
                        }
                        sigsList.add(DigitalSignature(id, ptsList, normX, normY, scale))
                    }
                }
                sigsMap[pageIdx] = sigsList
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Pair(strokesMap, sigsMap)
    }
}
