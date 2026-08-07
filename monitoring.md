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

Measures the difference between the student's Monitoring Performance during a learning cycle and the
corresponding Consolidation Evidence.

### Calculation

\[
\Delta D=P_M-CE_k
\]

where

- \(P_M\) is the Monitoring Performance of the completed cycle;
- \(CE_k\) is the grade obtained in the corresponding Consolidation Evidence.

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

# Operational Status

Operational Status identifies students requiring intervention during the current learning cycle.

| Status    | Trigger Condition                                                                                      | Instructor Action                                  |
|-----------|--------------------------------------------------------------------------------------------------------|----------------------------------------------------|
| On Track  | No missing submissions, \(P_M\ge6.0\), and \(A<15\%\)                                                  | No intervention.                                   |
| Attention | One missing submission OR \(P_M<6.0\) caused by one Monitoring Evidence OR \(15\%\le A<20\%\)          | Standardized notification and attendance reminder. |
| Critical  | Two or more missing submissions OR \(P_M<6.0\) in two consecutive Monitoring Evidences OR \(A\ge20\%\) | Individual meeting and formal risk registration.   |

# Additional Flags

Additional Flags do not replace the Operational Status.

They indicate situations requiring complementary investigation.

| Flag                        | Trigger            |
|-----------------------------|--------------------|
| **Performance Discrepancy** | \(\Delta D\ge3.0\) |

# Dashboard Architecture

## Class Overview

Students are ordered by Operational Status:

1. Critical
2. Attention
3. On Track

Additional Flags are displayed independently.

### Cards containers

| Field                   | Description                                                 |
|-------------------------|-------------------------------------------------------------|
| Student                 | Student effective name                                      |
| Regularity              | Missing Monitoring Evidence submissions                     |
| Performance             | Current Monitoring Performance                              |
| Attendance              | Current attendance percentage                               |
| Performance Discrepancy | Difference between Monitoring and Consolidation Performance |
| Operational Status      | Current intervention priority                               |
| Additional Flags        | Complementary alerts                                        |
| Recommended Action      | Suggested instructor action                                 |

## Student View

Displays the current indicators:

- Regularity;
- Performance;
- Attendance;
- Performance Discrepancy.

# Design Principles

- Monitor observable work rhythm rather than learning state.
- Use only evidence already produced during the class.
- Keep indicators independent whenever possible.
- Missing submissions and low performance represent different phenomena.
- Consolidation Evidence is not used to calculate monitoring indicators.
- Indicators are designed to generate operational instructor actions.
- Automated indicators support, but never replace, instructor judgment.