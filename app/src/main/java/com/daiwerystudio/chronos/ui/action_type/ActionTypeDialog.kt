/*
* Дата создания: 06.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.action_type

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

/**
 * Представляет из себя диалоговое окно в виде нижней панели. Используется для создания и
 * редактирования типов действий. Ключевая особенность состоит в том, что диалог всегда
 * получает тип действия и состояния: изменяется или создается. Это означает, что при
 * создании тип действия должен инициализироваться во фрагментах, которые его передают.
 * Это нужно, чтобы диалог мог изменять только UI часть типа дейсвтия. А такое свойство, как
 * parent, задавалось отдельно. Использует DialogViewModel для сохранения данных.
 *
 * Возможная модификация: вместо InputText использовать компоненты материального дизайна, чтобы
 * вместо ручного создания иконки "error" делать это лаконичнее. Не использование встроенного
 * setError обусловлено тем, что оно некрасивое.
 */
class ActionTypeDialog : BottomSheetDialogFragment() {
    /**
     * ViewModel.
     */
    private val viewModel: DialogViewModel
    by lazy { ViewModelProvider(this).get(DialogViewModel::class.java) }
    /**
     * Репозиторий для взаимодействия с базой данных. Данные из базы данных не извлекаются,
     * поэтому помещать его в ViewModel нет смысла.
     */
    private val repository = ActionTypeRepository.get()
    /**
     * Привязка данных.
     */
    private lateinit var binding: DialogActionTypeBinding
    /**
     * Тип действия, который получает диалог из Bundle.
     */
    private lateinit var actionType: ActionType
    /**
     * Определяет, создается или изменяется тип действия. Диалог получает его из Bundle.
     */
    private var isCreated: Boolean = false

    /**
     * Выполняет перед созданиес интефейса. Получает данные из Bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isCreated = arguments?.getBoolean("isCreated") as Boolean
        actionType = arguments?.getSerializable("actionType") as ActionType
        // Необходимо скопировать значение, т.к. передается ссылка.
        // Это нужно, чтобы RecyclerView смог засечь изменение данных и перерисовал holder.
        actionType = actionType.copy()

        if (viewModel.data != null) actionType = viewModel.data as ActionType
    }

    /**
     * Создание UI и его настройка.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DialogActionTypeBinding.inflate(inflater, container, false)
        val view = binding.root
        binding.actionType = actionType


        binding.color.setOnClickListener{
            ColorPickerDialog.Builder(context, R.style.App_ColorPickerDialog)
                .setPreferenceName("ColorPickerDialog")
                .setPositiveButton(resources.getString(R.string.select), object : ColorEnvelopeListener {
                        override fun onColorSelected(envelope: ColorEnvelope, fromUser: Boolean) {
                            actionType.color = envelope.color
                            binding.color.setColorFilter(envelope.color)
                        }
                    })
                .setNegativeButton(resources.getString(R.string.cancel)) { dialogInterface, _ -> dialogInterface.dismiss() }
                .setBottomSpace(12)
                .show()
        }


        binding.name.addTextChangedListener{
            actionType.name = binding.name.text.toString()

            if (actionType.name != "") binding.error.visibility = View.INVISIBLE
            else binding.error.visibility = View.VISIBLE

        }


        if (isCreated) binding.button.text = resources.getString(R.string.add)
        else binding.button.text = resources.getString(R.string.edit)

        binding.button.setOnClickListener{
            if (actionType.name != ""){
                if (isCreated) repository.addActionType(actionType)
                else repository.updateActionType(actionType)

                this.dismiss()
            // Это нужно для того, чтоюы при первом появлении пустого TextInput ошибки не было,
            // а после нажатия кнопки, без изменения TextInput, появлялась ошибка.
            } else binding.error.visibility = View.VISIBLE
        }

        return view
    }


    /**
     * Выполняется при уничтожении диалога. Нужно, чтобы сохранить данные, если уничтожение
     * произошло при перевороте устройства.
     */
    override fun onDestroy() {
        super.onDestroy()
        viewModel.data = actionType
    }
}