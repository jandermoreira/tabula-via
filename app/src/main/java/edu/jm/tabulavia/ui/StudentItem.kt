package edu.jm.tabulavia.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.graphics.Color // Adicionar esta importação para Color.Unspecified

@Composable
fun StudentItem(
    student: Student,
    modifier: Modifier = Modifier,
    isAbsent: Boolean = false
) {
    GridItemCard(modifier = modifier.alpha(if (isAbsent) 0.5f else 1f)) {
        Icon(
            painter = painterResource(id = R.drawable.student_coconut),
            contentDescription = "Ícone do Aluno",
            modifier = Modifier.size(40.dp),
            tint = Color.Unspecified // Definir tint como Unspecified para manter as cores originais da imagem
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = student.displayName,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
