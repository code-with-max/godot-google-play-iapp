package one.allme.plugin.androidiapp

import one.allme.plugin.androidiapp.utils.IAPP_utils

import android.app.Activity
import android.util.Log
import android.widget.Toast

import android.os.Handler
import android.os.Looper
import java.lang.Thread

import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.AcknowledgePurchaseParams
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
//import com.android.billingclient.api.consumePurchase
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import kotlin.coroutines.resume
//import kotlin.coroutines.suspendCoroutine
import org.godotengine.godot.Godot
import org.godotengine.godot.Dictionary
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot


class AndroidIAPP(godot: Godot?): GodotPlugin(godot),
    PurchasesUpdatedListener,
    BillingClientStateListener {


//    private val purchasesUpdatedListener =
//        PurchasesUpdatedListener { billingResult, purchases -> }
//    private val acknowledgePurchaseResponseListener: AcknowledgePurchaseResponseListener = AcknowledgePurchaseResponseListener { billingResult -> }


//    private val billingClient: BillingClient = BillingClient
//        .newBuilder(activity!!)
//        .enablePendingPurchases()
//        .setListener(this)
//        .build()
    private lateinit var billingClient: BillingClient

//    private val billingClient: BillingClient = BillingClient
//        .newBuilder(activity!!)
//        .setListener(this)
//        .enablePendingPurchases(
//            PendingPurchasesParams.newBuilder().build() // Простое создание без дополнительных методов
//        )
//        .build()


    // Signals
    // Echo
    private val helloResponseSignal = SignalInfo(
        "helloResponse", String::class.java,
        )

    // Information
    private val startConnectionSignal = SignalInfo(
        "startConnection",
        )
    private val connectedSignal = SignalInfo(
        "connected",
        )
    private val disconnectedSignal = SignalInfo(
        "disconnected",
        )

    // Query purchases
    private val queryPurchasesSignal = SignalInfo(
        "query_purchases", Dictionary::class.java,
        )
    private val queryPurchasesErrorSignal = SignalInfo(
        "query_purchases_error", Dictionary::class.java,
        )

    // Query product details
    private val queryProductDetailsSignal = SignalInfo(
        "query_product_details", Dictionary::class.java,
        )
    private val queryProductDetailsErrorSignal = SignalInfo(
        "query_product_details_error", Dictionary::class.java,
        )

    // Purchase processing
    private val purchaseSignal = SignalInfo(
        "purchase", Dictionary::class.java,
        )
    private val purchaseErrorSignal = SignalInfo(
        "purchase_error", Dictionary::class.java,
        )

    // Purchase updating
    private val purchaseUpdatedSignal = SignalInfo(
        "purchase_updated", Dictionary::class.java,
        )
    private val purchaseCancelledSignal = SignalInfo(
        "purchase_cancelled", Dictionary::class.java,
        )
    private val purchaseUpdatedErrorSignal = SignalInfo(
        "purchase_update_error", Dictionary::class.java,
        )

    // Purchases consuming
    private val purchaseConsumedSignal = SignalInfo(
        "purchase_consumed", Dictionary::class.java,
        )
    private val purchaseConsumedErrorSignal = SignalInfo(
        "purchase_consumed_error", Dictionary::class.java,
        )

    // Purchases acknowledge
    private val purchaseAcknowledgedSignal = SignalInfo(
        "purchase_acknowledged", Dictionary::class.java,
        )
    private val purchaseAcknowledgedErrorSignal = SignalInfo(
        "purchase_acknowledged_error", Dictionary::class.java,
        )


    private fun requireActivityForPurchase(
        errorSignal: SignalInfo,
        errorDict: Dictionary
    ): Activity? {
        return activity?.also {
            Log.v(pluginName, "Activity available for purchase")
        } ?: run {
            Log.e(pluginName, "Cannot proceed: Activity is null")
            emitSignal(errorSignal.name, errorDict)
            null
        }
    }

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
            purchaseSignal,
            purchaseErrorSignal,
            purchaseUpdatedSignal,
            purchaseCancelledSignal,
            purchaseUpdatedErrorSignal,
            purchaseConsumedSignal,
            purchaseConsumedErrorSignal,
            purchaseAcknowledgedSignal,
            purchaseAcknowledgedErrorSignal
        )
    }


    override fun getPluginName(): String {
        return ("AndroidIAPP")
    }


    @get:UsedByGodot
    val isReady: Boolean
        get() {
            Log.v(pluginName, "Is ready: ${billingClient.isReady}")
            return billingClient.isReady
        }


    // TODO Type mismatch. Check if it is possible/useful
    // https://developer.android.com/reference/com/android/billingclient/api/BillingClient.ConnectionState
