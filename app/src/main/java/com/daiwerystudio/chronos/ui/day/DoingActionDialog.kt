/*
* Дата создания: 12.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 28.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: изменена логика извлечения типов действий и добавлена взаимодействие с
* SelectActionTypeViewModel.
*/

package com.daiwerystudio.chronos.ui.day

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.lifecycle.ViewModelProvider
import com.daiwerystudio.chronos.database.Action
import com.daiwerystudio.chronos.database.ActionRepository
import com.daiwerystudio.chronos.database.ActionTypeRepository
import com.daiwerystudio.chronos.databinding.DialogDoingActionBinding
import com.daiwerystudio.chronos.ui.DataViewModel
import com.daiwerystudio.chronos.ui.widgets.SelectActionTypeViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.*

class DoingActionDialog : BottomSheetDialogFragment() {
    private val dataViewModel: DataViewModel
            by lazy { ViewModelProvider(this).get(DataViewModel::class.java) }
    private val viewModel: SelectActionTypeViewModel
            by lazy { ViewModelProvider(this).get(SelectActionTypeViewModel::class.java) }
    private val mActionRepository = ActionRepository.get()
    private val mActionTypeRepository = ActionTypeRepository.get()
    private lateinit var binding: DialogDoingActionBinding
    private var startTime = System.currentTimeMillis()
    private var actionTypeID: String = ""
    private val local = TimeZone.getDefault().getOffset(System.currentTimeMillis())

    /*  Интерфейс, который сообщает, какое действие хочет делать пользователь.  */
    private var mAddDoingActionTypeListener: AddDoingActionTypeListener? = null
    fun interface AddDoingActionTypeListener{
        fun addDoingActionType(actionTypeID: String, startTime: Long)
    }
    fun setAddDoingActionTypeListener(addDoingActionTypeListener: AddDoingActionTypeListener){
        mAddDoingActionTypeListener = addDoingActionTypeListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        actionTypeID = arguments?.getString("actionTypeID") as String
        if (dataViewModel.data != null) actionTypeID = dataViewModel.data as String

        // Сперва нужно получить id родителя.
        viewModel.parentID = arguments?.getString("parentID") as String
        if (viewModel.isAll.value == null) viewModel.isAll.value = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DialogDoingActionBinding.inflate(inflater, container, false)
        binding.start = startTime

        // Отмена клавиатуры.
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        viewModel.actionTypes.observe(viewLifecycleOwner, {
            binding.selectActionType.setData(it)
        })
        val actionType = mActionTypeRepository.getActionType(actionTypeID)
        actionType.observe(viewLifecycleOwner, {
            if (it != null && binding.selectActionType.selectedActionType != null)
                binding.selectActionType.setSelectedActionType(it)
        })
        binding.selectActionType.setOnSelectListener{ actionTypeID = it.id }
        binding.selectActionType.setOnEditIsAllListener{ viewModel.isAll.value = it }



        binding.startTime.setOnClickListener{
            val localTime = startTime+local
            val day = localTime/(24*60*60*1000)
            val time = (localTime%(24*60*60*1000)).toInt()
            val hour = time/(60*60*1000)
            val minute = (time-hour*60*60*1000)/(60*1000)

            val dialog = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("")
                .build()

            dialog.addOnPositiveButtonClickListener {
                startTime = day*24*60*60*1000+(dialog.hour*60+dialog.minute)*60*1000-local
                binding.start = startTime
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }

        binding.startDay.setOnClickListener{
            val localTime = startTime+local
            val time = localTime%(24*60*60*1000)

            val dialog = MaterialDatePicker.Builder.datePicker()
                .setSelection(localTime)
                .build()

            dialog.addOnPositiveButtonClickListener {
                startTime = it+time-local
                binding.start = startTime
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }

        binding.button.setOnClickListener {
            var permission = true
            if (startTime > System.currentTimeMillis())
                permission = false

            if (permission){
                mAddDoingActionTypeListener?.addDoingActionType(actionTypeID, startTime)

                this.dismiss()
            }
        }

        return binding.root
    }


    /**
     * Выполняется при уничтожении диалога. Нужно, чтобы сохранить данные, если уничтожение
     * произошло при перевороте устройства.
     */
    override fun onDestroy() {
        super.onDestroy()
        dataViewModel.data = actionTypeID
    }
}