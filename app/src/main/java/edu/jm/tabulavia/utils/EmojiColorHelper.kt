package edu.jm.tabulavia.utils

import androidx.compose.ui.graphics.Color

/**
 * Helper utility to provide deterministic visual identifiers for students.
 * Maps unique identifiers to specific emojis and color schemes to ensure
 * UI consistency across the application.
 */
object EmojiColorHelper {

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
        "🐕",
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

    fun mapIdToEmoji(id: String): String {
        val cleanedId = id.toLongOrNull() ?: return animalEmojis.first()
        val index = (cleanedId * 137 % animalEmojis.size.toLong()).toInt()
        return animalEmojis[index]
    }

    /**
     * Generates a deterministic color based on a student identifier using the HSV model.
     * * @param studentNumber The unique identifier of the student.
     * @return A Compose Color object with calculated Hue, Saturation, and Value.
     */
    fun mapIdToColor(id: String): Color {
        val cleanedId = id.toLongOrNull() ?: return Color.Companion.Red

        val hue = (cleanedId * 137 % 360).toFloat()
        val saturation = if (cleanedId % 2L == 0L) 0.9f else 0.7f
        val value = if (cleanedId % 3L == 0L) 0.9f else 1f

        return Color.hsv(hue, saturation, value)
    }
}