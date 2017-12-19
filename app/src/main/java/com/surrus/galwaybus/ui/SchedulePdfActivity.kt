package com.surrus.galwaybus.ui

import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import com.surrus.galwaybus.Constants
import com.surrus.galwaybus.R
import es.voghdev.pdfviewpager.library.remote.DownloadFile
import java.lang.Exception
import es.voghdev.pdfviewpager.library.RemotePDFViewPager
import es.voghdev.pdfviewpager.library.adapter.PDFPagerAdapter
import kotlinx.android.synthetic.main.activity_schedule_pdf.*


class SchedulePdfActivity : AppCompatActivity(), DownloadFile.Listener {

    private var routeId: String = ""
    private var routeName: String = ""
    private var schedulePdfUrl: String = ""

    private var remotePDFViewPager: RemotePDFViewPager? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_pdf)


        if (savedInstanceState != null) {
            routeId = savedInstanceState.getString(Constants.ROUTE_ID)
            routeName = savedInstanceState.getString(Constants.ROUTE_NAME)
            schedulePdfUrl = savedInstanceState.getString(Constants.SCHEDULE_PDF)
        } else {
            routeId = intent.extras[Constants.ROUTE_ID] as String
            routeName = intent.extras[Constants.ROUTE_NAME] as String
            schedulePdfUrl = intent.extras[Constants.SCHEDULE_PDF] as String
        }
        title = routeId + " - " + routeName


        // For Android 5.0 and later use PdfRenderer to show PDF, otherwise use WebView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            progressBar.visibility = View.VISIBLE
            remotePDFViewPager = RemotePDFViewPager(this, schedulePdfUrl, this)

        } else {

            webView.visibility = View.VISIBLE
            webView.settings.javaScriptEnabled = true
            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    view.loadUrl(url)
                    return true
                }
            }

            val url = "http://drive.google.com/viewerng/viewer?embedded=true&url=" + schedulePdfUrl
            webView.loadUrl(url)
        }

    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(Constants.ROUTE_ID, routeId)
        outState.putString(Constants.ROUTE_NAME, routeName)
        outState.putString(Constants.SCHEDULE_PDF, schedulePdfUrl)
        super.onSaveInstanceState(outState)
    }



    override fun onSuccess(url: String?, destinationPath: String?) {
        pdfViewPager.visibility = View.VISIBLE
        pdfViewPager.adapter = PDFPagerAdapter(this, destinationPath);
        progressBar.visibility = View.GONE
    }

    override fun onFailure(e: Exception?) {
        progressBar.visibility = View.GONE
    }

    override fun onProgressUpdate(progress: Int, total: Int) {
    }

}
