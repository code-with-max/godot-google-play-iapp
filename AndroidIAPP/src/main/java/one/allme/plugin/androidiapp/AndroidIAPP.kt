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
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import org.godotengine.godot.Godot
import org.godotengine.godot.Dictionary
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot


/**
 * AndroidIAPP is a Godot plugin for handling in-app purchases using the Google Play Billing Library.
 *
 * This plugin provides functionality to:
 * - Connect to the Google Play Billing service.
 * - Query for available products (both one-time purchases and subscriptions).
 * - Initiate purchase flows for products.
 * - Handle purchase updates, including new purchases, cancellations, and errors.
 * - Acknowledge and consume purchases.
 *
 * The plugin communicates with Godot through signals, providing updates on the status of various operations.
 *
 * It is important to call `endConnection()` when the plugin is no longer in use to release resources.
 *
 * @param godot The Godot instance.
 */
class AndroidIAPP(godot: Godot?): GodotPlugin(godot),
    PurchasesUpdatedListener,
    BillingClientStateListener {

    private lateinit var billingClient: BillingClient
    private val pluginName = "AndroidIAPP"

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
            purchaseAcknowledgedErrorSignal,
            billingInfoSignal,
            priceChangeAcknowledgedSignal,
            priceChangeErrorSignal,
            inAppMessageResultSignal,
            alternativeBillingOnlyTransactionReportedSignal,
        )
    }

    /**
     * Sends an informational signal to Godot.
     * This can be used for debugging or providing context for other signals.
     * @param returnDict A Dictionary containing the information to be sent.
     */
    private fun sendInfoSignal(returnDict: Dictionary) {
        returnDict["plugin_name"] = pluginName
        emitSignal(billingInfoSignal.name, returnDict)
    }

    /**
     * Safely gets the current Activity, returning null if it's not available.
     * Sends an info signal to Godot if the activity is not available.
     * @param returnDict A Dictionary to which diagnostic information will be added.
     * @return The current Activity, or null if it's not available.
     */
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

    /**
     * Checks if the BillingClient is initialized and ready for use.
     * @return `true` if the BillingClient is ready, `false` otherwise.
     */
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

    /**
     * A simple function to check if the plugin is loaded and responding.
     * It shows a Toast message on the Android device.
     * @param says The message to be displayed in the Toast.
     */
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

    /**
     * Initializes the BillingClient and starts a connection to the Google Play Billing service.
     * This must be called before any other billing operations can be performed.
     */
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
                .enablePendingPurchases(
                    PendingPurchasesParams.newBuilder()
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

    /**
     * Ends the connection to the Google Play Billing service.
     * This should be called when the plugin is no longer needed to release resources.
     */
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
        // Try to restart the connection on the next request to
        // Google Play by calling the startConnection() method.
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

    /**
     * Queries for active purchases of a given type.
     * This is useful for restoring purchases when the app starts.
     * @param productType The type of product to query for (e.g., "inapp" or "subs"). Defaults to "inapp".
     */
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

    /**
     * Queries for details of a list of products.
     * @param listOfProductsIDs An array of product ID strings.
     * @param productType The type of products to query (e.g., "inapp" or "subs"). Defaults to "inapp".
     */
    @UsedByGodot
    fun queryProductDetails(
        listOfProductsIDs: Array<String>, productType: String = ProductType.INAPP) {
        if (!isReady) {
            Log.e(pluginName, "Billing client is not ready. Cannot query product details.")
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

        billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->
            val returnDict = Dictionary()
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.i(pluginName, "Product details found")
                returnDict["response_code"] = billingResult.responseCode
                returnDict["product_details_list"] = IAPP_utils.convertProductDetailsListToArray(productDetailsList)
                emitSignal(queryProductDetailsSignal.name, returnDict)
            } else {
                Log.i(pluginName, "No product details found or an error occurred.")
                returnDict["response_code"] = billingResult.responseCode
                returnDict["debug_message"] = billingResult.debugMessage
                emitSignal(queryProductDetailsErrorSignal.name, returnDict)
            }
        }
    }

    /**
     * Initiates the purchase flow for a one-time product.
     * @param listOfProductsIDs An array containing the ID of the product to purchase. Only the first ID is used.
     * @param isOfferPersonalized A boolean indicating if the offer is personalized.
     */
    @UsedByGodot
    fun purchase(listOfProductsIDs: Array<String>,
                         isOfferPersonalized: Boolean) {

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

    /**
     * Initiates the purchase flow for a subscription.
     * @param listOfProductsIDs An array containing the ID of the subscription product to purchase. Only the first ID is used.
     * @param basePlanIDs An array containing the ID of the base plan. Only the first ID is used.
     * @param isOfferPersonalized A boolean indicating if the offer is personalized.
     */
    @UsedByGodot
    fun subscribe(listOfProductsIDs: Array<String>,
                          basePlanIDs: Array<String>,
                          isOfferPersonalized: Boolean) {

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

    private fun launchPurchaseFlow(activity: Activity, productID: String, productType: String, basePlanID: String?, isOfferPersonalized: Boolean) {
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

        billingClient.queryProductDetailsAsync(queryProductDetailsParams) { queryDetailsResult, productDetailsList ->
            val returnDict = Dictionary()
            if (queryDetailsResult.responseCode != BillingClient.BillingResponseCode.OK || productDetailsList.isNullOrEmpty()) {
                Log.e(pluginName, "Error getting product details for $productID")
                returnDict["response_code"] = queryDetailsResult.responseCode
                returnDict["debug_message"] = queryDetailsResult.debugMessage
                emitSignal(queryProductDetailsErrorSignal.name, returnDict)
                return@queryProductDetailsAsync
            }

            val productDetails = productDetailsList[0]
            val productDetailsParamsList = mutableListOf<BillingFlowParams.ProductDetailsParams>()

            val builder = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(productDetails)

            if (productType == ProductType.SUBS) {
                val offerDetails = productDetails.subscriptionOfferDetails?.firstOrNull { it.basePlanId == basePlanID }
                if (offerDetails != null) {
                    builder.setOfferToken(offerDetails.offerToken)
                } else {
                    Log.e(pluginName, "Base Plan ID $basePlanID not found in $productID subscription")
                    returnDict["debug_message"] = "Base Plan ID $basePlanID not found in $productID subscription"
                    emitSignal(purchaseErrorSignal.name, returnDict)
                    return@queryProductDetailsAsync
                }
            }

            productDetailsParamsList.add(builder.build())

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .setIsOfferPersonalized(isOfferPersonalized)
                .build()

            val purchasingResult = billingClient.launchBillingFlow(activity, flowParams)
            if (purchasingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.e(pluginName, "$productID purchasing failed")
                returnDict["response_code"] = purchasingResult.responseCode
                returnDict["debug_message"] = purchasingResult.debugMessage
                returnDict["product_id"] = productID
                if (basePlanID != null) returnDict["base_plan_id"] = basePlanID
                emitSignal(purchaseErrorSignal.name, returnDict)
            } else {
                 Log.i(pluginName, "Product $productID purchasing launched successfully")
            }
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

    /**
     * Consumes a one-time purchase.
     * Consuming a purchase makes it available to be purchased again.
     * @param purchaseToken The token of the purchase to consume.
     */
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

    /**
     * Acknowledges a purchase.
     * All purchases must be acknowledged within three days. Failure to acknowledge a purchase will result in the purchase being refunded.
     * @param purchaseToken The token of the purchase to acknowledge.
     */
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

    /**
     * Shows an in-app message to the user.
     * This can be used to, for example, ask the user to update their payment method.
     * This is a stub function and is not yet implemented.
     */
    @UsedByGodot
    fun showInAppMessages() {
        // This is a stub function.
        // The implementation would involve calling the showInAppMessages API.
        // See https://developer.android.com/google/play/billing/features/in-app-messaging
        Log.w(pluginName, "showInAppMessages is not yet implemented.")
        val returnDict = Dictionary()
        returnDict["status"] = "not_implemented"
        returnDict["fun_name"] = "showInAppMessages"
        returnDict["debug_message"] = "showInAppMessages is not yet implemented."
        emitSignal(inAppMessageResultSignal.name, returnDict)
        sendInfoSignal(returnDict)
    }

    /**
     * Launches the price change confirmation flow.
     * This flow is used to ask the user to agree to a new price for a subscription.
     * This is a stub function and is not yet implemented.
     * @param productDetails The product details of the subscription with the pending price change.
     */
    @UsedByGodot
    fun launchPriceChangeConfirmationFlow(productDetails: Dictionary) {
        // This is a stub function.
        // The implementation would involve creating a PriceChangeFlowParams object and
        // calling billingClient.launchPriceChangeConfirmationFlow.
        // See https://developer.android.com/google/play/billing/subscriptions#price-change
        Log.w(pluginName, "launchPriceChangeConfirmationFlow is not yet implemented.")
        val returnDict = Dictionary()
        returnDict["status"] = "not_implemented"
        returnDict["fun_name"] = "launchPriceChangeConfirmationFlow"
        returnDict["debug_message"] = "launchPriceChangeConfirmationFlow is not yet implemented."
        returnDict["see_details"] = "https://developer.android.com/google/play/billing/subscriptions#price-change"
        emitSignal(priceChangeErrorSignal.name, returnDict)
        sendInfoSignal(returnDict)
    }

    /**
     * Creates a reporting details object for an alternative billing transaction.
     * This is a stub function and is not yet implemented.
     * @return A Dictionary containing the reporting details.
     */
    @UsedByGodot
    fun createAlternativeBillingOnlyReportingDetails() {
        // This is a stub function.
        // The implementation would involve calling the createAlternativeBillingOnlyReportingDetails API.
        // See https://developer.android.com/google/play/billing/alternative
        Log.w(pluginName, "createAlternativeBillingOnlyReportingDetails is not yet implemented.")
        val returnDict = Dictionary()
        returnDict["status"] = "not_implemented"
        returnDict["fun_name"] = "createAlternativeBillingOnlyReportingDetails"
        returnDict["debug_message"] = "createAlternativeBillingOnlyReportingDetails is not yet implemented."
        returnDict["see_details"] = "https://developer.android.com/google/play/billing/alternative"
        // TODO: Implement own signal
        sendInfoSignal(returnDict)
    }

    /**
     * Reports an alternative billing only transaction to Google Play.
     * This is a stub function and is not yet implemented.
     * @param reportingDetails The reporting details for the transaction.
     */
    @UsedByGodot
    fun reportAlternativeBillingOnlyTransaction(reportingDetails: Dictionary) {
        // This is a stub function.
        // The implementation would involve calling the reportAlternativeBillingOnlyTransaction API.
        // See https://developer.android.com/google/play/billing/alternative
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
