/*
* Дата создания: 21.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.note

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Note
import com.daiwerystudio.chronos.database.Union
import com.daiwerystudio.chronos.databinding.FragmentNoteBinding

class NoteFragment : Fragment()  {
    private val viewModel: NoteViewModel
            by lazy { ViewModelProvider(this).get(NoteViewModel::class.java) }
    private lateinit var binding: FragmentNoteBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Нужно скопировать, чтобы изменения были видны тольно после сохранения в базе данных.
        viewModel.note = (arguments?.getSerializable("note") as Note).copy()
        viewModel.union = arguments?.getSerializable("union") as Union?
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentNoteBinding.inflate(inflater, container, false)
        binding.note = viewModel.note

        binding.nameEditTextView.addTextChangedListener {
            viewModel.note.name = it.toString()
        }

        binding.noteEditTextView.addTextChangedListener {
            viewModel.note.note = it.toString()
        }

        binding.toolBar.setNavigationOnClickListener {
            AlertDialog.Builder(context, R.style.Style_AlertDialog)
                .setTitle(R.string.are_you_sure)
                .setPositiveButton(R.string.yes) { _, _ ->
                    it.findNavController().navigateUp()
                }
                .setNegativeButton(R.string.no){ _, _ -> }
                .setCancelable(false).create().show()
        }
        binding.toolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.save -> {
                    AlertDialog.Builder(context, R.style.Style_AlertDialog)
                        .setTitle(R.string.are_you_sure)
                        .setPositiveButton(R.string.yes) { _, _ ->
                            viewModel.saveNote()
                            requireActivity().findNavController(R.id.nav_host_fragment).popBackStack()
                        }
                        .setNegativeButton(R.string.no){ _, _ -> }
                        .setCancelable(false).create().show()
                    true
                }
                else -> false
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    AlertDialog.Builder(context, R.style.Style_AlertDialog)
                        .setTitle(R.string.are_you_sure)
                        .setPositiveButton(R.string.yes) { _, _ ->
                            viewModel.saveNote()
                            isEnabled = false
                            activity?.onBackPressed()
                        }
                        .setNegativeButton(R.string.no){ _, _ -> }
                        .setCancelable(false).create().show()
                }
            })

        return binding.root
    }
}