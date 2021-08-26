/*
* Дата создания: 17.08.2021.
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.action_type

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.daiwerystudio.chronos.database.ActionType
import com.daiwerystudio.chronos.database.ActionTypeRepository
import com.daiwerystudio.chronos.ui.union.UnionViewModel

class UnionActionTypeViewModel: UnionViewModel() {
    private val mRepository = ActionTypeRepository.get()

    // Добавляем подписку на parentID.
    var parent: LiveData<ActionType> =
        Transformations.switchMap(showing) { mRepository.getActionType(it.first) }
        private set
}