/**
 * Defines the intervention actions recommended by the monitoring system.
 */
package edu.jm.tabulavia.model

/**
 * Represents a generic intervention action recommended based on monitoring indicators.
 *
 * These actions follow the Action Catalog defined in the pedagogical guidelines
 * and are intended to support instructor decision-making.
 *
 * @property id The unique identifier of the action (e.g., A1, A2).
 * @property description The descriptive text of the action in Portuguese for UI display.
 * @property meaning The detailed pedagogical meaning of the action in Portuguese.
 */
enum class InterventionAction(
    val id: String,
    val description: String,
    val meaning: String
) {
    A1(
        "A1",
        "Entrar em contato com o estudante.",
        "Iniciar comunicação direta sobre o problema identificado."
    ),
    A2(
        "A2",
        "Investigar possíveis dificuldades acadêmicas ou administrativas.",
        "Explorar as causas raiz que afetam o progresso acadêmico ou a situação administrativa do estudante."
    ),
    A3(
        "A3",
        "Recomendar revisão dos materiais de estudo.",
        "Direcionar o estudante para revisar o conteúdo instrucional relevante."
    ),
    A4(
        "A4",
        "Recomendar a conclusão ou recuperação de atividades pendentes.",
        "Direcionar o estudante para concluir atividades ausentes ou de recuperação."
    ),
    A5(
        "A5",
        "Fornecer feedback individualizado.",
        "Fornecer feedback específico sobre o desempenho, participação ou processo de trabalho do estudante."
    ),
    A6(
        "A6",
        "Elaborar um plano de recuperação individual.",
        "Definir um conjunto estruturado de atividades de recuperação e expectativas para o estudante."
    ),
    A7(
        "A7",
        "Encaminhar o estudante para apoio institucional, quando aplicável.",
        "Direcionar o estudante para um serviço de apoio institucional apropriado quando a situação exigir suporte adicional."
    )
}
