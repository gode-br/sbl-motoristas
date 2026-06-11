package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.DashboardScreen
import com.example.ui.FinancialSummary
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        DashboardScreen(
          summary = FinancialSummary(
            totalEarnings = 350.00,
            totalFuelCost = 85.50,
            totalOtherCost = 25.00,
            totalDailyCost = 110.50,
            totalFixedExpenses = 30.00,
            totalNetProfit = 209.50,
            totalKm = 145.0,
            totalHours = 8.5,
            earningsPerKm = 2.41,
            costPerKm = 0.76,
            netPerHour = 24.64,
            fuelCostPercentage = 24.43
          )
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
