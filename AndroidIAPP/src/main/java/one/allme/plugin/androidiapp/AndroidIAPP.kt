package one.allme.plugin.androidiapp

import one.allme.plugin.androidiapp.utils.IAPP_utils

import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.godotengine.godot.Godot
import org.godotengine.godot.Dictionary
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot


class AndroidIAPP(godot: Godot?): GodotPlugin(godot), PurchasesUpdatedListener, BillingClientStateListener {

//    private val purchasesUpdatedListener =
//        PurchasesUpdatedListener { billingResult, purchases -> }


    private val billingClient: BillingClient = BillingClient
        .newBuilder(activity!!)
        .enablePendingPurchases()
        .setListener(this)
        .build()


    // Signals
    // Echo
    private val helloResponseSignal = SignalInfo("helloResponse", String::class.java)
    // Information
    private val startConnectionSignal = SignalInfo("startConnection")
    private val connectedSignal = SignalInfo("connected")
    private val disconnectedSignal = SignalInfo("disconnected")
    // Query purchases
    private val queryPurchasesSignal = SignalInfo("query_purchases", Dictionary::class.java)
    private val queryPurchasesErrorSignal = SignalInfo("query_purchases_error", Dictionary::class.java)
    // Query product details
    private val queryProductDetailsSignal = SignalInfo("query_product_response", Dictionary::class.java)
    private val queryProductDetailsErrorSignal = SignalInfo("query_product_response_error", Dictionary::class.java)
    // Purchase updating
    private val purchaseUpdatedSignal = SignalInfo("purchase_updated", Dictionary::class.java)
    private val purchaseCanceled = SignalInfo("purchase_canceled", Any::class.java)
    private val purchaseUpdatedErrorSignal = SignalInfo("purchase_update_failed", Dictionary::class.java)
    // Purchases consuming
    private val purchaseConsumedSignal = SignalInfo("purchase_consumed", Dictionary::class.java)
    private val purchaseConsumedErrorSignal = SignalInfo("purchase_consumed_error", Dictionary::class.java)
    // Error signals
    private val purchasingFailedSignal = SignalInfo("purchasing_failed", Any::class.java)
    private val queryDetailsFailedSignal = SignalInfo("query_details_failed", Dictionary::class.java)



    override fun getPluginSignals(): Set<SignalInfo> {
        Log.i(pluginName, "Registering plugin signals")
        return setOf(
            helloResponseSignal,
            startConnectionSignal,
            connectedSignal,
            disconnectedSignal,
            queryPurchasesSignal,
            queryPurchasesErrorSignal,
            queryProductDetailsSignal,
            queryProductDetailsErrorSignal,
            purchasingFailedSignal,
            queryDetailsFailedSignal,
            purchaseUpdatedSignal,
//            purchaseCanceledSignal,
            purchaseUpdatedErrorSignal,
            purchaseConsumedSignal,
            purchaseConsumedErrorSignal,
        )
    }


    override fun getPluginName(): String {
        return ("AndroidIAPP")
    }


    // Just say hello func
    @UsedByGodot
    fun sayHello(says: String = "Hello from AndroidIAPP plugin") {
        runOnUiThread {
            Toast.makeText(activity, says, Toast.LENGTH_LONG).show()
            emitSignal(helloResponseSignal.name, says)
            Log.i(pluginName, says)
        }
    }


    @UsedByGodot
    private fun startConnection() {
        billingClient.startConnection(this)
        emitSignal(startConnectionSignal.name)
        Log.v(pluginName, "Billing service start connection")
    }


