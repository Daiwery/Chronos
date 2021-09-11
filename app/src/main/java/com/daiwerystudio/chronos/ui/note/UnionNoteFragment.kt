/*
* Дата создания: 21.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.note

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.databinding.FragmentUnionNoteBinding
import com.daiwerystudio.chronos.ui.union.ID
import com.daiwerystudio.chronos.ui.union.ItemAnimator
import com.daiwerystudio.chronos.ui.union.UnionAbstractFragment
import com.daiwerystudio.chronos.ui.union.UnionPopupMenu

class UnionNoteFragment : UnionAbstractFragment() {
    override val viewModel: UnionNoteViewModel
        by lazy { ViewModelProvider(this).get(UnionNoteViewModel::class.java) }
    private lateinit var binding: FragmentUnionNoteBinding


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentUnionNoteBinding.inflate(inflater, container, false)

        viewModel.parent.observe(viewLifecycleOwner, {
            binding.note = it
        })

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
                    val bundle = Bundle().apply{
                        putSerializable("note", viewModel.parent.value!!)
                    }
                    requireActivity().findNavController(R.id.nav_host_fragment).navigate(R.id.action_global_navigation_note, bundle)
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