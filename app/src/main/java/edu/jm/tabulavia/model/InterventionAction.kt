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
    A1("A1", "Entrar em contato com o estudante."),
    A2("A2", "Verificar possíveis dificuldades acadêmicas ou administrativas."),
    A3("A3", "Recomendar revisão dos materiais de estudo."),
    A4("A4", "Recomendar a conclusão ou recuperação de atividades pendentes."),
    A5("A5", "Fornecer orientação ao estudante."),
    A6("A6", "Elaborar um plano de recuperação individual."),
    A7("A7", "Encaminhar o estudante para apoio institucional, quando aplicável."),
}
