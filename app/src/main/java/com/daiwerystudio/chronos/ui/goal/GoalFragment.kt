package com.daiwerystudio.chronos.ui.goal

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Goal
import com.daiwerystudio.chronos.databinding.FragmentGoalBinding
import com.daiwerystudio.chronos.databinding.ItemProgressGoalBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewAchievedGoalBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewNotAchievedGoalBinding
import com.daiwerystudio.chronos.ui.CustomItemTouchCallback
import com.daiwerystudio.chronos.ui.ItemAnimator
import java.util.*


class GoalFragment : Fragment() {
    // ViewModel
    private val viewModel: GoalViewModel
    by lazy { ViewModelProvider(this).get(GoalViewModel::class.java) }
    // Data Binding
    private lateinit var binding: FragmentGoalBinding
    // Arguments
    private lateinit var idParent: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        // Get arguments
        idParent = arguments?.getString("idParent") as String
        // Get data
        viewModel.getGoalsFromParent(idParent)
        viewModel.getGoal(idParent)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Data Binding
        binding = FragmentGoalBinding.inflate(inflater, container, false)
        val view = binding.root
        setLoadingView()


        // Observe
        viewModel.parentGoal.observe(viewLifecycleOwner, { parentGoal ->
            // Binding
            binding.goal = parentGoal
            // Title
            (activity as AppCompatActivity).supportActionBar?.title = parentGoal.name
        })


