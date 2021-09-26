/*
* Дата создания: 06.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 17.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: добавлена логика взаимодействия с union.
*/

package com.daiwerystudio.chronos.ui.action_type

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.database.ActionTypeRepository
import com.daiwerystudio.chronos.database.Union
import com.daiwerystudio.chronos.database.UnionRepository
import com.daiwerystudio.chronos.databinding.DialogActionTypeBinding
import com.daiwerystudio.chronos.ui.DataViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener

/**
 * Ключевой особенностью является тот факт, что диалог всегда получает тип действия.
 * Изменения или создание регулируется параметром isCreated. Это необходимо, чтобы
 * разделить UI и функционально необходимые данные, которые будут регулироваться внешне.
 */
class ActionTypeDialog : BottomSheetDialogFragment() {
    private val viewModel: DataViewModel
        by lazy { ViewModelProvider(this).get(DataViewModel::class.java) }
    private val mActionTypeRepository = ActionTypeRepository.get()
    private val mUnionRepository = UnionRepository.get()
    private lateinit var binding: DialogActionTypeBinding

    private lateinit var actionType: ActionType
    private var isCreated: Boolean = false
    private var union: Union? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isCreated = arguments?.getBoolean("isCreated") as Boolean
        actionType = arguments?.getSerializable("actionType") as ActionType
        // Необходимо скопировать значение, т.к. передается ссылка.
        // Это нужно, чтобы RecyclerView смог засечь изменение данных и перерисовал holder.
        actionType = actionType.copy()

        // Значение равно null только при isCreated = false.
        union = arguments?.getSerializable("union") as Union?

        // Восстанавливаем значение, если оно есть.
        if (viewModel.data != null) actionType = viewModel.data as ActionType
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DialogActionTypeBinding.inflate(inflater, container, false)
        binding.actionType = actionType

        binding.color.setOnClickListener{
            ColorPickerDialog.Builder(context, R.style.Style_ColorPickerDialog)
                .setPreferenceName("ColorPickerDialog")
                .setPositiveButton(resources.getString(R.string.select), object : ColorEnvelopeListener {
                        override fun onColorSelected(envelope: ColorEnvelope, fromUser: Boolean) {
                            actionType.color = envelope.color
                            binding.color.setColorFilter(envelope.color)
                        }
                    })
                .setNegativeButton(resources.getString(R.string.cancel)) { dialogInterface, _ -> dialogInterface.dismiss() }
                .setBottomSpace(12).attachAlphaSlideBar(false).show()
        }

        binding.actionTypeName.editText?.doOnTextChanged { text, _, _, _ ->
            actionType.name = text.toString()
            if (actionType.name == "")  binding.actionTypeName.error = resources.getString(R.string.error_name)
            else binding.actionTypeName.error = null
        }

        if (isCreated) {
            binding.button.text = resources.getString(R.string.add)
            binding.button.setIconResource(R.drawable.ic_baseline_add_24)
        }
        else {
            binding.button.text = resources.getString(R.string.edit)
            binding.button.setIconResource(R.drawable.ic_baseline_edit_24)
        }

        binding.button.setOnClickListener{
            var permission = true
            if (actionType.name == "") {
                permission = false
                binding.actionTypeName.error = resources.getString(R.string.error_name)
            } else binding.actionTypeName.error = null

            if (permission){
                if (isCreated) {
                    mActionTypeRepository.addActionType(actionType)
                    mUnionRepository.addUnion(union!!)
                }
                else mActionTypeRepository.updateActionType(actionType)

                this.dismiss()
            }
        }

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()

        // Сохраняем значение.
        viewModel.data = actionType
    }
}