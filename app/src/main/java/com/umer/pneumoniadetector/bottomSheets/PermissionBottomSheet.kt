package com.umer.pneumoniadetector.bottomSheets

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat.checkSelfPermission
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.umer.pneumoniadetector.R
import com.umer.pneumoniadetector.databinding.PermissionBottomSheetBinding


class PermissionBottomSheet(
    context: Context,
    showBottomSheet: Boolean,
    descriptionText : String = "",
    permissionName: String ,
    private val permissionListener: PermissionListener
) : BottomSheetDialog(context, R.style.BottomSheetTheme) {
    private var binding: PermissionBottomSheetBinding =
        PermissionBottomSheetBinding.inflate(layoutInflater)

    init {
        setContentView(binding.root)
        setCancelable(false)
        setListeners()
        if (checkSelfPermission(
                context,
                permissionName
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (showBottomSheet) {
                show()
            }
        }
        if (descriptionText.isNotEmpty()){
            binding.descriptionText.text = descriptionText
        }
    }

    private fun setListeners() {
        binding.closeBtn.setOnClickListener {
            dismiss()
        }
        binding.settingButton.setOnClickListener {
            permissionListener.onSettingClicked()
        }
    }
}

interface PermissionListener {
    fun onSettingClicked()
}