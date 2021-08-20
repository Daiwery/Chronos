/*
* Дата создания: 12.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.day

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.lifecycle.ViewModelProvider
import com.daiwerystudio.chronos.database.ActionTypeRepository
import com.daiwerystudio.chronos.databinding.DialogDoingActionBinding
import com.daiwerystudio.chronos.ui.DataViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.*


/**
 * Представляет из себя диалог в виде нижней панели. По логике абсолютно такое же, как и
 * ActionDialog. За тем исключение, что дает выбрать только start и action type.
 *
 * Возможная модификация: экран загрузки при загрузке DatePicker, ибо он долго грузится,
 * никак не уведомляя пользователя.
 */
class DoingActionDialog : BottomSheetDialogFragment() {
    /**
     * ViewModel.
     */
    private val viewModel: DataViewModel
    by lazy { ViewModelProvider(this).get(DataViewModel::class.java) }

    /**
     * Репозиторий для взаимодействия с базой данных. Нужен для работы
     * SelectActionTypeView.
     */
    private val actionTypeRepository = ActionTypeRepository.get()

    /**
     * Выбранный тип действия. Получает из Bundle.
     */
    private var actionTypeID: String = ""

    /**
     * Привязка данных.
     */
    private lateinit var binding: DialogDoingActionBinding

    /**
     * Смещение времени в часовом поезде.
     */
    private val local = TimeZone.getDefault().getOffset(System.currentTimeMillis())/1000

    /**
     * Время начала действия.
     */
    private var startTime = System.currentTimeMillis()/1000

    /**
     * Интерфейс, который сообщает, какое действие хочет делать пользователь.
     */
    private var mAddDoingActionTypeListener: AddDoingActionTypeListener? = null
    fun interface AddDoingActionTypeListener{
        fun addDoingActionType(actionTypeID: String, startTime: Long)
    }
    fun setAddDoingActionTypeListener(addDoingActionTypeListener: AddDoingActionTypeListener){
        mAddDoingActionTypeListener = addDoingActionTypeListener
    }

    /**
     * Выполняет перед созданиес интефейса. Получает данные из Bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        actionTypeID = arguments?.getString("actionTypeID") as String
        if (viewModel.data != null) actionTypeID = viewModel.data as String
    }

    /**
     * Создание UI и его настройка.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DialogDoingActionBinding.inflate(inflater, container, false)
        val view = binding.root
        binding.start = startTime

        // Отмена клавиатуры.
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        val actionTypes = actionTypeRepository.getAllActionType()
        actionTypes.observe(viewLifecycleOwner, {
            if (it != null) binding.selectActionType.setData(it)
        })

        //val actionType = actionTypeRepository.getActionType(actionTypeID)
//        actionType.observe(viewLifecycleOwner, {
//            binding.selectActionType.setSelectActionType(it)
//        })
        binding.selectActionType.setOnSelectListener{
            actionTypeID = it.id
        }


        binding.startTime.setOnClickListener{
            val localTime = startTime+local
            val day = localTime/(24*60*60)
            val time = (localTime%(24*60*60)).toInt()
            val hour = time/3600
            val minute = (time-hour*3600)/60

            val dialog = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("")
                .build()

            dialog.addOnPositiveButtonClickListener {
                startTime = day*24*60*60+(dialog.hour*60 + dialog.minute)*60-local
                binding.start = startTime
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }

        binding.startDay.setOnClickListener{
            val localTime = startTime+local
            val time = localTime%(24*60*60)

            val dialog = MaterialDatePicker.Builder.datePicker()
                .setSelection(localTime*1000)
                .build()

            dialog.addOnPositiveButtonClickListener {
                startTime = dialog.selection!!/1000+time-local
                binding.start = startTime
            }
            dialog.show(activity?.supportFragmentManager!!, "TimePickerDialog")
        }

        binding.button.setOnClickListener {
            var permission = true
            if (startTime > System.currentTimeMillis())
                permission = false

            if (permission){
                mAddDoingActionTypeListener?.addDoingActionType(actionTypeID, startTime)

                this.dismiss()
            }
        }

        return view
    }


    /**
     * Выполняется при уничтожении диалога. Нужно, чтобы сохранить данные, если уничтожение
     * произошло при перевороте устройства.
     */
    override fun onDestroy() {
        super.onDestroy()
        viewModel.data = actionTypeID
    }
}