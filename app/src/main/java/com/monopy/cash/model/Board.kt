package com.monopy.cash.model

/**
 * The board from the Marathi "Business — नवा व्यापार" set.
 *
 * Square names come from the board and card photos. The rent ladders on the printed
 * cards are too small to read reliably, so they are derived here on Monopoly's
 * proportions and rounded to clean figures. This file is the single place to correct
 * any of it: edit a [Tile] and the board, the title-deed cards and rent collection all
 * follow.
 */

enum class TileType {
    GO,
    CITY,
    TRANSPORT,
    UTILITY,
    RESORT,
    CHANCE,
    TAX,
    JAIL,
    REST,
    GO_TO_JAIL
}

data class Tile(
    val index: Int,
    /** Name as printed on the board. */
    val name: String,
    /** Latin reading, shown under the Marathi on the title-deed card. */
    val latin: String,
    val type: TileType,
    val group: String? = null,
    val colorArgb: Int = 0,
    val price: Long = 0,
    val houseCost: Long = 0,
    /** Rent with 0, 1, 2, 3, 4 houses and a hotel. */
    val rent: List<Long> = emptyList()
) {
    val isOwnable: Boolean
        get() = type == TileType.CITY || type == TileType.TRANSPORT ||
            type == TileType.UTILITY || type == TileType.RESORT
}

/** Colour bands, matching the printed card groups. */
object Groups {
    const val BROWN = "brown"
    const val SKY = "sky"
    const val PINK = "pink"
    const val ORANGE = "orange"
    const val RED = "red"
    const val YELLOW = "yellow"
    const val GREEN = "green"
    const val BLUE = "blue"
    const val TRANSPORT = "transport"
    const val UTILITY = "utility"
    const val RESORT = "resort"

    val colors: Map<String, Int> = mapOf(
        BROWN to 0xFF8D6E63.toInt(),
        SKY to 0xFF4FC3F7.toInt(),
        PINK to 0xFFEC407A.toInt(),
        ORANGE to 0xFFFB8C00.toInt(),
        RED to 0xFFE53935.toInt(),
        YELLOW to 0xFFFDD835.toInt(),
        GREEN to 0xFF43A047.toInt(),
        BLUE to 0xFF1E88E5.toInt(),
        TRANSPORT to 0xFF37474F.toInt(),
        UTILITY to 0xFF00897B.toInt(),
        RESORT to 0xFF8E24AA.toInt()
    )
}

/** Pay day for passing GO. */
const val SALARY = 5_000L

/** Flat charge to leave the jail square. */
const val BAIL = 2_000L

object BoardData {

    private fun city(
        index: Int,
        name: String,
        latin: String,
        group: String,
        price: Long,
        houseCost: Long,
        rent: List<Long>
    ) = Tile(
        index = index,
        name = name,
        latin = latin,
        type = TileType.CITY,
        group = group,
        colorArgb = Groups.colors.getValue(group),
        price = price,
        houseCost = houseCost,
        rent = rent
    )

    private fun transport(index: Int, name: String, latin: String) = Tile(
        index = index,
        name = name,
        latin = latin,
        type = TileType.TRANSPORT,
        group = Groups.TRANSPORT,
        colorArgb = Groups.colors.getValue(Groups.TRANSPORT),
        price = 5_000,
        // Rent by how many of the four are held by the same owner.
        rent = listOf(1_000, 2_000, 4_000, 8_000)
    )

    private fun utility(index: Int, name: String, latin: String) = Tile(
        index = index,
        name = name,
        latin = latin,
        type = TileType.UTILITY,
        group = Groups.UTILITY,
        colorArgb = Groups.colors.getValue(Groups.UTILITY),
        price = 3_000
        // Rent is the dice roll times 200, or times 500 when both are held.
    )

    private fun resort(index: Int, name: String, latin: String, price: Long) = Tile(
        index = index,
        name = name,
        latin = latin,
        type = TileType.RESORT,
        group = Groups.RESORT,
        colorArgb = Groups.colors.getValue(Groups.RESORT),
        price = price,
        rent = listOf(2_500, 6_000)
    )

