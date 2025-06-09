package com.asistanemrahcloudstream

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.movie
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.Jsoup

class FilmModu : MainAPI() {
    override var mainUrl = "https://www.filmmodu.nl"
    override var name = "FilmModu"
    override val hasMainPage = true
    override var lang = "tr"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override val mainPage = mainPageOf(
        "$mainUrl/filmler/page/" to "Filmler",
        "$mainUrl/diziler/page/" to "Diziler",
        "$mainUrl/animeler/page/" to "Animeler",
        "$mainUrl/arsiv/page/" to "Arşiv",
        "$mainUrl/turler/page/" to "Film Türleri",
        "$mainUrl/yillar/page/" to "Film Yılları",
        "$mainUrl/en-cok-izlenen/page/" to "En Çok İzlenen Filmler",
        "$mainUrl/seri-filmler/page/" to "Seri Filmler",
        "$mainUrl/oyuncular/page/" to "Oyuncular",
        "$mainUrl/yonetmenler/page/" to "Yönetmenler",
        "$mainUrl/altyazili/page/" to "Altyazılı Filmler",
        "$mainUrl/turkce-dublaj/page/" to "Türkçe Dublaj Filmler"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data + page).document
        val home = document.select("div.list-content div.ml-item").mapNotNull {
            val title = it.selectFirst("h2")?.text() ?: return@mapNotNull null
            val link = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val poster = it.selectFirst("img")?.attr("data-original") ?: ""
            val isTv = link.contains("/dizi/")
            val quality = it.selectFirst(".mli-quality")?.text()

            newMovieSearchResponse(title, link, if (isTv) TvType.TvSeries else TvType.Movie) {
                this.posterUrl = poster
                this.quality = getQualityFromString(quality)
            }
        }

        return newHomePageResponse(request.name, home)
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1")?.text() ?: throw ErrorLoadingException("No title found")
        val poster = document.selectFirst(".poster img")?.attr("src")
        val description = document.selectFirst(".desc")?.text()
        val year = document.selectFirst(".mvici-right span")?.text()?.toIntOrNull()
        val tags = document.select("a[href*=/tur/]").map { it.text() }
        val isTv = url.contains("/dizi/")
        val trailer = document.select("iframe[src*='youtube']").attr("src")
        val recommendations = document.select(".movie_list ul li").mapNotNull {
            val recTitle = it.selectFirst(".title")?.text() ?: return@mapNotNull null
            val recLink = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val recPoster = it.selectFirst("img")?.attr("src")
            MovieSearchResponse(
                recTitle,
                recLink,
                this.name,
                TvType.Movie,
                recPoster,
                null,
                null,
                null
            )
        }

        val episodes = document.select("#player-iframe embed, #player-iframe iframe").map {
            val src = it.attr("src")
            Episode(
                name = "Bölüm",
                data = src
            )
        }

        return if (isTv) {
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.year = year
                this.tags = tags
                this.plot = description
                this.recommendations = recommendations
                this.trailerUrl = trailer
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, episodes.firstOrNull()?.data ?: "") {
                this.posterUrl = poster
                this.year = year
                this.tags = tags
                this.plot = description
                this.recommendations = recommendations
                this.trailerUrl = trailer
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        loadExtractor(data, mainUrl, subtitleCallback, callback)
        return true
    }
}
