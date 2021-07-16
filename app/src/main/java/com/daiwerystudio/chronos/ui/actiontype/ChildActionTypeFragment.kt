package com.daiwerystudio.chronos.ui.actiontype

import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.R
import com.google.android.material.floatingactionbutton.FloatingActionButton


class ChildActionTypeFragment: Fragment() {
    private val childActionTypeViewModel: ChildActionTypeViewModel
    by lazy { ViewModelProviders.of(this).get(ChildActionTypeViewModel::class.java) }
    private lateinit var actionTypeRecyclerView: RecyclerView
    private var actionTypeAdapter: ActionTypeAdapter? = ActionTypeAdapter(emptyList())

    val bundle = Bundle()
    private lateinit var parentActionType: ActionType

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_action_type, container, false)

        // Доступ к меню
        val appCompatActivity = activity as AppCompatActivity
        val appBar = appCompatActivity.supportActionBar
        appBar?.setTitle(parentActionType.name)


        // Настройка RecyclerView
        actionTypeRecyclerView = view.findViewById(R.id.action_type_recycler_view) as RecyclerView
        actionTypeRecyclerView.layoutManager = LinearLayoutManager(context)
        actionTypeRecyclerView.adapter = actionTypeAdapter

        // Настройка кнопки
        val fab = view.findViewById(R.id.fab) as FloatingActionButton
        fab.setOnClickListener{ v: View ->
            bundle.putString("idParentActionType", parentActionType.id.toString())
            v.findNavController().navigate(R.id.action_navigation_child_action_type_to_navigation_item_action_type, bundle)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        childActionTypeViewModel.actionTypes.observe(viewLifecycleOwner, Observer { actionTypes -> updateUI(actionTypes) })
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
            bundle.putSerializable("parentActionType", actionType)
            v.findNavController().navigate(R.id.action_navigation_child_action_type_self, bundle)
        }
    }

    private inner class ActionTypeAdapter(var actionTypes: List<ActionType>): RecyclerView.Adapter<ActionTypeHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionTypeHolder {
            val view = layoutInflater.inflate(R.layout.list_item_action_type, parent, false)
            return ActionTypeHolder(view)
        }

        override fun getItemCount() = actionTypes.size

        override fun onBindViewHolder(holder: ActionTypeHolder, position: Int) {
            val act = actionTypes[position]
            holder.bind(act)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        // Get parentActionType
        parentActionType = arguments?.getSerializable("parentActionType") as ActionType

        // Update actionTypes
        childActionTypeViewModel.getActionTypesFromParent(parentActionType.id.toString())
    }

    override fun onStart() {
        super.onStart()

        bundle.clear()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.fragment_child_action_type, menu)
    }

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
}