package com.github.andreyasadchy.xtra.ui.common

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.model.Account
import com.github.andreyasadchy.xtra.model.NotLoggedIn
import com.github.andreyasadchy.xtra.ui.login.LoginActivity
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.ui.settings.SettingsActivity
import com.github.andreyasadchy.xtra.util.nullIfEmpty
import kotlinx.android.synthetic.main.fragment_media.*


abstract class MediaFragment : Fragment(), Scrollable {

    protected var previousItem = -1
    protected var currentFragment: Fragment? = null
    protected var firstLaunch = true

    open val spinnerItems: Array<String>
        get() = resources.getStringArray(R.array.spinnerMedia)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        previousItem = savedInstanceState?.getInt("previousItem", -1) ?: -1
        firstLaunch = savedInstanceState == null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_media, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity() as MainActivity
        val account = Account.get(activity)
        search.setOnClickListener { activity.openSearch() }
        menu.setOnClickListener { it ->
            PopupMenu(activity, it).apply {
                inflate(R.menu.top_menu)
                menu.findItem(R.id.login).title = if (account !is NotLoggedIn) getString(R.string.log_out) else getString(R.string.log_in)
                setOnMenuItemClickListener {
                    when(it.itemId) {
                        R.id.settings -> { activity.startActivityFromFragment(this@MediaFragment, Intent(activity, SettingsActivity::class.java), 3) }
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
                    }
                    true
                }
                show()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("previousItem", previousItem)
        super.onSaveInstanceState(outState)
    }

    abstract fun onSpinnerItemSelected(position: Int): Fragment

    override fun scrollToTop() {
        appBar?.setExpanded(true, true)
        (currentFragment as? Scrollable)?.scrollToTop()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 3 && resultCode == Activity.RESULT_OK) {
            requireActivity().recreate()
        }
    }
}