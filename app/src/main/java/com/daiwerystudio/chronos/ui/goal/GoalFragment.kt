/*
* Дата создания: 19.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.goal

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.databinding.FragmentGoalBinding

class GoalFragment : Fragment()  {
    private val viewModel: GoalViewModel
            by lazy { ViewModelProvider(this).get(GoalViewModel::class.java) }
    private lateinit var binding: FragmentGoalBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.goalID.value = arguments?.getString("goalID")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentGoalBinding.inflate(inflater, container, false)
        val view = binding.root

        viewModel.goal.observe(viewLifecycleOwner, {
            binding.goal = it
            binding.appBar.title = it.name
        })

        binding.isAchieved.setOnClickListener {
            viewModel.goal.value?.isAchieved = binding.isAchieved.isChecked
        }

        binding.note.addTextChangedListener {
            viewModel.goal.value?.note = it.toString()
        }

        binding.appBar.setNavigationOnClickListener {
            it.findNavController().navigateUp()
        }
        binding.appBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.edit -> {
                    viewModel.updateGoal()
                    val dialog = GoalDialog()
                    dialog.arguments = Bundle().apply{
                        putSerializable("goal", viewModel.goal.value!!)
                        putBoolean("isCreated", false)
                    }
                    dialog.show(requireActivity().supportFragmentManager, "GoalDialog")
                    true
                }
                R.id.delete -> {
                    AlertDialog.Builder(context, R.style.App_AlertDialog)
                        .setTitle(resources.getString(R.string.are_you_sure))
                        .setPositiveButton(R.string.yes) { _, _ ->
                            viewModel.deleteUnionWithChild(viewModel.goalID.value!!)
                            requireActivity().findNavController(R.id.nav_host_fragment).popBackStack()
                        }
                        .setNegativeButton(R.string.no){ _, _ -> }
                        .setCancelable(false)
                        .create()
                        .show()
                    true
                }
                else -> false
            }
        }

        return view
    }

    override fun onPause() {
        super.onPause()

        viewModel.updateGoal()
    }
}