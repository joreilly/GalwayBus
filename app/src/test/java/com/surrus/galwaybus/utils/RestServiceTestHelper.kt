package com.surrus.galwaybus.utils

import android.content.Context
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader


object RestServiceTestHelper {

    fun convertStreamToString(inputStream: InputStream): String {
        val reader = BufferedReader(InputStreamReader(inputStream))
        return reader.use { it.readText() }
    }

    @Throws(Exception::class)
    fun getStringFromFile(filePath: String): String {
        val stream = this.javaClass.classLoader.getResourceAsStream(filePath)

        val ret = convertStreamToString(stream)
        //Make sure you close all streams.
        stream.close()
        return ret
    }
}