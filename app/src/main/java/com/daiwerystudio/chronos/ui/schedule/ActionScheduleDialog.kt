/*
* Дата создания: 07.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 28.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: изменена логика извлечения типов действий и добавлено взаимодействие с
* SelectActionTypeViewModel.
*
* Дата изменения: 11.09.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: удаление таблицы дня в расписании. Теперь только один тип.
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
import com.daiwerystudio.chronos.ui.FORMAT_TIME
import com.daiwerystudio.chronos.ui.formatTime
import com.daiwerystudio.chronos.ui.SelectActionTypeViewModel
import com.daiwerystudio.chronos.ui.action_type.ActionTypeDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.format.FormatStyle
import java.util.*

class ActionScheduleDialog : BottomSheetDialogFragment() {
    private val viewModel: SelectActionTypeViewModel
        by lazy { ViewModelProvider(this).get(SelectActionTypeViewModel::class.java) }
    private val dataViewModel: DataViewModel
        by lazy { ViewModelProvider(this).get(DataViewModel::class.java) }
    private val mScheduleRepository = ScheduleRepository.get()
    private lateinit var binding: DialogActionScheduleBinding
    private lateinit var actionSchedule: ActionSchedule
    private var isCreated: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        binding.startTime.editText?.setText(formatTime(actionSchedule.startTime, false, FormatStyle.SHORT, FORMAT_TIME))
        binding.endTime.editText?.setText(formatTime(actionSchedule.endTime, false, FormatStyle.SHORT, FORMAT_TIME))

        viewModel.actionTypes.observe(viewLifecycleOwner, {
            binding.selectActionType.setData(it)
        })
        binding.selectActionType.setSelectedActionType(actionSchedule.actionTypeID)
        binding.selectActionType.setOnSelectListener{ actionSchedule.actionTypeID = it.id }
        binding.selectActionType.setOnEditIsAllListener{ viewModel.isAll.value = it }
        binding.selectActionType.setOnAddListener{
            val id = UUID.randomUUID().toString()
            val actionType = ActionType(id=id)
            val union = Union(id=id,
                parent=if (it == "" && viewModel.isAll.value == false) viewModel.parentID else it,
                indexList=0, type=TYPE_ACTION_TYPE)

            val dialog = ActionTypeDialog()
            dialog.arguments = Bundle().apply{
                putSerializable("actionType", actionType)
                putSerializable("union", union)
                putBoolean("isCreated", true)
            }
            dialog.show(requireActivity().supportFragmentManager, "ActionTypeDialog")
        }

        binding.startTime.editText?.setOnClickListener {
            val hour = actionSchedule.startTime.toInt()/(1000*60*60)
            val minute = (actionSchedule.startTime.toInt()-hour*1000*60*60)/(1000*60)

            val dialog = MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour).setMinute(minute).setTitleText("").build()
            dialog.addOnPositiveButtonClickListener {
                actionSchedule.startTime = (dialog.hour*60+dialog.minute)*1000*60L
                binding.startTime.editText?.setText(
                    formatTime(actionSchedule.startTime, false, FormatStyle.SHORT, FORMAT_TIME))
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }

        binding.endTime.editText?.setOnClickListener {
            val hour = actionSchedule.endTime.toInt()/(1000*60*60)
            val minute = (actionSchedule.endTime.toInt()-hour*1000*60*60)/(1000*60)

            val dialog = MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour).setMinute(minute).setTitleText("").build()
            dialog.addOnPositiveButtonClickListener {
                actionSchedule.endTime = (dialog.hour*60+dialog.minute)*1000*60L
                binding.endTime.editText?.setText(
                    formatTime(actionSchedule.endTime, false, FormatStyle.SHORT, FORMAT_TIME))
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }

        if (isCreated) binding.button.text = resources.getString(R.string.add)
        else binding.button.text = resources.getString(R.string.edit)

        binding.button.setOnClickListener {
            var permission = true
            if (actionSchedule.actionTypeID == "") permission = false
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

