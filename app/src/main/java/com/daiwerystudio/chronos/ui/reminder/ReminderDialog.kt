/*
* Дата создания: 21.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.reminder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Reminder
import com.daiwerystudio.chronos.database.ReminderRepository
import com.daiwerystudio.chronos.database.Union
import com.daiwerystudio.chronos.database.UnionRepository
import com.daiwerystudio.chronos.databinding.DialogReminderBinding
import com.daiwerystudio.chronos.ui.DataViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.*


class ReminderDialog : BottomSheetDialogFragment() {
    private val viewModel: DataViewModel
            by lazy { ViewModelProvider(this).get(DataViewModel::class.java) }
    private val mReminderRepository = ReminderRepository.get()
    private val mUnionRepository = UnionRepository.get()
    private lateinit var binding: DialogReminderBinding

    private lateinit var reminder: Reminder
    var isCreated: Boolean = false
    private var union: Union? = null

    private val local = TimeZone.getDefault().getOffset(System.currentTimeMillis())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isCreated = arguments?.getBoolean("isCreated") as Boolean
        reminder = arguments?.getSerializable("reminder") as Reminder
        // Необходимо скопировать значение, т.к. передается ссылка.
        // Это нужно, чтобы RecyclerView смог засечь изменение данных и перерисовал holder.
        reminder = reminder.copy()

        // Значение равно null только при isCreated = false.
        union = arguments?.getSerializable("union") as Union?

        // Восстанавливаем значение, если оно есть.
        if (viewModel.data != null) reminder = viewModel.data as Reminder
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DialogReminderBinding.inflate(inflater, container, false)
        binding.reminder = reminder

        binding.text.addTextChangedListener{
            reminder.text = it.toString()
            if (reminder.text != "") binding.error.visibility = View.INVISIBLE
            else binding.error.visibility = View.VISIBLE
        }

        binding.time.setOnClickListener{
            val localTime = reminder.time+local
            val day = localTime/(1000*60*60*24)
            val time = (localTime%(1000*60*60*24)).toInt()
            val hour = time/(1000*60*60)
            val minute = (time-hour*1000*60*60)/(1000*60)

            val dialog = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("")
                .build()

            dialog.addOnPositiveButtonClickListener {
                reminder.time = day*1000*60*60*24+(dialog.hour*60+dialog.minute)*1000*60-local
                binding.reminder = reminder
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }

        binding.day.setOnClickListener{
            val localTime = reminder.time+local
            val time = localTime%(1000*60*60*24)

            val dialog = MaterialDatePicker.Builder.datePicker()
                .setSelection(localTime)
                .build()

            dialog.addOnPositiveButtonClickListener {
                reminder.time = it+time-local
                binding.reminder = reminder
            }
            dialog.show(activity?.supportFragmentManager!!, "DatePickerDialog")
        }

        if (isCreated) binding.button.text = resources.getString(R.string.add)
        else binding.button.text = resources.getString(R.string.edit)
        binding.button.setOnClickListener{
            var permission = true
            if (reminder.text == "") permission = false
            if (reminder.time < System.currentTimeMillis()) permission = false

            if (permission) {
                if (isCreated) {
                    mReminderRepository.addReminder(reminder)
                    mUnionRepository.addUnion(union!!)
                } else mReminderRepository.updateReminder(reminder)

                this.dismiss()
            }
        }

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()

        // Сохраняем значение.
        viewModel.data = reminder
    }
}