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
import com.daiwerystudio.chronos.lineItemListActionType
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.databinding.FragmentActionTypeBinding
import com.daiwerystudio.chronos.databinding.ListItemActionTypeBinding


class ActionTypeFragment: Fragment() {
    // ViewModel
    private val actionTypeViewModel: ActionTypeViewModel
    by lazy { ViewModelProvider(this).get(ActionTypeViewModel::class.java) }
    // Data Binding
    private lateinit var binding: FragmentActionTypeBinding
    // Bundle
    val bundle = Bundle()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Data Binding
        binding = FragmentActionTypeBinding.inflate(inflater, container, false)
        val view = binding.root

        // Setting recyclerView
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ActionTypeAdapter(emptyList())
        }

        // Setting fab
        binding.fab.setOnClickListener{ v: View ->
            v.findNavController().navigate(R.id.action_navigation_action_type_to_navigation_item_action_type, bundle)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Observation of actionTypes
        actionTypeViewModel.actionTypes.observe(viewLifecycleOwner, Observer {
                actionTypes -> binding.recyclerView.adapter = ActionTypeAdapter(actionTypes)
        })

        val appCompatActivity = activity as AppCompatActivity
        // Menu
        val appBar = appCompatActivity.supportActionBar
        appBar?.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.main_color)))
        // Status bar
        appCompatActivity.window.setStatusBarColor(resources.getColor(R.color.main_color))
    }

    override fun onStart() {
        super.onStart()
        bundle.clear()
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
            this.colorsChildActionTypes = actionTypeViewModel.getColorsActionTypesFromParent(actionType.id.toString())

            binding.actionType = this.actionType
            this.colorsChildActionTypes.observe(viewLifecycleOwner, Observer { colorsChildActionTypes ->
                binding.multiColorLineImage.setImageDrawable(lineItemListActionType(colorsChildActionTypes))
            })
        }

        override fun onClick(v: View) {
            bundle.putSerializable("parentActionType", actionType)
            v.findNavController().navigate(R.id.action_navigation_action_type_to_navigation_child_action_type, bundle)
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
            val actionType = actionTypes[position]
            holder.bind(actionType)
        }
    }
}