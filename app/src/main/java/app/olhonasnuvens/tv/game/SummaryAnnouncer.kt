package app.olhonasnuvens.tv.game

/**
 * Só devolve um texto novo. A comparação é o resumo inteiro: pontuação e fase
 * não bastam, porque o foco do menu, o modo, o recorde e a ajuda também mudam.
 */
class SummaryAnnouncer {
    private var published: String? = null

    fun update(summary: String): String? {
        if (summary == published) return null
        published = summary
        return summary
    }
}
