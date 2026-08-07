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
 */
enum class InterventionAction(val id: String, val description: String) {
    A1("A1", "Monitorar o estudante no próximo ciclo de aprendizagem."),
    A2("A2", "Entrar em contato com o estudante."),
    A3("A3", "Verificar possíveis dificuldades acadêmicas ou administrativas."),
    A4("A4", "Recomendar revisão dos materiais de estudo."),
    A5("A5", "Recomendar a conclusão ou recuperação de atividades pendentes."),
    A6("A6", "Fornecer feedback individual."),
    A7("A7", "Agendar uma reunião individual."),
    A8("A8", "Elaborar um plano de recuperação individual."),
    A9("A9", "Revisar as evidências coletadas durante o ciclo de aprendizagem."),
    A10("A10", "Encaminhar o estudante para apoio institucional, quando aplicável.")
}
