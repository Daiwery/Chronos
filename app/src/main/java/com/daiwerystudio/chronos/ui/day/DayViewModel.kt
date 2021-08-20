/*
* Дата создания: 11.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.day

import android.icu.util.TimeZone
import androidx.lifecycle.*
import com.daiwerystudio.chronos.database.*
import java.util.concurrent.Executors

/**
 * Является ViewModel. Логика идентична остальным ViewModel.
 * За тем исключением, что имеет очень спецевическую связь с базой данных.
 */
class DayViewModel: ViewModel() {
    /**
     * Репозиторий для взаимодействия с базой данных.
     */
    private val mActionRepository = ActionRepository.get()

    /**
     * Добавляет действие в базу данных.
     */
    fun addAction(action: Action){
        mActionRepository.addAction(action)
    }

    /**
     * Смещение времени в часовом поезде.
     */
    val local = TimeZone.getDefault().getOffset(System.currentTimeMillis())/1000

    /**
     *  Здесь хранится день, полученной из фрагмента.
     *  Это нужно, чтобы при перевороте устройства, данные из базы данных заного не извлекались.
     *  День является локальным днем.
     */
    var day: Int = 0
        set(value) {
            if (value != field || day == 0) {
                field = value
            }
        }

    /**
     * Время пробуждения. Одно значение на всю программу.
     */
    var startDayTime: MutableLiveData<Long> = MutableLiveData()
        private set

    /**
     * Наблюдатель за startDayTime. При изменении его значения, нам нужно заного
     * посчитать startTime и endTime у действий в относительном расписании.
     */
    private val mObserverStartDayTime = Observer<Long> {
        Executors.newSingleThreadExecutor().execute {
            if (mActiveSchedules.value != null) {
                val actionsRelativeSchedule = mutableListOf<ActionSchedule>()

                mActiveSchedules.value!!.forEach {
                    if (it.type == TYPE_SCHEDULE_RELATIVE) {
                        // actionsRelativeSchedule.addAll(getActionsRelativeSchedule(it))
                    }
                }

                this.mActionsRelativeSchedule = actionsRelativeSchedule
                updateActionsSchedule()
            }
        }
    }

    /**
     * Данные из базы данных в обертке LiveData. Значение LiveData меняется при изменении
     * startDayTime. То есть при изменении startDayTime получаем новые данные и уведомляем
     * об этом подписчиков actions.
     * Так нужно сделать, чтобы фрагмент уведомлялся как при изменении базы данных, так и
     * при изменении startDayTime.
     */
    var actions: LiveData<List<Action>> = Transformations.switchMap(startDayTime) { getData(it) }
        private set

    /**
     * Извлекает данные из базы данных. При обновлении startDayTime вызывается эту функцию.
     */
    private fun getData(startDayTime: Long): LiveData<List<Action>> {
        return mActionRepository.getActionsFromInterval(day*60L*60*24-local+startDayTime,
            (day+1)*60L*60*24-local+startDayTime)
    }

    /**
     * Репозиторий для связи с базой данных.
     */
    private val mScheduleRepository = ScheduleRepository.get()

    /**
     * Значение в этой переменной фиктивно. Переменная нужна лишь для того, чтобы
     * ROOM сообщал об изменении в базе данных actions_schedule_table
     */
    // private var observeDataBase: LiveData<Int> = mScheduleRepository.observeDataBase()

    /**
     * То, что необходимо выполнить при изменении базе данных actions_schedule_table.
     * Выполняет тоже самое, что и mObserverActiveSchedules.
     */
    private var mObserverDataBase = Observer<Int> {
        Executors.newSingleThreadExecutor().execute{
            if (mActiveSchedules.value != null){
                val actionsAbsoluteSchedule = mutableListOf<ActionSchedule>()
                val actionsRelativeSchedule = mutableListOf<ActionSchedule>()
                mActiveSchedules.value!!.forEach {
//                    when (it.type) {
//                        TYPE_SCHEDULE_ABSOLUTE -> actionsAbsoluteSchedule.addAll(getActionsAbsoluteSchedule(it))
//                        TYPE_SCHEDULE_RELATIVE -> actionsRelativeSchedule.addAll(getActionsRelativeSchedule(it))
//                        else -> throw IllegalArgumentException("Invalid type")
//                    }
                }

                this.mActionsAbsoluteSchedule = actionsAbsoluteSchedule
                this.mActionsRelativeSchedule = actionsRelativeSchedule
                updateActionsSchedule()
            }
        }
    }

    /**
     * Список активных и неиспорченных расписаний в обертке LiveData.
     */
    private var mActiveSchedules: LiveData<List<Schedule>> = mScheduleRepository.getActiveAndNotCorruptSchedules()

