package com.surrus.galwaybus.screenshots

import android.content.Context
import android.content.Intent
import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import com.surrus.galwaybus.ui.HomeActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy

class ScreenshotTest {
    @get:Rule
    val homeActivityRule = ActivityTestRule<HomeActivity>(HomeActivity::class.java, true, false)

    private var context: Context? = null

    @Before
    fun setUp() {
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())
        context = InstrumentationRegistry.getTargetContext()
    }


    @Test
    fun testGetScreenshots() {
        var intent = Intent()
        homeActivityRule.launchActivity(intent)


        Thread.sleep(2000)

        Screengrab.screenshot("01main_screen")

        Thread.sleep(5000)

        Screengrab.screenshot("02stops_screen")
    }
}