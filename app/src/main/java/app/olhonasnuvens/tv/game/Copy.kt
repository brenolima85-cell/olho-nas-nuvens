package app.olhonasnuvens.tv.game

/** Textos visíveis do jogo. A interface é fixa em pt-BR. */
object Copy {
    const val TITLE = "Olho nas Nuvens"
    const val CLINIC = "Clínica de Olhos"
    const val PROVISIONAL = "Clínica de Olhos · marca provisória, sem logo oficial"
    const val SUBTITLE = "Um jogo calmo para a sala de espera"
    const val DISCLAIMER =
        "Entretenimento apenas. Não faz diagnóstico e não promete melhora da visão."
    const val EASY = "Modo fácil"
    const val NORMAL = "Modo normal"
    const val EASY_HINT = "Vãos mais largos e ritmo mais lento"
    const val NORMAL_HINT = "O ritmo aumenta um pouco, com limite"
    const val HOW_TO = "Como jogar"
    const val HOW_TO_HINT = "Ver os controles do remoto"
    const val HELP_TITLE = "Como jogar"
    const val BACK = "Voltar"
    const val PAUSED = "Pausado"
    const val PAUSED_HINT = "A partida está parada."
    const val CONTINUE = "Continuar"
    const val RETRY = "Tentar de novo"
    const val TO_TITLE = "Voltar ao início"
    const val GAME_OVER = "Fim de jogo"
    const val HIT_CLOUD = "O olho encostou na nuvem."
    const val POINTS = "Pontos"
    const val BEST = "Recorde"
    const val NEW_RECORD = "Novo recorde"

    val HELP_LINES = listOf(
        "Pressione OK no controle remoto, ou A no controle, para o olho subir.",
        "Sem apertar, a gravidade faz o olho descer.",
        "No começo, a descida é mais lenta por um instante.",
        "Passe pelo vão entre as nuvens para ganhar um ponto.",
        "O modo fácil é mais lento e tem vãos mais largos.",
        "O modo normal acelera aos poucos, até um limite.",
        "Voltar pausa a partida. No pause, Voltar continua.",
        "Para sair da partida, escolha Voltar ao início.",
        "É só diversão: não examina os olhos e não trata a visão.",
    )

    fun bestLabel(difficulty: Difficulty): String = when (difficulty) {
        Difficulty.EASY -> "Recorde do modo fácil"
        Difficulty.NORMAL -> "Recorde do modo normal"
    }
}
