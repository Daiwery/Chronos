package com.daiwerystudio.chronos.UI.Act

import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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

    private var parentAct: String? = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_act, container, false)

        actRecyclerView = view.findViewById(R.id.act_recyclerView) as RecyclerView
        actRecyclerView.layoutManager = LinearLayoutManager(context)
        actRecyclerView.adapter = actAdapter

        val fab = view.findViewById(R.id.fab) as FloatingActionButton
        fab.setOnClickListener{ v: View ->
            bundle.putString("parentAct", parentAct)
            v.findNavController().navigate(R.id.action_navigation_act_to_navigation_item_act, bundle) }

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
            bundle.putString("parentAct", act.id.toString())
            v.findNavController().navigate(R.id.action_navigation_act_self, bundle)
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

        // Получаем аргументы
        parentAct = arguments?.getString("parentAct")
        if (parentAct == null){
            parentAct = ""
        }

        // Обновляем acts
        actViewModel.getActsFromParent(parentAct!!)
    }

    override fun onStart() {
        super.onStart()

        bundle.clear()
    }
}