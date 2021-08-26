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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.databinding.FragmentUnionActionTypeBinding
import com.daiwerystudio.chronos.ui.CustomItemTouchCallback
import com.daiwerystudio.chronos.ui.union.ID
import com.daiwerystudio.chronos.ui.union.ItemAnimator
import com.daiwerystudio.chronos.ui.union.UnionAbstractFragment
import com.daiwerystudio.chronos.ui.union.UnionPopupMenu
import java.util.*

class UnionActionTypeFragment: UnionAbstractFragment() {
    override val viewModel: UnionActionTypeViewModel
        by lazy { ViewModelProvider(this).get(UnionActionTypeViewModel::class.java) }
    private lateinit var binding: FragmentUnionActionTypeBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentUnionActionTypeBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter()
            itemAnimator = ItemAnimator()
        }
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        binding.selectTypeShowing.setTypeShowing(viewModel.showing.typeShowing)
        binding.selectTypeShowing.setSelectTypeShowingListener{
            binding.loadingView.visibility = View.VISIBLE
            viewModel.showing.setTypeShowing(it)
        }

        viewModel.parent.observe(viewLifecycleOwner, {
            binding.actionType = it
        })

        viewModel.data.observe(viewLifecycleOwner, {
            (binding.recyclerView.adapter as Adapter).updateData(it)
        })

        binding.fab.setOnClickListener{
            val popup = UnionPopupMenu(requireActivity().supportFragmentManager, requireContext(), it)
            popup.setUnionBuilder(object : UnionPopupMenu.UnionBuilder {
                override fun getParent(): String = viewModel.showing.parentID
                override fun getIndexList(): Int = viewModel.data.value!!.size
            })
            popup.show()
        }

        binding.toolBar.setNavigationOnClickListener {
            it.findNavController().navigateUp()
        }
        binding.toolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.edit -> {
                    viewModel.updateUnions()
                    val dialog = ActionTypeDialog()
                    dialog.arguments = Bundle().apply{
                        putSerializable("actionType", viewModel.parent.value!!)
                        putBoolean("isCreated", false)
                    }
                    dialog.show(requireActivity().supportFragmentManager, "ActionTypeDialog")
                    true
                }
                R.id.delete -> {
                    AlertDialog.Builder(context, R.style.App_AlertDialog)
                        .setTitle(resources.getString(R.string.are_you_sure))
                        .setPositiveButton(R.string.yes) { _, _ ->
                            viewModel.deleteUnionWithChild(viewModel.showing.parentID)
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


    private val itemTouchHelper by lazy { val simpleItemTouchCallback = object :
        CustomItemTouchCallback(requireContext(),
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {
        private val mAdapter = binding.recyclerView.adapter!! as Adapter

        override fun onMove(recyclerView: RecyclerView,
                            viewHolder: RecyclerView.ViewHolder,
                            target: RecyclerView.ViewHolder): Boolean {
            val from = viewHolder.adapterPosition
            val to = target.adapterPosition

            viewModel.swap(from, to)
            Collections.swap(mAdapter.data, from, to)
            mAdapter.notifyItemMoved(from, to)

            return true
        }

        override fun onClickPositiveButton(viewHolder: RecyclerView.ViewHolder) {
            viewModel.deleteUnionWithChild(mAdapter.data[viewHolder.adapterPosition].second.id)
        }

        override fun onClickNegativeButton(viewHolder: RecyclerView.ViewHolder) {
            mAdapter.notifyItemChanged(viewHolder.adapterPosition)
        }
    }

        ItemTouchHelper(simpleItemTouchCallback)
    }
}

