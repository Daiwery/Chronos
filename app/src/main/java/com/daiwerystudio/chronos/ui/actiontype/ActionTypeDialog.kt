package com.daiwerystudio.chronos.ui.actiontype


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.database.ActionTypeRepository
import com.daiwerystudio.chronos.databinding.DialogActionTypeBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener


class ActionTypeDialog : BottomSheetDialogFragment() {
    // Database
    private val repository = ActionTypeRepository.get()
    // Data Binding
    private lateinit var binding: DialogActionTypeBinding
    // Arguments
    private var parentActionType: ActionType? = null
    private var actionType: ActionType? = null
    // Cringe Logic
    private var isCreated: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get arguments
        parentActionType = arguments?.getSerializable("parentActionType") as ActionType?
        actionType = arguments?.getSerializable("actionType") as ActionType?

        // Preprocessing (Cringe)
        if (actionType == null) {
            actionType = ActionType()
            isCreated = true
        } else {
            actionType = actionType!!.copy()
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Data Binding
        binding = DialogActionTypeBinding.inflate(inflater, container, false)
        val view = binding.root
        binding.actionType = actionType


        // Чтобы был поверх клавиатуры
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)


        // Setting color picker
        binding.actionTypeColor.setOnClickListener{
            // Dialog
            ColorPickerDialog.Builder(context, R.style.App_ColorPickerDialog)
                .setPreferenceName("ColorPickerDialog")
                .setPositiveButton(resources.getString(R.string.select), object : ColorEnvelopeListener {
                        override fun onColorSelected(envelope: ColorEnvelope, fromUser: Boolean) {
                            actionType!!.color = envelope.color
                            binding.actionTypeColor.setColorFilter(envelope.color)
                        }
                    })
                .setNegativeButton(resources.getString(R.string.cancel)) {
                        dialogInterface, _ -> dialogInterface.dismiss() }
                .setBottomSpace(12)
                .show()
        }

        // Text on button
        if (isCreated) {
            binding.button.text = resources.getString(R.string.add)
        } else {
            binding.button.text = resources.getString(R.string.edit)
        }


        // Setting button
        binding.button.setOnClickListener{
            val name = binding.actionTypeName.text.toString()
            val color = actionType!!.color

            if (name != ""){
                actionType!!.name = name
                actionType!!.color = color

                if (isCreated){
                    var parent = ""
                    if (parentActionType != null) parent = parentActionType!!.id
                    actionType!!.parent = parent
                    repository.addActionType(actionType!!)
                } else {
                    repository.updateActionType(actionType!!)
                }
            }

            this.dismiss()
        }

        return view
    }
}