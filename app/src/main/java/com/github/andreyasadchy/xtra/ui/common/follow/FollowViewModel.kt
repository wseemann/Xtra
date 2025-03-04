package com.github.andreyasadchy.xtra.ui.common.follow

import com.github.andreyasadchy.xtra.model.Account

interface FollowViewModel {
    val userId: String?
    val userLogin: String?
    val userName: String?
    val channelLogo: String?
    val follow: FollowLiveData
    val game: Boolean
        get() = false

    fun setUser(account: Account, helixClientId: String?, gqlClientId: String?, gqlClientId2: String?, setting: Int)
}