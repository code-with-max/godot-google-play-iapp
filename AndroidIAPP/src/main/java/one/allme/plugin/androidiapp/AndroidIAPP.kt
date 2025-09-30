package one.allme.plugin.androidiapp

import one.allme.plugin.androidiapp.utils.IAPP_utils
import android.app.Activity
import android.util.Log
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.QueryPurchasesParams
import org.godotengine.godot.Godot
import org.godotengine.godot.Dictionary
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot

/**
 * AndroidIAPP is a Godot plugin for handling in-app purchases using the Google Play Billing Library.
 */
class AndroidIAPP(godot: Godot?) : GodotPlugin(godot), PurchasesUpdatedListener, BillingClientStateListener {
    private lateinit var billingClient: BillingClient
    private val pluginName = "AndroidIAPP"
    private val productDetailsMapInapp = mutableMapOf<String, ProductDetails>()
    private val productDetailsMapSubs = mutableMapOf<String, ProductDetails>()
    private var obfuscatedAccountId: String = ""
    private var obfuscatedProfileId: String = ""

    // Signals
    private val helloResponseSignal = SignalInfo("helloResponse", String::class.java)
    private val startConnectionSignal = SignalInfo("startConnection")
    private val connectedSignal = SignalInfo("connected")
    private val disconnectedSignal = SignalInfo("disconnected")
    private val queryPurchasesSignal = SignalInfo("query_purchases", Dictionary::class.java)
    private val queryPurchasesErrorSignal = SignalInfo("query_purchases_error", Dictionary::class.java)
    private val queryProductDetailsSignal = SignalInfo("query_product_details", Dictionary::class.java)
    private val queryProductDetailsErrorSignal = SignalInfo("query_product_details_error", Dictionary::class.java)
    private val purchaseSignal = SignalInfo("purchase", Dictionary::class.java)
    private val purchaseErrorSignal = SignalInfo("purchase_error", Dictionary::class.java)
    private val purchaseUpdatedSignal = SignalInfo("purchase_updated", Dictionary::class.java)
    private val purchaseCancelledSignal = SignalInfo("purchase_cancelled", Dictionary::class.java)
    private val purchaseUpdatedErrorSignal = SignalInfo("purchase_update_error", Dictionary::class.java)
    private val purchaseConsumedSignal = SignalInfo("purchase_consumed", Dictionary::class.java)
    private val purchaseConsumedErrorSignal = SignalInfo("purchase_consumed_error", Dictionary::class.java)
    private val purchaseAcknowledgedSignal = SignalInfo("purchase_acknowledged", Dictionary::class.java)
    private val purchaseAcknowledgedErrorSignal = SignalInfo("purchase_acknowledged_error", Dictionary::class.java)
    private val billingInfoSignal = SignalInfo("billing_info", Dictionary::class.java)
    private val priceChangeAcknowledgedSignal = SignalInfo("price_change_acknowledged", Dictionary::class.java)
    private val priceChangeErrorSignal = SignalInfo("price_change_error", Dictionary::class.java)
    private val inAppMessageResultSignal = SignalInfo("in_app_message_result", Dictionary::class.java)
    private val alternativeBillingOnlyTransactionReportedSignal = SignalInfo("alternative_billing_only_transaction_reported", Dictionary::class.java)

    override fun getPluginName(): String {
        return pluginName
    }

    override fun getPluginSignals(): Set<SignalInfo> {
        Log.i(pluginName, "Registering plugin signals")
        return setOf(
            helloResponseSignal, startConnectionSignal, connectedSignal, disconnectedSignal,
            queryPurchasesSignal, queryPurchasesErrorSignal, queryProductDetailsSignal,
            queryProductDetailsErrorSignal, purchaseSignal, purchaseErrorSignal,
            purchaseUpdatedSignal, purchaseCancelledSignal, purchaseUpdatedErrorSignal,
            purchaseConsumedSignal, purchaseConsumedErrorSignal, purchaseAcknowledgedSignal,
            purchaseAcknowledgedErrorSignal, billingInfoSignal, priceChangeAcknowledgedSignal,
            priceChangeErrorSignal, inAppMessageResultSignal, alternativeBillingOnlyTransactionReportedSignal
        )
    }

    private fun sendInfoSignal(returnDict: Dictionary) {
        returnDict["plugin_name"] = pluginName
        emitSignal(billingInfoSignal.name, returnDict)
    }

