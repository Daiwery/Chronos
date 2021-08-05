package com.daiwerystudio.chronos.ui.actiontype


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.database.ActionTypeRepository
import com.daiwerystudio.chronos.databinding.DialogActionTypeBinding
import com.daiwerystudio.chronos.ui.DialogViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener


class ActionTypeDialog : BottomSheetDialogFragment() {
    // ViewModel
    private val viewModel: DialogViewModel
    by lazy { ViewModelProvider(this).get(DialogViewModel::class.java) }
    // Database
    private val repository = ActionTypeRepository.get()
    // Data Binding
    private lateinit var binding: DialogActionTypeBinding
    // Arguments
    private lateinit var actionType: ActionType
    private var isCreated: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get arguments
        actionType = arguments?.getSerializable("actionType") as ActionType
        actionType = actionType.copy()
        isCreated = arguments?.getBoolean("isCreated") as Boolean

        // Recovery
        if (viewModel.data != null) actionType = viewModel.data as ActionType
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
        binding.color.setOnClickListener{
            // Dialog
            ColorPickerDialog.Builder(context, R.style.App_ColorPickerDialog)
                .setPreferenceName("ColorPickerDialog")
                .setPositiveButton(resources.getString(R.string.select), object : ColorEnvelopeListener {
                        override fun onColorSelected(envelope: ColorEnvelope, fromUser: Boolean) {
                            actionType.color = envelope.color
                            binding.color.setColorFilter(envelope.color)
                        }
                    })
                .setNegativeButton(resources.getString(R.string.cancel)) {
                        dialogInterface, _ -> dialogInterface.dismiss() }
                .setBottomSpace(12)
                .show()
        }

        binding.name.addTextChangedListener{
            actionType.name = binding.name.text.toString()
            if (actionType.name != ""){
                binding.error.visibility = View.INVISIBLE
            } else {
                binding.error.visibility = View.VISIBLE
            }
        }

        // Text on button
        if (isCreated) {
            binding.button.text = resources.getString(R.string.add)
        } else {
            binding.button.text = resources.getString(R.string.edit)
        }
        // Setting button
        binding.button.setOnClickListener{
            if (actionType.name != ""){
                if (isCreated){
                    repository.addActionType(actionType)
                } else {
                    repository.updateActionType(actionType)
                }

                this.dismiss()
            } else {
                binding.error.visibility = View.VISIBLE
            }
        }

        return view
    }


    override fun onDestroy() {
        super.onDestroy()

        viewModel.data = actionType
    }
}