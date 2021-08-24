/*
* Дата создания: 23.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.schedule

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.databinding.FragmentOnceScheduleBinding


class OnceScheduleFragment : Fragment() {
    private val viewModel: ScheduleViewModel
            by lazy { ViewModelProvider(this).get(ScheduleViewModel::class.java) }
    private lateinit var binding: FragmentOnceScheduleBinding

    // Если true, то можно создать фрагмент дня, если нет, то нельзя.
    private var permission: Boolean = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.scheduleID.value = arguments?.getString("scheduleID")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentOnceScheduleBinding.inflate(inflater, container, false)

        viewModel.schedule.observe(viewLifecycleOwner, {
            binding.appBar.title = it.name
        })
        viewModel.daysScheduleIDs.observe(viewLifecycleOwner, { // Длина массива равна 1.
            if (permission) {
                val fragment = DayScheduleFragment()
                fragment.arguments = Bundle().apply {
                    putString("dayScheduleID", it.first())
                }

                requireActivity().supportFragmentManager.beginTransaction()
                    .add(R.id.fragmentContainerView, fragment).commit()

                permission = false
            }
        })


        binding.appBar.setNavigationOnClickListener {
            it.findNavController().navigateUp()
        }
        binding.appBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.edit -> {
                    val dialog = ScheduleDialog()
                    dialog.arguments = Bundle().apply{
                        putSerializable("schedule", viewModel.schedule.value!!)
                        putBoolean("isCreated", false)
                    }
                    dialog.show(requireActivity().supportFragmentManager, "GoalDialog")
                    true
                }
                R.id.delete -> {
                    AlertDialog.Builder(context, R.style.App_AlertDialog)
                        .setTitle(resources.getString(R.string.are_you_sure))
                        .setPositiveButton(R.string.yes) { _, _ ->
                            requireActivity().findNavController(R.id.nav_host_fragment).popBackStack()
                            viewModel.deleteUnionWithChild(viewModel.scheduleID.value!!)
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

        return binding.root
    }
}