    val tiles: List<Tile> = listOf(
        Tile(0, "GO", "Start", TileType.GO),
        city(1, "पाटणा", "Patna", Groups.BROWN, 2_000, 1_000, listOf(100, 500, 1_500, 4_500, 6_000, 7_500)),
        Tile(2, "सुवर्ण संधी", "Golden chance", TileType.CHANCE),
        city(3, "नागपूर", "Nagpur", Groups.BROWN, 2_000, 1_000, listOf(100, 500, 1_500, 4_500, 6_000, 7_500)),
        Tile(4, "संपत्ति कर", "Property tax", TileType.TAX, price = 2_000),
        transport(5, "रेल्वे", "Railway"),
        city(6, "आग्रा", "Agra", Groups.SKY, 2_500, 1_000, listOf(200, 1_000, 3_000, 9_000, 12_500, 15_000)),
        Tile(7, "सुवर्ण संधी", "Golden chance", TileType.CHANCE),
        city(8, "जयपूर", "Jaipur", Groups.SKY, 2_500, 1_000, listOf(200, 1_000, 3_000, 9_000, 12_500, 15_000)),
        city(9, "लखनौ", "Lucknow", Groups.SKY, 2_500, 1_000, listOf(250, 1_250, 3_750, 10_000, 13_750, 17_500)),
        Tile(10, "तुरुंग", "Jail — just visiting", TileType.JAIL),
        city(11, "उटकमंड", "Ootacamund", Groups.PINK, 3_000, 1_500, listOf(300, 1_500, 4_500, 12_500, 17_500, 20_000)),
        utility(12, "इलेक्ट्रिक कंपनी", "Electric Company"),
        city(13, "पुणे", "Pune", Groups.PINK, 3_000, 1_500, listOf(300, 1_500, 4_500, 12_500, 17_500, 20_000)),
        city(14, "गोवा", "Goa", Groups.PINK, 3_000, 1_500, listOf(350, 1_750, 5_000, 14_000, 19_000, 22_500)),
        transport(15, "मोटर बोट", "Motor Boat"),
        city(16, "शिमला", "Shimla", Groups.ORANGE, 3_500, 1_500, listOf(400, 2_000, 6_000, 18_000, 25_000, 30_000)),
        Tile(17, "सुवर्ण संधी", "Golden chance", TileType.CHANCE),
        city(18, "अमृतसर", "Amritsar", Groups.ORANGE, 3_500, 1_500, listOf(400, 2_000, 6_000, 18_000, 25_000, 30_000)),
        city(19, "श्रीनगर", "Srinagar", Groups.ORANGE, 3_500, 1_500, listOf(450, 2_250, 6_500, 19_000, 26_000, 32_500)),
        Tile(20, "विश्रांती", "Rest house", TileType.REST),
        city(21, "चंदीगड", "Chandigarh", Groups.RED, 4_000, 2_000, listOf(500, 2_500, 7_500, 22_500, 30_000, 37_500)),
        resort(22, "SPORTS CLUB", "Sports Club", 9_000),
        city(23, "बंगलोर", "Bangalore", Groups.RED, 4_000, 2_000, listOf(500, 2_500, 7_500, 22_500, 30_000, 37_500)),
        city(24, "महाबळेश्वर", "Mahabaleshwar", Groups.RED, 4_000, 2_000, listOf(550, 2_750, 8_000, 24_000, 32_000, 40_000)),
        transport(25, "बस", "Bus"),
        city(26, "हैदराबाद", "Hyderabad", Groups.YELLOW, 5_000, 2_000, listOf(600, 3_000, 9_000, 25_000, 35_000, 45_000)),
        city(27, "मद्रास", "Madras", Groups.YELLOW, 5_000, 2_000, listOf(600, 3_000, 9_000, 25_000, 35_000, 45_000)),
        utility(28, "पाणी खाते", "Water Works"),
        city(29, "कलकत्ता", "Calcutta", Groups.YELLOW, 5_000, 2_000, listOf(650, 3_250, 9_500, 27_000, 37_000, 47_500)),
        Tile(30, "तुरुंगात जा", "Go to jail", TileType.GO_TO_JAIL),
        city(31, "काश्मीर", "Kashmir", Groups.GREEN, 6_000, 2_500, listOf(800, 4_000, 10_000, 30_000, 40_000, 50_000)),
        city(32, "अहमदाबाद", "Ahmedabad", Groups.GREEN, 6_000, 2_500, listOf(800, 4_000, 10_000, 30_000, 40_000, 50_000)),
        resort(33, "WATER WORLD", "Water World Resort", 7_500),
        city(34, "सूरत", "Surat", Groups.GREEN, 6_000, 2_500, listOf(900, 4_500, 11_000, 32_500, 42_500, 52_500)),
        transport(35, "एअर इंडिया", "Air India"),
        Tile(36, "सुवर्ण संधी", "Golden chance", TileType.CHANCE),
        city(37, "मुंबई", "Mumbai", Groups.BLUE, 8_000, 2_500, listOf(1_000, 5_000, 15_000, 40_000, 55_000, 75_000)),
        Tile(38, "आयकर", "Income tax", TileType.TAX, price = 1_000),
        city(39, "दिल्ली", "Delhi", Groups.BLUE, 10_000, 2_500, listOf(1_500, 7_500, 22_500, 55_000, 70_000, 100_000))
    )

    val size: Int get() = tiles.size

    fun tile(index: Int): Tile = tiles[((index % size) + size) % size]

    /** Every tile in the same colour band, used for the "owns the set" rules. */
    fun groupTiles(group: String?): List<Tile> =
        if (group == null) emptyList() else tiles.filter { it.group == group }

    /**
     * Grid position of a tile on an 11x11 board: index 0 is the bottom-right corner and
     * play runs anticlockwise, the way the printed board is laid out.
     */
    fun cell(index: Int): Pair<Int, Int> = when (val i = ((index % size) + size) % size) {
        in 0..10 -> 10 to (10 - i)
        in 11..19 -> (10 - (i - 10)) to 0
        in 20..30 -> 0 to (i - 20)
        else -> (i - 30) to 10
    }
}
