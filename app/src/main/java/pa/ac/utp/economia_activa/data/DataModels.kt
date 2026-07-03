package pa.ac.utp.economia_activa.data

data class Product(
    val id: Long = -1,
    val name: String,
    val lastPrice: Double,
    val category: String,
    val unit: String,
    val barcode: String? = null
)

data class ShoppingList(
    val id: Long = -1,
    val name: String,
    val date: String,
    val budget: Double,
    val isFinished: Boolean = false
)

data class ListItem(
    val id: Long = -1,
    val listId: Long,
    val productId: Long,
    val productName: String,      // Denormalized or joined for convenience
    val productCategory: String,
    val quantity: Double,
    val unitPrice: Double,
    val isBought: Boolean = false
)

data class PeriodicExpense(
    val id: Long = -1,
    val concept: String,
    val amount: Double,
    val category: String,
    val frequency: String, // "Mensual", "Semanal", etc.
    val payDay: Int,
    val isActive: Boolean = true
)

data class ExpenseLog(
    val id: Long = -1,
    val concept: String,
    val amount: Double,
    val date: String, // YYYY-MM-DD
    val category: String,
    val listId: Long? = null // Optional, linked to shopping list
)
