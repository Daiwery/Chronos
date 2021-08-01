package com.daiwerystudio.chronos.ui.day

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.R


class ItemActionFragment : Fragment() {
    private val itemActionViewModel: ItemActionViewModel by lazy { ViewModelProviders.of(this).get(
        ItemActionViewModel::class.java) }
    private lateinit var actionTypeRecyclerView: RecyclerView
    private var actionTypeAdapter: ItemActionFragment.ActionTypeAdapter? = ActionTypeAdapter(emptyList())

    private var parentActionTypesList: List<String> = listOf("")
    private lateinit var actionTypeTextView: TextView


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_item_action, container, false)

        // Настройка RecyclerView
        actionTypeRecyclerView = view.findViewById(R.id.action_type_recycler_view) as RecyclerView
        actionTypeRecyclerView.layoutManager = LinearLayoutManager(context)
        actionTypeRecyclerView.adapter = actionTypeAdapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        itemActionViewModel.actionTypes.observe(viewLifecycleOwner, Observer { actionTypes -> updateUI(actionTypes) })
    }

    private fun updateUI(actionsType: List<ActionType>) {
        actionTypeAdapter = ActionTypeAdapter(actionsType)
        actionTypeRecyclerView.adapter = actionTypeAdapter
    }

    private inner class ActionTypeHolder(view: View): RecyclerView.ViewHolder(view), View.OnClickListener {
        private lateinit var actionType: ActionType

        val nameTextView: TextView = itemView.findViewById(R.id.action_type_name)
        val colorImageView: ImageView = itemView.findViewById(R.id.action_type_color)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(actionType: ActionType) {
            this.actionType = actionType
            nameTextView.text = this.actionType.name
            colorImageView.setColorFilter(actionType.color)
        }

        override fun onClick(v: View) {

        }
    }

    private inner class ActionTypeAdapter(var actionTypes: List<ActionType>): RecyclerView.Adapter<ActionTypeHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionTypeHolder {
            val view = layoutInflater.inflate(R.layout.item_recycler_view_action_type, parent, false)
            return ActionTypeHolder(view)
        }

        override fun getItemCount() = actionTypes.size

        override fun onBindViewHolder(holder: ActionTypeHolder, position: Int) {
            val act = actionTypes[position]
            holder.bind(act)
        }
    }

}