    override fun onBillingServiceDisconnected() {
        emitSignal(disconnectedSignal.name)
        Log.v(pluginName, "Billing service disconnected")
        // Try to restart the connection on the next request to
        // Google Play by calling the startConnection() method.
    }


    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            emitSignal(connectedSignal.name)
            Log.v(pluginName, "Billing service connected")
            // The BillingClient is ready. You can query purchases here.
        }
    }


    // https://developer.android.com/reference/com/android/billingclient/api/BillingClient.ProductType
    @UsedByGodot
    private fun queryPurchases(productType: String = ProductType.INAPP) {
        val params = QueryPurchasesParams
            .newBuilder()
            .setProductType(productType)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchaseList ->
            val returnDict = Dictionary() // from Godot type Dictionary
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.v(pluginName, "Purchases found")
                returnDict["response_code"] = billingResult.responseCode
                returnDict["purchases_list"] = IAPP_utils.convertPurchasesListToArray(purchaseList)
                emitSignal(queryPurchasesSignal.name, returnDict)
            } else {
                Log.v(pluginName, "No purchase found")
                returnDict["response_code"] = billingResult.responseCode
                returnDict["debug_message"] = billingResult.debugMessage
                returnDict["purchases_list"] = null
                emitSignal(queryPurchasesErrorSignal.name, returnDict)
            }
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
//    }


    @UsedByGodot
    private fun queryProductDetails(listOfProductsIDs: Array<String>, productType: String = ProductType.INAPP) {
        val products = ArrayList<QueryProductDetailsParams.Product>()

        for (productID in listOfProductsIDs) {
            products.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productID)
                .setProductType(productType)
                .build())
        }

        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()

        billingClient.queryProductDetailsAsync(queryProductDetailsParams) {
            billingResult, productDetailsList ->
            val returnDict = Dictionary() // from Godot type Dictionary
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.v(pluginName, "Product details found")
                returnDict["response_code"] = billingResult.responseCode
                returnDict["product_details_list"] = IAPP_utils.convertProductDetailsListToArray(productDetailsList)
                emitSignal(queryProductDetailsSignal.name, returnDict)
            } else {
                Log.v(pluginName, "No product details found")
                returnDict["response_code"] = billingResult.responseCode
                returnDict["debug_message"] = billingResult.debugMessage
                emitSignal(queryProductDetailsErrorSignal.name, returnDict)
            }
        }

    }

    @UsedByGodot
    private fun purchase(listOfProductsIDs: Array<String>,
                         productType: String,
                         isOfferPersonalized: Boolean) {

        val activity = activity!!

        // There can be only one!
        val productID = listOfProductsIDs[0]
        Log.v(pluginName, "Starting purchase flow for $productID product")

        // Before launching purchase flow, query product details.
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productID)
                        .setProductType(productType)
                        .build()
                )
            )
            .build()
        // Querying details for purchasing product.
        billingClient.queryProductDetailsAsync(queryProductDetailsParams) {
            queryDetailsResult, productDetailsList ->
            if (queryDetailsResult.responseCode != BillingClient.BillingResponseCode.OK) {
                // Error getting details, say something to godot users.
                Log.v(pluginName, "Error getting $productID details")
                emitSignal(queryDetailsFailedSignal.name, queryDetailsResult.responseCode, queryDetailsResult.debugMessage)
            } else {
                // Product details found successfully. Launch purchase flow.
                Log.v(pluginName, "Details for $productID found")
                val productDetailsParamsList = listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder().apply {
                            setProductDetails(productDetailsList[0])
                            val offerDetails = productDetailsList[0].subscriptionOfferDetails?.get(0)
                            if (offerDetails != null) {
                                // Optional, setting offer token only for subscriptions.
                                setOfferToken(offerDetails.offerToken)
                            }
                        }.build())
                val flowParams = BillingFlowParams
                    .newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    // https://developer.android.com/google/play/billing/integrate#personalized-price
                    .setIsOfferPersonalized(isOfferPersonalized)
                    .build()

                // Poneslos govno po trubam
                val purchasingResult = billingClient.launchBillingFlow(activity, flowParams)
                if (purchasingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Purchasing successfully launched.
                    // Result will be received in onPurchasesUpdated() method.
                    when (productType) {
                        ProductType.INAPP -> {
                            Log.v(pluginName, "Product $productID purchasing launched successfully")
                        }
                        ProductType.SUBS -> {
                            Log.v(pluginName, "Subscription $productID purchasing launched successfully")
                        }
                        else -> {
                            Log.v(pluginName, "Untyped $productID purchasing launched successfully :)")
                        }
                    }
                } else {
                    // Error purchasing. Says something to Godot users.
                    Log.v(pluginName, "$productID purchasing failed")
                    emitSignal(purchasingFailedSignal.name, purchasingResult.responseCode, purchasingResult.debugMessage)
             }
            }
        }
    }


    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        val returnDict = Dictionary() // from Godot type Dictionary
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            // All good, some purchase(s) have been updated
            Log.v(pluginName, "Purchases updated successfully")
            returnDict["response_code"] = billingResult.responseCode
            returnDict["purchases_list"] = IAPP_utils.convertPurchasesListToArray(purchases)
            emitSignal(purchaseUpdatedSignal.name, returnDict)
        } else {
            // Purchasing errors. Says something to Godot users.
            Log.v(pluginName, "Error purchase updating, response code: ${billingResult.responseCode}")
            returnDict["response_code"] = billingResult.responseCode
            returnDict["debug_message"] = billingResult.debugMessage
            emitSignal(purchaseUpdatedErrorSignal.name, returnDict)
        }
    }


    // Consume purchase
    // Kotlin coroutines are not supported in the current version of Godot (4.2).
    // Use this feature in later versions.
//    @UsedByGodot
//    suspend fun consumePurchase(purchaseToken: String) {
//        val consumeParams = ConsumeParams.newBuilder()
//            .setPurchaseToken(purchaseToken)
//            .build()
//        val consumeResult = withContext(Dispatchers.IO) {
//            client.consumePurchase(consumeParams)
//        }
//    }

    // Java style consuming purchase
    @UsedByGodot
    fun consumePurchase(savedPurchaseToken: String) {
        //Log.v(pluginName, "Consuming purchase $savedPurchaseToken")
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(savedPurchaseToken)
            .build()
        billingClient.consumeAsync(consumeParams) { billingResult, purchaseToken ->
            val returnDict = Dictionary() // from Godot type Dictionary
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.v(pluginName, "Purchase $purchaseToken consumed successfully")
                returnDict["response_code"] = billingResult.responseCode
                returnDict["purchase_token"] = purchaseToken
                emitSignal(purchaseConsumedSignal.name, returnDict)
            } else {
                Log.v(pluginName, "Error purchase consuming, response code: ${billingResult.responseCode}")
                returnDict["response_code"] = billingResult.responseCode
                returnDict["debug_message"] = billingResult.debugMessage
                returnDict["purchase_token"] = purchaseToken
                emitSignal(purchaseConsumedErrorSignal.name, returnDict)
            }
        }
    }
}





