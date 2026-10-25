package com.monopy.cash.model

/** What a सुवर्ण संधी card does when it is drawn. */
sealed interface ChanceEffect {
    data class Collect(val amount: Long) : ChanceEffect
    data class Pay(val amount: Long) : ChanceEffect
    data class MoveTo(val index: Int, val collectSalary: Boolean = true) : ChanceEffect
    data class MoveBack(val steps: Int) : ChanceEffect
    data class CollectFromEach(val amount: Long) : ChanceEffect
    data class PayEach(val amount: Long) : ChanceEffect
    data class PayPerHouse(val perHouse: Long) : ChanceEffect
    data object GoToJail : ChanceEffect
}

data class ChanceCard(
    val id: Int,
    val text: String,
    val latin: String,
    val effect: ChanceEffect
)

/** The सुवर्ण संधी deck. Drawn cards are identified by id so every device shows the same one. */
object ChanceDeck {
    val cards: List<ChanceCard> = listOf(
        ChanceCard(0, "GO वर जा आणि पगार घ्या", "Advance to GO and collect your salary", ChanceEffect.MoveTo(0)),
        ChanceCard(1, "बँकेची चूक तुमच्या फायद्याची — ₹2,000 मिळवा", "Bank error in your favour — collect 2,000", ChanceEffect.Collect(2_000)),
        ChanceCard(2, "डॉक्टरांची फी — ₹1,000 भरा", "Doctor's fee — pay 1,000", ChanceEffect.Pay(1_000)),
        ChanceCard(3, "सरळ तुरुंगात जा", "Go directly to jail", ChanceEffect.GoToJail),
        ChanceCard(4, "मुंबईला जा", "Advance to Mumbai", ChanceEffect.MoveTo(37)),
        ChanceCard(5, "दिल्लीला जा", "Advance to Delhi", ChanceEffect.MoveTo(39)),
        ChanceCard(6, "तीन घरे मागे जा", "Go back three squares", ChanceEffect.MoveBack(3)),
        ChanceCard(7, "वाढदिवस — प्रत्येकाकडून ₹1,000 मिळवा", "Birthday — collect 1,000 from every player", ChanceEffect.CollectFromEach(1_000)),
        ChanceCard(8, "पार्टी द्या — प्रत्येकाला ₹500 द्या", "Throw a party — pay every player 500", ChanceEffect.PayEach(500)),
        ChanceCard(9, "आयकर परतावा — ₹1,500 मिळवा", "Income tax refund — collect 1,500", ChanceEffect.Collect(1_500)),
        ChanceCard(10, "घरांची दुरुस्ती — प्रत्येक घरामागे ₹500 भरा", "Repairs — pay 500 for every house you own", ChanceEffect.PayPerHouse(500)),
        ChanceCard(11, "शेअर बाजार तेजीत — ₹3,000 मिळवा", "Stock market rises — collect 3,000", ChanceEffect.Collect(3_000)),
        ChanceCard(12, "गोव्याला सहल — गोव्याला जा", "Trip to Goa — advance to Goa", ChanceEffect.MoveTo(14)),
        ChanceCard(13, "विमा हप्ता — ₹1,000 भरा", "Insurance premium — pay 1,000", ChanceEffect.Pay(1_000)),
        ChanceCard(14, "एअर इंडियाकडे जा", "Advance to Air India", ChanceEffect.MoveTo(35)),
        ChanceCard(15, "विश्रांतीगृहात जा", "Go to the rest house", ChanceEffect.MoveTo(20))
    )

    fun card(id: Int): ChanceCard = cards[((id % cards.size) + cards.size) % cards.size]
}
