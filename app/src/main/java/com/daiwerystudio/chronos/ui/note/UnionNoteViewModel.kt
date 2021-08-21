/*
* Дата создания: 21.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.note

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.daiwerystudio.chronos.database.Note
import com.daiwerystudio.chronos.database.NoteRepository
import com.daiwerystudio.chronos.ui.union.UnionViewModel

class UnionNoteViewModel : UnionViewModel() {
    private val mRepository = NoteRepository.get()

    // Добавляем подписку на parentID.
    var parent: LiveData<Note> =
        Transformations.switchMap(parentID) { mRepository.getNote(it) }
        private set
}