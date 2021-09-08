/*
* Дата создания: 06.09.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.folder

import android.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.databinding.FragmentUnionFolderBinding
import com.daiwerystudio.chronos.ui.union.ID
import com.daiwerystudio.chronos.ui.union.ItemAnimator
import com.daiwerystudio.chronos.ui.union.UnionAbstractFragment
import com.daiwerystudio.chronos.ui.union.UnionPopupMenu

class UnionFolderFragment : UnionAbstractFragment() {
    override val viewModel: UnionFolderViewModel
            by lazy { ViewModelProvider(this).get(UnionFolderViewModel::class.java) }
    private lateinit var binding: FragmentUnionFolderBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentUnionFolderBinding.inflate(inflater, container, false)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter()
            itemAnimator = ItemAnimator()
        }
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        binding.selectTypeShowing.setTypeShowing(viewModel.showing.typeShowing)
        if (viewModel.showing.typeShowing != -1) {
            binding.selectTypeShowing.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            binding.selectTypeShowing.requestLayout()
        }
        binding.selectTypeShowing.setSelectTypeShowingListener{
            binding.loadingView.visibility = View.VISIBLE
            viewModel.showing.setTypeShowing(it)
        }

        binding.toolBar.setOnClickListener {
            if (binding.selectTypeShowing.height == 0)
                binding.selectTypeShowing.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            else binding.selectTypeShowing.layoutParams.height = 0
            binding.selectTypeShowing.requestLayout()
        }

        viewModel.parent.observe(viewLifecycleOwner, {
            binding.folder = it
        })

        viewModel.data.observe(viewLifecycleOwner, {
            (binding.recyclerView.adapter as Adapter).updateData(it)
        })

        binding.fab.setOnClickListener{
            val popup = UnionPopupMenu(requireActivity().supportFragmentManager, requireContext(), it)
            popup.setUnionBuilder(object : UnionPopupMenu.UnionBuilder {
                override fun getParent(): String = viewModel.showing.parentID
            })
            popup.show()
        }

        binding.toolBar.setNavigationOnClickListener {
            it.findNavController().navigateUp()
        }
        binding.toolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.edit -> {
                    val dialog = FolderDialog()
                    dialog.arguments = Bundle().apply{
                        putSerializable("folder", viewModel.parent.value!!)
                        putBoolean("isCreated", false)
                    }
                    dialog.show(requireActivity().supportFragmentManager, "FolderDialog")
                    true
                }
                R.id.delete -> {
                    AlertDialog.Builder(context, R.style.Style_AlertDialog)
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


    private inner class Adapter: UnionAbstractFragment.UnionAdapter() {
        override fun updateData(newData: List<Pair<Int, ID>>) {
            super.updateData(newData)

            if (data.isEmpty()) setEmptyView()
            else setNullView()
        }
    }

}