    /**
     * Так как список расписаний в обертке LiveData, то у нас есть подписка на изменения
     * соответствующей базы данных.
     *
     * При изменении списка расписаний нам нужно заного извлечь из базы данных нужные действия.
     */
    private val mObserverActiveSchedules = Observer<List<Schedule>> {
        Executors.newSingleThreadExecutor().execute{
            val actionsAbsoluteSchedule = mutableListOf<ActionSchedule>()
            val actionsRelativeSchedule = mutableListOf<ActionSchedule>()
            it.forEach {
//                when (it.type) {
//                    TYPE_SCHEDULE_ABSOLUTE -> actionsAbsoluteSchedule.addAll(getActionsAbsoluteSchedule(it))
//                    TYPE_SCHEDULE_RELATIVE -> actionsRelativeSchedule.addAll(getActionsRelativeSchedule(it))
//                    else -> throw IllegalArgumentException("Invalid type")
//                }
            }

            this.mActionsAbsoluteSchedule = actionsAbsoluteSchedule
            this.mActionsRelativeSchedule = actionsRelativeSchedule
            updateActionsSchedule()
        }
    }

    /**
     * Извлекает из базы данных действия у абсолютного расписания.
     */
//    private fun getActionsAbsoluteSchedule(schedule: Schedule): List<ActionSchedule>{
//        val actionsAbsoluteSchedule = mutableListOf<ActionSchedule>()
//
//        val dayIndex = (day-schedule.dayStart).toInt()%schedule.countDays
//        actionsAbsoluteSchedule.addAll(mScheduleRepository.getActionsScheduleFromDayIndex(schedule.id, dayIndex))
//        // Получаем действия из следующего дня, так как они могут быть ночью.
//        val nextDay = mScheduleRepository.getActionsScheduleFromDayIndex(schedule.id, (dayIndex+1)%schedule.countDays)
//        nextDay.forEach {
//            it.startTime += 24*60*60L
//            it.endTime += 24*60*60L
//        }
//        actionsAbsoluteSchedule.addAll(nextDay)
//
//        return actionsAbsoluteSchedule
//    }

    /**
     * Извлекает из базы данных действия у относительного расписания
     * и заного расчитывает startTime и endTime.
     */
//    private fun getActionsRelativeSchedule(schedule: Schedule): List<ActionSchedule>{
//        val dayIndex = (day-schedule.dayStart).toInt()%schedule.countDays
//        val actionsRelativeSchedule = mScheduleRepository.getActionsScheduleFromDayIndex(schedule.id, dayIndex)
//
//        actionsRelativeSchedule.forEachIndexed { i, actionSchedule ->
//            var start = actionSchedule.startAfter
//            start += if (i != 0) actionsRelativeSchedule[i-1].endTime
//            else startDayTime.value ?: 6*60*60L
//
//            actionsRelativeSchedule[i].startTime = start
//            actionsRelativeSchedule[i].endTime = start+actionSchedule.duration
//        }
//
//        return actionsRelativeSchedule
//    }

    /**
     * Выполняется при инициализации. Устанавливаем наблюдателей.
     */
    init {
        mActiveSchedules.observeForever(mObserverActiveSchedules)
        startDayTime.observeForever(mObserverStartDayTime)
        // observeDataBase.observeForever(mObserverDataBase)
    }

    /**
     * Переменная для хранения действий из абсолютных расписаний. Это нужно для оптимизации:
     * при изменениях не делать все заного, а делать только конкретные действия.
     */
    private var mActionsAbsoluteSchedule = mutableListOf<ActionSchedule>()

    /**
     * Переменная для хранения действий из относительных расписаний. Это нужно для оптимизации:
     * при изменениях не делать все заного, а делать только конкретные действия.
     */
    private var mActionsRelativeSchedule = mutableListOf<ActionSchedule>()

    /**
     * Все необходимые действия из расписаний. Обертка MutableLiveData нужна по той причине,
     * что необходимо менять значение динамически.
     *
     * Имеет наблюдателей во фрагменте.
     */
    var actionsSchedule: MutableLiveData<List<ActionSchedule>> = MutableLiveData()
        private set

    /**
     * Соединяет mActionsAbsoluteSchedule и mActionsRelativeSchedule и уставаливает
     * соединенный список в actionsSchedule.
     */
    private fun updateActionsSchedule(){
        val actionsSchedule = mutableListOf<ActionSchedule>()
        actionsSchedule.addAll(mActionsAbsoluteSchedule)
        actionsSchedule.addAll(mActionsRelativeSchedule)

        // Используем post, так как мы будем находится не в основном потоке.
        this.actionsSchedule.postValue(actionsSchedule)
    }

    /**
     * Удаляет заданное действие.
     */
    fun deleteAction(action: Action){
        mActionRepository.deleteAction(action)
    }

    /**
     * Репозиторий для взаимодействия с базой данных.
     */
    private val mActionTypeRepository = ActionTypeRepository.get()

    /**
     * Ивлекает из базы данных тип действия в обертке LiveData. Необходим для UI.
     */
    fun getActionType(id: String): LiveData<ActionType> = mActionTypeRepository.getActionType(id)

    /**
     * Выполняется при удалении ViewModel. Необходимо удалить наблюдателей. дабы избежать
     * утечки памяти.
     */
    override fun onCleared() {
        super.onCleared()

        mActiveSchedules.removeObserver(mObserverActiveSchedules)
        startDayTime.removeObserver(mObserverStartDayTime)
        // observeDataBase.removeObserver(mObserverDataBase)
    }
}