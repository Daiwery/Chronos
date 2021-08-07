/*
* Дата создания: 07.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Schedule
import com.daiwerystudio.chronos.database.ScheduleRepository
import com.daiwerystudio.chronos.databinding.DialogScheduleBinding
import com.daiwerystudio.chronos.ui.DialogViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


/** Представляет из себя диалог в виде нижней панели. По логике абсолютно такое же, как и
 * ActionTypeDialog. За тем исключение, что работает с Schedule.
 *
 * Возможная модификация: вместо встроенного spinner-а использовать spinner из материального
 * дизайна.
 *
 * Возможная модификация: вместо InputText использовать компоненты материального дизайна, чтобы
 * вместо ручного создания иконки "error" делать это лаконичнее и легче. Не использование встроенного
 * setError обусловлено тем, что оно некрасивое.
 */
class ScheduleDialog : BottomSheetDialogFragment() {
    /**
     * ViewModel.
     */
    private val viewModel: DialogViewModel
    by lazy { ViewModelProvider(this).get(DialogViewModel::class.java) }
    /**
     * Репозиторий для взаимодействия с базой данных. Данные из базы данных не извлекаются,
     * поэтому помещать его в ViewModel нет смысла.
     */
    private val repository = ScheduleRepository.get()
    /**
     * Привязка данных.
     */
    private lateinit var binding: DialogScheduleBinding
    /**
     * Расписание, которое получает диалог из Bundle.
     */
    private lateinit var schedule : Schedule
    /**
     * Определяет, создается или изменяется ли диалог. Диалог получает его из Bundle.
     */
    private var isCreated: Boolean = true

    /**
     * Выполняет перед созданиес интефейса. Получает данные из Bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isCreated = arguments?.getBoolean("isCreated") as Boolean
        schedule = arguments?.getSerializable("schedule") as Schedule
        // Необходимо скопировать значение, т.к. передается ссылка.
        // Это нужно, чтобы RecyclerView смог засечь изменение данных и перерисовал holder.
        schedule = schedule.copy()

        if (viewModel.data != null) schedule = viewModel.data as  Schedule
    }

    /**
     * Создание UI и его настройка.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DialogScheduleBinding.inflate(inflater, container, false)
        val view = binding.root
        binding.schedule = schedule


        ArrayAdapter.createFromResource(requireContext(), R.array.types_schedule,
            R.layout.item_spinner).also { adapter ->
            adapter.setDropDownViewResource(R.layout.item_spinner)
            binding.type.adapter = adapter
        }
        binding.type.setSelection(schedule.type)


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
                schedule.dayStart = System.currentTimeMillis()/(1000*60*60*24)-binding.currentDay.text.toString().toInt()+1
                binding.errorCurrentDay.visibility = View.INVISIBLE
            } else binding.errorCurrentDay.visibility = View.VISIBLE
        }


        binding.type.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?,
                                        position: Int, id: Long) {
                schedule.type = position
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
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
                if (isCreated) repository.addSchedule(schedule)
                else repository.updateSchedule(schedule)

                this.dismiss()
            }
        }

        return view
    }

    /**
     * Выполняется при уничтожении диалога. Нужно, чтобы сохранить данные, если уничтожение
     * произошло при перевороте устройства.
     */
    override fun onDestroy() {
        super.onDestroy()
        viewModel.data = schedule
    }
}