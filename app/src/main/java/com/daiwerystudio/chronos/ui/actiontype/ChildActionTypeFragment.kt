package com.daiwerystudio.chronos.ui.actiontype

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.databinding.FragmentChildActionTypeBinding
import com.daiwerystudio.chronos.databinding.ItemRecyclerViewActionTypeBinding
import com.daiwerystudio.chronos.ui.CustomItemTouchCallback
import com.daiwerystudio.chronos.ui.ItemAnimator


class ChildActionTypeFragment: Fragment() {
    // ViewModel
    private val viewModel: ChildActionTypeViewModel
    by lazy { ViewModelProvider(this).get(ChildActionTypeViewModel::class.java) }
    // Data Binding
    private lateinit var binding: FragmentChildActionTypeBinding
    // Arguments
    private lateinit var idParent: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        // Get arguments
        idParent = arguments?.getString("idParent") as String
        // Get data
        viewModel.getActionTypesFromParent(idParent)
        viewModel.getActionType(idParent)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Data Binding
        binding = FragmentChildActionTypeBinding.inflate(inflater, container, false)
        val view = binding.root
        setLoadingView()

        // Observe
        viewModel.parentActionType.observe(viewLifecycleOwner, { actionType ->
            // Binding
            binding.parentActionType = actionType
            // Title
            (activity as AppCompatActivity).supportActionBar?.title = actionType.name
        })


        // Setting recyclerView
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter(emptyList())
            itemAnimator = ItemAnimator()
        }
        // Observation
        viewModel.actionTypes.observe(viewLifecycleOwner, { actionTypes ->
            setLoadingView()
            (binding.recyclerView.adapter as Adapter).setData(actionTypes)
        })
        // Support swipe.
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)


        // Setting fab
        binding.fab.setOnClickListener{
            // Dialog
            val dialog = ActionTypeDialog()
            dialog.arguments = Bundle().apply{
                putSerializable("parentActionType", viewModel.parentActionType.value!!)
            }
            dialog.show(this.requireActivity().supportFragmentManager, "ActionTypeDialog")
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
                // Dialog
                val dialog = ActionTypeDialog()
                dialog.arguments = Bundle().apply{
                    putSerializable("actionType", viewModel.parentActionType.value!!)
                }
                dialog.show(this.requireActivity().supportFragmentManager, "ActionTypeDialog")

                return true
            }
            R.id.delete -> {
                // Dialog
                AlertDialog.Builder(context, R.style.App_AlertDialog)
                    .setTitle(resources.getString(R.string.are_you_sure))
                    .setPositiveButton(R.string.yes) { _, _ ->
                        viewModel.deleteActionTypeWithChild(viewModel.parentActionType.value!!)
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
    class DiffUtilCallback(private val oldList: List<ActionType>,
                           private val newList: List<ActionType>): DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return oldList[oldPosition].id == newList[newPosition].id
        }

        override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            return oldList[oldPosition] == newList[newPosition]
        }
    }



    private inner class Holder(private val binding: ItemRecyclerViewActionTypeBinding):
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
            binding.actionType = this.actionType

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
            v.findNavController().navigate(R.id.action_navigation_child_action_type_self, bundle)
        }
    }

    private inner class Adapter(var actionTypes: List<ActionType>): RecyclerView.Adapter<Holder>(){
        // Cringe Logic for animation
        private var lastPosition = -1

        fun setData(newData: List<ActionType>){
            // Находим, что изменилось
            val diffUtilCallback = DiffUtilCallback(actionTypes, newData)
            val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)
            // Update data
            actionTypes = newData
            // Notify
            diffResult.dispatchUpdatesTo(this)

            // Show view
            if (actionTypes.isEmpty()){
                setEmptyView()
            } else {
                setNullView()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return Holder(DataBindingUtil.inflate(layoutInflater,
                R.layout.item_recycler_view_action_type,
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
        CustomItemTouchCallback(requireContext(),0,
            ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {

        private val mAdapter = binding.recyclerView.adapter!!

        override fun onMove(recyclerView: RecyclerView,
                            viewHolder: RecyclerView.ViewHolder,
                            target: RecyclerView.ViewHolder): Boolean {
            return false
        }

        override fun onClickPositiveButton(viewHolder: RecyclerView.ViewHolder,
                                           direction: Int) {
            viewModel.deleteActionTypeWithChild(viewModel.actionTypes.value!![viewHolder.adapterPosition])
        }

        override fun onClickNegativeButton(viewHolder: RecyclerView.ViewHolder,
                                           direction: Int) {
            mAdapter.notifyItemChanged(viewHolder.adapterPosition)
        }
        }
        ItemTouchHelper(simpleItemTouchCallback)
    }
}