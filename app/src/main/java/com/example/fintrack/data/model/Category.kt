package com.example.fintrack.data.model

data class Category(
    val id: String,
    val name: String,
    val iconName: String,
    val colorHex: Long,
    val isIncome: Boolean = false,
    val keywords: List<String> = emptyList()
) {
    companion object {
        val DEFAULT_CATEGORIES = listOf(
            Category(
                id = "cat_food",
                name = "Food & Dining",
                iconName = "Restaurant",
                colorHex = 0xFFFF7043, // Coral / Orange
                keywords = listOf("swiggy", "zomato", "mcdonalds", "kfc", "dominos", "pizza", "burger", "cafe", "starbucks", "restaurant", "hotel", "biryani", "dhaba", "barbeque", "eats", "food")
            ),
            Category(
                id = "cat_groceries",
                name = "Groceries",
                iconName = "ShoppingCart",
                colorHex = 0xFF4CAF50, // Green
                keywords = listOf("blinkit", "zepto", "instamart", "bigbasket", "dmart", "spencer", "supermarket", "provision", "kirana", "milk", "vegetables", "fruits", "grocery")
            ),
            Category(
                id = "cat_shopping",
                name = "Shopping",
                iconName = "ShoppingBag",
                colorHex = 0xFF9C27B0, // Purple
                keywords = listOf("amazon", "flipkart", "myntra", "meesho", "ajio", "nykaa", "tata cliq", "zara", "h&m", "retail", "store", "lifestyle", "shoppers stop", "mall")
            ),
            Category(
                id = "cat_travel",
                name = "Travel & Fuel",
                iconName = "DirectionsCar",
                colorHex = 0xFF0288D1, // Light Blue
                keywords = listOf("uber", "ola", "rapido", "irctc", "makemytrip", "goibibo", "yatra", "indigo", "air india", "petrol", "fuel", "indian oil", "bharat petroleum", "hpcl", "shell", "fastag", "toll", "metro", "auto", "railway")
            ),
            Category(
                id = "cat_bills",
                name = "Bills & Utilities",
                iconName = "Receipt",
                colorHex = 0xFFF57C00, // Deep Orange
                keywords = listOf("electricity", "bescom", "torrent", "tatapower", "power", "water", "gas", "indane", "bharatgas", "airtel", "jio", "vi", "vodafone", "broadband", "act", "wifi", "billdesk", "recharge", "dth", "tata sky")
            ),
            Category(
                id = "cat_entertainment",
                name = "Entertainment",
                iconName = "Movie",
                colorHex = 0xFFE91E63, // Pink
                keywords = listOf("netflix", "prime video", "spotify", "hotstar", "disney", "youtube", "bookmyshow", "pvr", "inox", "cinema", "theatre", "gaming", "steam", "playstation")
            ),
            Category(
                id = "cat_health",
                name = "Health & Medical",
                iconName = "LocalHospital",
                colorHex = 0xFFE53935, // Red
                keywords = listOf("apollo", "pharmeasy", "1mg", "tata 1mg", "netmeds", "medplus", "hospital", "clinic", "pharmacy", "medical", "doctor", "pathology", "diagnostic", "dental")
            ),
            Category(
                id = "cat_investment",
                name = "Investments",
                iconName = "TrendingUp",
                colorHex = 0xFF00897B, // Teal
                keywords = listOf("zerodha", "groww", "upstox", "angel one", "indmoney", "mutual fund", "sip", "kuvera", "coin", "shares", "nse", "bse", "stock", "ppf", "nps", "fixed deposit")
            ),
            Category(
                id = "cat_salary",
                name = "Salary & Income",
                iconName = "AccountBalanceWallet",
                colorHex = 0xFF2E7D32, // Dark Green
                isIncome = true,
                keywords = listOf("salary", "payroll", "stipend", "bonus", "dividend", "interest", "refund", "cashback", "incentive")
            ),
            Category(
                id = "cat_transfer",
                name = "Transfer",
                iconName = "SwapHoriz",
                colorHex = 0xFF546E7A, // Blue Grey
                keywords = listOf("transfer to", "sent to", "received from", "self transfer", "imps", "neft")
            ),
            Category(
                id = "cat_other",
                name = "Other Expense",
                iconName = "MoreHoriz",
                colorHex = 0xFF78909C, // Grey
                keywords = emptyList()
            )
        )
    }
}
