# Student Work Rhythm Monitoring Dashboard

# Operational Scope and Premises

## Objective

The objective is to monitor each student's work rhythm throughout the semester. It does not attempt
to estimate the student's learning state. Instead, it identifies observable signals that indicate:

- maintenance of the expected work rhythm;
- interruption of participation;
- administrative risk;
- inconsistencies between monitoring and consolidation evidence.

The indicators are intended exclusively to support instructor decisions.

## Approval Criterion

The operational minimum passing grade is

\[
N_{min}=6.0
\]

## Evidence Types

The model uses only evidence already produced by the class.

### Monitoring Evidence

Monitoring evidence is used to follow the student's work rhythm during a learning cycle.

Typical examples include:

- online questionnaires;
- programming exercises;
- laboratory activities;
- practical assignments.

Monitoring evidences are represented as

\[
ME_1,ME_2,\ldots,ME_n
\]

### Consolidation Evidence

Consolidation evidence is used to verify individual performance after a learning cycle.

Typical examples include:

- examinations;
- projects;
- practical assessments.

Consolidation evidences are represented as

\[
CE_1,CE_2,\ldots,CE_n
\]

Consolidation evidence is used exclusively for:

- consolidation;
- performance discrepancy analysis.

## Learning Cycle

A learning cycle is defined as a sequence of Monitoring Evidences terminated by one Consolidation
Evidence.

Each cycle is processed independently and historical indicators do not influence the calculations of
subsequent cycles.

# Monitoring Indicators

## Regularity

### Definition

Measures whether the student maintains the expected submission rhythm during the current learning
cycle.

### Metric

Number of missing Monitoring Evidence submissions.

### Interpretation

| Condition                       | Interpretation                       | Status    |
|---------------------------------|--------------------------------------|-----------|
| No missing submissions          | Regular participation                | Regular   |
| One missing submission          | Attention signal                     | Attention |
| Two or more missing submissions | Critical interruption of work rhythm | Critical  |

## Performance

### Definition

Represents the student's performance on Monitoring Evidence during the current learning cycle.

Only submitted and graded Monitoring Evidences are considered.
Missing submissions are excluded from this calculation and are monitored exclusively through the
Regularity indicator.

### Calculation

\[
P_M=
\frac{\sum_{i=1}^{m}G_i}{m}
\]

where

- \(G_i\) is the grade obtained in a graded Monitoring Evidence;
- \(m\) is the number of submitted and graded Monitoring Evidences in the current cycle.

### Interpretation

| Condition              | Interpretation             | Status    |
|------------------------|----------------------------|-----------|
| \(P_M \ge 6.0\)        | On Track                   | Regular   |
| \(4.0 \leq P_M < 6.0\) | Below Expected Rhythm      | Attention |
| \(P_M < 4.0\)          | Way below Expected Rhythim | Critical  |

## Attendance

### Definition

Measures the student's accumulated absence rate.

### Calculation

\[
A=
\frac{\text{Accumulated Absences}}
{\text{Total Planned Sessions}}
\times100
\]

### Interpretation

| Condition          | Interpretation       | Status    |
|--------------------|----------------------|-----------|
| \(A<15\%\)         | Normal attendance    | Regular   |
| \(15\%\le A<20\%\) | Attendance attention | Attention |
| \(A\ge20\%\)       | Attendance risk      | Critical  |

## Performance Discrepancy

### Definition

Measures the difference between the student's Monitoring Performance and the Consolidation Evidence
belonging to the same learning cycle.

### Calculation

\[
\Delta D=P_M-CE_k
\]

where

- \(P_M\) is the Monitoring Performance of the same cycle;
- \(CE_k\) is the grade obtained in the corresponding Consolidation Evidence of that cycle.

This metric is used only with consolidation evidences.

### Objective

Identify significant inconsistencies between continuous monitoring evidence and individual
consolidation evidence.

The indicator does not infer the cause of the discrepancy.

### Interpretation

| Condition                  | Interpretation                                     | Status    |
|----------------------------|----------------------------------------------------|-----------|
| \(\Delta D<3.0\)           | No significant discrepancy                         | Regular   |
| \(3.0 \le \Delta D < 5.0\) | Discrepancy requiring instructor analysis          | Attention |
| \(\Delta D \ge 5 \)        | Critital discrepancy requiring instructor analysis | Critical  |

# Intervention Actions

The dashboard does not prescribe instructional decisions. Instead, it recommends generic
intervention actions based on the status of each monitoring indicator. The recommended actions are
intended exclusively to support instructor decision-making and follow the concept of
*teacher-actionable insights* adopted in Learning Analytics research. :contentReference[oaicite:0]
{index=0}

## Action Catalog

| ID | Action                                                       |
|----|--------------------------------------------------------------|
| A1 | Contact the student.                                         |
| A2 | Verify possible academic or administrative difficulties.     |
| A3 | Recommend review of the learning materials.                  |
| A4 | Recommend completion or recovery of pending activities.      |
| A5 | Provide individual feedback.                                 |
| A6 | Schedule an individual meeting.                              |
| A7 | Develop an individual recovery plan.                         |
| A8 | Refer the student to institutional support, when applicable. |

## Intervention Matrix

| Indicator               | On Track | Attention | Critical       |
|-------------------------|----------|-----------|----------------|
| Regularity              | —        | A1, A4    | A1, A4, A6, A7 |
| Performance             | —        | A3, A5    | A3, A5, A6, A7 |
| Attendance              | —        | A1, A2    | A1, A2, A6, A8 |
| Performance Discrepancy | —        | A5        | A2, A5, A6     |

## Operational Rules

### On Track

No individual intervention is recommended.

### Attention

Apply the actions associated with the corresponding indicator at the **Attention** level.

### Critical

Apply the actions associated with the corresponding indicator at the **Critical** level with
priority.

