package com.daiwerystudio.chronos.UI.Act

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.DataBase.Act
import com.daiwerystudio.chronos.R
import com.google.android.material.floatingactionbutton.FloatingActionButton


class ActFragment: Fragment() {
    private val actViewModel: ActViewModel by lazy { ViewModelProviders.of(this).get(ActViewModel::class.java) }
    private lateinit var actRecyclerView: RecyclerView
    private var actAdapter: ActAdapter? = ActAdapter(emptyList())
    val bundle = Bundle()

    private var parentAct: Act? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_act, container, false)

        // Доступ к меню
        val appCompatActivity = activity as AppCompatActivity
        val appBar = appCompatActivity.supportActionBar
        if (parentAct != null){
            appBar?.setTitle(parentAct!!.name)
        }

        // Настройка RecyclerView
        actRecyclerView = view.findViewById(R.id.act_recyclerView) as RecyclerView
        actRecyclerView.layoutManager = LinearLayoutManager(context)
        actRecyclerView.adapter = actAdapter

        // Настройка кнопки
        val fab = view.findViewById(R.id.fab) as FloatingActionButton
        fab.setOnClickListener{ v: View ->
            // Этот класс ипользуется одновременно для act и actChild
            if (parentAct == null){
                bundle.putString("idParentAct", "")
                v.findNavController().navigate(R.id.action_navigation_act_to_navigation_item_act, bundle)
            } else {
                bundle.putString("idParentAct", parentAct!!.id.toString())
                v.findNavController().navigate(R.id.action_navigation_child_act_to_navigation_item_act, bundle)
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        actViewModel.acts.observe(viewLifecycleOwner, Observer { acts -> updateUI(acts) })
    }

    private fun updateUI(acts: List<Act>) {
        actAdapter = ActAdapter(acts)
        actRecyclerView.adapter = actAdapter
    }


    private inner class ActHolder(view: View): RecyclerView.ViewHolder(view), View.OnClickListener {
        private lateinit var act: Act

        val nameTextView: TextView = itemView.findViewById(R.id.act_name)
        val colorImageView: ImageView = itemView.findViewById(R.id.act_color)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(act: Act) {
            this.act = act
            nameTextView.text = this.act.name
            colorImageView.setColorFilter(act.color)
        }

        override fun onClick(v: View) {
            // Этот класс ипользуется одновременно для act и actChild
            bundle.putSerializable("parentAct", act)
            if (parentAct == null){
                v.findNavController().navigate(R.id.action_navigation_act_to_navigation_child_act, bundle)
            } else {
                v.findNavController().navigate(R.id.action_navigation_child_act_self, bundle)
            }
        }
    }

    private inner class ActAdapter(var acts: List<Act>): RecyclerView.Adapter<ActHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActHolder {
            val view = layoutInflater.inflate(R.layout.list_item_act, parent, false)
            return ActHolder(view)
        }

        override fun getItemCount() = acts.size

        override fun onBindViewHolder(holder: ActHolder, position: Int) {
            val act = acts[position]
            holder.bind(act)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        // Get parentAct
        parentAct = arguments?.getSerializable("parentAct") as Act?

        // Update acts
        if (parentAct == null){
            actViewModel.getActsFromParent("")
        } else {
            actViewModel.getActsFromParent(parentAct!!.id.toString())
        }
    }

    override fun onStart() {
        super.onStart()

        bundle.clear()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        if (parentAct != null)
            inflater.inflate(R.menu.fragment_act, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.edit_act -> {
                // Изменяем текущее действие
                bundle.putSerializable("act", parentAct)
                requireActivity().findNavController(R.id.nav_host_fragment)
                    .navigate(R.id.action_navigation_child_act_to_navigation_item_act, bundle)
                return true
            }
            R.id.delete_act -> {
                actViewModel.deleteActWithChild(parentAct!!)
                requireActivity().findNavController(R.id.nav_host_fragment).popBackStack()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }
}