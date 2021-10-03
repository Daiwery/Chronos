/*
* Дата создания: 23.09.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui

import androidx.lifecycle.ViewModel

/**
 * Класс, в котором описаны методы обработки действий.
 *
 * Главная особенность: если действия пересекают друг друга,
 * то их нужно отодвинуть в сторону, чтобы не рисовать их друг на друге.
 * Основная суть алгоритма, обрабатывающее это, состоит в превращении отрезка времени
 * в две точки с указанием, конец это или начало, и последующей сортировке этих точек по
 * координате. После чего можно найти те действия, которые пересекаются, и делать с ними,
 * что требуется.
 */
abstract class ClockViewModel: ViewModel() {
    /**
     * Вспомогательный класс. Хранит информацию об одной точке. Нужен в алгоритме для обработки
     * данных.
     */
    data class ActionPoint(
        val id: String,
        val isStart: Boolean,
        val coordinate: Float
    )

    /**
     * Вспомогательный класс. Хранит информацию об временном интервале.
     * Нужен в алгоритме для обработки данных.
     * Ключевое свойство - index. Этот индекс указывает, в каком столбце
     * стоит нарисовать действие.
     */
    data class ActionInterval(
        val id: String,
        val actionTypeID: String,
        val start: Long,
        val end: Long,
        var index: Int = 0
    )

    /**
     * Вспомогательный класс. Связывает тип действия и время. Нужен, чтобы определить,
     * какое действие в какой промежуток времени идет.
     */
    data class ActionTypeTime(
        var actionTypeID: String? = "",
        var start: Float = 0f,
        var end: Float = 0f
    )

    /**
     * Хранит информацию о секции, в которой пересекаются действия.
     */
    data class ActionSection(
        var intervals: List<ActionInterval>
    )

    /**
     * Выозвращает индекс для интервала.
     */
    abstract fun getIndexForInterval(point: ActionPoint, columns: List<String>): Int

    fun processingActionIntervals(intervals: List<ActionInterval>): List<ActionSection> {
        val rawPoints = mutableListOf<ActionPoint>()
        intervals.forEach{
            var start = it.start/(24f*60*60*1000)
            var end = it.end/(24f*60*60*1000)

            // Возможно, что действие выйдет за пределы текущего дня.
            if (start < 0f ) start = 0f
            if (end > 1f ) end = 1f

            rawPoints.add(ActionPoint(it.id, true, start))
            rawPoints.add(ActionPoint(it.id,false, end))
        }
        val points = rawPoints.sortedBy { it.coordinate }


        // Типы действий с временем начала и конца.
        val actionTypesTime = mutableListOf<ActionTypeTime>()
        var actionTypeTime = ActionTypeTime()

        // Это нужно для определения областей пересечения.
        val sections = mutableListOf<ActionSection>()
        var intervalsForSection = mutableListOf<ActionInterval>()

        // Массив id действий, которые сейчас активны в столбце с номер, равным индексу.
        // Если id='', то это свободный столбец. По умолчанию всегда есть один столбец.
        var columns = mutableListOf("")

        // Список активных действий, то есть тех, которые есть в данный момент времени.
        val active = mutableListOf<String>()
        points.forEach { point ->
            if (point.isStart) {
                // Если это начало, то добавляем id в список активных id.
                active.add(point.id)
                // Добавляем интервал в список.
                intervalsForSection.add(intervals.first { it.id == point.id })

                // Если сейчас только одно активное действие.
                if (active.size == 1) {
                    columns[0] = point.id

                    // Начали одно новое действие.
                    actionTypeTime.actionTypeID = intervals.first { it.id == point.id }.actionTypeID
                    actionTypeTime.start = point.coordinate
                }

                // Если это первое пересечение.
                // Нельзя использовать active.size == 2, так как в данный момент может быть
                // только одно активное действие, но которое не в первом столбце. То есть
                // пересечение еще не закончилось.
                if (columns.size == 1) {
                    // Устанавливаем конец действия в начале секции.
                    // Так как при пересечении неизвестно, что делать.
                    actionTypeTime.end = point.coordinate
                    actionTypesTime.add(actionTypeTime)
                    actionTypeTime = ActionTypeTime()

                    // Указываем, что тут пересечение, и нельзя определить тип действия, который
                    // нужно выполнить.
                    actionTypeTime.actionTypeID = null
                    actionTypeTime.start = point.coordinate
                }

                // Если больше одного, то действия пересекаются.
                if (active.size > 1) {
                    val index = getIndexForInterval(point, columns)
                    intervals.first { it.id == point.id }.index = index
                    if (columns.size <= index) columns.add(point.id)
                    else columns[index] = point.id
                }
            } else {
                active.remove(point.id)
                columns[columns.indexOfFirst { it == point.id }] = ""

                // Если сейчас только одно активное действие, то мы только что
                // вышли из пересечения. Но это действие может еще идти,
                // причем не в первом столбце, и может пересечься с другим/и.
                // Поэтому необходимо сохранить весь порядок.
                if (active.size == 1) {
                    // Конец пересечения.
                    actionTypeTime.end = point.coordinate
                    actionTypesTime.add(actionTypeTime)
                    actionTypeTime = ActionTypeTime()

                    // И начало нового действия. Так как теперь можно определить,
                    // какое действие необходимо выполнить.
                    actionTypeTime.actionTypeID = intervals.first { it.id == active[0] }.actionTypeID
                    actionTypeTime.start = point.coordinate
                }

                // Если нет активный действий.
                if (active.size == 0) {
                    // Конец всех действий.
                    actionTypeTime.end = point.coordinate
                    actionTypesTime.add(actionTypeTime)
                    actionTypeTime = ActionTypeTime()

                    // Добавляем секцию.
                    sections.add(ActionSection(intervalsForSection))

                    // Готовимся к следующему пересечению.
                    columns = mutableListOf("")
                    intervalsForSection = mutableListOf()
                }
            }
        }
        return sections
    }
}
