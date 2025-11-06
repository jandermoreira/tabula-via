package edu.jm.classsupervision.model // Verifique se este é o nome do seu pacote

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Representa a tabela 'students' no banco de dados.
 */
@Entity(
    tableName = "students",
    // Define a chave estrangeira para criar um relacionamento com a tabela 'classes'.
    foreignKeys = [ForeignKey(
        entity = Class::class,      // A tabela pai da relação.
        parentColumns = ["classId"],// A chave primária na tabela pai.
        childColumns = ["classId"], // A coluna nesta tabela que referencia a chave pai.
        onDelete = ForeignKey.CASCADE // O que acontece se a turma for deletada.
    )],
    // Cria um índice na coluna 'classId' para otimizar as consultas.
    indices = [Index("classId")]
)
data class Student(
    @PrimaryKey(autoGenerate = true)
    val studentId: Long = 0,

    val name: String,
    val studentNumber: String, // Ex: Matrícula

    // Este campo armazena o ID da turma à qual o aluno pertence.
    // Ele deve corresponder a um 'classId' válido na tabela 'classes'.
    val classId: Long
)
