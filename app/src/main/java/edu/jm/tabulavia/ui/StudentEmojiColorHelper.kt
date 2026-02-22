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

    fun mapStudentIdToEmoji(studentId: Long): String {
        val index = (studentId * 137 % animalEmojis.size.toLong()).toInt()
        return animalEmojis[index]
    }

    /**
     * Generates a deterministic color based on a student identifier using the HSV model.
     * * @param studentId The unique identifier of the student.
     * @return A Compose Color object with calculated Hue, Saturation, and Value.
     */
    fun generateColorFromId(studentId: Long): Color {
        val hue = (studentId * 137 % 360).toFloat()
        val saturation = if (studentId % 2L == 0L) 0.9f else 0.7f
        val value = if (studentId % 3L == 0L) 0.9f else 1f

        return Color.hsv(hue, saturation, value)
    }
}