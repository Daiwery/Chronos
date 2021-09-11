/*
* Дата создания: 17.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 10.09.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: добавлен поиск (фильтр).
*/

package com.daiwerystudio.chronos.ui.union

import android.animation.LayoutTransition
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.daiwerystudio.chronos.databinding.FragmentUnionPreviewBinding

class UnionPreviewFragment : UnionAbstractFragment() {
    override val viewModel: UnionViewModel
        by lazy { ViewModelProvider(this).get(UnionViewModel::class.java) }
    private lateinit var binding: FragmentUnionPreviewBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentUnionPreviewBinding.inflate(inflater, container, false)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter()
            itemAnimator = ItemAnimator()
        }
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        binding.selectFilterType.setTypeShowing(viewModel.information.filterType)
        binding.selectFilterType.setSelectTypeShowingListener {
            binding.loadingView.visibility = View.VISIBLE
            viewModel.information.setFilterType(it)
        }

        binding.search.setText(viewModel.information.filterString)
        binding.search.addTextChangedListener {
            binding.loadingView.visibility = View.VISIBLE
            if ((binding.recyclerView.adapter as Adapter).data.isNotEmpty())
                binding.recyclerView.scrollToPosition(0)
            viewModel.information.setFilterName(it?.toString())
        }

        binding.rootView.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        binding.toolBar.setOnClickListener {
            if (binding.selectFilterType.visibility == View.GONE) {
                binding.selectFilterType.visibility = View.VISIBLE
                binding.imageView9.rotation = 90f
            }
            else {
                binding.selectFilterType.visibility = View.GONE
                binding.imageView9.rotation = 0f
            }
            binding.selectFilterType.requestLayout()
        }

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