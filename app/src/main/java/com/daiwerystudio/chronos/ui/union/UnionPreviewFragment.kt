/*
* Дата создания: 17.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 10.09.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: добавлен поиск (фильтр).
*/

package com.daiwerystudio.chronos.ui.union

import android.animation.ObjectAnimator
import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.core.view.doOnPreDraw
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.databinding.FragmentUnionPreviewBinding
import com.daiwerystudio.chronos.ui.widgets.UnionFabMenu
import com.google.android.material.bottomnavigation.BottomNavigationView

class UnionPreviewFragment : UnionAbstractFragment() {
    override val viewModel: UnionViewModel
        by lazy { ViewModelProvider(this).get(UnionViewModel::class.java) }
    private lateinit var binding: FragmentUnionPreviewBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentUnionPreviewBinding.inflate(inflater, container, false)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = UnionAdapter()
            itemAnimator = UnionItemAnimator()
        }
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {}
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (actionMode == null && viewModel.information.filterType == null
                    && viewModel.information.filterString == null) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING)
                        if (binding.fab.state != UnionFabMenu.STATE_INVISIBLE) binding.fab.hide()
                        else binding.fab.show()
                }
            }
        })

        binding.selectFilterType.setTypeShowing(viewModel.information.filterType)
        binding.selectFilterType.setSelectTypeShowingListener {
            binding.loadingView.visibility = View.VISIBLE
            if ((binding.recyclerView.adapter as UnionAdapter).data.isNotEmpty())
                binding.recyclerView.scrollToPosition(0)
            viewModel.information.setFilterType(it)
        }

        binding.search.setText(viewModel.information.filterString)
        binding.search.addTextChangedListener {
            binding.loadingView.visibility = View.VISIBLE
            if ((binding.recyclerView.adapter as UnionAdapter).data.isNotEmpty())
                binding.recyclerView.scrollToPosition(0)
            if (binding.search.isFocused)
                viewModel.information.setFilterName(it?.toString())
        }
        binding.search.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.toolBar.isClickable = false
                setNullView()
                binding.search.setText("")
                viewModel.information.setFilterName("")

                binding.fab.hide()
                requireActivity().findViewById<BottomNavigationView>(R.id.nav_view).visibility = View.GONE

                TransitionManager.beginDelayedTransition(binding.rootView)
                binding.selectFilterType.visibility = View.VISIBLE
                ObjectAnimator.ofFloat(binding.imageView9, "rotation",  90f)
                    .setDuration(300).apply { interpolator = OvershootInterpolator() }.start()
            }
            else {
                binding.toolBar.isClickable = true
                setLoadingView()
                binding.search.setText("")
                viewModel.information.setFilterName(null)

                binding.fab.show()
                requireActivity().findViewById<BottomNavigationView>(R.id.nav_view).visibility = View.VISIBLE
                if (viewModel.information.filterType == null){
                    TransitionManager.beginDelayedTransition(binding.rootView)
                    binding.selectFilterType.visibility = View.GONE
                    ObjectAnimator.ofFloat(binding.imageView9, "rotation",  0f)
                        .setDuration(300).apply { interpolator = OvershootInterpolator() }.start()
                }
            }
        }

        binding.toolBar.setOnClickListener {
            TransitionManager.beginDelayedTransition(binding.rootView)
            if (binding.selectFilterType.visibility == View.VISIBLE) {
                binding.selectFilterType.visibility = View.GONE
                binding.fab.show()
                ObjectAnimator.ofFloat(binding.imageView9, "rotation",  0f)
                    .setDuration(300).apply { interpolator = OvershootInterpolator() }.start()
            }
            else {
                binding.selectFilterType.visibility = View.VISIBLE
                binding.fab.hide()
                ObjectAnimator.ofFloat(binding.imageView9, "rotation",  90f)
                    .setDuration(300).apply { interpolator = OvershootInterpolator() }.start()
            }
        }

        viewModel.data.observe(viewLifecycleOwner, {
            (binding.recyclerView.adapter as UnionAdapter).updateData(it)
        })

        binding.fab.setOnMenuItemClickListener{ createUnionItem(it) }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.search.isFocused || binding.fab.state == UnionFabMenu.STATE_OPENED) {
                    binding.search.clearFocus()
                    binding.fab.close()
                } else {
                    isEnabled = false
                    activity?.onBackPressed()
                }
            }
        })

        return binding.root
    }

    override fun setEmptyView(){
        binding.loadingView.visibility = View.GONE
        binding.emptyView.visibility = View.VISIBLE
    }

    override fun setNullView(){
        binding.loadingView.visibility = View.GONE
        binding.emptyView.visibility = View.GONE
    }

    private fun setLoadingView(){
        binding.loadingView.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE
    }

    override fun hideFab(){
        binding.fab.hide()
    }

    override fun showFab(){
        binding.fab.show()
    }

    override fun notifyAdapterItemsChange(payload: Boolean){
        binding.recyclerView.adapter?.notifyItemRangeChanged(0, viewModel.data.value!!.size, payload)
    }
}