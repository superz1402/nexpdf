package com.nexpdf.app

import android.app.Application
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader

class NexPdfApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
    }
}
