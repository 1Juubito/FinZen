package com.finzen.app.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.CurrencyBitcoin
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalTaxi
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector

object IconCatalog {

    val fallback: ImageVector = Icons.Rounded.Category

    val all: List<Pair<String, ImageVector>> = listOf(
        "restaurant" to Icons.Rounded.Restaurant,
        "fastfood" to Icons.Rounded.Fastfood,
        "cafe" to Icons.Rounded.LocalCafe,
        "bar" to Icons.Rounded.LocalBar,
        "cart" to Icons.Rounded.ShoppingCart,
        "shopping" to Icons.Rounded.ShoppingBag,
        "clothing" to Icons.Rounded.Checkroom,
        "car" to Icons.Rounded.DirectionsCar,
        "bus" to Icons.Rounded.DirectionsBus,
        "taxi" to Icons.Rounded.LocalTaxi,
        "fuel" to Icons.Rounded.LocalGasStation,
        "travel" to Icons.Rounded.Flight,
        "home" to Icons.Rounded.Home,
        "apartment" to Icons.Rounded.Apartment,
        "bills" to Icons.Rounded.ReceiptLong,
        "receipt" to Icons.Rounded.Receipt,
        "internet" to Icons.Rounded.Wifi,
        "phone" to Icons.Rounded.Smartphone,
        "health" to Icons.Rounded.MedicalServices,
        "love" to Icons.Rounded.Favorite,
        "gym" to Icons.Rounded.FitnessCenter,
        "beauty" to Icons.Rounded.Spa,
        "education" to Icons.Rounded.School,
        "book" to Icons.Rounded.MenuBook,
        "leisure" to Icons.Rounded.SportsEsports,
        "music" to Icons.Rounded.MusicNote,
        "subscriptions" to Icons.Rounded.Subscriptions,
        "pets" to Icons.Rounded.Pets,
        "baby" to Icons.Rounded.ChildCare,
        "gifts" to Icons.Rounded.CardGiftcard,
        "redeem" to Icons.Rounded.Redeem,
        "salary" to Icons.Rounded.Payments,
        "freelance" to Icons.Rounded.Work,
        "investments" to Icons.Rounded.TrendingUp,
        "trending_up" to Icons.Rounded.TrendingUp,
        "chart" to Icons.Rounded.ShowChart,
        "crypto" to Icons.Rounded.CurrencyBitcoin,
        "shield" to Icons.Rounded.Shield,
        "sales" to Icons.Rounded.Sell,
        "refund" to Icons.Rounded.Savings,
        "money" to Icons.Rounded.AttachMoney,
        "wallet" to Icons.Rounded.AccountBalanceWallet,
        "bank" to Icons.Rounded.AccountBalance,
        "savings" to Icons.Rounded.Savings,
        "card" to Icons.Rounded.CreditCard,
        "target" to Icons.Rounded.TrackChanges,
        "other" to Icons.Rounded.MoreHoriz,
    )

    private val map: Map<String, ImageVector> = all.toMap()

    fun icon(key: String?): ImageVector = map[key] ?: fallback
}

object PaletteColors {
    val swatches: List<String> = listOf(
        "#FF6B6B", "#FF924C", "#FFB23E", "#F7B731", "#82C91E", "#2BB673",
        "#12B886", "#22B8CF", "#4DABF7", "#3B8ED0", "#5C7CFA", "#7C5CFC",
        "#9775FA", "#E64980", "#F06595", "#A1887F", "#1B2733", "#868E96",
    )
}
