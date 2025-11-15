package edu.jm.tabulavia.model

/**
 * Enum para representar a tendência de uma habilidade com base no histórico de avaliações.
 */
enum class SkillTrend {
    IMPROVING, // ("Melhorando"),
    STABLE, // ("Estável"),
    DECLINING // ("Regredindo")
}

/**
 * Data class que representa o estado calculado de uma habilidade para um aluno.
 * Este não é um objeto de banco de dados, mas sim um modelo de UI/ViewModel.
 *
 * @param skillName O nome da habilidade.
 * @param currentLevel O nível atual calculado (pode ser o mais recente, média, etc.).
 * @param trend A tendência de progresso (melhorando, estável, regredindo).
 * @param assessmentCount O número de avaliações consideradas no cálculo.
 * @param lastAssessedTimestamp A data/hora da última avaliação.
 */
data class SkillStatus(
    val skillName: String,
    val currentLevel: SkillLevel,
    val trend: SkillTrend,
    val assessmentCount: Int,
    val lastAssessedTimestamp: Long
)
