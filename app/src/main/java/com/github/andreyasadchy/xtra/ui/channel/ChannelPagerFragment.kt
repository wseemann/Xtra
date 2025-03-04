package com.github.andreyasadchy.xtra.ui.channel

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.model.Account
import com.github.andreyasadchy.xtra.model.NotLoggedIn
import com.github.andreyasadchy.xtra.model.ui.Stream
import com.github.andreyasadchy.xtra.model.ui.User
import com.github.andreyasadchy.xtra.ui.Utils
import com.github.andreyasadchy.xtra.ui.common.Scrollable
import com.github.andreyasadchy.xtra.ui.common.follow.FollowFragment
import com.github.andreyasadchy.xtra.ui.common.pagers.MediaPagerFragment
import com.github.andreyasadchy.xtra.ui.login.LoginActivity
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.ui.settings.SettingsActivity
import com.github.andreyasadchy.xtra.util.*
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_channel.*
import kotlinx.android.synthetic.main.fragment_media_pager.view.*


@AndroidEntryPoint
class ChannelPagerFragment : MediaPagerFragment(), FollowFragment, Scrollable {

    companion object {
        fun newInstance(id: String?, login: String?, name: String?, channelLogo: String?, updateLocal: Boolean = false, streamId: String? = null) = ChannelPagerFragment().apply {
            arguments = Bundle().apply {
                putString(C.CHANNEL_ID, id)
                putString(C.CHANNEL_LOGIN, login)
                putString(C.CHANNEL_DISPLAYNAME, name)
                putString(C.CHANNEL_PROFILEIMAGE, channelLogo)
                putBoolean(C.CHANNEL_UPDATELOCAL, updateLocal)
                putString(C.STREAM_ID, streamId)
            }
        }
    }

