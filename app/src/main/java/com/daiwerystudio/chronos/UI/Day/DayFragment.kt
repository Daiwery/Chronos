package com.daiwerystudio.chronos.UI.Day

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.DataBase.Action
import com.daiwerystudio.chronos.DataBase.ActionType
import com.daiwerystudio.chronos.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*


class DayFragment: Fragment() {
    private val actionViewModel: DayViewModel by lazy { ViewModelProviders.of(this).get(DayViewModel::class.java) }
    private lateinit var actionRecyclerView: RecyclerView
    private var actionAdapter: DayFragment.ActionAdapter? = ActionAdapter(emptyList())
    private var currentStartDayTime: Long = (System.currentTimeMillis()/(24*60*60*1000))*(24*60*60*1000)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_day, container, false)

        // Настройка RecyclerView
        actionRecyclerView = view.findViewById(R.id.action_recycler_view) as RecyclerView
        actionRecyclerView.layoutManager = LinearLayoutManager(context)
        actionRecyclerView.adapter = actionAdapter

        // Получем actions на текущем дне и обновляем currentStartDayTime
        currentStartDayTime = (System.currentTimeMillis()/(24*60*60*1000))*(24*60*60*1000)
        actionViewModel.getActionsFromTimes(currentStartDayTime, currentStartDayTime+24*60*60*1000)

        // Настройка кнопки
        val fab = view.findViewById(R.id.fab) as FloatingActionButton
        fab.setOnClickListener{ v: View ->
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        actionViewModel.actions.observe(viewLifecycleOwner, Observer { action -> updateUI(action) })
    }

    private fun updateUI(actions: List<Action>) {
        actionAdapter = ActionAdapter(actions)
        actionRecyclerView.adapter = actionAdapter
    }

    private inner class ActionHolder(view: View): RecyclerView.ViewHolder(view), View.OnClickListener {
        private lateinit var action: Action
        private lateinit var actionType: LiveData<ActionType>

        val nameTextView: TextView = itemView.findViewById(R.id.action_type_name)
        val colorImageView: ImageView = itemView.findViewById(R.id.action_type_color)
        val actionTimeTextView: TextView = itemView.findViewById(R.id.action_time)

        init {
            itemView.setOnClickListener(this)
        }

        @SuppressLint("SimpleDateFormat")
        fun bind(action: Action) {
            this.action = action
            this.actionType = actionViewModel.getActionType(action.idActionType)

            this.actionType.observe(viewLifecycleOwner, Observer { actionType ->
                nameTextView.text = actionType.name
                colorImageView.setColorFilter(actionType.color)
            })

            // Показываем длительность действия
            var start = action.start
            if (start < currentStartDayTime)
                start = currentStartDayTime
            var end = action.end
            if (end > currentStartDayTime+24*60*60*1000)
                end = currentStartDayTime+24*60*60*1000
            this.actionTimeTextView.text = SimpleDateFormat("HH:mm").format(Date(end-start))
        }

        override fun onClick(v: View) {
        }
    }


    private inner class ActionAdapter(var actions: List<Action>): RecyclerView.Adapter<ActionHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionHolder {
            val view = layoutInflater.inflate(R.layout.list_item_action, parent, false)
            return ActionHolder(view)
        }

        override fun getItemCount() = actions.size

        override fun onBindViewHolder(holder: ActionHolder, position: Int) {
            val act = actions[position]


            holder.bind(act)
        }
    }

}