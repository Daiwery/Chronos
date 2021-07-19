package com.daiwerystudio.chronos.ui.actiontype

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.databinding.FragmentActionTypeBinding
import com.daiwerystudio.chronos.databinding.FragmentChildActionTypeBinding
import com.daiwerystudio.chronos.databinding.ListItemActionTypeBinding
import com.daiwerystudio.chronos.lineItemListActionType



class ChildActionTypeFragment: Fragment() {
    // ViewModel
    private val childActionTypeViewModel: ChildActionTypeViewModel
    by lazy { ViewModelProvider(this).get(ChildActionTypeViewModel::class.java) }
    // Data Binding
    private lateinit var binding: FragmentChildActionTypeBinding
    // Bundle
    val bundle = Bundle()
    private lateinit var parentActionType: ActionType


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        // Get parentActionType and update actionTypes
        parentActionType = arguments?.getSerializable("parentActionType") as ActionType
        childActionTypeViewModel.getActionTypesFromParent(parentActionType.id.toString())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Data Binding
        binding = FragmentChildActionTypeBinding.inflate(inflater, container, false)
        val view = binding.root

        // Set parentActionType
        binding.parentActionType = parentActionType

        // Setting recyclerView
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ActionTypeAdapter(emptyList())
        }

        // Setting fab
        binding.fab.setOnClickListener{ v: View ->
            bundle.putSerializable("parentActionType", parentActionType)
            v.findNavController().navigate(R.id.action_navigation_child_action_type_to_navigation_item_action_type, bundle)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Observation of actionTypes
        childActionTypeViewModel.actionTypes.observe(viewLifecycleOwner, Observer {
                actionTypes -> binding.recyclerView.adapter = ActionTypeAdapter(actionTypes)
        })

        val appCompatActivity = activity as AppCompatActivity
        // Menu
        val appBar = appCompatActivity.supportActionBar
        appBar?.setBackgroundDrawable(ColorDrawable(parentActionType.color))
        // Status bar
        appCompatActivity.window.setStatusBarColor(parentActionType.color)
    }

    override fun onStart() {
        super.onStart()
        bundle.clear()
    }


    override fun onDestroy() {
        super.onDestroy()

        val appCompatActivity = activity as AppCompatActivity
        // Menu
        val appBar = appCompatActivity.supportActionBar
        appBar?.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.main_color)))
        // Status bar
        appCompatActivity.window.setStatusBarColor(resources.getColor(R.color.main_color))
    }

    // Set menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_child_action_type, menu)
    }

    // Click on element in menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.edit_action_type -> {
                // Изменяем текущий тип действия
                bundle.putSerializable("actionType", parentActionType)
                requireActivity().findNavController(R.id.nav_host_fragment)
                    .navigate(R.id.action_navigation_child_action_type_to_navigation_item_action_type, bundle)
                return true
            }
            R.id.delete_action_type -> {
                childActionTypeViewModel.deleteActWithChild(parentActionType)
                requireActivity().findNavController(R.id.nav_host_fragment).popBackStack()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }


    private inner class ActionTypeHolder(private val binding: ListItemActionTypeBinding):
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        private lateinit var actionType: ActionType
        private lateinit var colorsChildActionTypes: LiveData<List<Int>>

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(actionType: ActionType) {
            this.actionType = actionType
            this.colorsChildActionTypes = childActionTypeViewModel.getColorsActionTypesFromParent(actionType.id.toString())

            binding.actionType = this.actionType
            this.colorsChildActionTypes.observe(viewLifecycleOwner, Observer { colorsChildActionTypes ->
                binding.multiColorLineImage.setImageDrawable(lineItemListActionType(colorsChildActionTypes))
            })
        }

        override fun onClick(v: View) {
            bundle.putSerializable("parentActionType", actionType)
            v.findNavController().navigate(R.id.action_navigation_child_action_type_self, bundle)
        }
    }

    private inner class ActionTypeAdapter(var actionTypes: List<ActionType>): RecyclerView.Adapter<ActionTypeHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionTypeHolder {
            val binding = DataBindingUtil.inflate<ListItemActionTypeBinding>(
                layoutInflater,
                R.layout.list_item_action_type,
                parent,
                false)
            return ActionTypeHolder(binding)
        }

        override fun getItemCount() = actionTypes.size

        override fun onBindViewHolder(holder: ActionTypeHolder, position: Int) {
            val act = actionTypes[position]
            holder.bind(act)
        }
    }
}