/*
* Дата создания: 07.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 28.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: изменена логика извлечения типов действий и добавлена взаимодействие с
* SelectActionTypeViewModel.
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
import com.daiwerystudio.chronos.ui.DataViewModel
import com.daiwerystudio.chronos.ui.widgets.SelectActionTypeViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

class ActionScheduleDialog : BottomSheetDialogFragment() {
    private val viewModel: SelectActionTypeViewModel
        by lazy { ViewModelProvider(this).get(SelectActionTypeViewModel::class.java) }
    private val dataViewModel: DataViewModel
        by lazy { ViewModelProvider(this).get(DataViewModel::class.java) }
    private val mScheduleRepository = ScheduleRepository.get()
    private val mActionTypeRepository = ActionTypeRepository.get()
    private lateinit var binding: DialogActionScheduleBinding
    private lateinit var actionSchedule: ActionSchedule
    private var type: Int = 0
    private var isCreated: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        type = arguments?.getInt("type") as Int
        isCreated = arguments?.getBoolean("isCreated") as Boolean
        actionSchedule = arguments?.getSerializable("actionSchedule") as ActionSchedule
        // Необходимо скопировать значение, т.к. передается ссылка.
        // Это нужно, чтобы RecyclerView смог засечь изменение данных и перерисовал holder.
        actionSchedule = actionSchedule.copy()

        if (dataViewModel.data != null) actionSchedule = dataViewModel.data as ActionSchedule

        // Сперва нужно получить id родителя.
        viewModel.parentID = arguments?.getString("parentID") as String
        if (viewModel.isAll.value == null) viewModel.isAll.value = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DialogActionScheduleBinding.inflate(inflater, container, false)
        binding.actionSchedule = actionSchedule
        binding.type = type

        // Отмена клавиатуры.
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)


        viewModel.actionTypes.observe(viewLifecycleOwner, {
            binding.selectActionType.setData(it)
        })
        val actionType = mActionTypeRepository.getActionType(actionSchedule.actionTypeId)
        actionType.observe(viewLifecycleOwner, {
            if (it != null && binding.selectActionType.selectedActionType == null)
                binding.selectActionType.setSelectedActionType(it)
        })
        binding.selectActionType.setOnSelectListener{ actionSchedule.actionTypeId = it.id }
        binding.selectActionType.setOnEditIsAllListener{ viewModel.isAll.value = it }


        binding.startAfter.setOnClickListener{
            val hour = actionSchedule.startAfter.toInt()/(1000*60*60)
            val minute =  (actionSchedule.startAfter.toInt()-hour*1000*60*60)/(1000*60)

            val dialog = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("")
                .build()

            dialog.addOnPositiveButtonClickListener {
                actionSchedule.startAfter = (dialog.hour*60+dialog.minute)*1000*60L
                binding.actionSchedule = actionSchedule
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }
        binding.duration.setOnClickListener {
            val hour = actionSchedule.duration.toInt()/(1000*60*60)
            val minute =  (actionSchedule.duration.toInt()-hour*1000*60*60)/(1000*60)

            val dialog = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("")
                .build()

            dialog.addOnPositiveButtonClickListener {
                actionSchedule.duration = (dialog.hour*60+dialog.minute)*1000*60L
                binding.actionSchedule = actionSchedule
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }

        binding.startTime.setOnClickListener{
            val hour = actionSchedule.startTime.toInt()/(1000*60*60)
            val minute =  (actionSchedule.startTime.toInt()-hour*1000*60*60)/(1000*60)

            val dialog = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("")
                .build()

            dialog.addOnPositiveButtonClickListener {
                actionSchedule.startTime = (dialog.hour*60+dialog.minute)*1000*60L
                binding.actionSchedule = actionSchedule
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }
        binding.endTime.setOnClickListener{
            val hour = actionSchedule.endTime.toInt()/(1000*60*60)
            val minute =  (actionSchedule.endTime.toInt()-hour*1000*60*60)/(1000*60)

            val dialog = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("")
                .build()

            dialog.addOnPositiveButtonClickListener {
                actionSchedule.endTime = (dialog.hour*60+dialog.minute)*1000*60L
                binding.actionSchedule = actionSchedule
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }

        when (type) {
            TYPE_DAY_SCHEDULE_RELATIVE -> {
                binding.textView7.text = resources.getString(R.string.start_after)
                binding.textView8.text = resources.getString(R.string.duration)
            }
            TYPE_DAY_SCHEDULE_ABSOLUTE -> {
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
            if (actionSchedule.startTime > actionSchedule.endTime) permission = false

            if (permission){
                if (isCreated) mScheduleRepository.addActionSchedule(actionSchedule)
                else mScheduleRepository.updateActionSchedule(actionSchedule)

                this.dismiss()
            }
        }

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        dataViewModel.data = actionSchedule
    }
}

