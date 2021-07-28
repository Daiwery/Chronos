package com.daiwerystudio.chronos.ui.timetable


import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.Observer
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.ActionTimetable
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.database.ActionTypeRepository
import com.daiwerystudio.chronos.database.TimetableRepository
import com.daiwerystudio.chronos.databinding.DialogActionTimetableBinding
import com.daiwerystudio.chronos.databinding.ListItemActionTimetableDialogBinding
import com.daiwerystudio.chronos.databinding.ListItemRecyclerActionTimetableDialogBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*


class ActionTimetableDialog : BottomSheetDialogFragment() {
    // Database
    private val timetableRepository = TimetableRepository.get()
    private val actionTypeRepository = ActionTypeRepository.get()
    // Data Binding
    private lateinit var binding: DialogActionTimetableBinding
    // Array ids parent action types
    private var liveIDs: MutableLiveData<MutableList<String>> = MutableLiveData()
    // Bundle
    private lateinit var action: ActionTimetable
    // Cringe logic
    private var isCreated: Boolean = false


    init {
        liveIDs.value = mutableListOf("")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Arguments
        action = arguments?.getSerializable("action") as ActionTimetable
        // Preprocessing
        if (action.actionTypeId == "") isCreated = true
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Data Binding
        binding = DialogActionTimetableBinding.inflate(inflater, container, false)
        val view = binding.root

        
        // Setting recyclerView
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        liveIDs.observe(viewLifecycleOwner, Observer { ids ->
            binding.recyclerView.adapter = Adapter(ids)
        })
        

        // Setting text view
        binding.startAfterTextView.text = DateTimeFormatter.ofPattern("HH:mm").format(
            LocalTime.of(action.timeStart/60, action.timeStart%60))
        binding.durationTextView.text = DateTimeFormatter.ofPattern("HH:mm").format(
            LocalTime.of(action.duration/60, action.duration%60))


        // Setting TimePickerDialog for timeAfter
        binding.startAfterTextView.setOnClickListener {
            // Listener
            val listener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                action.timeStart = hour*60+minute
                binding.startAfterTextView.text = DateTimeFormatter.ofPattern("HH:mm").format(
                    LocalTime.of(action.timeStart/60, action.timeStart%60))
            }
            TimePickerDialog(context, listener, action.timeStart/60,
                action.timeStart%60, true).show()
        }
        // Setting TimePickerDialog for duration
        binding.durationTextView.setOnClickListener {
            // Listener
            val listener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                action.duration = hour*60 + minute
                binding.durationTextView.text = DateTimeFormatter.ofPattern("HH:mm").format(
                    LocalTime.of(action.duration/60, action.duration%60))
            }
            TimePickerDialog(context, listener, action.duration/60,
                action.duration%60, true).show()
        }


        // Text on the button
        if (isCreated) {
            binding.button.text = resources.getString(R.string.add)
        } else {
            binding.button.text = resources.getString(R.string.edit)
        }

        // Setting button
        binding.button.setOnClickListener {
            if (action.actionTypeId != ""){
                if (isCreated) {
                    timetableRepository.addActionTimetable(action)
                } else {
                    timetableRepository.updateActionTimetable(action)
                }

                this.dismiss()
            }
        }


        return view
    }

    fun updateIds(position: Int, actionType: ActionType){
        // Update
        action.actionTypeId = actionType.id.toString()

        // Сперва удаляем все, что после выбранной строки
        // Незабудь посмотреть прикол с position
        val size = liveIDs.value?.size
        for (i in position+1 until size!!){
            // Delete id
            liveIDs.value?.removeAt(position+1)
            binding.recyclerView.adapter?.notifyItemRemoved(position+1)
        }
        // Add new id
        liveIDs.value?.add(actionType.id.toString())
        binding.recyclerView.adapter?.notifyItemInserted(position+1)
        // Update recyclerView
        binding.recyclerView.scrollToPosition(position+1)
    }


    // Суть в том, что этот recyclerView будет состоять из горизонтального recyclerView, содержащих action types
    private inner class Adapter(var ids: List<String>): RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder{
            return Holder(DataBindingUtil.inflate(layoutInflater,
                R.layout.list_item_recycler_action_timetable_dialog,
                parent, false))
        }

        override fun getItemCount() = ids.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder as Holder).bind(ids[position])
        }

        fun select(position: Int, actionType: ActionType){
            updateIds(position, actionType)
        }


        private inner class Holder(private val binding: ListItemRecyclerActionTimetableDialogBinding): RecyclerView.ViewHolder(binding.root){
            fun bind(id: String) {
                // Initialization recyclerView
                this.binding.recyclerView.apply {
                    layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                    adapter = ActionTypeAdapter(emptyList())
                }

                // Set data
                val live = actionTypeRepository.getActionTypesFromParent(id)
                live.observe(viewLifecycleOwner, Observer { actionTypes ->
                    this.binding.recyclerView.adapter = ActionTypeAdapter(actionTypes)
                })
            }

            fun select(actionType: ActionType){
                this@Adapter.select(adapterPosition, actionType)
            }


            // Infinity inner...
            // Sorry, man
            private inner class ActionTypeAdapter(var actionTypes: List<ActionType>): RecyclerView.Adapter<RecyclerView.ViewHolder>(){
                // This magic logic
                private var selectPosition: Int = -1
                fun select(position: Int, actionType: ActionType){
                    val _select = selectPosition
                    selectPosition = position
                    this.notifyItemChanged(_select)
                    this.notifyItemChanged(selectPosition)

                    // Отправляем выбранный элемент дальше
                    this@Holder.select(actionType)
                }

                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionTypeHolder {
                    return ActionTypeHolder(DataBindingUtil.inflate(layoutInflater,
                        R.layout.list_item_action_timetable_dialog,
                        parent, false))
                }

                override fun getItemCount() = actionTypes.size

                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    (holder as ActionTypeHolder).bind(actionTypes[position])
                    // Frame
                    if (position == selectPosition) {
                        holder.setVisibilityFrame(View.VISIBLE)
                    } else {
                        holder.setVisibilityFrame(View.INVISIBLE)
                    }
                }


                private inner class ActionTypeHolder(private val binding: ListItemActionTimetableDialogBinding):
                    RecyclerView.ViewHolder(binding.root), View.OnClickListener {
                    private lateinit var actionType: ActionType

                    init {
                        itemView.setOnClickListener(this)
                    }

                    fun bind(actionType: ActionType) {
                        this.actionType = actionType
                        binding.actionType = actionType
                    }

                    fun setVisibilityFrame(visibility: Int){
                        binding.frame.visibility = visibility
                    }

                    // Это все ради обработки нажания в классах выше
                    override fun onClick(v: View) {
                        this@ActionTypeAdapter.select(adapterPosition, this.actionType)
                    }
                }
            }
        }

    }
}

