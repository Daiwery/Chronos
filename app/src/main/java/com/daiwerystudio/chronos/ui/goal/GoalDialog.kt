/*
* Дата создания: 06.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*
* Дата изменения: 17.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
* Изменения: добавлена логика взаимодействия с union.
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
import com.daiwerystudio.chronos.database.Union
import com.daiwerystudio.chronos.database.UnionRepository
import com.daiwerystudio.chronos.databinding.DialogGoalBinding
import com.daiwerystudio.chronos.ui.DataViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Ключевой особенностью является тот факт, что диалог всегда получает цель.
 * Изменения или создание регулируется параметром isCreated. Это необходимо, чтобы
 * разделить UI и функционально необходимые данные, которые будут регулироваться внешне.
 */
class GoalDialog : BottomSheetDialogFragment() {
    private val viewModel: DataViewModel
        by lazy { ViewModelProvider(this).get(DataViewModel::class.java) }
    private val mGoalRepository = GoalRepository.get()
    private val mUnionRepository = UnionRepository.get()
    private lateinit var binding: DialogGoalBinding

    private lateinit var goal: Goal
    var isCreated: Boolean = false
    private var union: Union? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isCreated = arguments?.getBoolean("isCreated") as Boolean
        goal = arguments?.getSerializable("goal") as Goal
        // Необходимо скопировать значение, т.к. передается ссылка.
        // Это нужно, чтобы RecyclerView смог засечь изменение данных и перерисовал holder.
        goal = goal.copy()

        // Значение равно null только при isCreated = false.
        union = arguments?.getSerializable("union") as Union?

        // Восстанавливаем значение, если оно есть.
        if (viewModel.data != null) goal = viewModel.data as Goal
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DialogGoalBinding.inflate(inflater, container, false)
        val view = binding.root
        binding.goal = goal

        binding.goalName.addTextChangedListener{
            goal.name = it.toString()
            if (goal.name != "") binding.error.visibility = View.INVISIBLE
            else binding.error.visibility = View.VISIBLE
        }

        if (isCreated) binding.button.text = resources.getString(R.string.add)
        else binding.button.text = resources.getString(R.string.edit)
        binding.button.setOnClickListener{
            if (goal.name != ""){
                if (isCreated) {
                    mGoalRepository.addGoal(goal)
                    mUnionRepository.addUnion(union!!)
                }
                else mGoalRepository.updateGoal(goal)

                this.dismiss()
            } else {
                // Это нужно для того, чтоюы при первом появлении пустого TextInput ошибки не было,
                // а после нажатия кнопки, без изменения TextInput, появлялась ошибка.
                binding.error.visibility = View.VISIBLE
            }
        }

        return view
    }

    override fun onDestroy() {
        super.onDestroy()

        // Сохраняем значение.
        viewModel.data = goal
    }

}