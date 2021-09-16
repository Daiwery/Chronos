/*
* Дата создания: 16.09.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.calendar

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.databinding.FragmentCalendarBinding

class CalendarFragment : Fragment() {
    private lateinit var binding: FragmentCalendarBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }
}