    private val viewModel: ChannelPagerViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_channel, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity() as MainActivity
        val account = Account.get(activity)
        setAdapter(ChannelPagerAdapter(this, requireArguments()))
        if (activity.isInLandscapeOrientation) {
            appBar.setExpanded(false, false)
        }
        requireArguments().getString(C.CHANNEL_DISPLAYNAME).let {
            if (it != null) {
                userLayout.visible()
                userName.visible()
                userName.text = it
            } else {
                userName.gone()
            }
        }
        requireArguments().getString(C.CHANNEL_PROFILEIMAGE).let {
            if (it != null) {
                userLayout.visible()
                userImage.visible()
                userImage.loadImage(this, it, circle = true)
            } else {
                userImage.gone()
            }
        }
        toolbar.apply {
            navigationIcon = Utils.getNavigationIcon(activity)
            setNavigationOnClickListener { activity.popFragment() }
        }
        search.setOnClickListener { activity.openSearch() }
        menu.setOnClickListener { it ->
            PopupMenu(activity, it).apply {
                inflate(R.menu.top_menu)
                menu.findItem(R.id.login).title = if (account !is NotLoggedIn) getString(R.string.log_out) else getString(R.string.log_in)
                setOnMenuItemClickListener {
                    when(it.itemId) {
                        R.id.settings -> { activity.startActivityFromFragment(this@ChannelPagerFragment, Intent(activity, SettingsActivity::class.java), 3) }
                        R.id.login -> {
                            if (account is NotLoggedIn) {
                                activity.startActivityForResult(Intent(activity, LoginActivity::class.java), 1)
                            } else {
                                AlertDialog.Builder(activity).apply {
                                    setTitle(getString(R.string.logout_title))
                                    account.login?.nullIfEmpty()?.let { user -> setMessage(getString(R.string.logout_msg, user)) }
                                    setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.dismiss() }
                                    setPositiveButton(getString(R.string.yes)) { _, _ -> activity.startActivityForResult(Intent(activity, LoginActivity::class.java), 2) }
                                }.show()
                            }
                        }
                        else -> menu.close()
                    }
                    true
                }
                show()
            }
        }
        pagerLayout.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            private val layoutParams = collapsingToolbar.layoutParams as AppBarLayout.LayoutParams
            private val originalScrollFlags = layoutParams.scrollFlags

            override fun onPageSelected(position: Int) {
                layoutParams.scrollFlags = if (position != 2) {
                    originalScrollFlags
                } else {
                    appBar.setExpanded(false, isResumed)
                    AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                }
            }
        })
        TabLayoutMediator(pagerLayout.tabLayout, pagerLayout.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.videos)
                1 -> getString(R.string.clips)
                else -> getString(R.string.chat)
            }
        }.attach()
    }

    override val currentFragment: Fragment?
        get() = childFragmentManager.findFragmentByTag("f${pagerLayout.viewPager.currentItem}")

    override fun initialize() {
        val activity = requireActivity() as MainActivity
        watchLive.setOnClickListener { activity.startStream(Stream(
            id = requireArguments().getString(C.STREAM_ID),
            channelId = requireArguments().getString(C.CHANNEL_ID),
            channelLogin = requireArguments().getString(C.CHANNEL_LOGIN),
            channelName = requireArguments().getString(C.CHANNEL_DISPLAYNAME),
            profileImageUrl = requireArguments().getString(C.CHANNEL_PROFILEIMAGE)))
        }
        viewModel.init(requireArguments().getString(C.CHANNEL_ID), requireArguments().getString(C.CHANNEL_LOGIN), requireArguments().getString(C.CHANNEL_DISPLAYNAME), requireArguments().getString(C.CHANNEL_PROFILEIMAGE))
        viewModel.loadStream(requireContext().prefs().getString(C.HELIX_CLIENT_ID, "ilfexgv3nnljz3isbm257gzwrzr7bi"), Account.get(requireContext()).helixToken, requireContext().prefs().getString(C.GQL_CLIENT_ID, "kimne78kx3ncx6brgo4mv6wki5h1ko"))
        viewModel.stream.observe(viewLifecycleOwner) { stream ->
            updateStreamLayout(stream)
            if (stream?.user != null) {
                updateUserLayout(stream.user)
            } else {
                viewModel.loadUser(requireContext().prefs().getString(C.HELIX_CLIENT_ID, "ilfexgv3nnljz3isbm257gzwrzr7bi"), Account.get(requireContext()).helixToken)
            }
        }
        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                updateUserLayout(user)
            }
        }
        if ((requireContext().prefs().getString(C.UI_FOLLOW_BUTTON, "0")?.toInt() ?: 0) < 2) {
            initializeFollow(
                fragment = this,
                viewModel = viewModel,
                followButton = follow,
                setting = requireContext().prefs().getString(C.UI_FOLLOW_BUTTON, "0")?.toInt() ?: 0,
                account = Account.get(activity),
                helixClientId = requireContext().prefs().getString(C.HELIX_CLIENT_ID, "ilfexgv3nnljz3isbm257gzwrzr7bi"),
                gqlClientId = requireContext().prefs().getString(C.GQL_CLIENT_ID, "kimne78kx3ncx6brgo4mv6wki5h1ko"),
                gqlClientId2 = requireContext().prefs().getString(C.GQL_CLIENT_ID2, "kd1unb4b3q4t58fwlpcbzcbnm76a8fp")
            )
        }
    }

    private fun updateStreamLayout(stream: Stream?) {
        val activity = requireActivity() as MainActivity
        if (stream?.type?.lowercase() == "rerun") {
            watchLive.text = getString(R.string.watch_rerun)
            watchLive.setOnClickListener { activity.startStream(stream) }
        } else {
            if (stream?.viewerCount != null) {
                watchLive.text = getString(R.string.watch_live)
                watchLive.setOnClickListener { activity.startStream(stream) }
            } else {
                if (stream?.user?.lastBroadcast != null) {
                    TwitchApiHelper.formatTimeString(requireContext(), stream.user.lastBroadcast!!).let {
                        if (it != null)  {
                            lastBroadcast.visible()
                            lastBroadcast.text = requireContext().getString(R.string.last_broadcast_date, it)
                        } else {
                            lastBroadcast.gone()
                        }
                    }
                }
            }
        }
        stream?.channelLogo.let {
            if (it != null) {
                userLayout.visible()
                userImage.visible()
                userImage.loadImage(this, it, circle = true)
                requireArguments().putString(C.CHANNEL_PROFILEIMAGE, it)
            } else {
                userImage.gone()
            }
        }
        stream?.channelName.let {
            if (it != null && it != requireArguments().getString(C.CHANNEL_DISPLAYNAME)) {
                userLayout.visible()
                userName.visible()
                userName.text = it
                requireArguments().putString(C.CHANNEL_DISPLAYNAME, it)
            }
        }
        stream?.channelLogin.let {
            if (it != null && it != requireArguments().getString(C.CHANNEL_LOGIN)) {
                requireArguments().putString(C.CHANNEL_LOGIN, it)
            }
        }
        stream?.id.let {
            if (it != null && it != requireArguments().getString(C.STREAM_ID)) {
                requireArguments().putString(C.STREAM_ID, it)
            }
        }
        if (stream?.title != null) {
            streamLayout.visible()
            title.visible()
            title.text = stream.title.trim()
        } else {
            title.gone()
        }
        if (stream?.gameName != null) {
            streamLayout.visible()
            gameName.visible()
            gameName.text = stream.gameName
            if (stream.gameId != null) {
                gameName.setOnClickListener { activity.openGame(stream.gameId, stream.gameName) }
            }
        } else {
            gameName.gone()
        }
        if (stream?.viewerCount != null) {
            streamLayout.visible()
            viewers.visible()
            viewers.text = TwitchApiHelper.formatViewersCount(requireContext(), stream.viewerCount ?: 0)
        } else {
            viewers.gone()
        }
        if (requireContext().prefs().getBoolean(C.UI_UPTIME, true)) {
            if (stream?.startedAt != null) {
                TwitchApiHelper.getUptime(requireContext(), stream.startedAt).let {
                    if (it != null)  {
                        streamLayout.visible()
                        uptime.visible()
                        uptime.text = requireContext().getString(R.string.uptime, it)
                    } else {
                        uptime.gone()
                    }
                }
            }
        }
    }

    private fun updateUserLayout(user: User) {
        if (!userImage.isVisible && user.channelLogo != null) {
            userLayout.visible()
            userImage.visible()
            userImage.loadImage(this, user.channelLogo, circle = true)
            requireArguments().putString(C.CHANNEL_PROFILEIMAGE, user.channelLogo)
        }
        if (user.bannerImageURL != null) {
            bannerImage.visible()
            bannerImage.loadImage(this, user.bannerImageURL)
            if (userName.isVisible) {
                userName.setShadowLayer(4f, 0f, 0f, Color.BLACK)
            }
        } else {
            bannerImage.gone()
        }
        if (user.createdAt != null) {
            userCreated.visible()
            userCreated.text = requireContext().getString(R.string.created_at, TwitchApiHelper.formatTimeString(requireContext(), user.createdAt))
            if (user.bannerImageURL != null) {
                userCreated.setTextColor(Color.LTGRAY)
                userCreated.setShadowLayer(4f, 0f, 0f, Color.BLACK)
            }
        } else {
            userCreated.gone()
        }
        if (user.followersCount != null) {
            userFollowers.visible()
            userFollowers.text = requireContext().getString(R.string.followers, TwitchApiHelper.formatCount(requireContext(), user.followersCount))
            if (user.bannerImageURL != null) {
                userFollowers.setTextColor(Color.LTGRAY)
                userFollowers.setShadowLayer(4f, 0f, 0f, Color.BLACK)
            }
        } else {
            userFollowers.gone()
        }
        val broadcasterType = if (user.broadcasterType != null) { TwitchApiHelper.getUserType(requireContext(), user.broadcasterType) } else null
        val type = if (user.type != null) { TwitchApiHelper.getUserType(requireContext(), user.type) } else null
        val typeString = if (broadcasterType != null && type != null) "$broadcasterType, $type" else broadcasterType ?: type
        if (typeString != null) {
            userType.visible()
            userType.text = typeString
            if (user.bannerImageURL != null) {
                userType.setTextColor(Color.LTGRAY)
                userType.setShadowLayer(4f, 0f, 0f, Color.BLACK)
            }
        } else {
            userType.gone()
        }
        if (requireArguments().getBoolean(C.CHANNEL_UPDATELOCAL)) {
            viewModel.updateLocalUser(requireContext(), user)
        }
    }

    override fun onNetworkRestored() {
        viewModel.retry(requireContext().prefs().getString(C.HELIX_CLIENT_ID, "ilfexgv3nnljz3isbm257gzwrzr7bi"), Account.get(requireContext()).helixToken, requireContext().prefs().getString(C.GQL_CLIENT_ID, "kimne78kx3ncx6brgo4mv6wki5h1ko"))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            appBar.setExpanded(false, false)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 3 && resultCode == Activity.RESULT_OK) {
            requireActivity().recreate()
        }
    }

    override fun scrollToTop() {
        appBar?.setExpanded(true, true)
        (currentFragment as? Scrollable)?.scrollToTop()
    }
}