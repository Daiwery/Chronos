package com.daiwerystudio.chronos.ui.timetable

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.daiwerystudio.chronos.R

class ActionsTimetableFragment : Fragment() {
    // ViewModel
    private val viewModel: ActionsTimetableViewModel
    by lazy { ViewModelProvider(this).get(ActionsTimetableViewModel::class.java) }
    // Binding
    // private lateinit var binding: Binding


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_actions_timetable, container, false)
    }


}