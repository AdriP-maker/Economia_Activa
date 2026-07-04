package pa.ac.utp.economia_activa.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "economia_activa.db"
        private const val DATABASE_VERSION = 3

        // Tablas
        private const val TABLE_PRODUCTS = "productos"
        private const val TABLE_LISTS = "listas_compras"
        private const val TABLE_ITEMS = "items_lista"
        private const val TABLE_PERIODIC_EXPENSES = "gastos_periodicos"
        private const val TABLE_EXPENSE_HISTORY = "gastos_historial"

        // Columnas Productos
        private const val COL_PROD_ID = "id"
        private const val COL_PROD_NAME = "nombre"
        private const val COL_PROD_LAST_PRICE = "precio_ultimo"
        private const val COL_PROD_CAT = "categoria"
        private const val COL_PROD_UNIT = "unidad"
        private const val COL_PROD_BARCODE = "codigo_barra"

        // Columnas Listas
        private const val COL_LIST_ID = "id"
        private const val COL_LIST_NAME = "nombre"
        private const val COL_LIST_DATE = "fecha"
        private const val COL_LIST_BUDGET = "presupuesto"
        private const val COL_LIST_FINISHED = "finalizada"

        // Columnas Items
        private const val COL_ITEM_ID = "id"
        private const val COL_ITEM_LIST_ID = "lista_id"
        private const val COL_ITEM_PROD_ID = "producto_id"
        private const val COL_ITEM_PROD_NAME = "producto_nombre"
        private const val COL_ITEM_PROD_CAT = "producto_categoria"
        private const val COL_ITEM_QTY = "cantidad"
        private const val COL_ITEM_PRICE = "precio_unitario"
        private const val COL_ITEM_BOUGHT = "comprado"

        // Columnas Gastos Periódicos
        private const val COL_PER_ID = "id"
        private const val COL_PER_CONCEPT = "concepto"
        private const val COL_PER_AMOUNT = "monto"
        private const val COL_PER_CAT = "categoria"
        private const val COL_PER_FREQ = "frecuencia"
        private const val COL_PER_DAY = "dia_pago"
        private const val COL_PER_ACTIVE = "activo"

        // Columnas Historial Gastos
        private const val COL_HIST_ID = "id"
        private const val COL_HIST_CONCEPT = "concepto"
        private const val COL_HIST_AMOUNT = "monto"
        private const val COL_HIST_DATE = "fecha"
        private const val COL_HIST_CAT = "categoria"
        private const val COL_HIST_LIST_ID = "lista_id"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 1. Crear tabla productos
        db.execSQL("""
            CREATE TABLE $TABLE_PRODUCTS (
                $COL_PROD_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PROD_NAME TEXT UNIQUE NOT NULL,
                $COL_PROD_LAST_PRICE REAL DEFAULT 0.0,
                $COL_PROD_CAT TEXT NOT NULL,
                $COL_PROD_UNIT TEXT NOT NULL,
                $COL_PROD_BARCODE TEXT
            )
        """)

        // 2. Crear tabla listas
        db.execSQL("""
            CREATE TABLE $TABLE_LISTS (
                $COL_LIST_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_LIST_NAME TEXT NOT NULL,
                $COL_LIST_DATE TEXT NOT NULL,
                $COL_LIST_BUDGET REAL NOT NULL,
                $COL_LIST_FINISHED INTEGER DEFAULT 0
            )
        """)

        // 3. Crear tabla items de lista
        db.execSQL("""
            CREATE TABLE $TABLE_ITEMS (
                $COL_ITEM_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ITEM_LIST_ID INTEGER NOT NULL,
                $COL_ITEM_PROD_ID INTEGER NOT NULL,
                $COL_ITEM_PROD_NAME TEXT NOT NULL,
                $COL_ITEM_PROD_CAT TEXT NOT NULL,
                $COL_ITEM_QTY REAL DEFAULT 1.0,
                $COL_ITEM_PRICE REAL DEFAULT 0.0,
                $COL_ITEM_BOUGHT INTEGER DEFAULT 0,
                FOREIGN KEY($COL_ITEM_LIST_ID) REFERENCES $TABLE_LISTS($COL_LIST_ID) ON DELETE CASCADE
            )
        """)

        // 4. Crear tabla gastos periódicos
        db.execSQL("""
            CREATE TABLE $TABLE_PERIODIC_EXPENSES (
                $COL_PER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PER_CONCEPT TEXT NOT NULL,
                $COL_PER_AMOUNT REAL NOT NULL,
                $COL_PER_CAT TEXT NOT NULL,
                $COL_PER_FREQ TEXT NOT NULL,
                $COL_PER_DAY INTEGER,
                $COL_PER_ACTIVE INTEGER DEFAULT 1
            )
        """)

        // 5. Crear tabla historial de gastos
        db.execSQL("""
            CREATE TABLE $TABLE_EXPENSE_HISTORY (
                $COL_HIST_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_HIST_CONCEPT TEXT NOT NULL,
                $COL_HIST_AMOUNT REAL NOT NULL,
                $COL_HIST_DATE TEXT NOT NULL,
                $COL_HIST_CAT TEXT NOT NULL,
                $COL_HIST_LIST_ID INTEGER
            )
        """)

        // Sembrar datos iniciales (Deshabilitado para entregar app vacía)
        // seedInitialProducts(db)
        // seedInitialExpenses(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ITEMS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PRODUCTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LISTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PERIODIC_EXPENSES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EXPENSE_HISTORY")
        onCreate(db)
    }

    private fun seedInitialProducts(db: SQLiteDatabase) {
        val initialProducts = listOf(
            ContentValues().apply { put(COL_PROD_NAME, "Arroz de Primera"); put(COL_PROD_LAST_PRICE, 1.25); put(COL_PROD_CAT, "Alimentos y Super"); put(COL_PROD_UNIT, "1kg") },
            ContentValues().apply { put(COL_PROD_NAME, "Leche Entera"); put(COL_PROD_LAST_PRICE, 1.60); put(COL_PROD_CAT, "Alimentos y Super"); put(COL_PROD_UNIT, "1L") },
            ContentValues().apply { put(COL_PROD_NAME, "Pan Molde"); put(COL_PROD_LAST_PRICE, 2.10); put(COL_PROD_CAT, "Alimentos y Super"); put(COL_PROD_UNIT, "paquete") },
            ContentValues().apply { put(COL_PROD_NAME, "Aceite Vegetal"); put(COL_PROD_LAST_PRICE, 3.50); put(COL_PROD_CAT, "Alimentos y Super"); put(COL_PROD_UNIT, "1L") },
            ContentValues().apply { put(COL_PROD_NAME, "Huevo Docena"); put(COL_PROD_LAST_PRICE, 2.20); put(COL_PROD_CAT, "Alimentos y Super"); put(COL_PROD_UNIT, "docena") },
            ContentValues().apply { put(COL_PROD_NAME, "Pechuga de Pollo"); put(COL_PROD_LAST_PRICE, 4.50); put(COL_PROD_CAT, "Alimentos y Super"); put(COL_PROD_UNIT, "1kg") },
            ContentValues().apply { put(COL_PROD_NAME, "Azúcar"); put(COL_PROD_LAST_PRICE, 0.95); put(COL_PROD_CAT, "Alimentos y Super"); put(COL_PROD_UNIT, "1kg") },
            ContentValues().apply { put(COL_PROD_NAME, "Café"); put(COL_PROD_LAST_PRICE, 2.80); put(COL_PROD_CAT, "Alimentos y Super"); put(COL_PROD_UNIT, "paquete") },
            ContentValues().apply { put(COL_PROD_NAME, "Detergente Ropa"); put(COL_PROD_LAST_PRICE, 4.25); put(COL_PROD_CAT, "Otros Gastos"); put(COL_PROD_UNIT, "unidad") },
            ContentValues().apply { put(COL_PROD_NAME, "Jabón de Baño"); put(COL_PROD_LAST_PRICE, 0.85); put(COL_PROD_CAT, "Otros Gastos"); put(COL_PROD_UNIT, "unidad") }
        )
        for (prod in initialProducts) {
            db.insert(TABLE_PRODUCTS, null, prod)
        }
    }

    private fun seedInitialExpenses(db: SQLiteDatabase) {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = format.format(Date())

        // Sembrar algunos gastos periódicos de ejemplo
        val periodicExpenses = listOf(
            ContentValues().apply { put(COL_PER_CONCEPT, "Servicio de Luz"); put(COL_PER_AMOUNT, 45.00); put(COL_PER_CAT, "Servicios Básicos"); put(COL_PER_FREQ, "Mensual"); put(COL_PER_DAY, 15); put(COL_PER_ACTIVE, 1) },
            ContentValues().apply { put(COL_PER_CONCEPT, "Servicio de Agua"); put(COL_PER_AMOUNT, 12.50); put(COL_PER_CAT, "Servicios Básicos"); put(COL_PER_FREQ, "Mensual"); put(COL_PER_DAY, 20); put(COL_PER_ACTIVE, 1) },
            ContentValues().apply { put(COL_PER_CONCEPT, "Planes de Internet y Celular"); put(COL_PER_AMOUNT, 35.00); put(COL_PER_CAT, "Servicios Básicos"); put(COL_PER_FREQ, "Mensual"); put(COL_PER_DAY, 5); put(COL_PER_ACTIVE, 1) }
        )
        for (expense in periodicExpenses) {
            db.insert(TABLE_PERIODIC_EXPENSES, null, expense)
        }

        // Sembrar algunos registros históricos de gastos del mes actual para ver datos iniciales en los gráficos
        val initialHistory = listOf(
            ContentValues().apply { put(COL_HIST_CONCEPT, "Luz - Mes Anterior"); put(COL_HIST_AMOUNT, 42.10); put(COL_HIST_DATE, currentDate); put(COL_HIST_CAT, "Servicios Básicos"); put(COL_HIST_LIST_ID, -1) },
            ContentValues().apply { put(COL_HIST_CONCEPT, "Farmacia - Medicinas"); put(COL_HIST_AMOUNT, 15.50); put(COL_HIST_DATE, currentDate); put(COL_HIST_CAT, "Salud"); put(COL_HIST_LIST_ID, -1) },
            ContentValues().apply { put(COL_HIST_CONCEPT, "Pasajes de Bus"); put(COL_HIST_AMOUNT, 8.00); put(COL_HIST_DATE, currentDate); put(COL_HIST_CAT, "Transporte"); put(COL_HIST_LIST_ID, -1) }
        )
        for (hist in initialHistory) {
            db.insert(TABLE_EXPENSE_HISTORY, null, hist)
        }
    }

    // ==========================================
    // MÉTODOS CRUD - PRODUCTOS
    // ==========================================

    fun getOrCreateProduct(name: String, category: String, unit: String, barcode: String? = null, initialPrice: Double = 0.0): Product {
        val db = this.writableDatabase
        val nameLower = name.trim().lowercase(Locale.getDefault())

        val cursor = db.query(TABLE_PRODUCTS, null, "LOWER($COL_PROD_NAME) = ?", arrayOf(nameLower), null, null, null)
        if (cursor.moveToFirst()) {
            val prodId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_PROD_ID))
            val existingBarcode = cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_BARCODE))
            val existingPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PROD_LAST_PRICE))
            
            var finalPrice = existingPrice
            val updateValues = ContentValues()
            
            // Si tiene código de barras nuevo, lo actualizamos
            if (barcode != null && barcode.isNotEmpty() && existingBarcode != barcode) {
                updateValues.put(COL_PROD_BARCODE, barcode)
            }
            
            // Si el precio actual es 0.0 y el nuevo precio es válido, lo actualizamos
            if (existingPrice == 0.0 && initialPrice > 0.0) {
                finalPrice = initialPrice
                updateValues.put(COL_PROD_LAST_PRICE, initialPrice)
            }
            
            if (updateValues.size() > 0) {
                db.update(TABLE_PRODUCTS, updateValues, "$COL_PROD_ID = ?", arrayOf(prodId.toString()))
            }
            
            val prod = Product(
                prodId,
                cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_NAME)),
                finalPrice,
                cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_CAT)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_UNIT)),
                barcode ?: existingBarcode
            )
            cursor.close()
            return prod
        }
        cursor.close()

        // Si no existe, crearlo
        val values = ContentValues().apply {
            put(COL_PROD_NAME, name.trim())
            put(COL_PROD_LAST_PRICE, initialPrice)
            put(COL_PROD_CAT, category)
            put(COL_PROD_UNIT, unit)
            put(COL_PROD_BARCODE, barcode)
        }
        val id = db.insert(TABLE_PRODUCTS, null, values)
        return Product(id, name.trim(), initialPrice, category, unit, barcode)
    }

    fun getProductByBarcode(barcode: String): Product? {
        val db = this.readableDatabase
        val cursor = db.query(TABLE_PRODUCTS, null, "$COL_PROD_BARCODE = ?", arrayOf(barcode.trim()), null, null, null)
        if (cursor.moveToFirst()) {
            val prod = Product(
                cursor.getLong(cursor.getColumnIndexOrThrow(COL_PROD_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_NAME)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PROD_LAST_PRICE)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_CAT)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_UNIT)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_BARCODE))
            )
            cursor.close()
            return prod
        }
        cursor.close()
        return null
    }

    fun getAllProducts(): List<Product> {
        val list = mutableListOf<Product>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_PRODUCTS ORDER BY $COL_PROD_NAME ASC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(Product(
                    cursor.getLong(cursor.getColumnIndexOrThrow(COL_PROD_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_NAME)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PROD_LAST_PRICE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_CAT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_UNIT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_BARCODE))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun updateProductLastPrice(productId: Long, price: Double) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_PROD_LAST_PRICE, price)
        }
        db.update(TABLE_PRODUCTS, values, "$COL_PROD_ID = ?", arrayOf(productId.toString()))
    }

    // ==========================================
    // MÉTODOS CRUD - LISTAS DE COMPRAS
    // ==========================================

    fun insertShoppingList(name: String, budget: Double): Long {
        val db = this.writableDatabase
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = format.format(Date())
        val values = ContentValues().apply {
            put(COL_LIST_NAME, name.trim())
            put(COL_LIST_DATE, dateStr)
            put(COL_LIST_BUDGET, budget)
            put(COL_LIST_FINISHED, 0)
        }
        return db.insert(TABLE_LISTS, null, values)
    }

    fun getAllShoppingLists(): List<ShoppingList> {
        val list = mutableListOf<ShoppingList>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_LISTS ORDER BY $COL_LIST_ID DESC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(ShoppingList(
                    cursor.getLong(cursor.getColumnIndexOrThrow(COL_LIST_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_LIST_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_LIST_DATE)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LIST_BUDGET)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_LIST_FINISHED)) == 1
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getShoppingList(id: Long): ShoppingList? {
        val db = this.readableDatabase
        val cursor = db.query(TABLE_LISTS, null, "$COL_LIST_ID = ?", arrayOf(id.toString()), null, null, null)
        var list: ShoppingList? = null
        if (cursor.moveToFirst()) {
            list = ShoppingList(
                cursor.getLong(cursor.getColumnIndexOrThrow(COL_LIST_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_LIST_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_LIST_DATE)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LIST_BUDGET)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_LIST_FINISHED)) == 1
            )
        }
        cursor.close()
        return list
    }

    fun updateShoppingListStatus(listId: Long, isFinished: Boolean) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_LIST_FINISHED, if (isFinished) 1 else 0)
        }
        db.update(TABLE_LISTS, values, "$COL_LIST_ID = ?", arrayOf(listId.toString()))

        // Si se marca como finalizada, se registra automáticamente el total pagado en el historial de gastos
        if (isFinished) {
            val list = getShoppingList(listId) ?: return
            val total = getShoppingListTotalSpent(listId)
            if (total > 0) {
                insertExpenseLog("Mercado: ${list.name}", total, list.date, "Alimentos y Super", listId)
            }
        }
    }

    fun deleteShoppingList(listId: Long) {
        val db = this.writableDatabase
        db.delete(TABLE_ITEMS, "$COL_ITEM_LIST_ID = ?", arrayOf(listId.toString()))
        db.delete(TABLE_LISTS, "$COL_LIST_ID = ?", arrayOf(listId.toString()))
        db.delete(TABLE_EXPENSE_HISTORY, "$COL_HIST_LIST_ID = ?", arrayOf(listId.toString()))
    }

    // ==========================================
    // MÉTODOS CRUD - ELEMENTOS DE LISTA
    // ==========================================

    fun insertListItem(listId: Long, product: Product, quantity: Double, estimatedPrice: Double): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_ITEM_LIST_ID, listId)
            put(COL_ITEM_PROD_ID, product.id)
            put(COL_ITEM_PROD_NAME, product.name)
            put(COL_ITEM_PROD_CAT, product.category)
            put(COL_ITEM_QTY, quantity)
            put(COL_ITEM_PRICE, estimatedPrice)
            put(COL_ITEM_BOUGHT, 0)
        }
        return db.insert(TABLE_ITEMS, null, values)
    }

    fun getListItems(listId: Long): List<ListItem> {
        val list = mutableListOf<ListItem>()
        val db = this.readableDatabase
        val cursor = db.query(TABLE_ITEMS, null, "$COL_ITEM_LIST_ID = ?", arrayOf(listId.toString()), null, null, null)
        if (cursor.moveToFirst()) {
            do {
                list.add(ListItem(
                    cursor.getLong(cursor.getColumnIndexOrThrow(COL_ITEM_ID)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(COL_ITEM_LIST_ID)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(COL_ITEM_PROD_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_ITEM_PROD_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_ITEM_PROD_CAT)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ITEM_QTY)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ITEM_PRICE)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_ITEM_BOUGHT)) == 1
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun updateListItemBoughtState(itemId: Long, isBought: Boolean, finalPrice: Double, finalQty: Double) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_ITEM_BOUGHT, if (isBought) 1 else 0)
            put(COL_ITEM_PRICE, finalPrice)
            put(COL_ITEM_QTY, finalQty)
        }
        db.update(TABLE_ITEMS, values, "$COL_ITEM_ID = ?", arrayOf(itemId.toString()))

        // Si fue comprado, actualizar el último precio registrado en la tabla general de productos
        if (isBought) {
            val cursor = db.query(TABLE_ITEMS, arrayOf(COL_ITEM_PROD_ID), "$COL_ITEM_ID = ?", arrayOf(itemId.toString()), null, null, null)
            if (cursor.moveToFirst()) {
                val prodId = cursor.getLong(0)
                updateProductLastPrice(prodId, finalPrice)
            }
            cursor.close()
        }
    }

    fun deleteListItem(itemId: Long) {
        val db = this.writableDatabase
        db.delete(TABLE_ITEMS, "$COL_ITEM_ID = ?", arrayOf(itemId.toString()))
    }

    fun getShoppingListTotalSpent(listId: Long): Double {
        val db = this.readableDatabase
        // Sumamos solo lo comprado. Si la lista está activa, estimamos el valor sumando lo marcado como comprado + pendiente
        val cursor = db.rawQuery("SELECT SUM($COL_ITEM_QTY * $COL_ITEM_PRICE) FROM $TABLE_ITEMS WHERE $COL_ITEM_LIST_ID = ?", arrayOf(listId.toString()))
        var total = 0.0
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0)
        }
        cursor.close()
        return total
    }

    fun getShoppingListEstimatedTotal(listId: Long): Double {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT SUM($COL_ITEM_QTY * $COL_ITEM_PRICE) FROM $TABLE_ITEMS WHERE $COL_ITEM_LIST_ID = ?", arrayOf(listId.toString()))
        var total = 0.0
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0)
        }
        cursor.close()
        return total
    }

    // ==========================================
    // MÉTODOS CRUD - GASTOS PERIÓDICOS (RECURRENTES)
    // ==========================================

    fun insertPeriodicExpense(concept: String, amount: Double, category: String, frequency: String, payDay: Int): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_PER_CONCEPT, concept.trim())
            put(COL_PER_AMOUNT, amount)
            put(COL_PER_CAT, category)
            put(COL_PER_FREQ, frequency)
            put(COL_PER_DAY, payDay)
            put(COL_PER_ACTIVE, 1)
        }
        return db.insert(TABLE_PERIODIC_EXPENSES, null, values)
    }

    fun getAllPeriodicExpenses(): List<PeriodicExpense> {
        val list = mutableListOf<PeriodicExpense>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_PERIODIC_EXPENSES ORDER BY $COL_PER_DAY ASC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(PeriodicExpense(
                    cursor.getLong(cursor.getColumnIndexOrThrow(COL_PER_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_PER_CONCEPT)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PER_AMOUNT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_PER_CAT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_PER_FREQ)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_PER_DAY)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_PER_ACTIVE)) == 1
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun deletePeriodicExpense(id: Long) {
        val db = this.writableDatabase
        db.delete(TABLE_PERIODIC_EXPENSES, "$COL_PER_ID = ?", arrayOf(id.toString()))
    }

    fun updatePeriodicExpense(id: Long, concept: String, amount: Double, category: String, day: Int): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_PER_CONCEPT, concept)
            put(COL_PER_AMOUNT, amount)
            put(COL_PER_CAT, category)
            put(COL_PER_DAY, day)
        }
        return db.update(TABLE_PERIODIC_EXPENSES, values, "$COL_PER_ID = ?", arrayOf(id.toString())) > 0
    }

    // ==========================================
    // MÉTODOS CRUD - HISTORIAL DE GASTOS GENERALES
    // ==========================================

    fun insertExpenseLog(concept: String, amount: Double, date: String, category: String, listId: Long? = null): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_HIST_CONCEPT, concept.trim())
            put(COL_HIST_AMOUNT, amount)
            put(COL_HIST_DATE, date)
            put(COL_HIST_CAT, category)
            if (listId != null) {
                put(COL_HIST_LIST_ID, listId)
            } else {
                put(COL_HIST_LIST_ID, -1)
            }
        }
        return db.insert(TABLE_EXPENSE_HISTORY, null, values)
    }

    fun getAllExpenses(): List<ExpenseLog> {
        val list = mutableListOf<ExpenseLog>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_EXPENSE_HISTORY ORDER BY $COL_HIST_DATE DESC, $COL_HIST_ID DESC", null)
        if (cursor.moveToFirst()) {
            do {
                val listIdVal = cursor.getLong(cursor.getColumnIndexOrThrow(COL_HIST_LIST_ID))
                list.add(ExpenseLog(
                    cursor.getLong(cursor.getColumnIndexOrThrow(COL_HIST_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_HIST_CONCEPT)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(COL_HIST_AMOUNT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_HIST_DATE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_HIST_CAT)),
                    if (listIdVal == -1L) null else listIdVal
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun deleteExpenseLog(id: Long) {
        val db = this.writableDatabase
        db.delete(TABLE_EXPENSE_HISTORY, "$COL_HIST_ID = ?", arrayOf(id.toString()))
    }

    fun updateExpenseLog(id: Long, concept: String, amount: Double, category: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_HIST_CONCEPT, concept)
            put(COL_HIST_AMOUNT, amount)
            put(COL_HIST_CAT, category)
        }
        return db.update(TABLE_EXPENSE_HISTORY, values, "$COL_HIST_ID = ?", arrayOf(id.toString())) > 0
    }

    // ==========================================
    // ANÁLISIS, ESTADÍSTICAS Y AHORRO
    // ==========================================

    // Devuelve el gasto total del mes en curso (YYYY-MM)
    fun getMonthlySpent(yearMonth: String): Double {
        val db = this.readableDatabase
        // Filtrar por fecha que empiece con yearMonth (ej. "2026-07")
        val cursor = db.rawQuery(
            "SELECT SUM($COL_HIST_AMOUNT) FROM $TABLE_EXPENSE_HISTORY WHERE $COL_HIST_DATE LIKE ?",
            arrayOf("$yearMonth%")
        )
        var total = 0.0
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0)
        }
        cursor.close()

        // También sumamos los gastos fijos activos
        val cursorFixed = db.rawQuery("SELECT SUM($COL_PER_AMOUNT) FROM $TABLE_PERIODIC_EXPENSES WHERE $COL_PER_ACTIVE = 1", null)
        if (cursorFixed.moveToFirst()) {
            total += cursorFixed.getDouble(0)
        }
        cursorFixed.close()

        return total
    }

    // Devuelve los gastos agrupados por categoría para graficar en el mes actual
    fun getExpensesByCategory(yearMonth: String): Map<String, Double> {
        val map = mutableMapOf<String, Double>()
        val db = this.readableDatabase

        // 1. Sumar gastos del historial
        val cursor = db.rawQuery(
            "SELECT $COL_HIST_CAT, SUM($COL_HIST_AMOUNT) FROM $TABLE_EXPENSE_HISTORY WHERE $COL_HIST_DATE LIKE ? GROUP BY $COL_HIST_CAT",
            arrayOf("$yearMonth%")
        )
        if (cursor.moveToFirst()) {
            do {
                val cat = cursor.getString(0)
                val total = cursor.getDouble(1)
                map[cat] = total
            } while (cursor.moveToNext())
        }
        cursor.close()

        // 2. Sumar gastos periódicos activos
        val cursorFixed = db.rawQuery(
            "SELECT $COL_PER_CAT, SUM($COL_PER_AMOUNT) FROM $TABLE_PERIODIC_EXPENSES WHERE $COL_PER_ACTIVE = 1 GROUP BY $COL_PER_CAT",
            null
        )
        if (cursorFixed.moveToFirst()) {
            do {
                val cat = cursorFixed.getString(0)
                val total = cursorFixed.getDouble(1)
                map[cat] = (map[cat] ?: 0.0) + total
            } while (cursorFixed.moveToNext())
        }
        cursorFixed.close()

        return map
    }

    // Obtener promedio histórico y último precio de cada producto comprado para comparar inflación local
    fun getProductPriceAnalysis(): List<Triple<String, Double, Double>> {
        val analysisList = mutableListOf<Triple<String, Double, Double>>()
        val db = this.readableDatabase

        // Obtener productos que tienen registros en items de listas
        // Calculamos el promedio del precio unitario y comparamos con el último precio
        val query = """
            SELECT p.$COL_PROD_NAME, AVG(i.$COL_ITEM_PRICE), p.$COL_PROD_LAST_PRICE
            FROM $TABLE_ITEMS i
            JOIN $TABLE_PRODUCTS p ON i.$COL_ITEM_PROD_ID = p.$COL_PROD_ID
            WHERE i.$COL_ITEM_BOUGHT = 1
            GROUP BY p.$COL_PROD_ID
            HAVING COUNT(i.$COL_ITEM_ID) >= 2
        """
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            do {
                val name = cursor.getString(0)
                val avgPrice = cursor.getDouble(1)
                val lastPrice = cursor.getDouble(2)
                analysisList.add(Triple(name, avgPrice, lastPrice))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return analysisList
    }

    // Predicción: obtener productos populares comprados más de una vez para sugerir agregarlos a una nueva lista
    fun getPopularProducts(): List<Product> {
        val list = mutableListOf<Product>()
        val db = this.readableDatabase
        val query = """
            SELECT p.*, COUNT(i.$COL_ITEM_ID) as veces
            FROM $TABLE_ITEMS i
            JOIN $TABLE_PRODUCTS p ON i.$COL_ITEM_PROD_ID = p.$COL_PROD_ID
            GROUP BY p.$COL_PROD_ID
            ORDER BY veces DESC
            LIMIT 5
        """
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            do {
                list.add(Product(
                    cursor.getLong(cursor.getColumnIndexOrThrow(COL_PROD_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_NAME)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PROD_LAST_PRICE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_CAT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_UNIT))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun clearAllData() {
        val db = this.writableDatabase
        db.delete(TABLE_ITEMS, null, null)
        db.delete(TABLE_PRODUCTS, null, null)
        db.delete(TABLE_LISTS, null, null)
        db.delete(TABLE_PERIODIC_EXPENSES, null, null)
        db.delete(TABLE_EXPENSE_HISTORY, null, null)
    }
}