    private fun requireActivityForPurchase(returnDict: Dictionary): Activity? {
        return activity?.also {
            Log.i(pluginName, "Activity available for purchase (fun requireActivityForPurchase)")
            returnDict["requireActivityForPurchase"] = "OK: Activity available for purchase"
            sendInfoSignal(returnDict)
        } ?: run {
            Log.e(pluginName, "Cannot proceed: Activity is null")
            returnDict["requireActivityForPurchase"] = "ERROR: Cannot proceed: Activity is null"
            sendInfoSignal(returnDict)
            null
        }
    }

    @get:UsedByGodot
    val isReady: Boolean
        get() {
            if (!::billingClient.isInitialized) {
                Log.e(pluginName, "BillingClient is not initialized.")
                return false
            }
            Log.i(pluginName, "Is ready: ${billingClient.isReady}")
            return billingClient.isReady
        }

    @UsedByGodot
    fun sayHello(says: String = "Hello from AndroidIAPP plugin") {
        val returnDict = Dictionary()
        returnDict["fun_name"] = "sayHello"
        returnDict["says"] = says
        if (activity == null) {
            Log.e(pluginName, "Cannot show Toast: Activity is null")
            emitSignal(helloResponseSignal.name, "Error: Activity is null")
            returnDict["debug_message"] = "Cannot show Toast: Activity is null"
            sendInfoSignal(returnDict)
            return
        }
        val postToast: () -> Unit = {
            Toast.makeText(activity, says, Toast.LENGTH_LONG).show()
            Log.i(pluginName, says)
            emitSignal(helloResponseSignal.name, says)
            returnDict["debug_message"] = says
            sendInfoSignal(returnDict)
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            activity?.runOnUiThread(postToast)
        } else {
            postToast()
        }
    }



    @UsedByGodot
    fun startConnection() {
        Log.i(pluginName, "Starting billing service connection")
        if (activity == null) {
            Log.e(pluginName, "Cannot start BillingClient connection: Activity is null")
            val returnDict = Dictionary()
            returnDict["fun_name"] = "startConnection"
            returnDict["response_code"] = BillingClient.BillingResponseCode.ERROR
            returnDict["debug_message"] = "Cannot start BillingClient connection: Activity is null"
            sendInfoSignal(returnDict)
            return
        }
        if (::billingClient.isInitialized && billingClient.isReady) {
            Log.i(pluginName, "BillingClient is already connected.")
            emitSignal(connectedSignal.name)
            return
        }
        try {
            Log.i(pluginName, "Creating billing client")
            billingClient = BillingClient.newBuilder(activity!!)
                .setListener(this)
                .enableAutoServiceReconnection()
                .enablePendingPurchases(
                    PendingPurchasesParams.newBuilder()
                        .enablePrepaidPlans() // Explicitly enable support for prepaid plans.
                        .enableOneTimeProducts() // Explicitly enable support for pending one-time purchases.
                        .build()
                )
                .build()

            Log.i(pluginName, "Billing client created successfully, starting connection.")
            billingClient.startConnection(this)
            emitSignal(startConnectionSignal.name)
        } catch (e: Exception) {
            Log.e(pluginName, "Error initializing BillingClient: ${e.message}", e)
            val returnDict = Dictionary()
            returnDict["fun_name"] = "startConnection"
            returnDict["response_code"] = BillingClient.BillingResponseCode.ERROR
            returnDict["debug_message"] = "BillingClient initialization failed: ${e.message}"
            sendInfoSignal(returnDict)
        }
    }

    @UsedByGodot
    fun endConnection() {
        val returnDict = Dictionary()
        if (::billingClient.isInitialized && billingClient.isReady) {
            Log.i(pluginName, "Ending billing service connection.")
            returnDict["fun_name"] = "endConnection"
            returnDict["debug_message"] = "Ending billing service connection."
            sendInfoSignal(returnDict)
            billingClient.endConnection()
        }
    }

