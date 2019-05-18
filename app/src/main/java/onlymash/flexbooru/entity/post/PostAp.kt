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

package onlymash.flexbooru.entity.post
import com.google.gson.annotations.SerializedName

data class PostAp(
    @SerializedName("big_preview")
    val bigPreview: String,
    @SerializedName("color")
    val color: List<Int>,
    @SerializedName("download_count")
    val downloadCount: Int,
    @SerializedName("erotics")
    val erotics: Int,
    @SerializedName("ext")
    val ext: String,
    @SerializedName("height")
    val height: Int,
    @SerializedName("id")
    val id: Int,
    @SerializedName("md5")
    val md5: String,
    @SerializedName("md5_pixels")
    val md5Pixels: String,
    @SerializedName("medium_preview")
    val mediumPreview: String,
    @SerializedName("pubtime")
    val pubtime: String,
    @SerializedName("score")
    val score: Int,
    @SerializedName("score_number")
    val scoreNumber: Int,
    @SerializedName("size")
    val size: Int,
    @SerializedName("small_preview")
    val smallPreview: String,
    @SerializedName("spoiler")
    val spoiler: Boolean,
    @SerializedName("status")
    val status: Int,
    @SerializedName("width")
    val width: Int
)