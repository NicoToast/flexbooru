/*
 * Copyright (C) 2019. by onlymash <im@fiepi.me>, All rights reserved
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package onlymash.flexbooru.repository.post

import androidx.paging.PagedList
import androidx.paging.PagingRequestHelper
import onlymash.flexbooru.api.ApApi
import onlymash.flexbooru.api.url.ApUrlHelper
import onlymash.flexbooru.entity.Search
import onlymash.flexbooru.entity.TagBlacklist
import onlymash.flexbooru.entity.post.PostAp
import onlymash.flexbooru.entity.post.PostApResponse
import onlymash.flexbooru.extension.createStatusLiveData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Executor

class PostApBoundaryCallback(
    private val apApi: ApApi,
    private val handleResponse: (Search, MutableList<PostAp>?, MutableList<TagBlacklist>) -> Unit,
    private val ioExecutor: Executor,
    private val search: Search,
    private val tagBlacklists: MutableList<TagBlacklist>
) : PagedList.BoundaryCallback<PostAp>() {

    //paging request helper
    val helper = PagingRequestHelper(ioExecutor)
    // network state
    val networkState = helper.createStatusLiveData()

    //last response posts size
    var lastResponseSize = search.limit

    private fun createApCallback(it: PagingRequestHelper.Request.Callback)
            : Callback<PostApResponse> {
        return object : Callback<PostApResponse> {
            override fun onFailure(
                call: Call<PostApResponse>,
                t: Throwable) {
                it.recordFailure(t)
            }

            override fun onResponse(
                call: Call<PostApResponse>,
                response: Response<PostApResponse>
            ) {
                val data = response.body()
                if (data != null) {
                    val posts = data.posts
                }
            }
        }
    }

    override fun onZeroItemsLoaded() {
        helper.runIfNotRunning(PagingRequestHelper.RequestType.INITIAL) {
            apApi.getPosts(ApUrlHelper.getPostUrl(0, 20)).enqueue(createApCallback(it))
        }
    }

    override fun onItemAtEndLoaded(itemAtEnd: PostAp) {

    }


    override fun onItemAtFrontLoaded(itemAtFront: PostAp) {

    }
}