//    @get:UsedByGodot
//    val connectionState: Int
//        get() {
//            Log.v(pluginName, "Connection state: ${billingClient.connectionState}")
//            return billingClient.connectionState
//        }


    // Just say hello func
    @UsedByGodot
    fun sayHello(says: String = "Hello from AndroidIAPP plugin") {
        if (activity == null) {
            Log.e(pluginName, "Cannot show Toast: Activity is null")
            val returnDict = Dictionary()
            returnDict["debug_message"] = "Activity is null"
            emitSignal(helloResponseSignal.name, "Error: Activity is null")
            return
        }
        if (Looper.getMainLooper().thread !== Thread.currentThread()) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(activity, says, Toast.LENGTH_LONG).show()
                emitSignal(helloResponseSignal.name, says)
                Log.i(pluginName, says)
            }
        } else {
            Toast.makeText(activity, says, Toast.LENGTH_LONG).show()
            emitSignal(helloResponseSignal.name, says)
            Log.i(pluginName, says)
        }
    }

    @UsedByGodot
    private fun startConnection() {
        if (activity == null) {
            Log.e(pluginName, "Cannot start BillingClient connection: Activity is null")
            val returnDict = Dictionary()
            returnDict["response_code"] = BillingClient.BillingResponseCode.ERROR
            returnDict["debug_message"] = "Activity is null"
            emitSignal(disconnectedSignal.name, returnDict)
            return
        }
        billingClient = BillingClient.newBuilder(activity!!)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().build()
            )
            .build()
        billingClient.startConnection(this)
        emitSignal(startConnectionSignal.name)
        Log.v(pluginName, "Billing service start connection")
    }
