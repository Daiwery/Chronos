package com.daiwerystudio.chronos.ui.schedule


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.*
import com.daiwerystudio.chronos.databinding.DialogActionScheduleBinding
import com.daiwerystudio.chronos.ui.DialogViewModel
import com.daiwerystudio.chronos.ui.SelectActionTypeView
import com.daiwerystudio.chronos.ui.action_type.ActionTypeDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter


class ActionScheduleDialog : BottomSheetDialogFragment() {
    // ViewModel
    private val viewModel: DialogViewModel
    by lazy { ViewModelProvider(this).get(DialogViewModel::class.java) }
    // Database
    private val timetableRepository = ScheduleRepository.get()
    // Data Binding
    private lateinit var binding: DialogActionScheduleBinding
    // Arguments
    private lateinit var actionSchedule: ActionSchedule
    private var type: Int = 0
    private var isCreated: Boolean = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Arguments
        actionSchedule = arguments?.getSerializable("actionSchedule") as ActionSchedule
        actionSchedule = actionSchedule.copy()
        type = arguments?.getInt("type") as Int
        isCreated = arguments?.getBoolean("isCreated") as Boolean

        if (viewModel.data != null) actionSchedule = viewModel.data as ActionSchedule
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Data Binding
        binding = DialogActionScheduleBinding.inflate(inflater, container, false)
        val view = binding.root

        
        // Setting SelectActionTypeView
        if (actionSchedule.actionTypeId != "")
            binding.selectActionType.setSelectActionType(actionSchedule.actionTypeId)
        binding.selectActionType.setOnSelectListener(object : SelectActionTypeView.OnSelectListener{
            override fun onSelect(actionType: ActionType){
                actionSchedule.actionTypeId = actionType.id
            }
        })
        binding.selectActionType.setOnClickAddListener(object : SelectActionTypeView.OnClickAddListener{
            override fun onClickAdd(id: String) {
                val dialog = ActionTypeDialog()
                dialog.arguments = Bundle().apply{
                    putSerializable("actionType", ActionType(parent=id))
                    putBoolean("isCreated", true)
                }
                dialog.show(requireActivity().supportFragmentManager, "ActionTypeDialog")
            }
        })


        // Setting text view
        when (type) {
            TYPE_SCHEDULE_RELATIVE -> {
                binding.startAfterTextView.text = DateTimeFormatter.ofPattern("HH:mm").format(
                    LocalTime.ofSecondOfDay(actionSchedule.startAfter%(24*60*60)))
                binding.durationTextView.text = DateTimeFormatter.ofPattern("HH:mm").format(
                    LocalTime.ofSecondOfDay(actionSchedule.duration%(24*60*60)))
            }
            TYPE_SCHEDULE_ABSOLUTE -> {
                binding.startAfterTextView.text = DateTimeFormatter.ofPattern("HH:mm").format(
                    LocalTime.ofSecondOfDay(actionSchedule.startTime%(24*60*60)))
                binding.durationTextView.text = DateTimeFormatter.ofPattern("HH:mm").format(
                    LocalTime.ofSecondOfDay(actionSchedule.endTime%(24*60*60)))
            }
            else -> throw IllegalStateException("Invalid type")
        }


        // Setting TimePickerDialog for startAfter/startTime
        binding.startAfterTextView.setOnClickListener {
            when (type) {
                TYPE_SCHEDULE_RELATIVE -> {
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

                        binding.startAfterTextView.text =
                            DateTimeFormatter.ofPattern("HH:mm").format(
                                LocalTime.ofSecondOfDay(actionSchedule.startAfter)
                            )
                    }

                    dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
                }

                TYPE_SCHEDULE_ABSOLUTE -> {
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

                        binding.startAfterTextView.text =
                            DateTimeFormatter.ofPattern("HH:mm").format(
                                LocalTime.ofSecondOfDay(actionSchedule.startTime)
                            )
                    }

                    dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
                }

                else -> throw IllegalStateException("Invalid type")
            }
        }


        // Setting TimePickerDialog for duration/endTime
        binding.durationTextView.setOnClickListener {
            when (type) {
                TYPE_SCHEDULE_RELATIVE -> {
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

                        binding.startAfterTextView.text =
                            DateTimeFormatter.ofPattern("HH:mm").format(
                                LocalTime.ofSecondOfDay(actionSchedule.duration)
                            )
                    }

                    dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
                }

                TYPE_SCHEDULE_ABSOLUTE -> {
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

                        binding.startAfterTextView.text =
                            DateTimeFormatter.ofPattern("HH:mm").format(
                                LocalTime.ofSecondOfDay(actionSchedule.endTime)
                            )
                    }

                    dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
                }

                else -> throw IllegalStateException("Invalid type")
            }
        }


        // Text
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

        // Text on the button
        if (isCreated) {
            binding.button.text = resources.getString(R.string.add)
        } else {
            binding.button.text = resources.getString(R.string.edit)
        }

        // Setting button
        binding.button.setOnClickListener {
            if (actionSchedule.actionTypeId != ""){
                if (isCreated) {
                    timetableRepository.addActionSchedule(actionSchedule)
                } else {
                    timetableRepository.updateActionSchedule(actionSchedule)
                }

                this.dismiss()
            }
        }


        return view
    }


    override fun onDestroy() {
        super.onDestroy()

        viewModel.data = actionSchedule
    }
}

