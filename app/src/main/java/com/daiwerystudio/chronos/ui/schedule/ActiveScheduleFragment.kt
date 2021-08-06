package com.daiwerystudio.chronos.ui.schedule

import android.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Switch
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Schedule
import com.daiwerystudio.chronos.databinding.FragmentActiveScheduleBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewActiveScheduleBinding
import com.daiwerystudio.chronos.ui.CustomItemTouchCallback
import com.daiwerystudio.chronos.ui.ItemAnimator


class ActiveScheduleFragment : Fragment() {
    // ViewModel
    private val viewModel: ActiveScheduleViewModel
    by lazy { ViewModelProvider(this).get(ActiveScheduleViewModel::class.java) }
    // Data Binding
    private lateinit var binding: FragmentActiveScheduleBinding


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Data Binding
        binding = FragmentActiveScheduleBinding.inflate(inflater, container, false)
        val view = binding.root
        setLoadingView()

        // Setting recyclerView
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter(emptyList())
            itemAnimator = ItemAnimator()
        }
        // Observation
//        viewModel.schedules.observe(viewLifecycleOwner, { schedules ->
//            setLoadingView()
//            (binding.recyclerView.adapter as Adapter).setData(schedules)
//        })
        // Support swipe.
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)


        // Setting fab
        binding.fab.setOnClickListener{
            // Dialog
            val dialog = ScheduleDialog()
            dialog.arguments = Bundle().apply{
                    putSerializable("schedule", Schedule())
                    putBoolean("isCreated", true)
                }
            dialog.show(requireActivity().supportFragmentManager, "ScheduleDialog")
        }

        return view
    }


    private fun setLoadingView(){
        binding.loadingView.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE
    }
    private fun setEmptyView(){
        binding.loadingView.visibility = View.GONE
        binding.emptyView.visibility = View.VISIBLE
    }
    private fun setNullView(){
        binding.loadingView.visibility = View.GONE
        binding.emptyView.visibility = View.GONE
    }


    // Support animation recyclerView
    private class DiffUtilCallback(private val oldList: List<Schedule>,
                                   private val newList: List<Schedule>): DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return oldList[oldPosition].id == newList[newPosition].id
        }

        override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return oldList[oldPosition] == newList[newPosition]
        }
    }


    private inner class Holder(private val binding: ItemRecyclerViewActiveScheduleBinding):
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        private lateinit var schedule: Schedule

        init {
            itemView.setOnClickListener(this)

            // Setting edit
            binding.edit.setOnClickListener{
                // Dialog
                val dialog = ScheduleDialog()
                dialog.arguments = Bundle().apply{
                        putSerializable("schedule", schedule)
                        putBoolean("isCreated", false)
                    }
                dialog.show(requireActivity().supportFragmentManager, "ScheduleDialog")
            }

            // Setting Switch
            binding.activeSwitch.setOnClickListener { v ->
                // Dialog
                AlertDialog.Builder(context, R.style.App_AlertDialog)
                    .setTitle(resources.getString(R.string.are_you_sure))
                    .setPositiveButton(R.string.yes) { _, _ ->
                        schedule.isActive = (v as Switch).isChecked
                        viewModel.updateSchedules(schedule)
                    }
                    .setNegativeButton(R.string.no){ _, _ ->
                        (v as Switch).isChecked = false
                    }
                    .setCancelable(false)
                    .create()
                    .show()
            }
        }

        fun bind(schedule: Schedule) {
            this.schedule = schedule

            binding.schedule = this.schedule
            val array = resources.getStringArray(R.array.types_schedule)
            binding.type.text = array[schedule.type]
        }

        override fun onClick(v: View) {
            val bundle = Bundle().apply{
                putString("id", schedule.id)
            }
            v.findNavController().navigate(R.id.action_navigation_schedule_to_navigation_schedule, bundle)
        }
    }

    private inner class Adapter(var schedules: List<Schedule>):
        RecyclerView.Adapter<Holder>(){
        override fun getItemCount() = schedules.size

        // Cringe Logic for animation
        private var lastPosition = -1

        fun setData(newData: List<Schedule>){
            // Находим, что изменилось
            val diffUtilCallback = DiffUtilCallback(schedules, newData)
            val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)
            // Update data
            schedules = newData
            // Notify
            diffResult.dispatchUpdatesTo(this)

            // Show view
            if (schedules.isEmpty()){
                setEmptyView()
            } else {
                setNullView()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return Holder(DataBindingUtil.inflate(layoutInflater,
                R.layout.item_recycler_view_active_schedule,
                parent, false))
        }


        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(schedules[position])

            // Animation
            if (holder.adapterPosition > lastPosition){
                lastPosition = holder.adapterPosition

                val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.anim_add_item)
                holder.itemView.startAnimation(animation)
            }
        }
    }

    // Support swiped
    private val itemTouchHelper by lazy { val simpleItemTouchCallback = object :
        CustomItemTouchCallback(requireContext(),0,
            ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {

        private val mAdapter = binding.recyclerView.adapter!!

        override fun onMove(recyclerView: RecyclerView,
                            viewHolder: RecyclerView.ViewHolder,
                            target: RecyclerView.ViewHolder): Boolean {
            return false
        }

        override fun onClickPositiveButton(viewHolder: RecyclerView.ViewHolder) {
            viewModel.deleteScheduleWithActions(viewModel.schedules.value!![viewHolder.adapterPosition])
        }

        override fun onClickNegativeButton(viewHolder: RecyclerView.ViewHolder) {
            mAdapter.notifyItemChanged(viewHolder.adapterPosition)
        }
        }
        ItemTouchHelper(simpleItemTouchCallback)
    }
}