/*
* Дата создания: 17.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 24.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: добавлена логика взаимодействия с типом расписания.
*/

package com.daiwerystudio.chronos.ui.schedule

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.databinding.FragmentUnionScheduleBinding
import com.daiwerystudio.chronos.ui.union.ID
import com.daiwerystudio.chronos.ui.union.ItemAnimator
import com.daiwerystudio.chronos.ui.union.UnionAbstractFragment
import com.daiwerystudio.chronos.ui.union.UnionPopupMenu

class UnionScheduleFragment : UnionAbstractFragment() {
    override val viewModel: UnionScheduleViewModel
        by lazy { ViewModelProvider(this).get(UnionScheduleViewModel::class.java) }
    private lateinit var binding: FragmentUnionScheduleBinding


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentUnionScheduleBinding.inflate(inflater, container, false)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter()
            itemAnimator = ItemAnimator()
        }
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        viewModel.parent.observe(viewLifecycleOwner, {
            binding.toolBar.title = it.name
        })

        viewModel.data.observe(viewLifecycleOwner, {
            (binding.recyclerView.adapter as Adapter).updateData(it)
        })

        binding.fab.setOnClickListener{
            val popup = UnionPopupMenu(requireActivity().supportFragmentManager, requireContext(), it)
            popup.setUnionBuilder(object : UnionPopupMenu.UnionBuilder {
                override fun getParent(): String = viewModel.information.parentID
            })
            popup.show()
        }

        binding.toolBar.setNavigationOnClickListener {
            it.findNavController().navigateUp()
        }
        binding.toolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.edit -> {
                    val bundle = Bundle().apply {
                        putString("scheduleID", viewModel.information.parentID)
                    }
                    this.findNavController().navigate(R.id.action_global_navigation_schedule, bundle)
                   true
                }
                R.id.delete -> {
                    AlertDialog.Builder(context, R.style.Style_AlertDialog)
                        .setTitle(resources.getString(R.string.are_you_sure))
                        .setPositiveButton(R.string.yes) { _, _ ->
                            viewModel.deleteUnionWithChild(viewModel.information.parentID)
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

        return binding.root
    }

    private fun setEmptyView(){
        binding.loadingView.visibility = View.GONE
        binding.emptyView.visibility = View.VISIBLE
    }

    private fun setNullView(){
        binding.loadingView.visibility = View.GONE
        binding.emptyView.visibility = View.GONE
    }


    private inner class Adapter: UnionAdapter() {
        override fun updateData(newData: List<Pair<Int, ID>>) {
            super.updateData(newData)

            if (data.isEmpty()) setEmptyView()
            else setNullView()
        }
    }
}