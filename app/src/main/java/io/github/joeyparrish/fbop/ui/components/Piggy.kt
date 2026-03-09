// Copyright (c) 2026 Joey Parrish
// SPDX-License-Identifier: MIT

package io.github.joeyparrish.fbop.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import io.github.joeyparrish.fbop.BuildConfig
import io.github.joeyparrish.fbop.R
import java.time.LocalDate

private fun calculateEasterSunday(year: Int): LocalDate {
    // Gauss's Easter Sunday algorithm, simplified for Gregorian dates.
    // These pointlessly terse variable names are the same ones in
    // https://en.wikipedia.org/wiki/Date_of_Easter#Gauss_algorithm
    val a = year % 19
    val b = year % 4
    val c = year % 7
    val k = year / 100
    val p = (13 + 8 * k) / 25
    val q = k / 4
    val m = (15 - p + k - q) % 30
    val n = (4 + k - q) % 7

    val d = (19 * a + m) % 30
    val e = (2 * b + 4 * c + 6 * d + n) % 7

    val marchEaster = d + e + 22
    var aprilEaster = d + e - 9

    if (aprilEaster == 25 && d == 28 && e == 6 && a > 10) {
        aprilEaster = 18
    }

    if (aprilEaster == 26 && d == 29 && e == 6) {
        aprilEaster = 19
    }

    return if (marchEaster <= 31) {
        LocalDate.of(year, 3, marchEaster)
    } else {
        LocalDate.of(year, 4, aprilEaster)
    }
}

private data class PiggyVariant(
    @DrawableRes val res: Int,
    val isActiveToday: (LocalDate) -> Boolean
)

private val piggyVariants = listOf(
    // First Contact Day, April 5
    // A pig in Star Fleet uniform giving the vulcan salute
    PiggyVariant(R.drawable.pig_vulcan) { today ->
        today == LocalDate.of(today.year, 4, 5)
    },
    // St. Andrew's Day (National holiday in Scotland), November 30
    // Our piggy bank riding a unicorn over thistles
    PiggyVariant(R.drawable.pig_unicorn) { today ->
        today == LocalDate.of(today.year, 11, 30)
    },
    // Easter Sunday (western variant), various days in March & April
    // Our pig in an easter bunny outfit
    PiggyVariant(R.drawable.pig_easter) { today ->
        today == calculateEasterSunday(today.year)
    },
    // Our standard piggy bank image
    PiggyVariant(R.drawable.pig_standard) { _ -> true },
)

private val allPigResources = piggyVariants.map { it.res }

@DrawableRes
fun Piggy(): Int {
    val today = LocalDate.now()
    return piggyVariants.first { it.isActiveToday(today) }.res
}

@Composable
fun PigView(modifier: Modifier = Modifier, contentDescription: String? = null) {
    if (BuildConfig.DEBUG) {
        val initialPage = allPigResources.indexOf(Piggy())
        val pagerState = rememberPagerState(initialPage = initialPage) { allPigResources.size }
        HorizontalPager(state = pagerState, modifier = modifier) { page ->
            Image(
                painter = painterResource(allPigResources[page]),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    } else {
        Image(
            painter = painterResource(Piggy()),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}
