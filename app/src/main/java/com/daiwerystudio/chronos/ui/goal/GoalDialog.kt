/*
* Дата создания: 06.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 17.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: добавлена логика взаимодействия с union.
*/

package com.daiwerystudio.chronos.ui.goal

import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Goal
import com.daiwerystudio.chronos.database.GoalRepository
import com.daiwerystudio.chronos.database.Union
import com.daiwerystudio.chronos.database.UnionRepository
import com.daiwerystudio.chronos.databinding.DialogGoalBinding
import com.daiwerystudio.chronos.ui.DataViewModel
import com.daiwerystudio.chronos.ui.FORMAT_DAY
import com.daiwerystudio.chronos.ui.FORMAT_TIME
import com.daiwerystudio.chronos.ui.formatTime
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.format.FormatStyle
import java.util.*

/**
 * Ключевой особенностью является тот факт, что диалог всегда получает цель.
 * Изменения или создание регулируется параметром isCreated. Это необходимо, чтобы
 * разделить UI и функционально необходимые данные, которые будут регулироваться внешне.
 */
class GoalDialog : BottomSheetDialogFragment() {
    private val viewModel: DataViewModel
        by lazy { ViewModelProvider(this).get(DataViewModel::class.java) }
    private val mGoalRepository = GoalRepository.get()
    private val mUnionRepository = UnionRepository.get()
    private lateinit var binding: DialogGoalBinding

    private lateinit var goal: Goal
    private var isCreated: Boolean = false
    private var union: Union? = null
    private var isTemporal: Boolean? = null

    private val local = TimeZone.getDefault().getOffset(System.currentTimeMillis())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isCreated = arguments?.getBoolean("isCreated") as Boolean
        goal = arguments?.getSerializable("goal") as Goal
        // Необходимо скопировать значение, т.к. передается ссылка.
        // Это нужно, чтобы RecyclerView смог засечь изменение данных и перерисовал holder.
        goal = goal.copy()

        // Значение равно null, если isCreated = false или отправитель не хочет создавать union.
        union = arguments?.getSerializable("union") as Union?

        // Если true, то цель всегда временная.
        isTemporal = arguments?.getBoolean("isTemporal")

        // Восстанавливаем значение, если оно есть.
        if (viewModel.data != null) goal = viewModel.data as Goal
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DialogGoalBinding.inflate(inflater, container, false)
        binding.goal = goal
        binding.day.editText?.setText(formatTime(goal.deadline, true, FormatStyle.LONG, FORMAT_DAY))
        binding.time.editText?.setText(formatTime(goal.deadline, true, FormatStyle.SHORT, FORMAT_TIME))
        if (goal.deadline == 0L) {
            binding.checkBox.isChecked = true
            binding.day.visibility = View.GONE
            binding.time.visibility = View.GONE
        }

        binding.goalName.editText?.doOnTextChanged { text, _, _, _ ->
            goal.name = text.toString()
            if (goal.name == "")  binding.goalName.error = resources.getString(R.string.error_name)
            else binding.goalName.error = null
        }
        binding.goalNote.editText?.doOnTextChanged { text, _, _, _ -> goal.note = text.toString() }
        if (isCreated) binding.goalName.requestFocus()

        binding.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                goal.deadline = 0L
                binding.day.visibility = View.GONE
                binding.time.visibility = View.GONE
            } else {
                goal.deadline = System.currentTimeMillis()
                binding.day.visibility = View.VISIBLE
                binding.time.visibility = View.VISIBLE
            }

            binding.goal = goal
            if (goal.deadline != 0L) {
                binding.day.editText?.setText(formatTime(goal.deadline, true, FormatStyle.LONG, FORMAT_DAY))
                binding.time.editText?.setText(formatTime(goal.deadline, true, FormatStyle.SHORT, FORMAT_TIME))
            }
        }
        if (isTemporal == true) {
            binding.checkBox.isChecked = false  // Срабатывает слушатель, так как он уже установлен.
            binding.checkBox.visibility = View.GONE
        }

        binding.time.editText?.setOnClickListener{
            val localTime = goal.deadline+local
            val day = localTime/(1000*60*60*24)
            val time = (localTime%(1000*60*60*24)).toInt()
            val hour = time/(1000*60*60)
            val minute = (time-hour*1000*60*60)/(1000*60)

            val dialog = MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour).setMinute(minute).setTitleText("").build()
            dialog.addOnPositiveButtonClickListener {
                goal.deadline = day*1000*60*60*24+(dialog.hour*60+dialog.minute)*1000*60-local
                binding.time.editText?.setText(formatTime(goal.deadline, true, FormatStyle.SHORT, FORMAT_TIME))
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }

        binding.day.editText?.setOnClickListener{
            val localTime = goal.deadline+local
            val time = localTime%(1000*60*60*24)

            val dialog = MaterialDatePicker.Builder.datePicker().setSelection(localTime).build()
            dialog.addOnPositiveButtonClickListener {
                goal.deadline = it+time-local
                binding.day.editText?.setText(formatTime(goal.deadline, true, FormatStyle.LONG, FORMAT_DAY))
            }
            dialog.show(activity?.supportFragmentManager!!, "DatePickerDialog")
        }

        if (isCreated) {
            binding.button.text = resources.getString(R.string.add)
            binding.button.setIconResource(R.drawable.ic_baseline_add_24)
        }
        else {
            binding.button.text = resources.getString(R.string.edit)
            binding.button.setIconResource(R.drawable.ic_baseline_edit_24)
        }

        binding.button.setOnClickListener{
            var permission = true
            if (goal.name == "") {
                permission = false
                binding.goalName.error = resources.getString(R.string.error_name)
            } else binding.goalName.error = null

            if (permission){
                if (isCreated) mGoalRepository.addGoal(goal)
                else mGoalRepository.updateGoal(goal)

                if (union != null) mUnionRepository.addUnion(union!!)

                this.dismiss()
            }
        }

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()

        // Сохраняем значение.
        viewModel.data = goal
    }

}