package com.daiwerystudio.chronos.ui.timetable

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Timetable
import com.daiwerystudio.chronos.database.TimetableRepository
import com.daiwerystudio.chronos.databinding.DialogTimetableBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class TimetableDialog(var timetable: Timetable, var isCreated: Boolean) : BottomSheetDialogFragment() {
    // Database
    private val timetableRepository = TimetableRepository.get()
    // Data Binding
    private lateinit var binding: DialogTimetableBinding


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Data Binding
        binding = DialogTimetableBinding.inflate(inflater, container, false)
        val view = binding.root
        binding.timetable = timetable

        if (isCreated) {
            binding.button.text = resources.getString(R.string.add)
        } else {
            binding.button.text = resources.getString(R.string.edit)
        }

        // Чтобы был поверх клавиатуры
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        // Setting button
        binding.button.setOnClickListener {
            if (binding.nameEditText.text.toString() != "" && binding.countDaysEditText.text.toString() != "" && binding.presentDayEditText.text.toString() != ""){
                timetable.name = binding.nameEditText.text.toString()
                timetable.countDays = binding.countDaysEditText.text.toString().toInt()
                timetable.dayStart = timetable.dayStart-binding.presentDayEditText.text.toString().toInt()+1

                if (isCreated) {
                    timetableRepository.addTimetable(timetable)
                } else {
                    timetableRepository.updateTimetable(timetable)
                }

                this.dismiss()
            }

        }

        return view
    }

}