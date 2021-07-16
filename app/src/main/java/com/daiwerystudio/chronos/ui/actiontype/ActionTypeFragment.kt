package com.daiwerystudio.chronos.ui.actiontype

import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.databinding.FragmentActionTypeBinding
import com.daiwerystudio.chronos.databinding.ListItemActionTypeBinding


class ActionTypeFragment: Fragment() {
    // ViewModel
    private val actionTypeViewModel: ActionTypeViewModel
    by lazy { ViewModelProvider(this).get(ActionTypeViewModel::class.java) }

//    private lateinit var actionTypeRecyclerView: RecyclerView
//    private var actionTypeAdapter: ActionTypeAdapter? = ActionTypeAdapter(emptyList())

    private lateinit var binding: FragmentActionTypeBinding

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
            bundle.putString("idParentActionType", "")
            v.findNavController().navigate(R.id.action_navigation_action_type_to_navigation_item_action_type, bundle)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        actionTypeViewModel.actionTypes.observe(viewLifecycleOwner, Observer {
                actionTypes -> binding.recyclerView.adapter = ActionTypeAdapter(actionTypes)
        })
    }

    private inner class ActionTypeHolder(private val binding: ListItemActionTypeBinding):
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(actionType: ActionType) {
            binding.actionType = actionType
        }

        override fun onClick(v: View) {
//            bundle.putSerializable("parentActionType", actionType)
//            v.findNavController().navigate(R.id.action_navigation_action_type_to_navigation_child_action_type, bundle)
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

    override fun onStart() {
        super.onStart()

        bundle.clear()
    }
}