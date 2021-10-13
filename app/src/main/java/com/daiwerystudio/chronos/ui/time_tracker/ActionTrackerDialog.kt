/*
* Дата создания: 07.10.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.time_tracker

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.daiwerystudio.chronos.MainActivity
import com.daiwerystudio.chronos.MainActivity.Companion.PREFERENCES_TRACKING_ACTION_TYPE_ID
import com.daiwerystudio.chronos.MainActivity.Companion.PREFERENCES_TRACKING_START_TIME
import com.daiwerystudio.chronos.database.*
import com.daiwerystudio.chronos.databinding.DialogActionTrackerBinding
import com.daiwerystudio.chronos.ui.*
import com.daiwerystudio.chronos.ui.action_type.ActionTypeDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.format.FormatStyle
import java.util.*

class ActionTrackerDialog : BottomSheetDialogFragment() {
    private val dataViewModel: DataViewModel
        by lazy { ViewModelProvider(this).get(DataViewModel::class.java) }
    private val viewModel: SelectActionTypeViewModel
        by lazy { ViewModelProvider(this).get(SelectActionTypeViewModel::class.java) }
    private val mActionRepository = ActionRepository.get()
    private lateinit var binding: DialogActionTrackerBinding
    private val local = TimeZone.getDefault().getOffset(System.currentTimeMillis())

    /**
     * Переменная для связи с локальным файлом настроек. Нужна для хранения
     * времени начала выполняемого действия и типа действия.
     */
    private lateinit var preferences: SharedPreferences
    private var trackingActionTypeID: String = ""
    private var trackingStartTime: Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferences = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)

        if (dataViewModel.data != null) {
            trackingActionTypeID = (dataViewModel.data as Pair<*, *>).first as String
            trackingStartTime = (dataViewModel.data as Pair<*, *>).second as Long
        }

        // Сперва нужно получить id родителя.
        viewModel.parentID = ""
        if (viewModel.isAll.value == null) viewModel.isAll.value = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DialogActionTrackerBinding.inflate(inflater, container, false)
        binding.startDay.editText?.setText(formatTime(trackingStartTime, true, FormatStyle.LONG, FORMAT_DAY))
        binding.startTime.editText?.setText(formatTime(trackingStartTime, true, FormatStyle.SHORT, FORMAT_TIME))


        binding.selectActionType.setVisibilityIsAll(View.GONE)
        viewModel.actionTypes.observe(viewLifecycleOwner, {
            binding.selectActionType.setData(it)
        })
        binding.selectActionType.setOnSelectListener{ trackingActionTypeID = it.id }
        binding.selectActionType.setOnAddListener{
            val id = UUID.randomUUID().toString()
            val actionType = ActionType(id=id)
            val union = Union(id=id, parent=it, indexList=0, type= TYPE_ACTION_TYPE)

            val dialog = ActionTypeDialog()
            dialog.arguments = Bundle().apply{
                putSerializable("actionType", actionType)
                putSerializable("union", union)
                putBoolean("isCreated", true)
            }
            dialog.show(requireActivity().supportFragmentManager, "ActionTypeDialog")
        }


        binding.startTime.editText?.setOnClickListener{
            val localTime = trackingStartTime+local
            val day = localTime/(24*60*60*1000)
            val time = (localTime%(24*60*60*1000)).toInt()
            val hour = time/(60*60*1000)
            val minute = (time-hour*60*60*1000)/(60*1000)

            val dialog = MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour).setMinute(minute).setTitleText("").build()
            dialog.addOnPositiveButtonClickListener {
                trackingStartTime = day*24*60*60*1000+(dialog.hour*60+dialog.minute)*60*1000-local
                binding.startTime.editText?.setText(formatTime(trackingStartTime, true, FormatStyle.SHORT, FORMAT_TIME))
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }

        binding.startDay.editText?.setOnClickListener{
            val localTime = trackingStartTime+local
            val time = localTime%(24*60*60*1000)

            val dialog = MaterialDatePicker.Builder.datePicker().setSelection(localTime).build()
            dialog.addOnPositiveButtonClickListener {
                trackingStartTime = it+time-local
                binding.startDay.editText?.setText(formatTime(trackingStartTime, true, FormatStyle.LONG, FORMAT_DAY))
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }

        binding.button.setOnClickListener {
            var permission = true
            if (trackingActionTypeID == "") permission = false
            if (trackingStartTime > System.currentTimeMillis()) permission = false

            if (permission){
                // Если какое-то действие сейчас выполняется, то добавляем его.
                if (preferences.getString(PREFERENCES_TRACKING_ACTION_TYPE_ID , "") != "")
                    if (System.currentTimeMillis() - preferences.getLong(PREFERENCES_TRACKING_START_TIME, 0) > 0){
                        val action = Action()
                        action.actionTypeID = preferences.getString(PREFERENCES_TRACKING_ACTION_TYPE_ID , "")!!
                        action.startTime = preferences.getLong(PREFERENCES_TRACKING_START_TIME, 0)
                        action.endTime = System.currentTimeMillis()

                        mActionRepository.addAction(action)
                    }

                val editor = preferences.edit()
                editor.putLong(PREFERENCES_TRACKING_START_TIME, trackingStartTime).apply()
                editor.putString(PREFERENCES_TRACKING_ACTION_TYPE_ID, trackingActionTypeID).apply()
                (requireActivity() as MainActivity).showActionTracking()

                this.dismiss()
            }
        }

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        dataViewModel.data = Pair(trackingActionTypeID, trackingStartTime)
    }
}