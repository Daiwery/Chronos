/*
* Дата создания: 17.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.action_type

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.databinding.FragmentUnionActionTypeBinding
import com.daiwerystudio.chronos.ui.union.ID
import com.daiwerystudio.chronos.ui.union.ItemAnimator
import com.daiwerystudio.chronos.ui.union.UnionAbstractFragment
import com.daiwerystudio.chronos.ui.union.UnionPopupMenu

class UnionActionTypeFragment: UnionAbstractFragment() {
    override val viewModel: UnionActionTypeViewModel
        by lazy { ViewModelProvider(this).get(UnionActionTypeViewModel::class.java) }
    private lateinit var binding: FragmentUnionActionTypeBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentUnionActionTypeBinding.inflate(inflater, container, false)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter()
            itemAnimator = ItemAnimator()
        }
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (binding.fab.isShown && dy > 0) binding.fab.hide()
                if (!binding.fab.isShown && dy < 0) binding.fab.show()
            }
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) binding.fab.show()
            }
        })

        viewModel.parent.observe(viewLifecycleOwner, {
            binding.actionType = it
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
                    val dialog = ActionTypeDialog()
                    dialog.arguments = Bundle().apply{
                        putSerializable("actionType", viewModel.parent.value!!)
                        putBoolean("isCreated", false)
                    }
                    dialog.show(requireActivity().supportFragmentManager, "ActionTypeDialog")
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

