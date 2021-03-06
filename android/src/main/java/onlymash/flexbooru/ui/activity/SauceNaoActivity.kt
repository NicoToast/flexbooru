/*
 * Copyright (C) 2020. by onlymash <im@fiepi.me>, All rights reserved
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

package onlymash.flexbooru.ui.activity

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dekoservidoni.omfm.OneMoreFabMenu
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_sauce_nao.*
import kotlinx.android.synthetic.main.common_list.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import onlymash.flexbooru.R
import onlymash.flexbooru.common.Settings.isOrderSuccess
import onlymash.flexbooru.common.Settings.sauceNaoApiKey
import onlymash.flexbooru.di.kodeinCommon
import onlymash.flexbooru.extension.*
import onlymash.flexbooru.glide.GlideApp
import onlymash.flexbooru.saucenao.api.SauceNaoApi
import onlymash.flexbooru.saucenao.model.Result
import onlymash.flexbooru.saucenao.model.SauceNaoResponse
import onlymash.flexbooru.ui.viewmodel.SauceNaoViewModel
import onlymash.flexbooru.ui.viewmodel.getSauceNaoViewModel
import onlymash.flexbooru.extension.drawNavBar
import org.kodein.di.erased.instance
import java.io.IOException

const val SAUCE_NAO_SEARCH_URL_KEY = "sauce_nao_search_url"

private const val READ_IMAGE_REQUEST_CODE = 147

class SauceNaoActivity : BaseActivity() {

    companion object {
        fun startSearch(context: Context, url: String) {
            context.startActivity(
                Intent(context, SauceNaoActivity::class.java).apply {
                    putExtra(SAUCE_NAO_SEARCH_URL_KEY, url)
                }
            )
        }
    }

    private val api by kodeinCommon.instance<SauceNaoApi>("SauceNaoApi")

    private lateinit var sauceNaoViewModel: SauceNaoViewModel
    private var response: SauceNaoResponse? = null

    private lateinit var sauceNaoAdapter: SauceNaoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isOrderSuccess) {
            startActivity(Intent(this, PurchaseActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_sauce_nao)
        drawNavBar {
            list.updatePadding(bottom = it.systemWindowInsetBottom)
            sauce_nao_search_fab.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                bottomMargin = it.systemWindowInsetBottom +
                        resources.getDimensionPixelSize(R.dimen.margin_normal)
            }
        }
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setTitle(R.string.title_sauce_nao)
        }
        sauceNaoAdapter = SauceNaoAdapter()
        list.apply {
            layoutManager = LinearLayoutManager(this@SauceNaoActivity, RecyclerView.VERTICAL, false)
            adapter = sauceNaoAdapter
        }
        sauceNaoViewModel = getSauceNaoViewModel(api)
        sauceNaoViewModel.data.observe(this, Observer {
            response = it
            supportActionBar?.subtitle = String.format(getString(R.string.sauce_nao_remaining_times_today), it.header.longRemaining)
            sauceNaoAdapter.notifyDataSetChanged()
        })
        sauceNaoViewModel.isLoading.observe(this, Observer {
            progress_bar.isVisible = it
            if (it && error_msg.isVisible) {
                error_msg.isVisible = false
            }
        })
        sauceNaoViewModel.error.observe(this, Observer {
            if (!it.isNullOrBlank()) {
                error_msg.isVisible = true
                error_msg.text = it
            } else {
                error_msg.isVisible = false
            }
        })
        val url = intent?.getStringExtra(SAUCE_NAO_SEARCH_URL_KEY)
        if (!url.isNullOrEmpty()) {
            search(url)
        }
        sauce_nao_search_fab.setOptionsClick(object : OneMoreFabMenu.OptionsClick {
            override fun onOptionClick(optionId: Int?) {
                when (optionId) {
                    R.id.option_url -> searchByUrl()
                    R.id.option_file -> searchByFile()
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.sauce_nao, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sauce_nao_change_api_key -> {
                changeApiKey()
                true
            }
            R.id.action_sauce_nao_get_api_key -> {
                val url = "https://saucenao.com/user.php"
                launchUrl(url)
                true
            }
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun search(url: String) {
        val apiKey = sauceNaoApiKey
        if (apiKey.isNotEmpty()) {
            sauceNaoViewModel.searchByUrl(imageUrl = url, apiKey = apiKey)
        } else {
            error_msg.toVisibility(true)
            error_msg.setText(R.string.sauce_nao_api_key_unset)
        }
    }

    private fun searchByUrl() {
        val padding = resources.getDimensionPixelSize(R.dimen.spacing_mlarge)
        val layout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(padding, padding / 2, padding, 0)
        }
        val editText = EditText(this)
        layout.addView(editText)
        AlertDialog.Builder(this)
            .setTitle(R.string.sauce_nao_image_url)
            .setView(layout)
            .setPositiveButton(R.string.dialog_ok) { _, _ ->
                val url = (editText.text ?: "").toString().trim()
                if (url.startsWith("http")) {
                    search(url)
                } else {
                    Snackbar.make(root_container, R.string.sauce_nao_invalid_image_url, Snackbar.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .create()
            .show()
    }

    private fun searchByFile() {
        try {
            startActivityForResult(
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                },
                READ_IMAGE_REQUEST_CODE
            )
        } catch (_: ActivityNotFoundException) {}
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == READ_IMAGE_REQUEST_CODE) {
            data?.data?.also {
                search(it)
            }
        }
    }

    override fun onBackPressed() {
        if (sauce_nao_search_fab.isExpanded()) {
            sauce_nao_search_fab.collapse()
        } else {
            super.onBackPressed()
        }
    }

    private fun search(imageUri: Uri) {
        val apiKey = sauceNaoApiKey
        if (apiKey.isNotEmpty()) {
            lifecycleScope.launch {
                val byteArray = withContext(Dispatchers.IO) {
                    try {
                        contentResolver.openInputStream(imageUri)?.readBytes()
                    } catch (_: IOException) {
                        null
                    }
                }
                if (byteArray != null) {
                    sauceNaoViewModel.searchByImage(
                        apiKey = apiKey,
                        byteArray = byteArray,
                        fileExt = imageUri.toDecodedString().fileExt())
                }
            }
        } else {
            error_msg.isVisible = true
            error_msg.setText(R.string.sauce_nao_api_key_unset)
        }
    }

    private fun changeApiKey() {
        val padding = resources.getDimensionPixelSize(R.dimen.spacing_mlarge)
        val layout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(padding, padding / 2, padding, 0)
        }
        val editText = EditText(this).apply {
            setText(sauceNaoApiKey)
        }
        layout.addView(editText)
        AlertDialog.Builder(this)
            .setTitle(R.string.sauce_nao_change_api_key)
            .setView(layout)
            .setPositiveButton(R.string.dialog_ok) { _, _ ->
                val key = (editText.text ?: "").toString().trim()
                sauceNaoApiKey = key
                if (key.isEmpty()) {
                    error_msg.toVisibility(true)
                    error_msg.setText(R.string.sauce_nao_api_key_unset)
                } else {
                    error_msg.toVisibility(false)
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .create()
            .show()
    }

    inner class SauceNaoAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemCount(): Int = response?.results?.size ?: 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            SauceNaoViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_sauce_nao, parent, false))

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val result = response?.results?.get(position) ?: return
            (holder as SauceNaoViewHolder).bind(result)
        }

        inner class SauceNaoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            private val thumbnail: AppCompatImageView = itemView.findViewById(R.id.thumbnail)
            private val title: AppCompatTextView = itemView.findViewById(R.id.title)
            private val similarity: AppCompatTextView = itemView.findViewById(R.id.similarity)
            private val info1: AppCompatTextView = itemView.findViewById(R.id.info_1)
            private val info2: AppCompatTextView = itemView.findViewById(R.id.info_2)

            fun bind(result: Result) {
                similarity.text = result.header.similarity
                title.text = result.header.indexName
                when {
                    !result.data.characters.isNullOrEmpty() -> {
                        info1.text = String.format("Material: %s", result.data.material ?: "")
                        info2.text = String.format("Characters: %s", result.data.characters)
                    }
                    result.data.pixivId != null -> {
                        info1.text = String.format("Pixiv ID: %d", result.data.pixivId)
                        info2.text = String.format("Title: %s", result.data.title ?: "")
                    }
                    result.data.anidbAid != null -> {
                        info1.text = String.format("Anidb aid: %d", result.data.anidbAid)
                        info2.text = String.format("Source: %s", result.data.source ?: "")
                    }
                    result.data.seigaId != null -> {
                        info1.text = String.format("Seiga ID: %d", result.data.seigaId)
                        info2.text = String.format("Title: %s", result.data.title ?: "")
                    }
                    result.data.daId != null -> {
                        info1.text = String.format("Da ID: %d", result.data.daId)
                        info2.text = String.format("Title: %s", result.data.title ?: "")
                    }
                    result.data.engName != null -> {
                        info1.text = String.format("Eng name: %s", result.data.engName)
                        info2.text = String.format("Jp name: %s", result.data.jpName ?: "")
                    }
                }
                GlideApp.with(itemView.context)
                    .load(result.header.thumbnail)
                    .into(thumbnail)
                val urls = result.data.extUrls?.toTypedArray()
                if (!urls.isNullOrEmpty()) {
                    itemView.setOnClickListener {
                        AlertDialog.Builder(itemView.context)
                            .setTitle(R.string.sauce_nao_source)
                            .setItems(urls) { _, which ->
                                launchUrl(urls[which])
                            }
                            .create()
                            .show()
                    }
                }
            }
        }
    }
}
