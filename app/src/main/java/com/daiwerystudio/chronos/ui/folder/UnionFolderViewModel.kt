/*
* Дата создания: 06.09.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.folder

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.daiwerystudio.chronos.database.Folder
import com.daiwerystudio.chronos.database.FolderRepository
import com.daiwerystudio.chronos.ui.union.UnionViewModel

class UnionFolderViewModel : UnionViewModel() {
    private val mRepository = FolderRepository.get()

    // Добавляем подписку на parentID.
    var parent: LiveData<Folder> =
        Transformations.switchMap(information) { mRepository.getFolder(it) }
        private set
}