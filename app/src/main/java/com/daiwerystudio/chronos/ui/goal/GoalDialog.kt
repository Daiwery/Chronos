/*
* Дата создания: 06.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.goal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Goal
import com.daiwerystudio.chronos.database.GoalRepository
import com.daiwerystudio.chronos.databinding.DialogGoalBinding
import com.daiwerystudio.chronos.ui.DialogViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Представляет из себя диалог в виде нижней панели. По логике абсолютно такое же, как и
 * ActionTypeDialog. За тем исключение, что работает с Goal.
 */
class GoalDialog : BottomSheetDialogFragment() {
    /**
     * ViewModel.
     */
    private val viewModel: DialogViewModel
    by lazy { ViewModelProvider(this).get(DialogViewModel::class.java) }
    /**
    * Репозиторий для взаимодействия с базой данных. Данные из базы данных не извлекаются,
    * поэтому помещать его в ViewModel нет смысла.
    */
    private val goalRepository = GoalRepository.get()
    /**
     * Привязка данных.
     */
    private lateinit var binding: DialogGoalBinding
    /**
    * Цель, которую получает диалог из Bundle.
    */
    private lateinit var goal: Goal
    /**
     * Определяет, создается или изменяется цель. Диалог получает его из Bundle.
     */
    var isCreated: Boolean = false

    /**
     * Выполняет перед созданиес интефейса. Получает данные из Bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isCreated = arguments?.getBoolean("isCreated") as Boolean
        goal = arguments?.getSerializable("goal") as Goal
        // Необходимо скопировать значение, т.к. передается ссылка.
        // Это нужно, чтобы RecyclerView смог засечь изменение данных и перерисовал holder.
        goal = goal.copy()

        if (viewModel.data != null) goal = viewModel.data as Goal
    }

    /**
     * Создание UI и его настройка.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DialogGoalBinding.inflate(inflater, container, false)
        val view = binding.root
        binding.goal = goal

        binding.goalName.addTextChangedListener{
            goal.name = binding.goalName.text.toString()

            if (goal.name != "") binding.error.visibility = View.INVISIBLE
            else binding.error.visibility = View.VISIBLE
        }


        if (isCreated) {
            binding.button.text = resources.getString(R.string.add)
        } else {
            binding.button.text = resources.getString(R.string.edit)
        }


        binding.button.setOnClickListener{
            if (goal.name != ""){
                if (isCreated) goalRepository.addGoal(goal)
                else goalRepository.updateGoal(goal)

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

        viewModel.data = goal
    }

}