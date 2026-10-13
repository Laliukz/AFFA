package com.lkz.affa.domain.model

enum class MyListStatus(val label: String) {
    WATCHING("Viendo"),
    PLAN_TO_WATCH("Planeado"),
    COMPLETED("Completado")
}

data class MyListItem(
    val title: String,
    val slug: String = "",
    val imageUrl: String = "",
    val bannerUrl: String? = null,
    val score: Double? = null,
    val genres: List<String> = emptyList(),
    val status: MyListStatus = MyListStatus.PLAN_TO_WATCH,
    val addedAt: Long = System.currentTimeMillis()
)
