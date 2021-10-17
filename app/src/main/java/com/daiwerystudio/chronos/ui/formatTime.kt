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

fun formatTime(millis: Long, formatStyle: FormatStyle, type: Int,
               local: Boolean, isSystem24Hour: Boolean): String{
    var time = millis
    if (local) time += java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis())
    val text = when (type){
        FORMAT_TIME -> {
            val formatter = if (isSystem24Hour) DateTimeFormatter.ofPattern("HH:mm")
            else DateTimeFormatter.ofPattern("KK:mm a")

            LocalTime.ofSecondOfDay((time/1000)%(60*60*24)).format(formatter)
        }
        FORMAT_DAY -> LocalDate.ofEpochDay(time/(1000*60*60*24))
            .format(DateTimeFormatter.ofLocalizedDate(formatStyle))
        else -> throw IllegalArgumentException("Invalid type")
    }
   return text
}