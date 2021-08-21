/*
* Дата создания: 19.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.note

import androidx.lifecycle.ViewModel
import com.daiwerystudio.chronos.database.Note
import com.daiwerystudio.chronos.database.NoteRepository
import com.daiwerystudio.chronos.database.Union
import com.daiwerystudio.chronos.database.UnionRepository

class NoteViewModel : ViewModel() {
    private val mNoteRepository = NoteRepository.get()
    private val mUnionRepository = UnionRepository.get()

    lateinit var note: Note
    var union: Union? = null


    fun saveNote(){
        if (note.name != "" || note.note != "") {
            if (union == null) mNoteRepository.updateNote(note)
            else {
                mNoteRepository.addNote(note)
                mUnionRepository.addUnion(union!!)
            }
        }
    }

    fun deleteUnionWithChild(){
        mUnionRepository.deleteUnionWithChild(note.id)
    }
}