//        billingClient.startConnection(this)
//        emitSignal(startConnectionSignal.name)
//        Log.v(pluginName, "Billing service start connection")
//    }


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
    // productType should be ProductType.INAPP or ProductType.SUBS
    // TODO Default value of productType not setting. Need function overloading for fixing
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
    // Kotlin coroutines are not supported in the current version of Godot (4.2).
    // java.lang.NoSuchMethodError: no non-static method "Lone/allme/plugin/androidiapp/AndroidIAPP;.queryPurchasesAsync
    // Use this feature in later versions.
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
    private fun queryProductDetails(
        listOfProductsIDs: Array<String>, productType: String = ProductType.INAPP) {
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
                returnDict["product_details_list"] = IAPP_utils
                    .convertProductDetailsListToArray(productDetailsList)
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
                         isOfferPersonalized: Boolean) {

        val returnDict = Dictionary()
        returnDict["response_code"] = BillingClient.BillingResponseCode.ERROR
        returnDict["debug_message"] = "Activity is null"
        returnDict["product_id"] = if (listOfProductsIDs.isNotEmpty()) listOfProductsIDs[0] else ""

        val activity = requireActivityForPurchase(purchaseErrorSignal, returnDict) ?: return

        if (listOfProductsIDs.isEmpty()) {
            Log.e(pluginName, "Cannot start purchase: Product ID list is empty")
            returnDict["debug_message"] = "Product ID list is empty"
            emitSignal(purchaseErrorSignal.name, returnDict)
            return
        }

        // There can be only one!
        val productID = listOfProductsIDs[0]
        if (productID.isBlank()) {
            Log.e(pluginName, "Cannot start purchase: Product ID is blank")
            returnDict["debug_message"] = "Product ID is blank"
            emitSignal(purchaseErrorSignal.name, returnDict)
            return
        }
        Log.v(pluginName, "Starting purchase flow for $productID product")

        // Before launching purchase flow, query product details.
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productID)
                        .setProductType(ProductType.INAPP)
                        .build()
                )
            )
            .build()
        // Querying details for purchasing product.
        billingClient.queryProductDetailsAsync(queryProductDetailsParams) {
            queryDetailsResult, productDetailsList ->
            val returnDict = Dictionary() // from Godot type Dictionary
            if (queryDetailsResult.responseCode != BillingClient.BillingResponseCode.OK) {
                // Error getting details, say something to godot users.
                Log.v(pluginName, "Error getting $productID product details")
                returnDict["response_code"] = queryDetailsResult.responseCode
                returnDict["debug_message"] = queryDetailsResult.debugMessage
                emitSignal(queryProductDetailsErrorSignal.name, returnDict)
            } else {
                // Product details found successfully. Launch purchase flow.
                Log.v(pluginName, "Details for product $productID found")
                val productDetailsParamsList = listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder().apply {
                            // There can be only one!
                            setProductDetails(productDetailsList[0])
//                            val offerDetails = productDetailsList[0].subscriptionOfferDetails?.get(0)
//                            Log.v(pluginName, "Offer details token: ${offerDetails?.offerToken}")
//                            if (offerDetails != null) {
//                                // Optional, setting offer token only for subscriptions.
//                                setOfferToken(offerDetails.offerToken)
//                            }
                        }.build())
                val flowParams = BillingFlowParams
                    .newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    // https://developer.android.com/google/play/billing/integrate#personalized-price
                    .setIsOfferPersonalized(isOfferPersonalized)
                    .build()

                // Running purchase flow.
                val purchasingResult = billingClient.launchBillingFlow(activity, flowParams)
                if (purchasingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Purchasing successfully launched.
                    // Result will be received in onPurchasesUpdated() method.
                    Log.v(pluginName, "Product $productID purchasing launched successfully")
                } else {
                    // Error purchasing. Says something to Godot users.
                    Log.v(pluginName, "$productID purchasing failed")
                    returnDict["response_code"] = purchasingResult.responseCode
                    returnDict["debug_message"] = purchasingResult.debugMessage
                    returnDict["product_id"] = productID
                    emitSignal(purchaseErrorSignal.name, returnDict)
                }
            }
        }
    }


    // Process subscriptions
    @UsedByGodot
    private fun subscribe(listOfProductsIDs: Array<String>,
                          basePlanIDs: Array<String>,
                          isOfferPersonalized: Boolean) {

        val activity = activity!!

        // There can be only one!
        val productID = listOfProductsIDs[0]
        val basePlanID = basePlanIDs[0]
        Log.v(pluginName, "Starting purchase flow for $productID subscription")

        // Before launching purchase flow, query product details.
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productID)
                        .setProductType(ProductType.SUBS)
                        .build()
                )
            )
            .build()
        // Querying details for subscription.
        billingClient.queryProductDetailsAsync(queryProductDetailsParams) {
                queryDetailsResult, productDetailsList ->
            val returnDict = Dictionary() // from Godot type Dictionary
            if (queryDetailsResult.responseCode != BillingClient.BillingResponseCode.OK) {
                // Error getting details, say something to godot users.
                Log.v(pluginName, "Error getting $productID subscription details")
                returnDict["response_code"] = queryDetailsResult.responseCode
                returnDict["debug_message"] = queryDetailsResult.debugMessage
                emitSignal(queryProductDetailsErrorSignal.name, returnDict)
            } else {
                // Product details found successfully. Launch purchase flow.
                Log.v(pluginName, "Details for subscription $productID found")
                val productDetailsParamsList = listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder().apply {
                        // There can be only one!
                        setProductDetails(productDetailsList[0])
                        val offerDetails =
                            productDetailsList[0].subscriptionOfferDetails?.firstOrNull { it.basePlanId == basePlanID }
                        if (offerDetails != null) {
                            setOfferToken(offerDetails.offerToken)
                        } else {
                            Log.v(
                                pluginName,
                                "Base Plan ID $basePlanID not found in $productID subscription"
                            )
                            returnDict["debug_message"] =
                                "Base Plan ID $basePlanID not found in $productID subscription"
                            emitSignal(purchaseErrorSignal.name, returnDict)
                        }
                    }.build()
                )
                val flowParams = BillingFlowParams
                    .newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    // https://developer.android.com/google/play/billing/integrate#personalized-price
                    .setIsOfferPersonalized(isOfferPersonalized)
                    .build()

                // Running subscription flow.
                val purchasingResult = billingClient.launchBillingFlow(activity, flowParams)
                if (purchasingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Subscription successfully launched.
                    // Result will be received in onPurchasesUpdated() method.
                    Log.v(
                        pluginName,
                        "Subscription $productID with $basePlanID purchasing launched successfully"
                    )
                } else {
                    // Error purchasing. Says something to Godot users.
                    Log.v(pluginName, "$productID purchasing failed")
                    returnDict["response_code"] = purchasingResult.responseCode
                    returnDict["debug_message"] = purchasingResult.debugMessage
                    returnDict["product_id"] = productID
                    returnDict["base_plan_id"] = basePlanID
                    emitSignal(purchaseErrorSignal.name, returnDict)
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
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.v(pluginName, "User canceled purchase updating")
            returnDict["response_code"] = billingResult.responseCode
            returnDict["debug_message"] = billingResult.debugMessage
            emitSignal(purchaseCancelledSignal.name, returnDict)
        } else {
            // Purchasing errors. Says something to Godot users.
            Log.v(pluginName, "Error purchase updating, response code: ${billingResult.responseCode}")
            returnDict["response_code"] = billingResult.responseCode
            returnDict["debug_message"] = billingResult.debugMessage
            emitSignal(purchaseUpdatedErrorSignal.name, returnDict)
        }
    }


    // Consume purchase using Kotlin coroutines
    // Kotlin coroutines are not supported in the current version of Godot (4.2).
    // java.lang.NoSuchMethodError: no non-static method "Lone/allme/plugin/androidiapp/AndroidIAPP;.consumePurchaseKT
    // Use this feature in later versions.
    //
