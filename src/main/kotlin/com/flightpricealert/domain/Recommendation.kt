package com.flightpricealert.domain

enum class Recommendation(val emoji: String, val title: String, val message: String) {
    NONE(
        "⚪",
        "Sem recomendação",
        "Nenhuma condição especial identificada no momento."
    ),
    TRENDING_DOWN(
        "🟡",
        "Pode valer esperar um pouco",
        "O preço está abaixo da média e ainda caindo — talvez valha esperar mais um pouco antes de comprar."
    ),
    GOOD_PRICE(
        "🟢",
        "Bom momento para comprar",
        "O preço está bem abaixo da média histórica dessa rota."
    ),
    BEST_PRICE(
        "🟢",
        "Compre agora!",
        "Este é o menor preço já registrado para essa rota."
    )
}
