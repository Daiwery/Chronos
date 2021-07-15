package com.daiwerystudio.chronos.UI.Act

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import com.daiwerystudio.chronos.DataBase.Act
import com.daiwerystudio.chronos.R
import java.util.*


class CreateItemActFragment : Fragment() {
    private val createItemActViewModel: CreateItemActViewModel by lazy {
        ViewModelProviders.of(this).get(CreateItemActViewModel::class.java)
    }

    private var act: Act? = null
    private var idParentAct: String? = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_create_item_act, container, false)

        // Доступ к меню
        val appCompatActivity = activity as AppCompatActivity
        val appBar = appCompatActivity.supportActionBar

        val actNameTextView = view.findViewById(R.id.act_name) as TextView
        val save_button = view.findViewById(R.id.save_button) as Button


        // Если мы изменяем act, а не создаем
        if (act != null){
            actNameTextView.setText(act!!.name)
            save_button.setText("Изменить")
            appBar?.setTitle("Изменение")
        }

        save_button.setOnClickListener{ v: View ->
            val name = actNameTextView.text.toString()
            if (name != ""){
                if (act == null){
                    val act = Act()
                    act.name = name
                    act.parent = idParentAct.toString()

                    createItemActViewModel.addAct(act)
                } else {
                    act!!.name = name
                    createItemActViewModel.updateAct(act!!)
                }
            }

            v.findNavController().popBackStack()
        }

        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Получаем аргументы
        idParentAct = arguments?.getString("idParentAct")
        act = arguments?.getSerializable("act") as Act?
    }
}