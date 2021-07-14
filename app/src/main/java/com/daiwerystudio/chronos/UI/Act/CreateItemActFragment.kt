package com.daiwerystudio.chronos.UI.Act

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import com.daiwerystudio.chronos.DataBase.Act
import com.daiwerystudio.chronos.R
import java.util.*


class CreateItemActFragment : Fragment() {
    private val createItemActViewModel: CreateItemActViewModel by lazy {
        ViewModelProviders.of(this).get(CreateItemActViewModel::class.java)
    }

    private var idAct: String? = null
    private var parentAct: String? = ""
    private var nameAct: String? = null

    private var isNew: Boolean = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_create_item_act, container, false)

        val actNameTextView = view.findViewById(R.id.act_name) as TextView
        val save_button = view.findViewById(R.id.save_button) as Button


        // Если мы изменяем act, а не создаем
        if (nameAct != null){
            actNameTextView.setText(nameAct)
            save_button.setText("Изменить")
            isNew = false
        }

        save_button.setOnClickListener{ v: View ->
            val name = actNameTextView.text.toString()
            if (name != ""){
                if (isNew){
                    val act = Act()
                    act.name = name
                    act.parent = parentAct.toString()
                    createItemActViewModel.addAct(act)
                } else {
                    val act = Act(UUID.fromString(idAct))
                    act.parent = parentAct.toString()
                    act.name = name
                    createItemActViewModel.updateAct(act)
                }
            }

            v.findNavController().popBackStack()
        }

        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Получаем аргументы
        parentAct = arguments?.getString("parentAct")
        nameAct = arguments?.getString("nameAct")
        idAct = arguments?.getString("idAct")
    }
}