/*
* Дата создания: 11.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 28.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: изменена логика извлечения типов действий и добавлена взаимодействие с
* SelectActionTypeViewModel.
*/

package com.daiwerystudio.chronos.ui.time_tracker

import android.os.Bundle
import android.text.format.DateFormat.is24HourFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.*
import com.daiwerystudio.chronos.databinding.DialogActionBinding
import com.daiwerystudio.chronos.ui.DataViewModel
import com.daiwerystudio.chronos.ui.FORMAT_DAY
import com.daiwerystudio.chronos.ui.FORMAT_TIME
import com.daiwerystudio.chronos.ui.formatTime
import com.daiwerystudio.chronos.ui.SelectActionTypeViewModel
import com.daiwerystudio.chronos.ui.action_type.ActionTypeDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.format.FormatStyle
import java.util.*

class ActionDialog : BottomSheetDialogFragment() {
    private val dataViewModel: DataViewModel
        by lazy { ViewModelProvider(this).get(DataViewModel::class.java) }
    private val viewModel: SelectActionTypeViewModel
        by lazy { ViewModelProvider(this).get(SelectActionTypeViewModel::class.java) }
    private val mActionRepository = ActionRepository.get()
    private lateinit var binding: DialogActionBinding
    private lateinit var action: Action
    private var isCreated: Boolean = false
    private val local = TimeZone.getDefault().getOffset(System.currentTimeMillis())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isCreated = arguments?.getBoolean("isCreated") as Boolean
        action = arguments?.getSerializable("action") as Action
        // Необходимо скопировать значение, т.к. передается ссылка.
        // Это нужно, чтобы RecyclerView смог засечь изменение данных и перерисовал holder.
        action = action.copy()

        if (dataViewModel.data != null) action = dataViewModel.data as Action

        // Сперва нужно получить id родителя.
        viewModel.parentID = ""
        if (viewModel.isAll.value == null) viewModel.isAll.value = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DialogActionBinding.inflate(inflater, container, false)
        binding.action = action
        binding.startDay.editText?.setText(formatTime(
            action.startTime,
            FormatStyle.LONG,
            FORMAT_DAY,
            true,
            is24HourFormat(requireContext())
        ))
        binding.startTime.editText?.setText(formatTime(action.startTime, FormatStyle.SHORT,
            FORMAT_TIME, true, is24HourFormat(requireContext())))
        binding.endDay.editText?.setText(formatTime(action.endTime, FormatStyle.LONG,
            FORMAT_DAY, true, is24HourFormat(requireContext())))
        binding.endTime.editText?.setText(formatTime(action.endTime, FormatStyle.SHORT,
            FORMAT_TIME, true, is24HourFormat(requireContext())))

        binding.selectActionType.setVisibilityIsAll(View.GONE)
        viewModel.actionTypes.observe(viewLifecycleOwner, {
            binding.selectActionType.setData(it)
            binding.selectActionType.setSelectedActionType(action.actionTypeID)
        })
        binding.selectActionType.setOnSelectListener{
            binding.selectActionType.setError(false)
            action.actionTypeID = it.id
        }
        binding.selectActionType.setOnAddListener{
            val id = UUID.randomUUID().toString()
            val actionType = ActionType(id=id)
            val union = Union(id=id, parent=it, indexList=0, type=TYPE_ACTION_TYPE)

            val dialog = ActionTypeDialog()
            dialog.arguments = Bundle().apply{
                putSerializable("actionType", actionType)
                putSerializable("union", union)
                putBoolean("isCreated", true)
            }
            dialog.show(requireActivity().supportFragmentManager, "ActionTypeDialog")
        }


        binding.startTime.editText?.setOnClickListener{
            val localTime = action.startTime+local
            val day = localTime/(24*60*60*1000)
            val time = (localTime%(24*60*60*1000)).toInt()
            val hour = time/(60*60*1000)
            val minute = (time-hour*60*60*1000)/(60*1000)

            val isSystem24Hour = is24HourFormat(requireContext())
            val clockFormat = if (isSystem24Hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H

            val dialog = MaterialTimePicker.Builder().setTimeFormat(clockFormat)
                .setHour(hour).setMinute(minute).setTitleText("").build()
            dialog.addOnPositiveButtonClickListener {
                action.startTime = day*24*60*60*1000+(dialog.hour*60+dialog.minute)*60*1000-local
                binding.startTime.editText?.setText(formatTime(action.startTime, FormatStyle.SHORT,
                    FORMAT_TIME, true, is24HourFormat(requireContext())))
                setErrorTime(action.startTime > action.endTime)
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }

        binding.startDay.editText?.setOnClickListener{
            val localTime = action.startTime+local
            val time = localTime%(24*60*60*1000)

            val dialog = MaterialDatePicker.Builder.datePicker().setSelection(localTime).build()
            dialog.addOnPositiveButtonClickListener {
                action.startTime = it+time-local
                binding.startDay.editText?.setText(formatTime(action.startTime, FormatStyle.LONG,
                    FORMAT_DAY, true, is24HourFormat(requireContext())))
                setErrorTime(action.startTime > action.endTime)
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }

        binding.endTime.editText?.setOnClickListener{
            val localTime = action.endTime+local
            val day = localTime/(24*60*60*1000)
            val time = (localTime%(24*60*60*1000)).toInt()
            val hour = time/(60*60*1000)
            val minute = (time-hour*60*60*1000)/(60*1000)

            val isSystem24Hour = is24HourFormat(requireContext())
            val clockFormat = if (isSystem24Hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H

            val dialog = MaterialTimePicker.Builder().setTimeFormat(clockFormat)
                .setHour(hour).setMinute(minute).setTitleText("").build()
            dialog.addOnPositiveButtonClickListener {
                action.endTime = day*24*60*60*1000+(dialog.hour*60+dialog.minute)*60*1000-local
                binding.endTime.editText?.setText(formatTime(action.endTime, FormatStyle.SHORT,
                    FORMAT_TIME, true, is24HourFormat(requireContext())))
                setErrorTime(action.startTime > action.endTime)
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }

        binding.endDay.editText?.setOnClickListener{
            val localTime = action.endTime+local
            val time = localTime%(24*60*60*1000)

            val dialog = MaterialDatePicker.Builder.datePicker().setSelection(localTime).build()
            dialog.addOnPositiveButtonClickListener {
                action.endTime = it+time-local
                binding.endDay.editText?.setText(formatTime(action.endTime,
                    FormatStyle.LONG, FORMAT_DAY, true, is24HourFormat(requireContext())))
                setErrorTime(action.startTime > action.endTime)
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }

        if (isCreated) {
            binding.button.text = resources.getString(R.string.add)
            binding.button.setIconResource(R.drawable.ic_baseline_add_24)
        }
        else {
            binding.button.text = resources.getString(R.string.edit)
            binding.button.setIconResource(R.drawable.ic_baseline_edit_24)
        }

        binding.button.setOnClickListener {
            var permission = true
            if (action.actionTypeID == "") {
                permission = false
                binding.selectActionType.setError(true)
            }
            if (action.startTime > action.endTime) {
                permission = false
                setErrorTime(true)
            } else setErrorTime(false)


            if (permission){
                if (isCreated) mActionRepository.addAction(action)
                else mActionRepository.updateAction(action)

                this.dismiss()
            }
        }

        return binding.root
    }

    private fun setErrorTime(error: Boolean){
        if (error) {
            binding.endTime.error = " "
            binding.endDay.error = " "
        }
        else {
            binding.endTime.error = null
            binding.endDay.error = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dataViewModel.data = action
    }
}