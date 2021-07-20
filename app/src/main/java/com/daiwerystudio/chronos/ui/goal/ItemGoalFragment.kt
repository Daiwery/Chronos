package com.daiwerystudio.chronos.ui.goal

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Goal
import com.daiwerystudio.chronos.databinding.FragmentItemGoalBinding


class ItemGoalFragment : Fragment() {
    // ViewModel
    private val viewModel: ItemGoalViewModel
    by lazy { ViewModelProvider(this).get(ItemGoalViewModel::class.java) }
    // Data Binding
    private lateinit var binding: FragmentItemGoalBinding
    // Bundle
    private var goal: Goal? = null
    private var parentGoal: Goal? = null
    // Cringe Logic
    private var isCreate: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Get arguments
        goal = arguments?.getSerializable("goal") as Goal?
        parentGoal = arguments?.getSerializable("parentGoal") as Goal?
        // Preprocessing
        if (goal == null) {
            goal = Goal()
            isCreate = true
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Data Binding
        binding = FragmentItemGoalBinding.inflate(inflater, container, false)
        val view = binding.root
        binding.goal = goal

        // Menu
        if (!isCreate){
            val appCompatActivity = activity as AppCompatActivity
            val appBar = appCompatActivity.supportActionBar
            appBar?.title = resources.getString(R.string.edit_goal)
        }

        // Setting fab
        binding.fab.setOnClickListener{ v: View ->
            val name = binding.goalName.text.toString()
            val note = binding.goalNote.text.toString()
            var parent = ""
            if (parentGoal != null) parent = parentGoal!!.id.toString()

            if (name != ""){
                goal!!.name = name
                goal!!.parent = parent
                goal!!.note = note

                if (isCreate){
                    viewModel.addGoal(goal!!)
                } else {
                    viewModel.updateGoal(goal!!)
                }
            }

            view.findNavController().popBackStack()
        }

        return view
    }

}