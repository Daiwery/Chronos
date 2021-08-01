package com.daiwerystudio.chronos.ui.goal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.widget.addTextChangedListener
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Goal
import com.daiwerystudio.chronos.database.GoalRepository
import com.daiwerystudio.chronos.databinding.DialogGoalBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class GoalDialog : BottomSheetDialogFragment() {
    // Database
    private val goalRepository = GoalRepository.get()
    // Data Binding
    private lateinit var binding: DialogGoalBinding
    // Arguments
    private var goal: Goal? = null
    private var isCreated: Boolean = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Get arguments
        goal = arguments?.getSerializable("goal") as Goal?
        if (goal != null) goal = goal!!.copy()
        isCreated = arguments?.getBoolean("isCreated") as Boolean
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
            if (binding.goalName.text.toString() != ""){
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
            val name = binding.goalName.text.toString()

            if (name != ""){
                goal!!.name = name

                if (isCreated){
                    goalRepository.addGoal(goal!!)
                } else {
                    goalRepository.updateGoal(goal!!)
                }

                this.dismiss()

            } else {
                binding.error.visibility = View.VISIBLE
            }
        }

        return view
    }

}