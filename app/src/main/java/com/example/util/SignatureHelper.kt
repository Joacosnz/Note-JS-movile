package com.example.util

import com.example.ui.screens.DrawingPoint
import org.json.JSONArray
import org.json.JSONObject

object SignatureHelper {

    fun serializePoints(points: List<DrawingPoint>): String {
        val array = JSONArray()
        for (point in points) {
            val obj = JSONObject()
            obj.put("x", point.x.toDouble())
            obj.put("y", point.y.toDouble())
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializePoints(json: String): List<DrawingPoint> {
        if (json.isBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<DrawingPoint>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    DrawingPoint(
                        x = obj.getDouble("x").toFloat(),
                        y = obj.getDouble("y").toFloat()
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