        // Setting recyclerView
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter(emptyList())
            itemAnimator = ItemAnimator()
        }
        // Support swipe.
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)


        // Setting progress goal
        binding.progressGoal.apply{
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = ProgressGoalAdapter(emptyList())
        }


        // Observation
        viewModel.goals.observe(viewLifecycleOwner, { goals ->
            setLoadingView()
            // Update data
            (binding.recyclerView.adapter as Adapter).setData(goals)
            (binding.progressGoal.adapter as ProgressGoalAdapter).setData(goals)
        })



        // Setting fab
        binding.fab.setOnClickListener{
            // Dialog
            val dialog = GoalDialog()
            dialog.arguments = Bundle().apply{
                putSerializable("goal", Goal(parent=viewModel.parentGoal.value!!.id,
                    indexList=viewModel.goals.value!!.size))
                putBoolean("isCreated", true)
            }
            dialog.show(this.requireActivity().supportFragmentManager, "GoalTypeDialog")
        }


        // Setting checkBox
        binding.isAchieved.setOnClickListener { v ->
            viewModel.updateListGoals(viewModel.goals.value!!)
            if ((v as CheckBox).isChecked){
                // Dialog
                AlertDialog.Builder(context, R.style.App_AlertDialog)
                    .setTitle(resources.getString(R.string.are_you_sure))
                    .setPositiveButton(R.string.yes) { _, _ ->
                        viewModel.setAchievedGoalWithChild(viewModel.parentGoal.value!!)
                    }
                    .setNegativeButton(R.string.no){ _, _ ->
                        v.isChecked = false
                    }
                    .setCancelable(false)
                    .create()
                    .show()
            } else {
                val goal = viewModel.parentGoal.value!!
                goal.isAchieved = false
                viewModel.updateGoal(goal)
            }
        }


        // Setting goal note
        binding.goalNote.addTextChangedListener {
            val goal = viewModel.parentGoal.value!!
            goal.note = binding.goalNote.text.toString()
            viewModel.updateGoal(goal)
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


    // Set menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.edit_delete_menu, menu)
    }

    // Click on element in menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.edit -> {
                viewModel.updateListGoals(viewModel.goals.value!!)
                // Dialog
                val dialog = GoalDialog()
                dialog.arguments = Bundle().apply{
                    putSerializable("goal", viewModel.parentGoal.value!!)
                    putBoolean("isCreated", false)
                }
                dialog.show(requireActivity().supportFragmentManager, "GoalDialog")
                return true
            }
            R.id.delete -> {
                // Dialog
                AlertDialog.Builder(context, R.style.App_AlertDialog)
                    .setTitle(resources.getString(R.string.are_you_sure))
                    .setPositiveButton(R.string.yes) { _, _ ->
                        viewModel.deleteGoalWithChild(viewModel.parentGoal.value!!)
                        requireActivity().findNavController(R.id.nav_host_fragment).popBackStack()
                    }
                    .setNegativeButton(R.string.no){ _, _ ->
                    }
                    .setCancelable(false)
                    .create()
                    .show()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }


    // Support animation recyclerView
    private class DiffUtilCallback(private val oldList: List<Goal>,
                                   private val newList: List<Goal>): DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return oldList[oldPosition].id == newList[newPosition].id
        }

        override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return oldList[oldPosition] == newList[newPosition]
        }
    }


    private inner class AchievedGoalHolder(private val binding: ItemRecyclerViewAchievedGoalBinding):
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        private lateinit var goal: Goal

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(goal: Goal) {
            this.goal = goal

            binding.goalName.text = this.goal.name
        }

        override fun onClick(v: View) {
            val bundle = Bundle().apply{
                putString("idParent", goal.id)
            }
            v.findNavController().navigate(R.id.action_navigation_goal_self, bundle)
        }
    }

    private inner class NotAchievedGoalHolder(private val binding: ItemRecyclerViewNotAchievedGoalBinding):
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        private lateinit var goal: Goal

        init {
            itemView.setOnClickListener(this)

            // Setting edit
            binding.edit.setOnClickListener{
                viewModel.updateListGoals(viewModel.goals.value!!)
                // Dialog
                val dialog =GoalDialog()
                dialog.arguments = Bundle().apply{
                    putSerializable("goal", goal)
                    putBoolean("isCreated", false)
                }
                dialog.show(requireActivity().supportFragmentManager, "GoalDialog")
            }


            // Setting CheckBox
            binding.checkBox.setOnClickListener { v ->
                viewModel.updateListGoals(viewModel.goals.value!!)
                // Dialog
                AlertDialog.Builder(context, R.style.App_AlertDialog)
                    .setTitle(resources.getString(R.string.are_you_sure))
                    .setPositiveButton(R.string.yes) { _, _ ->
                        viewModel.setAchievedGoalWithChild(goal)
                    }
                    .setNegativeButton(R.string.no){ _, _ ->
                        (v as CheckBox).isChecked = false
                    }
                    .setCancelable(false)
                    .create()
                    .show()
            }
        }

        fun bind(goal: Goal) {
            this.goal = goal

            binding.goalName.text = goal.name
            if (goal.note != ""){
                binding.note.text = goal.note
                binding.note.visibility = View.VISIBLE
            } else {
                binding.note.visibility = View.GONE
            }

            val lifePercent = viewModel.getPercentAchieved(this.goal.id)
            lifePercent.observe(viewLifecycleOwner, { percent ->
                if (percent is Int) binding.progressBar.progress = percent
            })
        }

        override fun onClick(v: View) {
            val bundle = Bundle().apply{
                putString("idParent", goal.id)
            }
            v.findNavController().navigate(R.id.action_navigation_goal_self, bundle)
        }
    }


    private inner class Adapter(var goals: List<Goal>): RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        override fun getItemCount() = goals.size

        // Cringe Logic for animation
        private var lastPosition = -1
        fun setData(newData: List<Goal>){
            // Находим, что изменилось
            val diffUtilCallback = DiffUtilCallback(goals, newData)
            val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)
            // Update data
            goals = newData
            // Notify
            diffResult.dispatchUpdatesTo(this)

            // Show view
            if (goals.isEmpty()){
                setEmptyView()
            } else {
                setNullView()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder{
            return when (viewType){
                Companion.TYPE_ACHIEVED_GOAL -> AchievedGoalHolder(
                    DataBindingUtil.inflate(layoutInflater,
                    R.layout.item_recycler_view_achieved_goal,
                    parent,
                    false))

                Companion.TYPE_NOT_ACHIEVED_GOAL -> NotAchievedGoalHolder(
                    DataBindingUtil.inflate(layoutInflater,
                    R.layout.item_recycler_view_not_achieved_goal,
                    parent,
                    false))
                else -> throw IllegalArgumentException("Invalid view type")
            }
        }

        override fun getItemViewType(position: Int): Int {
            return if (goals[position].isAchieved) Companion.TYPE_ACHIEVED_GOAL
            else Companion.TYPE_NOT_ACHIEVED_GOAL
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val goal = goals[position]
            when (getItemViewType(position)){
                Companion.TYPE_ACHIEVED_GOAL -> (holder as AchievedGoalHolder).bind(goal)
                Companion.TYPE_NOT_ACHIEVED_GOAL -> (holder as NotAchievedGoalHolder).bind(goal)
                else -> throw IllegalArgumentException("Invalid view type")
            }

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
        CustomItemTouchCallback(requireContext(),
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {

        private val mAdapter = binding.recyclerView.adapter!!

        override fun onMove(recyclerView: RecyclerView,
                            viewHolder: RecyclerView.ViewHolder,
                            target: RecyclerView.ViewHolder): Boolean {
            // Yeah, symmetry
            val from = viewHolder.adapterPosition
            val to = target.adapterPosition

            // Update index
            viewModel.goals.value!![from].indexList = to
            viewModel.goals.value!![to].indexList = from

            // Update recyclerView
            Collections.swap(viewModel.goals.value!!, from, to)
            mAdapter.notifyItemMoved(from, to)
            // No symmetry :(
            binding.progressGoal.adapter!!.notifyItemMoved(from, to)

            return true
        }

        override fun onClickPositiveButton(viewHolder: RecyclerView.ViewHolder,
                                           direction: Int) {
            viewModel.deleteGoalWithChild(viewModel.goals.value!![viewHolder.adapterPosition])
        }

        override fun onClickNegativeButton(viewHolder: RecyclerView.ViewHolder,
                                           direction: Int) {
            mAdapter.notifyItemChanged(viewHolder.adapterPosition)
        }
        }

        ItemTouchHelper(simpleItemTouchCallback)
    }


    override fun onPause() {
        viewModel.updateListGoals(viewModel.goals.value!!)

        super.onPause()
    }




    /*               Visualization progress goal              */
    // Support animation recyclerView
    private class CallbackProgressBar(private val oldList: List<Goal>,
                                   private val newList: List<Goal>): DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return oldList[oldPosition].id == newList[newPosition].id
        }

        override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return if (newListSize > oldListSize && newPosition==newListSize-2) false
            else if (newListSize < oldListSize && newPosition==newListSize-1) false
            else oldList[oldPosition] == newList[newPosition]
        }
    }



    private inner class ProgressGoalHolder(private val binding: ItemProgressGoalBinding):
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        private lateinit var goal: Goal

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(goal: Goal, last: Boolean) {
            this.goal = goal

            binding.name.text = goal.name
            binding.point.isActivated = goal.isAchieved
            binding.bridge.isActivated = goal.isAchieved

            if (last) binding.bridge.visibility = View.GONE
            else binding.bridge.visibility = View.VISIBLE
        }

        override fun onClick(v: View) {
            val bundle = Bundle().apply{
                putString("idParent", goal.id)
            }
            v.findNavController().navigate(R.id.action_navigation_goal_self, bundle)
        }
    }

    private inner class ProgressGoalAdapter(var goals: List<Goal>): RecyclerView.Adapter<ProgressGoalHolder>(){
        override fun getItemCount() = goals.size

        // Cringe Logic for animation
        private var lastPosition = -1
        fun setData(newData: List<Goal>){
            // Находим, что изменилось
            val diffUtilCallback = CallbackProgressBar(goals, newData)
            val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)
            // Update data
            goals = newData
            // Notify
            diffResult.dispatchUpdatesTo(this)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgressGoalHolder{
            return ProgressGoalHolder(DataBindingUtil.inflate(layoutInflater,
                    R.layout.item_progress_goal,
                    parent,
                    false))
        }


        override fun onBindViewHolder(holder: ProgressGoalHolder, position: Int) {
            holder.bind(goals[position], position==itemCount-1)

            // Animation
            if (holder.adapterPosition > lastPosition){
                lastPosition = holder.adapterPosition

                val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.fade_in)
                holder.itemView.startAnimation(animation)
            }
        }
    }


    companion object {
        private const val TYPE_ACHIEVED_GOAL = 1
        private const val TYPE_NOT_ACHIEVED_GOAL = 2
    }

}