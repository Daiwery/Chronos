package com.daiwerystudio.chronos.ui.day

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.lifecycle.ViewModelProvider
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.*
import com.daiwerystudio.chronos.databinding.DialogActionBinding
import com.daiwerystudio.chronos.ui.DialogViewModel
import com.daiwerystudio.chronos.ui.action_type.ActionTypeDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.*

/**
 * Представляет из себя диалог в виде нижней панели. По логике абсолютно такое же, как и
 * ActionTypeDialog. За тем исключение, что работает с Action.
 *
 * Возможная модификация: экран загрузки при загрузке DatePicker, ибо он долго грузится,
 * никак не уведомляя пользователя.
 */
class ActionDialog : BottomSheetDialogFragment() {
    /**
     * ViewModel.
     */
    private val viewModel: DialogViewModel
    by lazy { ViewModelProvider(this).get(DialogViewModel::class.java) }
    /**
     * Репозиторий для взаимодействия с базой данных. Данные из базы данных не извлекаются,
     * поэтому помещать его в ViewModel нет смысла.
     */
    private val repository = ActionRepository.get()
    /**
     * Привязка данных.
     */
    private lateinit var binding: DialogActionBinding
    /**
     * Действие, которое получает диалог из Bundle.
     */
    private lateinit var action: Action
    /**
     * Определяет, создается или изменяется ли диалог. Диалог получает его из Bundle.
     */
    private var isCreated: Boolean = false
    /**
     * Смещение времени в часовом поезде.
     */
    private val local = TimeZone.getDefault().getOffset(System.currentTimeMillis())/1000

    /**
     * Выполняет перед созданиес интефейса. Получает данные из Bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isCreated = arguments?.getBoolean("isCreated") as Boolean
        action = arguments?.getSerializable("action") as Action
        // Необходимо скопировать значение, т.к. передается ссылка.
        // Это нужно, чтобы RecyclerView смог засечь изменение данных и перерисовал holder.
        action = action.copy()

        if (viewModel.data != null) action = viewModel.data as Action
    }

    /**
     * Создание UI и его настройка.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DialogActionBinding.inflate(inflater, container, false)
        val view = binding.root
        binding.action = action

        // Отмена клавиатуры.
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)


        if (action.actionTypeId != "")
            binding.selectActionType.setSelectActionType(action.actionTypeId)

        binding.selectActionType.setOnSelectListener{ action.actionTypeId = it.id }
        binding.selectActionType.setOnClickAddListener{
            val dialog = ActionTypeDialog()
            dialog.arguments = Bundle().apply{
                putSerializable("actionType", ActionType(parent=it))
                putBoolean("isCreated", true)
            }
            dialog.show(requireActivity().supportFragmentManager, "ActionTypeDialog")
        }

        binding.startTime.setOnClickListener{
            val localTime = action.startTime+local
            val day = localTime/(24*60*60)
            val time = (localTime%(24*60*60)).toInt()
            val hour = time/3600
            val minute = (time-hour*3600)/60

            val dialog = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("")
                .build()

            dialog.addOnPositiveButtonClickListener {
                action.startTime = day*24*60*60+(dialog.hour*60 + dialog.minute)*60-local
                binding.action = action
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }

        binding.startDay.setOnClickListener{
            val localTime = action.startTime+local
            val time = localTime%(24*60*60)

            val dialog = MaterialDatePicker.Builder.datePicker()
                .setSelection(localTime*1000)
                .build()

            dialog.addOnPositiveButtonClickListener {
                action.startTime = dialog.selection!!/1000+time-local
                binding.action = action
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }


        binding.endTime.setOnClickListener{
            val localTime = action.endTime+local
            val day = localTime/(24*60*60)
            val time = (localTime%(24*60*60)).toInt()
            val hour = time/3600
            val minute = (time-hour*3600)/60

            val dialog = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("")
                .build()

            dialog.addOnPositiveButtonClickListener {
                action.endTime = day*24*60*60+(dialog.hour*60 + dialog.minute)*60-local
                binding.action = action
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }

        binding.endDay.setOnClickListener{
            val localTime = action.endTime+local
            val time = localTime%(24*60*60)

            val dialog = MaterialDatePicker.Builder.datePicker()
                .setSelection(localTime*1000)
                .build()

            dialog.addOnPositiveButtonClickListener {
                action.endTime = dialog.selection!!/1000+time-local
                binding.action = action
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }


        if (isCreated) binding.button.text = resources.getString(R.string.add)
        else binding.button.text = resources.getString(R.string.edit)


        binding.button.setOnClickListener {
            var permission = true
            if (action.actionTypeId == "") permission = false
            if (action.startTime > action.endTime)
                permission = false

            if (permission){
                if (isCreated) repository.addAction(action)
                else repository.updateAction(action)

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
        viewModel.data = action
    }
}