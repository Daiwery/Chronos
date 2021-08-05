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



class ScheduleDialog : BottomSheetDialogFragment() {
    // ViewModel
    private val viewModel: DialogViewModel
    by lazy { ViewModelProvider(this).get(DialogViewModel::class.java) }
    // Database
    private val repository = ScheduleRepository.get()
    // Data Binding
    private lateinit var binding: DialogScheduleBinding
    // Arguments
    private lateinit var schedule : Schedule
    private var isCreated: Boolean = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Get arguments
        schedule = arguments?.getSerializable("schedule") as Schedule
        schedule = schedule.copy()
        isCreated = arguments?.getBoolean("isCreated") as Boolean

        // Recovery
        if (viewModel.data != null) schedule = viewModel.data as  Schedule
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Data Binding
        binding = DialogScheduleBinding.inflate(inflater, container, false)
        val view = binding.root
        binding.schedule = schedule


        // Чтобы был поверх клавиатуры
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)


        // Setting spinner
        ArrayAdapter.createFromResource(requireContext(), R.array.types_schedule,
            R.layout.item_spinner).also { adapter ->
            adapter.setDropDownViewResource(R.layout.item_spinner)
            binding.type.adapter = adapter
        }
        binding.type.setSelection(schedule.type)


        binding.name.addTextChangedListener{
            schedule.name = binding.name.text.toString()
            if (binding.name.text.toString() != ""){
                binding.errorName.visibility = View.INVISIBLE
            } else {
                binding.errorName.visibility = View.VISIBLE
            }
        }
        binding.countDays.addTextChangedListener{
            if (binding.countDays.text.toString() != ""){
                schedule.countDays = binding.countDays.text.toString().toInt()
                binding.errorCountDays.visibility = View.INVISIBLE
            } else {
                binding.errorCountDays.visibility = View.VISIBLE
            }
        }
        binding.currentDay.addTextChangedListener{
            if (binding.currentDay.text.toString() != ""){
                schedule.dayStart = System.currentTimeMillis()/(1000*60*60*24)-binding.currentDay.text.toString().toInt()+1
                binding.errorCurrentDay.visibility = View.INVISIBLE
            } else {
                binding.errorCurrentDay.visibility = View.VISIBLE
            }
        }
        binding.type.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?,
                                        position: Int, id: Long) {
                schedule.type = position
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }


        // Text on button
        if (isCreated) {
            binding.button.text = resources.getString(R.string.add)
        } else {
            binding.button.text = resources.getString(R.string.edit)
        }
        // Setting button
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
                    repository.addSchedule(schedule)
                } else {
                    repository.updateSchedule(schedule)
                }

                this.dismiss()
            }

        }

        return view
    }

    override fun onDestroy() {
        super.onDestroy()

        viewModel.data = schedule
    }

}