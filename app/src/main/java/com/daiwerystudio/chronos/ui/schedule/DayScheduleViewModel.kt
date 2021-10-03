/*
* Дата создания: 07.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 23.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: добавление логики взаимодействия с DaySchedule.
*
* Дата изменения: 11.09.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: удаление таблицы дня в расписании. Теперь только один тип.
*
* Дата изменения: 23.09.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: наследование от ClockViewModel.
*/

package com.daiwerystudio.chronos.ui.schedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.daiwerystudio.chronos.database.ActionSchedule
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.database.ActionTypeRepository
import com.daiwerystudio.chronos.database.ScheduleRepository
import com.daiwerystudio.chronos.ui.ClockViewModel
import com.daiwerystudio.chronos.ui.widgets.ActionsView
import java.util.concurrent.Executors

class DayScheduleViewModel : ClockViewModel() {
    private val mScheduleRepository = ScheduleRepository.get()
    private val mActionTypeRepository = ActionTypeRepository.get()

    /**
     * Специальный класс, необходимый для адаптера.
     */
    data class Section(
        val data: List<Pair<ActionSchedule, ActionType?>>
    )

    // День в расписании устанавливается из фрагмента.
    val daySchedule: MutableLiveData<Pair<String, Int>> = MutableLiveData()

    /*  Получаем список действий.  */
    val mActionsSchedule: LiveData<List<ActionSchedule>> =
        Transformations.switchMap(daySchedule) {
            mScheduleRepository.getActionsScheduleFromSchedule(it.first, it.second)
        }

    /*  Обрабатываем действия.  */
    private val mActionSections: LiveData<List<ActionSection>> =
        Transformations.switchMap(mActionsSchedule) { actionsSchedule ->
            val liveActionSections = MutableLiveData<List<ActionSection>>()

            Executors.newSingleThreadExecutor().execute {
                val actionIntervals = mutableListOf<ActionInterval>()
                actionsSchedule.forEach {
                    actionIntervals.add(ActionInterval(it.id, it.actionTypeID,
                        it.startTime, it.endTime))
                }
                liveActionSections.postValue(processingActionIntervals(actionIntervals))
            }

            liveActionSections
        }

    /*  Получаем типы действий, которые используются.  */
    private val mActionTypes: LiveData<List<ActionType>> =
        Transformations.switchMap(mActionSections) { actionSections ->
            val ids = mutableListOf<String>()
            actionSections.forEach { actionSection ->
                actionSection.intervals.forEach {
                    if (it.actionTypeID !in ids) ids.add(it.actionTypeID)
                }
            }

            mActionTypeRepository.getActionTypes(ids)
        }

    /*  Формируем секции с действиями и типами, с которыми они связаны.  */
    val sections: LiveData<List<Section>> =
        Transformations.switchMap(mActionTypes) { actionTypes ->
            val sections = mutableListOf<Section>()
            mActionSections.value!!.forEach { actionSection ->
                val data = mutableListOf<Pair<ActionSchedule, ActionType?>>()
                actionSection.intervals.forEach { actionInterval ->
                    val actionSchedule = mActionsSchedule.value!!.first { it.id == actionInterval.id }
                    val actionType = actionTypes.firstOrNull { it.id == actionSchedule.actionTypeID }
                    data.add(Pair(actionSchedule, actionType))
                }
                sections.add(Section(data))
            }

            MutableLiveData(sections)
        }

    /*  Формируем объекты для рисования.   */
    val actionDrawables: LiveData<List<ActionsView.ActionDrawable>> =
        Transformations.switchMap(mActionTypes) { actionTypes ->
            val actionDrawables = mutableListOf<ActionsView.ActionDrawable>()
            mActionSections.value!!.forEach { actionSection ->
                actionSection.intervals.forEach { actionInterval ->
                    val actionSchedule = mActionsSchedule.value!!.first { it.id == actionInterval.id }
                    val actionType = actionTypes.firstOrNull { it.id == actionSchedule.actionTypeID }

                    val color = actionType?.color ?: 0
                    val start = actionInterval.start/(24f*60*60*1000)
                    val end = actionInterval.end/(24f*60*60*1000)
                    val left = actionInterval.index.toFloat()
                    val right = left+1

                    actionDrawables.add(ActionsView.ActionDrawable(color, start, end, left, right))
                }
            }

            MutableLiveData(actionDrawables)
        }

    // Функция для алгоритма обработки действий.
    override fun getIndexForInterval(point: ActionPoint, columns: List<String>): Int {
        // Находим свободный индекс.
        val freeIndex = columns.indexOfFirst { it == "" }
        // Если не нашли, то ставим новый столбец.
        return if (freeIndex == -1) columns.size else freeIndex
    }

    fun deleteActionsSchedule(ids: List<String>){
        mScheduleRepository.deleteActionsSchedule(ids)
    }
}