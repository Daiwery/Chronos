package com.daiwerystudio.chronos.ui.actiontype

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.R


class ItemActionTypeFragment : Fragment() {
    private val itemActionTypeViewModel: ItemActionTypeViewModel by lazy {
        ViewModelProviders.of(this).get(ItemActionTypeViewModel::class.java)
    }

    private var actionType: ActionType? = null
    private var idParentActionType: String? = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_item_action_type, container, false)

        // Доступ к меню
        val appCompatActivity = activity as AppCompatActivity
        val appBar = appCompatActivity.supportActionBar

        val actionTypeNameTextView = view.findViewById(R.id.action_type_name) as TextView
        val save_button = view.findViewById(R.id.save_button) as Button


        // Если мы изменяем act, а не создаем
        if (actionType != null){
            actionTypeNameTextView.setText(actionType!!.name)
            save_button.setText("Изменить")
            appBar?.setTitle("Изменение")
        }

        save_button.setOnClickListener{ v: View ->
            val name = actionTypeNameTextView.text.toString()
            if (name != ""){
                if (actionType == null){
                    val actionType = ActionType()
                    actionType.name = name
                    actionType.parent = idParentActionType.toString()

                    itemActionTypeViewModel.addActionType(actionType)
                } else {
                    actionType!!.name = name
                    itemActionTypeViewModel.updateActionType(actionType!!)
                }
            }

            v.findNavController().popBackStack()
        }

        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Получаем аргументы
        idParentActionType = arguments?.getString("idParentActionType")
        actionType = arguments?.getSerializable("actionType") as ActionType?
    }
}