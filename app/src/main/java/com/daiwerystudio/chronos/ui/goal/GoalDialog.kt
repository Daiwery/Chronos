package com.daiwerystudio.chronos.ui.goal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Goal
import com.daiwerystudio.chronos.database.GoalRepository
import com.daiwerystudio.chronos.databinding.DialogGoalBinding
import com.daiwerystudio.chronos.ui.DialogViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class GoalDialog : BottomSheetDialogFragment() {
    // ViewModel
    private val viewModel: DialogViewModel
    by lazy { ViewModelProvider(this).get(DialogViewModel::class.java) }
    // Database
    private val goalRepository = GoalRepository.get()
    // Data Binding
    private lateinit var binding: DialogGoalBinding
    // Arguments
    private lateinit var goal: Goal
    var isCreated: Boolean = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Get arguments
        goal = arguments?.getSerializable("goal") as Goal
        goal = goal.copy()
        isCreated = arguments?.getBoolean("isCreated") as Boolean

        // Recovery
        if (viewModel.data != null) goal = viewModel.data as Goal
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Data Binding
        binding = DialogGoalBinding.inflate(inflater, container, false)
        val view = binding.root
        binding.goal = goal


        // Чтобы был поверх клавиатуры
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)


        binding.goalName.addTextChangedListener{
            goal.name = binding.goalName.text.toString()
            if (goal.name != ""){
                binding.error.visibility = View.INVISIBLE
            } else {
                binding.error.visibility = View.VISIBLE
            }
        }


        // Text on button
        if (isCreated) {
            binding.button.text = resources.getString(R.string.add)
        } else {
            binding.button.text = resources.getString(R.string.edit)
        }


        // Setting button
        binding.button.setOnClickListener{
            if (goal.name != ""){
                if (isCreated){
                    goalRepository.addGoal(goal)
                } else {
                    goalRepository.updateGoal(goal)
                }

                this.dismiss()

            } else {
                binding.error.visibility = View.VISIBLE
            }
        }

        return view
    }

    override fun onDestroy() {
        super.onDestroy()

        viewModel.data = goal
    }

}