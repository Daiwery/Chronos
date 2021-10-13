/*
* Дата создания: 06.09.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.folder

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.databinding.FragmentUnionFolderBinding
import com.daiwerystudio.chronos.ui.union.UnionAbstractFragment
import com.daiwerystudio.chronos.ui.union.UnionItemAnimator
import com.daiwerystudio.chronos.ui.widgets.UnionFabMenu
import com.google.android.material.transition.MaterialContainerTransform

class UnionFolderFragment : UnionAbstractFragment() {
    override val viewModel: UnionFolderViewModel
            by lazy { ViewModelProvider(this).get(UnionFolderViewModel::class.java) }
    private lateinit var binding: FragmentUnionFolderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.nav_host_fragment
            duration = 750
            scrimColor = Color.TRANSPARENT
            fadeMode = MaterialContainerTransform.FADE_MODE_OUT
        }
        sharedElementReturnTransition = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentUnionFolderBinding.inflate(inflater, container, false)
        binding.root.transitionName = viewModel.information.parentID

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = UnionAdapter()
            itemAnimator = UnionItemAnimator()
        }
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {}
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING)
                    if (binding.fab.state != UnionFabMenu.STATE_INVISIBLE) binding.fab.hide()
                    else binding.fab.show()
            }
        })

        viewModel.parent.observe(viewLifecycleOwner, {
            binding.folder = it
        })

        viewModel.data.observe(viewLifecycleOwner, {
            (binding.recyclerView.adapter as UnionAdapter).updateData(it)
        })

        binding.fab.setOnMenuItemClickListener{ createUnionItem(it) }

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

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.fab.isFocused) binding.fab.clearFocus()
                    else {
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