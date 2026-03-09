// Copyright (c) 2026 Joey Parrish
// SPDX-License-Identifier: MIT

package io.github.joeyparrish.fbop.ui.components

import androidx.annotation.DrawableRes
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
    val M = (15 - p + k - q) % 30
    val N = (4 + k - q) % 7

    val d = (19 * a + M) % 30
    val e = (2 * b + 4 * c + 6 * d + N) % 7

    val marchEaster = d + e + 22
    var aprilEaster = d + e - 9

    if (aprilEaster == 25 && d == 28 && e == 6 && a > 10) {
        aprilEaster = 18
    }

    if (aprilEaster == 26 && d == 29 && e == 6) {
        aprilEaster = 19
    }

    if (marchEaster <= 31) {
        return LocalDate.of(year, 3, marchEaster)
    } else {
        return LocalDate.of(year, 4, aprilEaster)
    }
}

@DrawableRes
fun Piggy(): Int {
    val today = LocalDate.now()
    return when (today) {
        // First Contact Day, April 5
        // A pig in Star Fleet uniform giving the vulcan salute
        LocalDate.of(today.year, 4, 5) -> R.drawable.pig_vulcan

        // St. Andrew's Day (National holiday in Scotland), November 30
        // Our piggy bank riding a unicorn over thistles
        LocalDate.of(today.year, 11, 30) -> R.drawable.pig_unicorn

        // Easter Sunday (western variant), various days in March & April
        // Our pig in an easter bunny outfit
        calculateEasterSunday(today.year) -> R.drawable.pig_easter

        // Our standard piggy bank image
        else -> R.drawable.pig_standard
    }
}
