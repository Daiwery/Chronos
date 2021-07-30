package com.daiwerystudio.chronos.ui.actiontype


import android.app.AlertDialog
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.*
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.databinding.FragmentActionTypeBinding
import com.daiwerystudio.chronos.databinding.ListItemActionTypeBinding



class ActionTypeFragment: Fragment() {
    // ViewModel
    private val viewModel: ActionTypeViewModel
    by lazy { ViewModelProvider(this).get(ActionTypeViewModel::class.java) }
    // Data Binding
    private lateinit var binding: FragmentActionTypeBinding



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Data Binding
        binding = FragmentActionTypeBinding.inflate(inflater, container, false)
        val view = binding.root
        // Loading view
        binding.loadingView.visibility = View.VISIBLE

        // Setting recyclerView
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter(emptyList())
            itemAnimator = ItemAnimator()
        }
        // Observation
        viewModel.actionTypes.observe(viewLifecycleOwner, { actionTypes ->
            // Loading view
            binding.loadingView.visibility = View.VISIBLE

            (binding.recyclerView.adapter as Adapter).setData(actionTypes)
        })
        // Support swipe. Смотри ниже.
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        // Setting fab
        binding.fab.setOnClickListener{
            // Dialog
            val dialog = ActionTypeDialog()
            dialog.show(this.requireActivity().supportFragmentManager, "ActionTypeDialog")
        }

        return view
    }




    // Support animation recyclerView
    private class DiffUtilCallback(private val oldList: List<ActionType>,
                                   private val newList: List<ActionType>): DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return oldList[oldPosition].id == newList[newPosition].id
        }

        override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            Log.d("TEST", "areContentsTheSame " +
                    "${oldList[oldPosition]} ${newList[newPosition]}")
            return oldList[oldPosition] == newList[newPosition]
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            Log.d("TEST", "getChangePayload $oldItemPosition")
            return super.getChangePayload(oldItemPosition, newItemPosition)
        }
    }


    // Support animation
    private class ItemAnimator: DefaultItemAnimator(){
        override fun animateAdd(holder: RecyclerView.ViewHolder?): Boolean {
            val itemView = holder!!.itemView

            // Listener
            val listener = object : Animation.AnimationListener{
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    dispatchAnimationFinished(holder)
                }
            }

            // Animation
            val animation = AnimationUtils.loadAnimation(itemView.context, R.anim.anim_add_item)
            animation.setAnimationListener(listener)
            itemView.startAnimation(animation)

            return true
        }
    }


    private inner class Holder(private val binding: ListItemActionTypeBinding):
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        private lateinit var actionType: ActionType

        init {
            itemView.setOnClickListener(this)

            // Setting edit
            binding.edit.setOnClickListener{
                // Dialog
                val dialog = ActionTypeDialog()
                dialog.arguments = Bundle().apply{
                    putSerializable("actionType", actionType)
                }

                dialog.show(requireActivity().supportFragmentManager, "ActionTypeDialog")
            }
        }

        fun bind(actionType: ActionType) {
            this.actionType = actionType
            binding.actionType = actionType

            // Set count child
            val live = viewModel.getCountChild(actionType.id)
            live.observe(viewLifecycleOwner, { count ->
                if (count != 0){
                    binding.textView.visibility = View.VISIBLE
                    binding.countChild.visibility = View.VISIBLE
                    binding.countChild.text = count.toString()
                } else {
                    binding.textView.visibility = View.GONE
                    binding.countChild.visibility = View.GONE
                }
            })

        }

        override fun onClick(v: View) {
            val bundle = Bundle().apply{
                putString("idParent", actionType.id)
            }
            v.findNavController().navigate(R.id.action_navigation_action_type_to_navigation_child_action_type, bundle)
        }
    }


    private inner class Adapter(private var actionTypes: List<ActionType>): RecyclerView.Adapter<Holder>(){
        // Cringe Logic for animation
        private var lastPosition = -1

        fun setData(newData: List<ActionType>){
            // Находим, что изменилось
            val diffUtilCallback = DiffUtilCallback(actionTypes, newData)
            val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)
            // Copy and update data
            actionTypes = newData
            // Notify
            diffResult.dispatchUpdatesTo(this)

            // Show view
            binding.loadingView.visibility = View.GONE
            if (actionTypes.isEmpty()){
                binding.emptyView.visibility = View.VISIBLE
            } else {
                binding.emptyView.visibility = View.GONE
            }

        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return Holder(DataBindingUtil.inflate(layoutInflater,
                R.layout.list_item_action_type,
                parent, false))
        }

        override fun getItemCount() = actionTypes.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            // Bind
            holder.bind(actionTypes[position])

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
        ItemTouchHelper.SimpleCallback(0,
            ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {

        private val mAdapter = binding.recyclerView.adapter!!
        private val icon = ContextCompat.getDrawable(context!!, R.drawable.ic_baseline_delete_24)
        private val background = ColorDrawable(resources.getColor(R.color.red_500))

        override fun onMove(recyclerView: RecyclerView,
                            viewHolder: RecyclerView.ViewHolder,
                            target: RecyclerView.ViewHolder): Boolean {
            return false
        }


        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val actionType = viewModel.actionTypes.value!![viewHolder.adapterPosition]
            // Dialog
            AlertDialog.Builder(context, R.style.App_AlertDialog)
                .setTitle(resources.getString(R.string.delete)+" '"+actionType.name+"'?")
                .setPositiveButton(R.string.yes) { _, _ ->
                    viewModel.deleteActionTypeWithChild(actionType)
                }
                .setNegativeButton(R.string.no){ _, _ ->
                    // Delete
                    mAdapter.notifyItemChanged(viewHolder.adapterPosition)
                }
                .setCancelable(false)
                .create()
                .show()
        }

        override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                                 dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

            val itemView = viewHolder.itemView
            val backgroundCornerOffset = 20

            val iconMargin = (itemView.height - icon!!.intrinsicHeight) / 2
            val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
            val iconBottom = iconTop + icon.intrinsicHeight


            // Swipe left, right and unSwiped
            when {
                dX > 0 -> {
                    val iconRight = itemView.left + iconMargin + icon.intrinsicWidth
                    val iconLeft = itemView.left + iconMargin
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                    background.setBounds(itemView.left, itemView.top,
                        itemView.left + dX.toInt() + backgroundCornerOffset, itemView.bottom)

                }
                dX < 0 -> {
                    val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
                    val iconRight = itemView.right - iconMargin
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                    background.setBounds(itemView.right + dX.toInt() - backgroundCornerOffset,
                        itemView.top, itemView.right, itemView.bottom)

                }
                else -> {
                    background.setBounds(0, 0, 0, 0)
                }
            }

            // Draw
            background.draw(c)
            icon.draw(c)
        }
    }

        ItemTouchHelper(simpleItemTouchCallback)
    }
}

