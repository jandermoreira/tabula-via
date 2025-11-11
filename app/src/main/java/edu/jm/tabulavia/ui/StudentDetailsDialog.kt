package edu.jm.tabulavia.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.model.SkillState
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.model.StudentSkill

@Composable
fun StudentDetailsDialog(
    student: Student,
    attendancePercentage: Float?,
    skills: List<StudentSkill>,
    onDismiss: () -> Unit,
    onEditSkills: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(student.name) },
        text = {
            Column {
                Text("Matrícula: ${student.studentNumber}")
                Spacer(modifier = Modifier.height(8.dp))
                if (attendancePercentage != null) {
                    Text("Frequência: %.0f%%".format(attendancePercentage))
                } else {
                    Text("Frequência: Impossível", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Habilidades", style = MaterialTheme.typography.titleMedium)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                if (skills.isEmpty()) {
                    Text("Nenhuma habilidade registrada.")
                } else {
                    skills.forEach { skill ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = skill.state.toIcon(),
                                contentDescription = skill.state.displayName,
                                tint = skill.state.toColor(),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(skill.skillName, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        },
        dismissButton = {
            Button(onClick = onEditSkills) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Editar Habilidades",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Editar")
            }
        }
    )
}

fun SkillState.toIcon(): ImageVector {
    return when (this) {
        SkillState.ALTO -> Icons.Filled.ArrowUpward
        SkillState.MEDIO -> Icons.Filled.DragHandle
        SkillState.BAIXO -> Icons.Filled.ArrowDownward
        SkillState.NAO_SE_APLICA -> Icons.Filled.Remove
    }
}

@Composable
fun SkillState.toColor(): Color {
    return when (this) {
        SkillState.ALTO -> Color(0xFF4CAF50) // Verde
        SkillState.MEDIO -> Color(0xFFFFC107) // Amarelo
        SkillState.BAIXO -> MaterialTheme.colorScheme.error // Vermelho
        SkillState.NAO_SE_APLICA -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) // Cinza
    }
}
