package com.github.andreyasadchy.xtra.ui.search.games

import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.github.andreyasadchy.xtra.model.ui.Game
import com.github.andreyasadchy.xtra.repository.ApiRepository
import com.github.andreyasadchy.xtra.repository.Listing
import com.github.andreyasadchy.xtra.ui.common.PagedListViewModel
import com.github.andreyasadchy.xtra.util.nullIfEmpty
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GameSearchViewModel @Inject constructor(
        private val repository: ApiRepository) : PagedListViewModel<Game>() {

    private val query = MutableLiveData<String>()
    private var helixClientId = MutableLiveData<String>()
    private var helixToken = MutableLiveData<String>()
    private var gqlClientId = MutableLiveData<String>()
    private var apiPref = MutableLiveData<ArrayList<Pair<Long?, String?>?>>()
    override val result: LiveData<Listing<Game>> = Transformations.map(query) {
        repository.loadSearchGames(it, helixClientId.value?.nullIfEmpty(), helixToken.value?.nullIfEmpty(), gqlClientId.value?.nullIfEmpty(), apiPref.value, viewModelScope)
    }

    fun setQuery(query: String, helixClientId: String? = null, helixToken: String? = null, gqlClientId: String? = null, apiPref: ArrayList<Pair<Long?, String?>?>) {
        if (this.helixClientId.value != helixClientId) {
            this.helixClientId.value = helixClientId
        }
        if (this.helixToken.value != helixToken) {
            this.helixToken.value = helixToken
        }
        if (this.gqlClientId.value != gqlClientId) {
            this.gqlClientId.value = gqlClientId
        }
        if (this.apiPref.value != apiPref) {
            this.apiPref.value = apiPref
        }
        if (this.query.value != query) {
            this.query.value = query
        }
    }
}