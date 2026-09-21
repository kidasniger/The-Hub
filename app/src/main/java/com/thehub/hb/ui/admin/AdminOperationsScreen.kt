package com.thehub.hb.ui.admin

import androidx.compose.runtime.Composable
import com.thehub.hb.data.repository.AdminControlRepository
import com.thehub.hb.data.repository.AdminRepository

@Composable
fun AdminOperationsScreen(repository: AdminRepository) {
    AdminControlCenterScreen(AdminControlRepository())
}
