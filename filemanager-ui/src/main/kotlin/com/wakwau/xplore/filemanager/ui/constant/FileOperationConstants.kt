// [Jalur Class]: com.wakwau.xplore.filemanager.ui.constant.FileOperationConstants
// [Penjelasan]: Konstanta identifier operasi berkas dan fallback pesan status berbasis resource ID dengan getter JVM.
package com.wakwau.xplore.filemanager.ui.constant

import com.wakwau.xplore.filemanager.ui.R

object FileOperationConstants {
    val OPERATION_COPY: Int = R.string.op_copy_started
    val OPERATION_MOVE: Int = R.string.op_move_started
    val OPERATION_DELETE: Int = R.string.op_delete_started
    val OPERATION_RENAME: Int = R.string.op_rename_started
    val OPERATION_CREATE_DIR: Int = R.string.op_create_dir_started
    
    val SUCCESS_COPY: Int = R.string.op_copy_completed
    val SUCCESS_MOVE: Int = R.string.op_move_completed
    val SUCCESS_DELETE: Int = R.string.op_delete_completed
    val SUCCESS_RENAME: Int = R.string.op_rename_completed
    val SUCCESS_CREATE_DIR: Int = R.string.op_create_dir_completed
}
