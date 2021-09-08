/*
* Дата создания: 08.09.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

const val FORMAT_TIME = 0
const val FORMAT_DAY = 1

fun formatTime(millis: Long, local: Boolean, formatStyle: FormatStyle, type: Int): String{
    var time = millis
    if (local) time += java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis())
    val text = when (type){
        FORMAT_TIME -> LocalTime.ofSecondOfDay((time/1000)%(60*60*24)).format(DateTimeFormatter.ofLocalizedTime(formatStyle))
        FORMAT_DAY -> LocalDate.ofEpochDay(time/(1000*60*60*24)).format(DateTimeFormatter.ofLocalizedDate(formatStyle))
        else -> throw IllegalArgumentException("Invalid type")
    }
   return text
}