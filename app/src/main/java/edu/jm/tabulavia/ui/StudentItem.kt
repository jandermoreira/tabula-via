package edu.jm.tabulavia.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.model.Student
import androidx.compose.ui.res.painterResource
import edu.jm.tabulavia.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext // Adicionar esta importação

@Composable
fun StudentItem(
    student: Student,
    modifier: Modifier = Modifier,
    isAbsent: Boolean = false
) {
    val context = LocalContext.current

    val iconIndex = (student.studentId.mod(80L) + 1).toInt()
    val iconName = "student_${iconIndex}"

    val drawableResId = context.resources.getIdentifier(iconName, "drawable", context.packageName)

    GridItemCard(modifier = modifier.alpha(if (isAbsent) 0.5f else 1f)) {
        if (drawableResId != 0) {
            Icon(
                painter = painterResource(id = drawableResId),
                contentDescription = "Ícone do Aluno ${iconIndex}", // Descrição de conteúdo mais específica
                modifier = Modifier.size(40.dp),
                tint = Color.Unspecified // Manter para preservar as cores originais do PNG
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.student_0),
                contentDescription = "Ícone do Aluno Padrão",
                modifier = Modifier.size(40.dp),
                tint = Color.Unspecified
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = student.displayName,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
