/*
* Дата создания: 07.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.lifecycle.ViewModelProvider
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.*
import com.daiwerystudio.chronos.databinding.DialogActionScheduleBinding
import com.daiwerystudio.chronos.ui.DialogViewModel
import com.daiwerystudio.chronos.ui.action_type.ActionTypeDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

/** Представляет из себя диалог в виде нижней панели. По логике абсолютно такое же, как и
 * ActionTypeDialog. За тем исключение, что работает с ActionSchedule.
 *
 * Для выбора типа действия используется самописный виджет SelectActionTypeView.
 *
 * Действия UI зависят от типа расписания.
 *
 * Возможная модификация: будет лучше, если для разных типов расписаний будут отдельные
 * диалоги. Если данная модификация добавлена, не забудте убрать предупреждение в database.Schedule.
 */
class ActionScheduleDialog : BottomSheetDialogFragment() {
    /**
     * ViewModel.
     */
    private val viewModel: DialogViewModel
    by lazy { ViewModelProvider(this).get(DialogViewModel::class.java) }

    /**
     * Репозиторий для взаимодействия с базой данных. Данные из базы данных не извлекаются,
     * поэтому помещать его в ViewModel нет смысла.
     */
    private val scheduleRepository = ScheduleRepository.get()

    /**
     * Репозиторий для взаимодействия с базой данных. Нужен для работы
     * SelectActionTypeView.
     */
    private val actionTypeRepository = ActionTypeRepository.get()

    /**
     * Привязка данных.
     */
    private lateinit var binding: DialogActionScheduleBinding

    /**
     * Действие в расписании, которое получает диалог из Bundle.
     */
    private lateinit var actionSchedule: ActionSchedule

    /**
     * Тип расписания, которое получает диалог из Bundle.
     */
    private var type: Int = 0

    /**
     * Определяет, создается или изменяется ли диалог. Диалог получает его из Bundle.
     */
    private var isCreated: Boolean = false


    /**
     * Выполняет перед созданиес интефейса. Получает данные из Bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        type = arguments?.getInt("type") as Int
        isCreated = arguments?.getBoolean("isCreated") as Boolean
        actionSchedule = arguments?.getSerializable("actionSchedule") as ActionSchedule
        // Необходимо скопировать значение, т.к. передается ссылка.
        // Это нужно, чтобы RecyclerView смог засечь изменение данных и перерисовал holder.
        actionSchedule = actionSchedule.copy()

        if (viewModel.data != null) actionSchedule = viewModel.data as ActionSchedule
    }

    /**
     * Создание UI и его настройка.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DialogActionScheduleBinding.inflate(inflater, container, false)
        val view = binding.root
        binding.actionSchedule = actionSchedule
        binding.type = type

        // Отмена клавиатуры.
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        val actionTypes = actionTypeRepository.getAllActionType()
        actionTypes.observe(viewLifecycleOwner, {
            binding.selectActionType.setData(it)
        })

        val actionType = actionTypeRepository.getActionType(actionSchedule.actionTypeId)
        actionType.observe(viewLifecycleOwner, {
            if (it != null) binding.selectActionType.setSelectActionType(it)
        })

        binding.selectActionType.setOnSelectListener{
            actionSchedule.actionTypeId = it.id
        }


        binding.startAfter.setOnClickListener{
            val hour = actionSchedule.startAfter.toInt()/3600
            val minute =  (actionSchedule.startAfter.toInt()-hour*3600)/60

            val dialog = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("")
                .build()

            dialog.addOnPositiveButtonClickListener {
                actionSchedule.startAfter = (dialog.hour * 60 + dialog.minute) * 60L
                binding.actionSchedule = actionSchedule
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }


        binding.duration.setOnClickListener {
            val hour = actionSchedule.duration.toInt()/3600
            val minute =  (actionSchedule.duration.toInt()-hour*3600)/60

            val dialog = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("")
                .build()

            dialog.addOnPositiveButtonClickListener {
                actionSchedule.duration = (dialog.hour * 60 + dialog.minute) * 60L
                binding.actionSchedule = actionSchedule
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }


        binding.startTime.setOnClickListener{
            val hour = actionSchedule.startTime.toInt()/3600
            val minute =  (actionSchedule.startTime.toInt()-hour*3600)/60

            val dialog = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("")
                .build()

            dialog.addOnPositiveButtonClickListener {
                actionSchedule.startTime = (dialog.hour * 60 + dialog.minute) * 60L
                binding.actionSchedule = actionSchedule
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }

        binding.endTime.setOnClickListener{
            val hour = actionSchedule.endTime.toInt()/3600
            val minute =  (actionSchedule.endTime.toInt()-hour*3600)/60

            val dialog = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("")
                .build()

            dialog.addOnPositiveButtonClickListener {
                actionSchedule.endTime = (dialog.hour * 60 + dialog.minute) * 60L
                binding.actionSchedule = actionSchedule
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }


        when (type) {
            TYPE_SCHEDULE_RELATIVE -> {
                binding.textView7.text = resources.getString(R.string.start_after)
                binding.textView8.text = resources.getString(R.string.duration)
            }
            TYPE_SCHEDULE_ABSOLUTE -> {
                binding.textView7.text = resources.getString(R.string.start)
                binding.textView8.text = resources.getString(R.string.end)
            }
            else -> throw IllegalStateException("Invalid type")
        }

        if (isCreated) binding.button.text = resources.getString(R.string.add)
        else binding.button.text = resources.getString(R.string.edit)


        binding.button.setOnClickListener {
            var permission = true
            if (actionSchedule.actionTypeId == "") permission = false
            if (type == TYPE_SCHEDULE_ABSOLUTE && actionSchedule.startTime > actionSchedule.endTime)
                permission = false

            if (permission){
                if (isCreated) scheduleRepository.addActionSchedule(actionSchedule)
                else scheduleRepository.updateActionSchedule(actionSchedule)

                this.dismiss()
            }
        }

        return view
    }

    /**
     * Выполняется при уничтожении диалога. Нужно, чтобы сохранить данные, если уничтожение
     * произошло при перевороте устройства.
     */
    override fun onDestroy() {
        super.onDestroy()
        viewModel.data = actionSchedule
    }
}

