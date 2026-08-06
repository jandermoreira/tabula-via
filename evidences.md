# Documento de Especificação: Plano de Acompanhamento Individual Inteligente

Este documento consolida as decisões estratégicas e técnicas para a implementação do Plano de Acompanhamento Individual no projeto **Tabula Via**, integrando dados do Moodle via Firestore.

---

## 1. Objetivos Pedagógicos

O objetivo central é transformar dados brutos de desempenho em diagnósticos acionáveis, permitindo que o professor identifique precocemente alunos em risco sem a necessidade de criar novos instrumentos de avaliação.

*   **Automatização:** Utilizar um scraper externo para colher dados do Moodle.
*   **Foco na Intervenção:** Diferenciar o que é acompanhamento (durante o processo) do que é consolidação (final do ciclo).
*   **Visão de Estado:** Classificar cada aluno em um estado claro de atenção (Normal, Revisão, Prioritário ou Recuperação).

---

## 2. Estratégia de Dados (Fluxo de Informação)

O sistema opera em um modelo de **Três Camadas**:

1.  **Entrada (Scraper):** Extrai nomes, datas e notas do Moodle e deposita no Firestore.
2.  **Persistência e Cache (Firestore/Room):** O Firestore atua como a "Fonte da Verdade". O aplicativo espelha esses dados no **Room (SQLite local)** para permitir cálculos de histórico e funcionamento offline.
3.  **Processamento (Aplicativo):** O aplicativo atua como o "Cérebro". Ele cruza a cronologia das atividades com as notas dos alunos para gerar o diagnóstico em tempo real.

---

## 3. Especificação Técnica do Firestore

A estrutura foi otimizada para fornecer uma **Visão de Turma** com o menor custo de leitura e maior eficiência para o scraper.

### Coleção: `evidences`
**Caminho:** `/users/{userEmail}/classes/{classId}/evidences/{evidenceId}`

O nome do documento deve ser o `evidenceId`

| Campo        | Tipo   | Descrição                                                            |
|:-------------|:-------|:---------------------------------------------------------------------|
| `evidenceId` | String | ID único (UUID ou ID do Moodle).                                     |
| `classId`    | String | ID da turma.                                                         |
| `name`       | String | Nome da fonte de evidência (ex: "Lista de Condicionais", "Prova 1"). |
| `deadline`   | Number | Data da atividade (garante a cronologia para todos).                 |
| `type`       | String | "MONITORING" (acompanhamento) ou "CONSOLIDATION" (consolidação).     |
| `scores`     | Map    | Lista de notas da evidência para cada aluno `{ studentId: nota }`.   |


---

## 4. Considerações de Implementação

*   **Cronologia:** É ditada pelo campo `deadline`. O app ordena as atividades por esta data para calcular a evolução e consistência.
*   **Reatividade:** O aplicativo não armazena o "Estado" no Firestore; ele o calcula sob demanda (com apoio do cache no Room), garantindo que o diagnóstico esteja sempre sincronizado com os dados mais recentes do scraper.
