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
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.databinding.FragmentUnionPreviewBinding


class UnionPreviewFragment : UnionAbstractFragment() {
    override val viewModel: UnionViewModel
        by lazy { ViewModelProvider(this).get(UnionViewModel::class.java) }
    private lateinit var binding: FragmentUnionPreviewBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentUnionPreviewBinding.inflate(inflater, container, false)
        binding.rootView.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter()
        }
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {}
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) binding.fab.show()
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) binding.fab.hide()
            }
        })

        binding.selectFilterType.setTypeShowing(viewModel.information.filterType)
        binding.selectFilterType.setSelectTypeShowingListener {
            binding.loadingView.visibility = View.VISIBLE
            if ((binding.recyclerView.adapter as Adapter).data.isNotEmpty())
                binding.recyclerView.scrollToPosition(0)
            viewModel.information.setFilterType(it)
        }

        binding.search.setText(viewModel.information.filterString)
        binding.search.addTextChangedListener {
            binding.loadingView.visibility = View.VISIBLE
            if ((binding.recyclerView.adapter as Adapter).data.isNotEmpty())
                binding.recyclerView.scrollToPosition(0)
            viewModel.information.setFilterName(it?.toString())
        }

        binding.toolBar.setOnClickListener {
            if (binding.selectFilterType.visibility == View.VISIBLE) {
                binding.selectFilterType.visibility = View.GONE
                binding.imageView9.rotation = 0f
            }
            else {
                binding.selectFilterType.visibility = View.VISIBLE
                binding.imageView9.rotation = 90f
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
                override fun getIndex(): Int = viewModel.data.value!!.size
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


    override fun notifyAdapterItemsChange(payload: Boolean){
        binding.recyclerView.adapter?.notifyItemRangeChanged(0, viewModel.data.value!!.size, payload)
    }

    private inner class Adapter: UnionAdapter() {
        override fun updateData(newData: List<Pair<Int, ID>>) {
            super.updateData(newData)

            if (data.isEmpty()) setEmptyView()
            else setNullView()
        }
    }
}