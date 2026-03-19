package edu.jm.tabulavia.utils

import androidx.compose.ui.graphics.Color

/**
 * Helper utility to provide deterministic visual identifiers for students.
 * Maps unique identifiers to specific emojis and color schemes to ensure
 * UI consistency across the application.
 */
object StudentEmojiColorHelper {

    /**
     * Comprehensive list of animal-themed emojis used for student avatars.
     * Maintains specific selection and exclusions as defined in the original set.
     */
    private val animalEmojis = listOf(
        "🐵",
        "🐒",
        "🦍",
        "🦧",
        "🐶",
        "🐕",
        "🦮",
        "🐕‍🦺",
        "🐩",
        "🐺",
        "🦊",
        "🦝",
        "🐱",
        "🐈",
        "🐈‍⬛",
        "🦁",
//        "🐯",
        "🐅",
        "🐆",
        "🐴",
        "🐎",
//        "🦄",
        "🦓",
//        "🦌",
        "🦬",
//        "🐮",
        "🐂",
        "🐃",
//        "🐄",
//        "🐷",
//        "🐖",
        "🐗",
//       "🐽",
        "🐏",
        "🐑",
        "🐐",
//       "🐪",
        "🐫",
        "🦙",
        "🦒",
        "🐘",
        "🦣",
        "🦏",
        "🦛",
        "🐭",
        "🐁",
//       "🐀",
        "🐹",
        "🐰",
        "🐇",
        "🐿️",
        "🦫",
        "🦔",
        "🦇",
//        "🐻",
        "🐻‍❄️",
        "🐨",
//        "🐼",
//       "🦥",
        "🦦",
//       "🦨",
        "🦘",
        "🦡",
//       "🐾",
        "🦃",
//       "🐔",
        "🐓",
//       "🐣",
//       "🐤",
//       "🐥",
        "🐦",
        "🐧",
        "🕊️",
        "🦅",
        "🦆",
        "🦢",
        "🦉",
        "🦤",
        "🦩",
//       "🦚",
        "🦜",
//       "🐸",
        "🐊",
        "🐢",
        "🦎",
        "🐍",
        "🐲",
        "🐉",
        "🦕",
        "🦖",
//       "🐳",
//        "🐋",
        "🐬",
        "🦭",
        "🐟",
        "🐠",
        "🐡",
        "🦈",
        "🐙",
//       "🐚",
//       "🐌",
        "🦋",
//       "🐛",
        "🐜",
        "🐝",
        "🪲",
        "🐞",
        "🦗",
//       "🪳",
        "🕷️",
//       "🕸️",
//       "🦂",
//       "🦟",
//       "🪰",
//       "🪱",
//       "🦠"
    )

//    private val foodEmojis = listOf(
//        "🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐",
//        "🍈", "🍒", "🍑", "🥭", "🍍", "🥥", "🥝", "🍅", "🍆", "🥑",
//        "🥦", "🥬", "🥒", "🌶️", "🫑", "🌽", "🥕", "🫒", "🧄", "🧅",
//        "🍄", "🥜", "🫘", "🌰", "🍞", "🥐", "🥖", "🫓", "🥨", "🥯",
//        "🥞", "🧇", "🧀", "🍖", "🍗", "🥩", "🥓", "🍔", "🍟", "🍕",
//        "🌭", "🥪", "🌮", "🌯", "🫔", "🥙", "🧆", "🥚", "🍳", "🥘",
//        "🍲", "🥣", "🥗", "🍿", "🧈", "🧂", "🥫", "🍱", "🍘", "🍙",
//        "🍚", "🍛", "🍜", "🍝", "🍠", "🍢", "🍣", "🍤", "🍥", "🥮",
//        "🍡", "🥟", "🥠", "🥡", "🦀", "🦞", "🦐", "🦑", "🦪", "🍦",
//        "🍧", "🍨", "🍩", "🍪", "🎂", "🍰", "🧁", "🥧", "🍫", "🍬",
//        "🍭", "🍮", "🍯", "🍼", "🥛", "☕", "🫖", "🍵", "🍶", "🍾",
////        "🍷", "🍸", "🍹",
////        "🍺", "🍻", "🥂", "🥃",
//        "🥤", "🧋", "🧃",
//        "🧉", "🧊"
//    )

    fun mapStudentIdToEmoji(studentId: String): String {
        val id = studentId.toLongOrNull() ?: return animalEmojis.first()
        val index = (id * 137 % animalEmojis.size.toLong()).toInt()
        return animalEmojis[index]
    }

    /**
     * Generates a deterministic color based on a student identifier using the HSV model.
     * * @param studentNumber The unique identifier of the student.
     * @return A Compose Color object with calculated Hue, Saturation, and Value.
     */
    fun generateColorFromId(studentNumber: String): Color {
        val id = studentNumber.toLongOrNull() ?: return Color.Companion.Red

        val hue = (id * 137 % 360).toFloat()
        val saturation = if (id % 2L == 0L) 0.9f else 0.7f
        val value = if (id % 3L == 0L) 0.9f else 1f

        return Color.Companion.hsv(hue, saturation, value)
    }
}