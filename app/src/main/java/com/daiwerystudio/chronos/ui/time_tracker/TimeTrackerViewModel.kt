/*
* Дата создания: 06.09.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 24.09.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: наследование от ClockViewModel.
*/

package com.daiwerystudio.chronos.ui.time_tracker

import android.icu.util.TimeZone
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.daiwerystudio.chronos.database.Action
import com.daiwerystudio.chronos.database.ActionRepository
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.database.ActionTypeRepository
import com.daiwerystudio.chronos.ui.ClockViewModel
import com.daiwerystudio.chronos.ui.widgets.ActionsView
import java.util.concurrent.Executors

class TimeTrackerViewModel : ClockViewModel() {
    private val mActionRepository = ActionRepository.get()
    private val mActionTypeRepository = ActionTypeRepository.get()
    val local = TimeZone.getDefault().getOffset(System.currentTimeMillis())

    /**
     * Специальный класс, необходимый для адаптера.
     */
    data class Section(
        val data: List<Pair<Action, ActionType?>>
    )

    // Локальный день.
    val day: MutableLiveData<Long> = MutableLiveData()

    /*  Получаем список действий.  */
    val mActions: LiveData<List<Action>> = Transformations.switchMap(day){
        // Нужно не забыть перевести время из локального в глобальное.
        mActionRepository.getActionsFromTimeInterval(it*1000*60*60*24-local,
            (it+1)*1000*60*60*24-local)
    }

    /*  Обрабатываем действия.  */
    private val mActionSections: LiveData<List<ActionSection>> =
        Transformations.switchMap(mActions) { actions ->
            val liveActionSections = MutableLiveData<List<ActionSection>>()

            Executors.newSingleThreadExecutor().execute {
                val actionIntervals = mutableListOf<ActionInterval>()
                actions.forEach {
                    actionIntervals.add(ActionInterval(it.id, it.actionTypeID,
                        (it.startTime+local)-day.value!!*(24*60*60*1000),
                        (it.endTime+local)-day.value!!*(24*60*60*1000)))
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
                val data = mutableListOf<Pair<Action, ActionType?>>()
                actionSection.intervals.forEach { actionInterval ->
                    val action = mActions.value!!.first { it.id == actionInterval.id }
                    val actionType = actionTypes.firstOrNull { it.id == action.actionTypeID }
                    data.add(Pair(action, actionType))
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
                    val actionSchedule = mActions.value!!.first { it.id == actionInterval.id }
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

    fun deleteActions(ids: List<String>){
        mActionRepository.deleteActions(ids)
    }
}