    override fun onBillingServiceDisconnected() {
        emitSignal(disconnectedSignal.name)
        Log.i(pluginName, "Billing service disconnected. Trying to reconnect...")
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            emitSignal(connectedSignal.name)
            Log.i(pluginName, "Billing service connected")
        } else {
            Log.e(pluginName, "Billing setup failed with response code: ${billingResult.responseCode}")
            val returnDict = Dictionary()
            returnDict["response_code"] = billingResult.responseCode
            returnDict["debug_message"] = billingResult.debugMessage
            sendInfoSignal(returnDict)
        }
    }

    @UsedByGodot
    fun queryPurchases(productType: String = ProductType.INAPP) {
        if (!isReady) {
            Log.e(pluginName, "Billing client is not ready. Cannot query purchases.")
            return
        }
        val params = QueryPurchasesParams.newBuilder().setProductType(productType).build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchaseList ->
            val returnDict = Dictionary()
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.i(pluginName, "Purchases found")
                returnDict["response_code"] = billingResult.responseCode
                returnDict["purchases_list"] = IAPP_utils.convertPurchasesListToArray(purchaseList)
                emitSignal(queryPurchasesSignal.name, returnDict)
            } else {
                Log.i(pluginName, "No purchase found or an error occurred.")
                returnDict["response_code"] = billingResult.responseCode
                returnDict["debug_message"] = billingResult.debugMessage
                returnDict["purchases_list"] = null
                emitSignal(queryPurchasesErrorSignal.name, returnDict)
            }
        }
    }

    @UsedByGodot
    fun queryProductDetails(listOfProductsIDs: Array<String>, productType: String = ProductType.INAPP) {
        if (!isReady) {
            Log.e(pluginName, "Billing client is not ready. Cannot query product details.")
            val returnDict = Dictionary().apply {
                put("response_code", BillingClient.BillingResponseCode.ERROR)
                put("debug_message", "Billing client is not ready")
            }
            emitSignal(queryProductDetailsErrorSignal.name, returnDict)
            return
        }

        val products = listOfProductsIDs.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it)
                .setProductType(productType)
                .build()
        }

        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()

        billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, queryProductDetailsResult ->
            val returnDict = IAPP_utils.convertQueryProductDetailsResultToDictionary(queryProductDetailsResult)
            returnDict["response_code"] = billingResult.responseCode

            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.i(pluginName, "Product details found")
                queryProductDetailsResult.productDetailsList.forEach { productDetails ->
                    val map = if (productType == ProductType.INAPP) productDetailsMapInapp else productDetailsMapSubs
                    map[productDetails.productId] = productDetails
                }
                emitSignal(queryProductDetailsSignal.name, returnDict)
            } else {
                Log.e(pluginName, "No product details found or an error occurred: ${billingResult.debugMessage}")
                returnDict["debug_message"] = billingResult.debugMessage
                emitSignal(queryProductDetailsErrorSignal.name, returnDict)
            }
        }
    }

    @UsedByGodot
    fun purchase(listOfProductsIDs: Array<String>, isOfferPersonalized: Boolean) {
        val returnDict = Dictionary()
        returnDict["response_code"] = BillingClient.BillingResponseCode.ERROR
        returnDict["debug_message"] = "Purchase called"
        returnDict["product_id"] = if (listOfProductsIDs.isNotEmpty()) listOfProductsIDs[0] else ""

        val activity = requireActivityForPurchase(returnDict) ?: return

        if (listOfProductsIDs.isEmpty()) {
            Log.e(pluginName, "Cannot start purchase: Product ID list is empty")
            returnDict["debug_message"] = "Product ID list is empty"
            emitSignal(purchaseErrorSignal.name, returnDict)
            return
        }

        val productID = listOfProductsIDs[0]
        if (productID.isBlank()) {
            Log.e(pluginName, "Cannot start purchase: Product ID is blank")
            returnDict["debug_message"] = "Product ID is blank"
            emitSignal(purchaseErrorSignal.name, returnDict)
            return
        }
        Log.i(pluginName, "Starting purchase flow for $productID product")
        launchPurchaseFlow(activity, productID, ProductType.INAPP, null, isOfferPersonalized)
    }

    @UsedByGodot
    fun subscribe(listOfProductsIDs: Array<String>, basePlanIDs: Array<String>, isOfferPersonalized: Boolean) {
        val returnDict = Dictionary()
        val activity = requireActivityForPurchase(returnDict) ?: return

        val productID = listOfProductsIDs.firstOrNull()
        val basePlanID = basePlanIDs.firstOrNull()

        if (productID.isNullOrBlank() || basePlanID.isNullOrBlank()) {
            Log.e(pluginName, "Product ID or Base Plan ID is missing.")
            returnDict["debug_message"] = "Product ID or Base Plan ID is missing."
            emitSignal(purchaseErrorSignal.name, returnDict)
            return
        }

        Log.i(pluginName, "Starting purchase flow for $productID subscription with base plan $basePlanID")
        launchPurchaseFlow(activity, productID, ProductType.SUBS, basePlanID, isOfferPersonalized)
    }

    private fun launchPurchaseFlow(
        activity: Activity,
        productID: String,
        productType: String,
        basePlanID: String? = null,
        isOfferPersonalized: Boolean = false,
        oldPurchaseToken: String? = null,
        replacementMode: Int = BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.UNKNOWN_REPLACEMENT_MODE
    ) {
        val returnDict = Dictionary().apply {
            put("product_id", productID)
            if (basePlanID != null) put("base_plan_id", basePlanID)
        }

        val productDetailsMap = when (productType) {
            BillingClient.ProductType.INAPP -> productDetailsMapInapp
            BillingClient.ProductType.SUBS -> productDetailsMapSubs
            else -> {
                returnDict["response_code"] = BillingClient.BillingResponseCode.DEVELOPER_ERROR
                returnDict["debug_message"] = "Unsupported product type: $productType"
                Log.e(pluginName, "Unsupported product type: $productType")
                emitSignal(purchaseErrorSignal.name, returnDict)
                return
            }
        }

        val productDetails = productDetailsMap[productID]
        if (productDetails == null) {
            returnDict["response_code"] = BillingClient.BillingResponseCode.DEVELOPER_ERROR
            returnDict["debug_message"] = "Product ID $productID not found. You must query product details first."
            Log.e(pluginName, "Product ID $productID not found. You must query product details first.")
            emitSignal(purchaseErrorSignal.name, returnDict)
            return
        }

        val productDetailsParamsList = mutableListOf<BillingFlowParams.ProductDetailsParams>()
        val builder = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(productDetails)

        if (productType == BillingClient.ProductType.SUBS) {
            val offerDetails = productDetails.subscriptionOfferDetails?.firstOrNull { it.basePlanId == basePlanID }
            if (offerDetails != null) {
                builder.setOfferToken(offerDetails.offerToken)
            } else {
                Log.e(pluginName, "Base Plan ID $basePlanID not found in $productID subscription")
                returnDict["response_code"] = BillingClient.BillingResponseCode.DEVELOPER_ERROR
                returnDict["debug_message"] = "Base Plan ID $basePlanID not found in $productID subscription"
                emitSignal(purchaseErrorSignal.name, returnDict)
                return
            }
        }

        productDetailsParamsList.add(builder.build())

        val flowParamsBuilder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .setIsOfferPersonalized(isOfferPersonalized)

        if (obfuscatedAccountId.isNotEmpty()) {
            flowParamsBuilder.setObfuscatedAccountId(obfuscatedAccountId)
        }
        if (obfuscatedProfileId.isNotEmpty()) {
            flowParamsBuilder.setObfuscatedProfileId(obfuscatedProfileId)
        }

        if (oldPurchaseToken != null && replacementMode != BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.UNKNOWN_REPLACEMENT_MODE) {
            val updateParams = BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                .setOldPurchaseToken(oldPurchaseToken)
                .setSubscriptionReplacementMode(replacementMode)
                .build()
            flowParamsBuilder.setSubscriptionUpdateParams(updateParams)
        }

        val purchasingResult = billingClient.launchBillingFlow(activity, flowParamsBuilder.build())
        returnDict["response_code"] = purchasingResult.responseCode

        if (purchasingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(pluginName, "$productID purchasing failed: ${purchasingResult.debugMessage}")
            returnDict["debug_message"] = purchasingResult.debugMessage
            emitSignal(purchaseErrorSignal.name, returnDict)
        } else {
            Log.i(pluginName, "Product $productID purchasing launched successfully")
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        val returnDict = Dictionary()
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    Log.i(pluginName, "Purchases updated successfully")
                    returnDict["response_code"] = billingResult.responseCode
                    returnDict["purchases_list"] = IAPP_utils.convertPurchasesListToArray(purchases)
                    emitSignal(purchaseUpdatedSignal.name, returnDict)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.i(pluginName, "User canceled purchase updating")
                returnDict["response_code"] = billingResult.responseCode
                returnDict["debug_message"] = billingResult.debugMessage
                emitSignal(purchaseCancelledSignal.name, returnDict)
            }
            else -> {
                Log.i(pluginName, "Error purchase updating, response code: ${billingResult.responseCode}")
                returnDict["response_code"] = billingResult.responseCode
                returnDict["debug_message"] = billingResult.debugMessage
                emitSignal(purchaseUpdatedErrorSignal.name, returnDict)
            }
        }
    }

    @UsedByGodot
    fun consumePurchase(purchaseToken: String) {
        if (!isReady) {
            Log.e(pluginName, "Billing client is not ready. Cannot consume purchase.")
            return
        }
        val consumeParams = ConsumeParams.newBuilder().setPurchaseToken(purchaseToken).build()
        billingClient.consumeAsync(consumeParams) { billingResult, outToken ->
            val returnDict = Dictionary()
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.i(pluginName, "Purchase consumed successfully: $outToken")
                returnDict["response_code"] = billingResult.responseCode
                returnDict["purchase_token"] = outToken
                emitSignal(purchaseConsumedSignal.name, returnDict)
            } else {
                Log.e(pluginName, "Error purchase consuming, response code: ${billingResult.responseCode}")
                returnDict["response_code"] = billingResult.responseCode
                returnDict["debug_message"] = billingResult.debugMessage
                returnDict["purchase_token"] = outToken
                emitSignal(purchaseConsumedErrorSignal.name, returnDict)
            }
        }
    }

    @UsedByGodot
    fun acknowledgePurchase(purchaseToken: String) {
        if (!isReady) {
            Log.e(pluginName, "Billing client is not ready. Cannot acknowledge purchase.")
            return
        }
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchaseToken).build()
        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            val returnDict = Dictionary()
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.i(pluginName, "Purchase acknowledged successfully: $purchaseToken")
                returnDict["response_code"] = billingResult.responseCode
                returnDict["purchase_token"] = purchaseToken
                emitSignal(purchaseAcknowledgedSignal.name, returnDict)
            } else {
                Log.e(pluginName, "Error purchase acknowledging, response code: ${billingResult.responseCode}")
                returnDict["response_code"] = billingResult.responseCode
                returnDict["debug_message"] = billingResult.debugMessage
                returnDict["purchase_token"] = purchaseToken
                emitSignal(purchaseAcknowledgedErrorSignal.name, returnDict)
            }
        }
    }

    @UsedByGodot
    fun showInAppMessages() {
        Log.w(pluginName, "showInAppMessages is not yet implemented.")
        val returnDict = Dictionary()
        returnDict["status"] = "not_implemented"
        returnDict["fun_name"] = "showInAppMessages"
        returnDict["debug_message"] = "showInAppMessages is not yet implemented."
        emitSignal(inAppMessageResultSignal.name, returnDict)
        sendInfoSignal(returnDict)
    }

    @UsedByGodot
    fun launchPriceChangeConfirmationFlow(productDetails: Dictionary) {
        Log.w(pluginName, "launchPriceChangeConfirmationFlow is not yet implemented.")
        val returnDict = Dictionary()
        returnDict["status"] = "not_implemented"
        returnDict["fun_name"] = "launchPriceChangeConfirmationFlow"
        returnDict["debug_message"] = "launchPriceChangeConfirmationFlow is not yet implemented."
        returnDict["see_details"] = "https://developer.android.com/google/play/billing/subscriptions#price-change"
        emitSignal(priceChangeErrorSignal.name, returnDict)
        sendInfoSignal(returnDict)
    }

    @UsedByGodot
    fun createAlternativeBillingOnlyReportingDetails() {
        Log.w(pluginName, "createAlternativeBillingOnlyReportingDetails is not yet implemented.")
        val returnDict = Dictionary()
        returnDict["status"] = "not_implemented"
        returnDict["fun_name"] = "createAlternativeBillingOnlyReportingDetails"
        returnDict["debug_message"] = "createAlternativeBillingOnlyReportingDetails is not yet implemented."
        returnDict["see_details"] = "https://developer.android.com/google/play/billing/alternative"
        sendInfoSignal(returnDict)
    }

    @UsedByGodot
    fun reportAlternativeBillingOnlyTransaction(reportingDetails: Dictionary) {
        Log.w(pluginName, "reportAlternativeBillingOnlyTransaction is not yet implemented.")
        val returnDict = Dictionary()
        returnDict["status"] = "not_implemented"
        returnDict["fun_name"] = "reportAlternativeBillingOnlyTransaction"
        returnDict["debug_message"] = "reportAlternativeBillingOnlyTransaction is not yet implemented."
        returnDict["see_details"] = "https://developer.android.com/google/play/billing/alternative"
        emitSignal(alternativeBillingOnlyTransactionReportedSignal.name, returnDict)
        sendInfoSignal(returnDict)
    }
}