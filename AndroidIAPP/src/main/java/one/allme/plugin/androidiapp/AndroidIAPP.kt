package one.allme.plugin.androidiapp

import one.allme.plugin.androidiapp.utils.IAPP_utils

import android.util.ArraySet
import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryPurchasesAsync
import org.godotengine.godot.Godot
import org.godotengine.godot.Dictionary
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot
import java.util.Objects


class AndroidIAPP(godot: Godot?): GodotPlugin(godot), PurchasesUpdatedListener, BillingClientStateListener {

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { _, _ -> }


    private val billingClient: BillingClient = BillingClient
        .newBuilder(activity!!)
        .enablePendingPurchases()
        .setListener(purchasesUpdatedListener)
        .build()


    // Signals
    private val helloResponse = SignalInfo("helloResponse", String::class.java)
    private val startConnection = SignalInfo("startConnection")
    private val connected = SignalInfo("connected")
    private val disconnected = SignalInfo("disconnected")
    private val queryPurchasesResponse = SignalInfo("query_purchases_response", Any::class.java)
    private val queryProductResponse = SignalInfo("query_product_response", Any::class.java)


    override fun getPluginSignals(): Set<SignalInfo> {
        Log.i(pluginName, "Registering plugin signals")
        return setOf(
            helloResponse,
            startConnection,
            connected,
            disconnected,
            queryPurchasesResponse,
            queryProductResponse,
        )
    }


    override fun getPluginName(): String {
        return ("AndroidIAPP")
    }


    // Just say hello func
    @UsedByGodot
    private fun sayHello(says: String = "Hello from AndroidIAPP plugin") {
        runOnUiThread {
            Toast.makeText(activity, says, Toast.LENGTH_LONG).show()
            emitSignal(helloResponse.name, says)
            Log.i(pluginName, says)
        }
    }


    @UsedByGodot
    private fun startConnection() {
        billingClient.startConnection(this)
        emitSignal(startConnection.name)
    }


    override fun onBillingServiceDisconnected() {
        emitSignal(disconnected.name)
        Log.v(pluginName, "Billing service disconnected")
        // Try to restart the connection on the next request to
        // Google Play by calling the startConnection() method.
    }


    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            emitSignal(connected.name)
            Log.v(pluginName, "Billing setup finished")
            // The BillingClient is ready. You can query purchases here.
        }
    }


    override fun onPurchasesUpdated(p0: BillingResult, p1: MutableList<Purchase>?) {
        TODO("Not yet implemented")
    }

    @UsedByGodot
    //type  INAPP, SUBS
    fun queryPurchases(type: String?) {
        val params = QueryPurchasesParams
            .newBuilder()
            .setProductType(ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchaseList ->
            val returnValue = Dictionary() // from Godot type Dictionary
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.v(pluginName, "Purchases found")
                returnValue["purchasesList"] = IAPP_utils.convertPurchasesListToArray(purchaseList)
                returnValue["responseCode"] = billingResult.responseCode

            } else {
                Log.v(pluginName, "No purchase found")
                returnValue["debug_message"] = billingResult.debugMessage
                returnValue["responseCode"] = billingResult.responseCode
                returnValue["purchases"] = null
            }
            emitSignal(queryPurchasesResponse.name, returnValue)
        }
    }

    // Use kotlin functions for queryPurchases
    // It crashes on Godot  4.2
//    @UsedByGodot
//    suspend fun queryPurchasesAsync() {
//        val params = QueryPurchasesParams.newBuilder()
//            .setProductType(ProductType.SUBS)
//
//        // uses queryPurchasesAsync Kotlin extension function
//        val purchasesResult = billingClient.queryPurchasesAsync(params.build())
//
//        // check purchasesResult.billingResult
//        // process returned purchasesResult.purchasesList, e.g. display the plans user owns
    }


    @UsedByGodot
    fun queryProductDetails(listOfProductsIDs: Array<String>) {
        val products = ArrayList<QueryProductDetailsParams.Product>()

        for (productID in listOfProductsIDs) {
            products.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build())
        }

        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()

        billingClient.queryProductDetailsAsync(queryProductDetailsParams) {
            billingResult, productDetailsList ->
            val returnValue = Dictionary() // from Godot type Dictionary
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.v(pluginName, "Product details found")
                returnValue["productDetailsList"] = IAPP_utils.convertProductDetailsListToArray(productDetailsList)
                returnValue["responseCode"] = billingResult.responseCode
            } else {
                Log.v(pluginName, "No product details found")
                returnValue["debug_message"] = billingResult.debugMessage
                returnValue["responseCode"] = billingResult.responseCode
                returnValue["productDetailsList"] = null
            }
            emitSignal(queryProductResponse.name, returnValue)
        }

    }

}