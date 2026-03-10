/**
 * Helper utility to provide deterministic visual identifiers for students.
 * Maps unique identifiers to specific emojis and color schemes to ensure
 * UI consistency across the application.
 */

package edu.jm.tabulavia.ui

import androidx.compose.ui.graphics.Color

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
        val index = (studentId.toLong() * 137 % animalEmojis.size.toLong()).toInt()
        return animalEmojis[index]
//        return "\uD83D\uDC64"
    }

    /**
     * Generates a deterministic color based on a student identifier using the HSV model.
     * * @param studentId The unique identifier of the student.
     * @return A Compose Color object with calculated Hue, Saturation, and Value.
     */
    fun generateColorFromId(studentId: String): Color {
        val hue = (studentId.toLong() * 137 % 360).toFloat()
        val saturation = if (studentId.toLong() % 2L == 0L) 0.9f else 0.7f
        val value = if (studentId.toLong() % 3L == 0L) 0.9f else 1f

        return Color.hsv(hue, saturation, value)
    }
}