//    @UsedByGodot
//    suspend fun consumePurchaseKT(purchaseTokenLocal: String) {
//        val consumeParams = ConsumeParams.newBuilder()
//            .setPurchaseToken(purchaseTokenLocal)
//            .build()
//        val consumeResult = withContext(Dispatchers.IO) {
//            billingClient.consumePurchase(consumeParams) }
//        val returnDict = Dictionary() // from Godot type Dictionary
//        if (consumeResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
//            Log.v(pluginName, "Purchase ${consumeResult.purchaseToken} acknowledged successfully")
//            returnDict["response_code"] = consumeResult.billingResult.responseCode
//            returnDict["purchase_token"] = consumeResult.purchaseToken
//            emitSignal(purchaseConsumedSignal.name, returnDict)
//        } else {
//            Log.v(pluginName, "Error purchase acknowledging, response code: ${consumeResult.billingResult.responseCode}")
//            returnDict["response_code"] = consumeResult.billingResult.responseCode
//            returnDict["debug_message"] = consumeResult.billingResult.debugMessage
//            emitSignal(purchaseConsumedSignal.name, returnDict)
//        }
//    }


    // Acknowledge purchase using Kotlin coroutines
    // Kotlin coroutines are not supported in the current version of Godot (4.2).
    // " java.lang.NoSuchMethodError: no non-static method "
    // Use this feature in later versions.
    //
//    @UsedByGodot
//    suspend fun acknowledgePurchaseKT(purchaseTokenLocal: String) {
//        val returnDict = Dictionary() // from Godot type Dictionary
//        val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
//            .setPurchaseToken(purchaseTokenLocal)
//            .build()
//        try {
//            withContext(Dispatchers.IO) {
//                val acknowledgeResult = suspendCoroutine<BillingResult> { continuation ->
//                    billingClient.acknowledgePurchase(acknowledgeParams) { billingResult ->
//                        continuation.resume(billingResult)
//                    }
//                }
//
//                if (acknowledgeResult.responseCode == BillingClient.BillingResponseCode.OK) {
//                    Log.v(pluginName, "Purchase $purchaseTokenLocal acknowledged successfully")
//                    returnDict["response_code"] = acknowledgeResult.responseCode
//                    returnDict["purchase_token"] = purchaseTokenLocal
//                    emitSignal(purchaseAcknowledgedSignal.name, returnDict)
//                } else {
//                    Log.v(pluginName, "Error purchase acknowledging, response code: ${acknowledgeResult.responseCode}")
//                    returnDict["response_code"] = acknowledgeResult.responseCode
//                    returnDict["debug_message"] = acknowledgeResult.debugMessage
//                    emitSignal(purchaseAcknowledgedErrorSignal.name, returnDict)
//                }
//            }
//        } catch (e: Exception) {
//            Log.v (pluginName, "Error purchase acknowledging, exception: ${e.message}")
//            returnDict["response_code"] = BillingClient.BillingResponseCode.ERROR
//            returnDict["debug_message"] = e.message
//            emitSignal(purchaseAcknowledgedErrorSignal.name, returnDict)
//        }
//    }



    // Java style consuming purchase
    @UsedByGodot
    fun consumePurchase(purchaseTokenLocal: String) {
        //Log.v(pluginName, "Consuming purchase $savedPurchaseToken")
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchaseTokenLocal)
            .build()
        billingClient.consumeAsync(consumeParams) { billingResult, purchaseToken ->
            val returnDict = Dictionary() // from Godot type Dictionary
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.v(pluginName, "Purchase consumed successfully: $purchaseToken")
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


    // Java style acknowledging purchase
    @UsedByGodot
    private fun acknowledgePurchase(purchaseToken: String) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams
            .newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            val returnDict = Dictionary() // from Godot type Dictionary
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.v(pluginName, "Purchase acknowledged successfully: $purchaseToken")
                returnDict["response_code"] = billingResult.responseCode
                returnDict["purchase_token"] = purchaseToken
                emitSignal(purchaseAcknowledgedSignal.name, returnDict)
            } else {
                Log.v(pluginName, "Error purchase acknowledging, response code: ${billingResult.responseCode}")
                returnDict["response_code"] = billingResult.responseCode
                returnDict["debug_message"] = billingResult.debugMessage
                returnDict["purchase_token"] = purchaseToken
                emitSignal(purchaseAcknowledgedErrorSignal.name, returnDict)
            }
        }
    }


}





