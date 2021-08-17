/*
* Дата создания: 07.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 17.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: добавлена логика взаимодействия с union.
*/

package com.daiwerystudio.chronos.ui.schedule

import android.icu.util.TimeZone
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Schedule
import com.daiwerystudio.chronos.database.ScheduleRepository
import com.daiwerystudio.chronos.database.Union
import com.daiwerystudio.chronos.database.UnionRepository
import com.daiwerystudio.chronos.databinding.DialogScheduleBinding
import com.daiwerystudio.chronos.ui.DialogViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Ключевой особенностью является тот факт, что диалог всегда получает расписание.
 * Изменения или создание регулируется параметром isCreated. Это необходимо, чтобы
 * разделить UI и функционально необходимые данные, которые будут регулироваться внешне.
 */
class ScheduleDialog : BottomSheetDialogFragment() {
    private val viewModel: DialogViewModel
        by lazy { ViewModelProvider(this).get(DialogViewModel::class.java) }
    private val mScheduleRepository = ScheduleRepository.get()
    private val mUnionRepository = UnionRepository.get()
    private lateinit var binding: DialogScheduleBinding
    private var union: Union? = null

    private lateinit var schedule : Schedule
    private var isCreated: Boolean = true
    private val local = TimeZone.getDefault().getOffset(System.currentTimeMillis())


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
        val view = binding.root
        binding.schedule = schedule

        binding.currentDay.setText((((System.currentTimeMillis()+local)/(1000*60*60*24)-schedule.dayStart)%schedule.countDays+1).toString())

        ArrayAdapter.createFromResource(requireContext(), R.array.types_schedule,
            R.layout.item_spinner).also { adapter ->
            adapter.setDropDownViewResource(R.layout.item_spinner)
            binding.type.adapter = adapter
        }
        binding.type.setSelection(schedule.type)

        binding.type.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?,
                                        position: Int, id: Long) {
                schedule.type = position
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        binding.name.addTextChangedListener{
            schedule.name = binding.name.text.toString()

            if (binding.name.text.toString() != "") binding.errorName.visibility = View.INVISIBLE
            else binding.errorName.visibility = View.VISIBLE
        }

        binding.countDays.addTextChangedListener{
            if (binding.countDays.text.toString() != ""){
                schedule.countDays = binding.countDays.text.toString().toInt()
                binding.errorCountDays.visibility = View.INVISIBLE
            } else binding.errorCountDays.visibility = View.VISIBLE
        }

        binding.currentDay.addTextChangedListener{
            if (binding.currentDay.text.toString() != ""){
                schedule.dayStart = (System.currentTimeMillis()+local)/(1000*60*60*24)-binding.currentDay.text.toString().toInt()+1
                binding.errorCurrentDay.visibility = View.INVISIBLE
            } else binding.errorCurrentDay.visibility = View.VISIBLE
        }

        if (isCreated) binding.button.text = resources.getString(R.string.add)
        else binding.button.text = resources.getString(R.string.edit)
        binding.button.setOnClickListener {
            var permission = true
            if (binding.name.text.toString() == ""){
                permission = false
                binding.errorName.visibility = View.VISIBLE
            }
            if (binding.countDays.text.toString() == ""){
                permission = false
                binding.errorCountDays.visibility = View.VISIBLE
            }
            if (binding.currentDay.text.toString() == ""){
                permission = false
                binding.errorCurrentDay.visibility = View.VISIBLE
            }


            if (permission){
                if (isCreated) {
                    mScheduleRepository.addSchedule(schedule)
                    mUnionRepository.addUnion(union!!)
                }
                else mScheduleRepository.updateSchedule(schedule)

                this.dismiss()
            }
        }

        return view
    }

    override fun onDestroy() {
        super.onDestroy()

        // Сохраняем значение.
        viewModel.data = schedule
    }
}