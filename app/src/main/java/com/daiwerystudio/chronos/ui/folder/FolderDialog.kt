/*
* Дата создания: 06.09.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui.folder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import com.daiwerystudio.chronos.R
import com.daiwerystudio.chronos.database.Folder
import com.daiwerystudio.chronos.database.FolderRepository
import com.daiwerystudio.chronos.database.Union
import com.daiwerystudio.chronos.database.UnionRepository
import com.daiwerystudio.chronos.databinding.DialogFolderBinding
import com.daiwerystudio.chronos.ui.DataViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FolderDialog : BottomSheetDialogFragment() {
    private val viewModel: DataViewModel
            by lazy { ViewModelProvider(this).get(DataViewModel::class.java) }
    private val mFolderRepository = FolderRepository.get()
    private val mUnionRepository = UnionRepository.get()
    private lateinit var binding: DialogFolderBinding

    private lateinit var folder: Folder
    private var isCreated: Boolean = false
    private var union: Union? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isCreated = arguments?.getBoolean("isCreated") as Boolean
        folder = arguments?.getSerializable("folder") as Folder
        // Необходимо скопировать значение, т.к. передается ссылка.
        // Это нужно, чтобы RecyclerView смог засечь изменение данных и перерисовал holder.
        folder = folder.copy()

        // Значение равно null только при isCreated = false.
        union = arguments?.getSerializable("union") as Union?

        // Восстанавливаем значение, если оно есть.
        if (viewModel.data != null) folder = viewModel.data as Folder
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DialogFolderBinding.inflate(inflater, container, false)
        binding.folder = folder

        binding.folderName.editText?.doOnTextChanged { text, _, _, _ ->
            folder.name = text.toString()
            if (folder.name == "")  binding.folderName.error = resources.getString(R.string.error_name)
            else binding.folderName.error = null
        }
        if (isCreated) binding.folderName.requestFocus()

        if (isCreated) {
            binding.button.text = resources.getString(R.string.add)
            binding.button.setIconResource(R.drawable.ic_baseline_add_24)
        }
        else {
            binding.button.text = resources.getString(R.string.edit)
            binding.button.setIconResource(R.drawable.ic_baseline_edit_24)
        }

        binding.button.setOnClickListener{
            var permission = true
            if (folder.name == "") {
                permission = false
                binding.folderName.error = resources.getString(R.string.error_name)
            } else binding.folderName.error = null

            if (permission){
                if (isCreated) {
                    mFolderRepository.addFolder(folder)
                    mUnionRepository.addUnion(union!!)
                }
                else mFolderRepository.updateFolder(folder)

                this.dismiss()
            }
        }

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()

        // Сохраняем значение.
        viewModel.data = folder
    }
}