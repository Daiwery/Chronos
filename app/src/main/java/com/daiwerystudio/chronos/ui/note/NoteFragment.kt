/*
* Дата создания: 21.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.note

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.daiwerystudio.chronos.database.Note
import com.daiwerystudio.chronos.database.Union
import com.daiwerystudio.chronos.databinding.FragmentNoteBinding

class NoteFragment : Fragment()  {
    private val viewModel: NoteViewModel
            by lazy { ViewModelProvider(this).get(NoteViewModel::class.java) }
    private lateinit var binding: FragmentNoteBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.note = arguments?.getSerializable("note") as Note
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
            it.findNavController().navigateUp()
        }

        return binding.root
    }

    override fun onPause() {
        super.onPause()

        viewModel.saveNote()
    }
}