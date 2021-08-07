/*
* Дата создания: 07.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.schedule

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Schedule
import com.daiwerystudio.chronos.databinding.FragmentScheduleBinding
import com.google.android.material.tabs.TabLayoutMediator

/**
 * Класс фрагмента, который видит пользователь при нажатии на расписание. Имеет основную логику
 * такую же, как и остальные фрагменты. Но есть одно очень важное отличие: этот фрагмент
 * является контейнром для фрагментов DayScheduleFragment (за исключением нескольких виджетов). Каждый
 * день в расписании представляет и себя отдельный фрагмент. Их расположением управляют ViewPager2
 * и TabLayout.
 *
 * Возможная модификация: процесс загрузки фрагмента очень странный. Экран загрузки исчезнает раньше,
 * чем фактически загружается фрагмент.
 * @see ScheduleViewModel
 */
class ScheduleFragment : Fragment() {
    /**
     * ViewModel.
     */
    private val viewModel: ScheduleViewModel
    by lazy { ViewModelProvider(this).get(ScheduleViewModel::class.java) }
    /**
     * Привязка данных.
     */
    private lateinit var binding: FragmentScheduleBinding

    /**
     * Выполняется перед созданием UI. Здесь фрагмент получает id и передает его в ViewModel.
     * Это делается для того, чтобы при перевороте устройства данные заного не извлекались.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Это необходимо для созданию меню по макету.
        setHasOptionsMenu(true)

        viewModel.id = arguments?.getSerializable("id") as String
    }

    /**
     * Создание UI.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentScheduleBinding.inflate(inflater, container, false)
        val view = binding.root


        binding.viewPager2.adapter = PagerAdapter(this, null)
        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
            if (binding.viewPager2.adapter!!.itemCount == 7) tab.text = resources.getStringArray(R.array.week)[position]
            else tab.text = resources.getString(R.string.day)+" "+(position+1).toString()
        }.attach()


        viewModel.schedule.observe(viewLifecycleOwner, {
            binding.schedule = it

            (activity as AppCompatActivity).supportActionBar?.title = it.name

            // Нельзя создавать новый адаптер, так как используется notifyItemRangeInserted
            (binding.viewPager2.adapter as PagerAdapter).setData(it)

            // При изменении данных, переходим в тот день, который является активным сейчас.
            val index = (System.currentTimeMillis()/(1000*60*60*24)-it.dayStart).toInt()%it.countDays
            binding.tabLayout.selectTab(binding.tabLayout.getTabAt(index))
        })


        binding.button.setOnClickListener {
            val schedule = viewModel.schedule.value!!
            val index = (System.currentTimeMillis()/(1000*60*60*24)-schedule.dayStart).toInt()%schedule.countDays
            binding.tabLayout.selectTab(binding.tabLayout.getTabAt(index))
        }


        binding.active.setOnClickListener {
            viewModel.schedule.value!!.isActive = binding.active.isChecked
            viewModel.updateSchedule(viewModel.schedule.value!!)
        }

        return view
    }

    /**
     * Уставнавливает меню и заполняет его по menu.edit_delete_menu.
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.edit_delete_menu, menu)
    }

    /**
     * Выполняется при нажатии на элемент в меню. В зависимости нажатого элемента либо позволяет
     * изменить родительский тип действия, либо удалить его и его дерево.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.edit -> {
                val dialog = ScheduleDialog()
                dialog.arguments = Bundle().apply{
                    putSerializable("schedule", viewModel.schedule.value!!)
                    putBoolean("isCreated", false)
                }
                dialog.show(requireActivity().supportFragmentManager, "ScheduleDialog")

                return true
            }
            R.id.delete -> {
                AlertDialog.Builder(context, R.style.App_AlertDialog)
                    .setTitle(resources.getString(R.string.are_you_sure))
                    .setPositiveButton(R.string.yes) { _, _ ->
                        viewModel.deleteScheduleWithActions(viewModel.schedule.value!!)
                        requireActivity().findNavController(R.id.nav_host_fragment).popBackStack()
                    }
                    .setNegativeButton(R.string.no){ _, _ -> }
                    .setCancelable(false)
                    .create()
                    .show()

                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    /**
     * Наследуется от FragmentStateAdapter. Является адаптером для pager. См. оф. документацию.
     */
    private inner class PagerAdapter(fragment: Fragment, var schedule: Schedule?): FragmentStateAdapter(fragment){
        fun setData(newData: Schedule) {
            val oldData = schedule ?: Schedule(countDays=0)
            schedule = newData

            if (newData.countDays > oldData.countDays)
                notifyItemRangeInserted(oldData.countDays,
                    newData.countDays-oldData.countDays)

            binding.loadingView.visibility = View.GONE
        }

        override fun getItemCount(): Int = schedule?.countDays ?: 0

        override fun createFragment(position: Int): Fragment {
            val fragment = DayScheduleFragment()
            fragment.arguments = Bundle().apply{
                putSerializable("schedule", schedule!!)
                putInt("dayIndex", position)
            }
            return fragment
        }

    }

}