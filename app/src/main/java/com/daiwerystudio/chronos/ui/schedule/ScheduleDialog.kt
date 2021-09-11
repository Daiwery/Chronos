/*
* Дата создания: 07.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 17.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: добавлена логика взаимодействия с union.
*
* Дата изменения: 24.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: добавлена логика взаимодействия с типом расписания.
*/

package com.daiwerystudio.chronos.ui.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.*
import com.daiwerystudio.chronos.databinding.DialogScheduleBinding
import com.daiwerystudio.chronos.ui.DataViewModel
import com.daiwerystudio.chronos.ui.FORMAT_DAY
import com.daiwerystudio.chronos.ui.formatTime
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.format.FormatStyle

/**
 * Ключевой особенностью является тот факт, что диалог всегда получает расписание.
 * Изменения или создание регулируется параметром isCreated. Это необходимо, чтобы
 * разделить UI и функционально необходимые данные, которые будут регулироваться внешне.
 * Этот диалог используется для создания как periodic, так и once расписаний.
 */
class ScheduleDialog : BottomSheetDialogFragment() {
    private val viewModel: DataViewModel
        by lazy { ViewModelProvider(this).get(DataViewModel::class.java) }
    private val mScheduleRepository = ScheduleRepository.get()
    private val mUnionRepository = UnionRepository.get()
    private lateinit var binding: DialogScheduleBinding
    private var union: Union? = null

    private lateinit var schedule : Schedule
    private var isCreated: Boolean = true
    private val local = java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isCreated = arguments?.getBoolean("isCreated") as Boolean
        schedule = arguments?.getSerializable("schedule") as Schedule
        // Необходимо скопировать значение, т.к. передается ссылка.
        // Это нужно, чтобы RecyclerView смог засечь изменение данных и перерисовал holder.
        schedule = schedule.copy()

        // Значение равно null только при isCreated = false.
        union = arguments?.getSerializable("union") as Union?

        // Восстанавливаем значение, если оно есть.
        if (viewModel.data != null) schedule = viewModel.data as  Schedule
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DialogScheduleBinding.inflate(inflater, container, false)
        binding.schedule = schedule
        binding.scheduleStart.editText?.setText(formatTime(schedule.start, true, FormatStyle.LONG, FORMAT_DAY))

        binding.scheduleName.editText?.doOnTextChanged { text, _, _, _ ->
            schedule.name = text.toString()
            if (schedule.name == "") binding.scheduleName.error = resources.getString(R.string.error_name)
            else binding.scheduleName.error = null
        }

        binding.scheduleCountDays.editText?.doOnTextChanged { text, _, _, _ ->
            if (text.toString() != "") {
                schedule.countDays = text.toString().toInt()
                binding.scheduleCountDays.error = null
            } else binding.scheduleCountDays.error = " "
        }

        binding.scheduleStart.editText?.setOnClickListener {
            val dialog = MaterialDatePicker.Builder.datePicker().setSelection(schedule.start+local).build()
            dialog.addOnPositiveButtonClickListener {
                schedule.start = it-local
                binding.scheduleStart.editText?.setText(formatTime(schedule.start, true, FormatStyle.LONG, FORMAT_DAY))
            }
            dialog.show(activity?.supportFragmentManager!!, "DatePickerDialog")
        }

        if (isCreated) binding.button.text = resources.getString(R.string.add)
        else binding.button.text = resources.getString(R.string.edit)
        binding.button.setOnClickListener {
            var permission = true
            if (schedule.name == ""){
                permission = false
                if (schedule.name == "") binding.scheduleName.error = resources.getString(R.string.error_name)
                else binding.scheduleName.error = null
            }
            if (isCreated && schedule.type == TYPE_SCHEDULE_PERIODIC)
                if (binding.scheduleCountDays.editText?.text.toString() == ""){
                    permission = false
                    binding.scheduleCountDays.error = " "
                } else binding.scheduleCountDays.error = null

            if (permission){
                if (isCreated) {
                    mScheduleRepository.addSchedule(schedule)
                    mUnionRepository.addUnion(union!!)
                    this.dismiss()

                    // Если расписание создается, то перемещаем пользователя в редактирование.
                    val bundle = Bundle().apply {
                        putString("scheduleID", schedule.id)
                    }
                    this.findNavController().navigate(R.id.action_global_navigation_schedule, bundle)
                }
                else {
                    mScheduleRepository.updateSchedule(schedule)
                    this.dismiss()
                }
            }
        }

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()

        // Сохраняем значение.
        viewModel.data = schedule
    }
}