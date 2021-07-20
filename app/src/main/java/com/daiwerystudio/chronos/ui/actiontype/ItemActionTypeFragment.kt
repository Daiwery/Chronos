package com.daiwerystudio.chronos.ui.actiontype

import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.databinding.FragmentItemActionTypeBinding
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorShape


class ItemActionTypeFragment : Fragment() {
    private val itemActionTypeViewModel: ItemActionTypeViewModel
    by lazy { ViewModelProvider(this).get(ItemActionTypeViewModel::class.java) }
    // Data Binding
    private lateinit var binding: FragmentItemActionTypeBinding
    // Bundle
    private var parentActionType: ActionType? = null
    private var actionType: ActionType? = null
    // Cringe Logic
    private var isCreate: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Get arguments
        parentActionType = arguments?.getSerializable("parentActionType") as ActionType?
        actionType = arguments?.getSerializable("actionType") as ActionType?
        // Preprocessing
        if (actionType == null) {
            actionType = ActionType()
            if (parentActionType != null) actionType!!.color = parentActionType!!.color

            isCreate = true
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Data Binding
        binding = FragmentItemActionTypeBinding.inflate(inflater, container, false)
        val view = binding.root
        binding.actionType = actionType

        // Menu
        if (!isCreate){
            val appCompatActivity = activity as AppCompatActivity
            val appBar = appCompatActivity.supportActionBar
            appBar?.title = resources.getString(R.string.edit_action_type)
        }

        // Setting fab
        binding.fab.setOnClickListener{ v: View ->
            val name = binding.actionTypeName.text.toString()
            val color = binding.colorPickerView.color
            var parent = ""
            if (parentActionType != null) parent = parentActionType!!.id.toString()

            if (name != ""){
                actionType!!.name = name
                actionType!!.color = color
                actionType!!.parent = parent

                if (isCreate){
                    itemActionTypeViewModel.addActionType(actionType!!)
                } else {
                    itemActionTypeViewModel.updateActionType(actionType!!)
                }
            }

            view.findNavController().popBackStack()
        }

        return view
    }
}