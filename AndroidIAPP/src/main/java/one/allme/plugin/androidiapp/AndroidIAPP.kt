package one.allme.plugin.androidiapp

import one.allme.plugin.androidiapp.utils.IappUtils
import android.app.Activity
import android.net.Uri
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
import com.android.billingclient.api.BillingProgramInformationDialogParams
import com.android.billingclient.api.BillingProgramReportingDetailsParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.GetBillingChoiceInfoParams
import com.android.billingclient.api.GetBillingConfigParams
import com.android.billingclient.api.InAppMessageParams
import com.android.billingclient.api.LaunchExternalLinkParams
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

    // New signals for Billing Library v9 methods
    private val billingConfigResponseSignal = SignalInfo("billing_config_response", Dictionary::class.java)
    private val alternativeBillingOnlyAvailabilitySignal = SignalInfo("alternative_billing_only_availability_response", Dictionary::class.java)
    private val alternativeBillingOnlyReportingDetailsSignal = SignalInfo("alternative_billing_only_reporting_details_response", Dictionary::class.java)
    private val alternativeBillingOnlyInformationDialogSignal = SignalInfo("alternative_billing_only_information_dialog_response", Dictionary::class.java)
    private val externalOfferAvailabilitySignal = SignalInfo("external_offer_availability_response", Dictionary::class.java)
    private val externalOfferReportingDetailsSignal = SignalInfo("external_offer_reporting_details_response", Dictionary::class.java)
    private val externalOfferInformationDialogSignal = SignalInfo("external_offer_information_dialog_response", Dictionary::class.java)
    private val billingProgramAvailabilitySignal = SignalInfo("billing_program_availability_response", Dictionary::class.java)
    private val billingProgramReportingDetailsSignal = SignalInfo("billing_program_reporting_details_response", Dictionary::class.java)
    private val billingProgramInformationDialogSignal = SignalInfo("billing_program_information_dialog_response", Dictionary::class.java)
    private val billingChoiceInfoSignal = SignalInfo("billing_choice_info_response", Dictionary::class.java)
    private val launchExternalLinkSignal = SignalInfo("launch_external_link_response", Dictionary::class.java)

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
            priceChangeErrorSignal, inAppMessageResultSignal, alternativeBillingOnlyTransactionReportedSignal,
            billingConfigResponseSignal, alternativeBillingOnlyAvailabilitySignal,
            alternativeBillingOnlyReportingDetailsSignal, alternativeBillingOnlyInformationDialogSignal,
            externalOfferAvailabilitySignal, externalOfferReportingDetailsSignal,
            externalOfferInformationDialogSignal, billingProgramAvailabilitySignal,
            billingProgramReportingDetailsSignal, billingProgramInformationDialogSignal,
            billingChoiceInfoSignal, launchExternalLinkSignal
        )
    }

    private fun sendInfoSignal(returnDict: Dictionary) {
        returnDict["plugin_name"] = pluginName
        emitSignal(billingInfoSignal.name, returnDict)
    }

    private fun BillingResult.toDictionary(): Dictionary {
        val dict = Dictionary()
        dict["response_code"] = responseCode
        dict["debug_message"] = debugMessage
        dict["sub_response_code"] = onPurchasesUpdatedSubResponseCode
        return dict
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

    /**
     * Проверка готовности BillingClient.
     * Используется в Godot: if billing.isReady(): ...
     */
    @UsedByGodot
    fun isReady(): Boolean {
        return if (::billingClient.isInitialized) {
            val readyState = billingClient.isReady
            Log.d(pluginName, "BILLING: isReady check: $readyState")
            readyState
        } else {
            Log.w(pluginName, "BILLING: isReady called but billingClient not initialized")
            false
        }
    }

    @UsedByGodot
    fun getConnectionState(): Int {
        return if (::billingClient.isInitialized) {
            billingClient.connectionState
        } else {
            BillingClient.ConnectionState.DISCONNECTED
        }
    }

    @UsedByGodot
    fun isFeatureSupported(feature: String): Dictionary {
        return if (::billingClient.isInitialized) {
            billingClient.isFeatureSupported(feature).toDictionary()
        } else {
            Dictionary().apply {
                put("response_code", BillingClient.BillingResponseCode.ERROR)
                put("debug_message", "BillingClient is not initialized")
            }
        }
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
    fun getBillingConfig() {
        if (!isReady()) {
            Log.e(pluginName, "Billing client is not ready. Cannot get billing config.")
            val returnDict = Dictionary().apply {
                put("response_code", BillingClient.BillingResponseCode.ERROR)
                put("debug_message", "Billing client is not ready")
            }
            emitSignal(billingConfigResponseSignal.name, returnDict)
            return
        }
        val params = GetBillingConfigParams.newBuilder().build()
        billingClient.getBillingConfigAsync(params) { billingResult, billingConfig ->
            val returnDict = billingResult.toDictionary()
            if (billingConfig != null) {
                returnDict["country_code"] = billingConfig.countryCode
            }
            emitSignal(billingConfigResponseSignal.name, returnDict)
        }
    }

    @UsedByGodot
    fun queryPurchases(productType: String = ProductType.INAPP, includeSuspended: Boolean = false) {
        if (!isReady()) {
            Log.e(pluginName, "Billing client is not ready. Cannot query purchases.")
            return
        }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(productType)
            .includeSuspendedSubscriptions(includeSuspended)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchaseList ->
            val returnDict = billingResult.toDictionary()
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.i(pluginName, "Purchases found")
                returnDict["purchases_list"] = IappUtils.convertPurchasesListToArray(purchaseList)
                emitSignal(queryPurchasesSignal.name, returnDict)
            } else {
                Log.i(pluginName, "No purchase found or an error occurred.")
                returnDict["purchases_list"] = null
                emitSignal(queryPurchasesErrorSignal.name, returnDict)
            }
        }
    }

    @UsedByGodot
    fun queryProductDetails(listOfProductsIDs: Array<String>, productType: String = ProductType.INAPP) {
        if (!isReady()) {
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
            val returnDict = IappUtils.convertQueryProductDetailsResultToDictionary(queryProductDetailsResult)
            returnDict.putAll(billingResult.toDictionary())

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
    fun purchase(listOfProductsIDs: Array<String>, isOfferPersonalized: Boolean, offerToken: String = "") {
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
        Log.i(pluginName, "Starting purchase flow for $productID product. offerToken: $offerToken")
        launchPurchaseFlow(activity, productID, ProductType.INAPP, null, null, isOfferPersonalized, manualOfferToken = offerToken)
    }

    @UsedByGodot
    fun subscribe(
        listOfProductsIDs: Array<String>,
        basePlanIDs: Array<String>,
        offerIDs: Array<String>,
        isOfferPersonalized: Boolean
    ) {
        val returnDict = Dictionary()
        val activity = requireActivityForPurchase(returnDict) ?: return

        val productID = listOfProductsIDs.firstOrNull()
        val basePlanID = basePlanIDs.firstOrNull()
        val offerID = offerIDs.firstOrNull()

        if (productID.isNullOrBlank() || basePlanID.isNullOrBlank()) {
            Log.e(pluginName, "Product ID or Base Plan ID is missing.")
            returnDict["debug_message"] = "Product ID or Base Plan ID is missing."
            emitSignal(purchaseErrorSignal.name, returnDict)
            return
        }

        Log.i(pluginName, "Starting purchase flow for $productID subscription with base plan $basePlanID")
        launchPurchaseFlow(activity, productID, ProductType.SUBS, basePlanID, offerID, isOfferPersonalized)
    }

    @UsedByGodot
    fun updateSubscription(
        listOfProductsIDs: Array<String>,
        basePlanIDs: Array<String>,
        offerIDs: Array<String>,
        isOfferPersonalized: Boolean,
        oldPurchaseToken: String,
        oldProductID: String,
        replacementMode: Int
    ) {
        val returnDict = Dictionary()
        val activity = requireActivityForPurchase(returnDict) ?: return

        val productID = listOfProductsIDs.firstOrNull()
        val basePlanID = basePlanIDs.firstOrNull()
        val offerID = offerIDs.firstOrNull()

        if (productID.isNullOrBlank() || basePlanID.isNullOrBlank() || oldPurchaseToken.isBlank() || oldProductID.isBlank()) {
            Log.e(pluginName, "Product ID, Base Plan ID, Old Purchase Token, or Old Product ID is missing.")
            returnDict["debug_message"] = "Product ID, Base Plan ID, Old Purchase Token, or Old Product ID is missing."
            emitSignal(purchaseErrorSignal.name, returnDict)
            return
        }

        Log.i(pluginName, "Starting subscription update flow for $productID with base plan $basePlanID")
        launchPurchaseFlow(activity, productID, ProductType.SUBS, basePlanID, offerID, isOfferPersonalized, oldPurchaseToken, oldProductID, replacementMode)
    }

    private fun launchPurchaseFlow(
        activity: Activity,
        productID: String,
        productType: String,
        basePlanID: String? = null,
        offerID: String? = null,
        isOfferPersonalized: Boolean = false,
        oldPurchaseToken: String? = null,
        oldProductID: String? = null,
        replacementMode: Int = BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.UNKNOWN_REPLACEMENT_MODE,
        manualOfferToken: String? = null
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

        var selectedOfferToken: String? = null

        if (!manualOfferToken.isNullOrEmpty()) {
            selectedOfferToken = manualOfferToken
            Log.i(pluginName, "Using manual offerToken: $selectedOfferToken")
        } else if (productType == BillingClient.ProductType.INAPP) {
            val offerList = productDetails.oneTimePurchaseOfferDetailsList
            if (offerList.isNullOrEmpty()) {
                Log.w(pluginName, "oneTimePurchaseOfferDetailsList is empty for $productID")
            } else {
                selectedOfferToken = offerList[0].offerToken
                Log.i(pluginName, "Using auto-selected offerToken for INAPP: $selectedOfferToken")
            }
        } else if (productType == BillingClient.ProductType.SUBS) {
            val offerDetails = if (offerID == null) {
                productDetails.subscriptionOfferDetails?.firstOrNull { it.basePlanId == basePlanID }
            } else {
                productDetails.subscriptionOfferDetails?.firstOrNull { it.basePlanId == basePlanID && it.offerId == offerID }
            }

            if (offerDetails != null) {
                selectedOfferToken = offerDetails.offerToken
                Log.i(pluginName, "Using selected offerToken for SUBS (basePlan: $basePlanID, offer: $offerID): $selectedOfferToken")
            } else {
                val errorMessage = if (offerID == null) {
                    "Base Plan ID $basePlanID not found in $productID subscription"
                } else {
                    "Offer ID $offerID with Base Plan ID $basePlanID not found in $productID subscription"
                }
                Log.e(pluginName, errorMessage)
                returnDict["response_code"] = BillingClient.BillingResponseCode.DEVELOPER_ERROR
                returnDict["debug_message"] = errorMessage
                emitSignal(purchaseErrorSignal.name, returnDict)
                return
            }
        }

        if (selectedOfferToken != null) {
            builder.setOfferToken(selectedOfferToken)
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

        if (oldPurchaseToken != null) {
            val updateParamsBuilder = BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                .setOldPurchaseToken(oldPurchaseToken)

            if (oldProductID != null && replacementMode != BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.UNKNOWN_REPLACEMENT_MODE) {
                val replacementParams = BillingFlowParams.ProductDetailsParams.SubscriptionProductReplacementParams.newBuilder()
                    .setOldProductId(oldProductID)
                    .setReplacementMode(replacementMode)
                    .build()
                builder.setSubscriptionProductReplacementParams(replacementParams)
            } else if (replacementMode != BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.UNKNOWN_REPLACEMENT_MODE) {
                @Suppress("DEPRECATION")
                updateParamsBuilder.setSubscriptionReplacementMode(replacementMode)
            }
            flowParamsBuilder.setSubscriptionUpdateParams(updateParamsBuilder.build())
        }

        val purchasingResult = billingClient.launchBillingFlow(activity, flowParamsBuilder.build())
        returnDict.putAll(purchasingResult.toDictionary())

        if (purchasingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(pluginName, "$productID purchasing failed: ${purchasingResult.debugMessage}")
            emitSignal(purchaseErrorSignal.name, returnDict)
        } else {
            Log.i(pluginName, "Product $productID purchasing launched successfully with offerToken: $selectedOfferToken")
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        val returnDict = billingResult.toDictionary()
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    Log.i(pluginName, "Purchases updated successfully")
                    returnDict["purchases_list"] = IappUtils.convertPurchasesListToArray(purchases)
                    emitSignal(purchaseUpdatedSignal.name, returnDict)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.i(pluginName, "User canceled purchase updating")
                emitSignal(purchaseCancelledSignal.name, returnDict)
            }
            else -> {
                Log.i(pluginName, "Error purchase updating, response code: ${billingResult.responseCode}")
                emitSignal(purchaseUpdatedErrorSignal.name, returnDict)
            }
        }
    }

    @UsedByGodot
    fun consumePurchase(purchaseToken: String) {
        if (!isReady()) {
            Log.e(pluginName, "Billing client is not ready. Cannot consume purchase.")
            return
        }
        val consumeParams = ConsumeParams.newBuilder().setPurchaseToken(purchaseToken).build()
        billingClient.consumeAsync(consumeParams) { billingResult, outToken ->
            val returnDict = billingResult.toDictionary()
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.i(pluginName, "Purchase consumed successfully: $outToken")
                returnDict["purchase_token"] = outToken
                emitSignal(purchaseConsumedSignal.name, returnDict)
            } else {
                Log.e(pluginName, "Error purchase consuming, response code: ${billingResult.responseCode}")
                returnDict["purchase_token"] = outToken
                emitSignal(purchaseConsumedErrorSignal.name, returnDict)
            }
        }
    }

    @UsedByGodot
    fun acknowledgePurchase(purchaseToken: String) {
        if (!isReady()) {
            Log.e(pluginName, "Billing client is not ready. Cannot acknowledge purchase.")
            return
        }
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchaseToken).build()
        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            val returnDict = billingResult.toDictionary()
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.i(pluginName, "Purchase acknowledged successfully: $purchaseToken")
                returnDict["purchase_token"] = purchaseToken
                emitSignal(purchaseAcknowledgedSignal.name, returnDict)
            } else {
                Log.e(pluginName, "Error purchase acknowledging, response code: ${billingResult.responseCode}")
                returnDict["purchase_token"] = purchaseToken
                emitSignal(purchaseAcknowledgedErrorSignal.name, returnDict)
            }
        }
    }

    @UsedByGodot
    fun showInAppMessages() {
        val returnDict = Dictionary()
        val activity = activity
        if (activity == null || !isReady()) {
            Log.e(pluginName, "Cannot show in-app messages: Activity is null or BillingClient is not ready.")
            returnDict["response_code"] = BillingClient.BillingResponseCode.ERROR
            returnDict["debug_message"] = "Activity is null or BillingClient is not ready"
            emitSignal(inAppMessageResultSignal.name, returnDict)
            return
        }
        val params = InAppMessageParams.newBuilder()
            .addAllInAppMessageCategoriesToShow()
            .build()
        val billingResult = billingClient.showInAppMessages(activity, params) { inAppMessageResult ->
            val resultDict = Dictionary()
            resultDict["response_code"] = inAppMessageResult.responseCode
            resultDict["purchase_token"] = inAppMessageResult.purchaseToken ?: ""
            emitSignal(inAppMessageResultSignal.name, resultDict)
        }
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            returnDict.putAll(billingResult.toDictionary())
            emitSignal(inAppMessageResultSignal.name, returnDict)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    @UsedByGodot
    fun launchPriceChangeConfirmationFlow(productDetails: Dictionary) {
        Log.w(pluginName, "launchPriceChangeConfirmationFlow was deprecated and removed in Billing Library 7+.")
        val returnDict = Dictionary()
        returnDict["status"] = "deprecated"
        returnDict["fun_name"] = "launchPriceChangeConfirmationFlow"
        returnDict["debug_message"] = "launchPriceChangeConfirmationFlow was deprecated and removed in Billing Library 7+."
        returnDict["see_details"] = "https://developer.android.com/google/play/billing/subscriptions#price-change"
        emitSignal(priceChangeErrorSignal.name, returnDict)
        sendInfoSignal(returnDict)
    }

    @UsedByGodot
    fun isAlternativeBillingOnlyAvailable() {
        if (!isReady()) {
            Log.e(pluginName, "Billing client is not ready.")
            val returnDict = Dictionary().apply {
                put("response_code", BillingClient.BillingResponseCode.ERROR)
                put("debug_message", "Billing client is not ready")
            }
            emitSignal(alternativeBillingOnlyAvailabilitySignal.name, returnDict)
            return
        }
        billingClient.isAlternativeBillingOnlyAvailableAsync { billingResult ->
            emitSignal(alternativeBillingOnlyAvailabilitySignal.name, billingResult.toDictionary())
        }
    }

    @UsedByGodot
    fun createAlternativeBillingOnlyReportingDetails() {
        if (!isReady()) {
            Log.e(pluginName, "Billing client is not ready.")
            val returnDict = Dictionary().apply {
                put("response_code", BillingClient.BillingResponseCode.ERROR)
                put("debug_message", "Billing client is not ready")
            }
            emitSignal(alternativeBillingOnlyReportingDetailsSignal.name, returnDict)
            return
        }
        billingClient.createAlternativeBillingOnlyReportingDetailsAsync { billingResult, reportingDetails ->
            val returnDict = billingResult.toDictionary()
            if (reportingDetails != null) {
                returnDict["external_transaction_token"] = reportingDetails.externalTransactionToken
            }
            emitSignal(alternativeBillingOnlyReportingDetailsSignal.name, returnDict)
        }
    }

    @UsedByGodot
    fun showAlternativeBillingOnlyInformationDialog() {
        val returnDict = Dictionary()
        val activity = activity
        if (activity == null || !isReady()) {
            Log.e(pluginName, "Cannot show alternative billing only dialog: Activity is null or BillingClient is not ready.")
            returnDict["response_code"] = BillingClient.BillingResponseCode.ERROR
            returnDict["debug_message"] = "Activity is null or BillingClient is not ready"
            emitSignal(alternativeBillingOnlyInformationDialogSignal.name, returnDict)
            return
        }
        val billingResult = billingClient.showAlternativeBillingOnlyInformationDialog(activity) { dialogResult ->
            emitSignal(alternativeBillingOnlyInformationDialogSignal.name, dialogResult.toDictionary())
        }
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            emitSignal(alternativeBillingOnlyInformationDialogSignal.name, billingResult.toDictionary())
        }
    }

    @Suppress("UNUSED_PARAMETER")
    @UsedByGodot
    fun reportAlternativeBillingOnlyTransaction(reportingDetails: Dictionary) {
        Log.w(pluginName, "reportAlternativeBillingOnlyTransaction was replaced in Billing Library 7+ by createAlternativeBillingOnlyReportingDetails.")
        val returnDict = Dictionary()
        returnDict["status"] = "deprecated"
        returnDict["fun_name"] = "reportAlternativeBillingOnlyTransaction"
        returnDict["debug_message"] = "reportAlternativeBillingOnlyTransaction was replaced in Billing Library 7+ by createAlternativeBillingOnlyReportingDetails."
        returnDict["see_details"] = "https://developer.android.com/google/play/billing/alternative"
        emitSignal(alternativeBillingOnlyTransactionReportedSignal.name, returnDict)
        sendInfoSignal(returnDict)
    }

    @Suppress("DEPRECATION")
    @UsedByGodot
    fun isExternalOfferAvailable() {
        if (!isReady()) {
            Log.e(pluginName, "Billing client is not ready.")
            val returnDict = Dictionary().apply {
                put("response_code", BillingClient.BillingResponseCode.ERROR)
                put("debug_message", "Billing client is not ready")
            }
            emitSignal(externalOfferAvailabilitySignal.name, returnDict)
            return
        }
        billingClient.isExternalOfferAvailableAsync { billingResult ->
            emitSignal(externalOfferAvailabilitySignal.name, billingResult.toDictionary())
        }
    }

    @Suppress("DEPRECATION")
    @UsedByGodot
    fun createExternalOfferReportingDetails() {
        if (!isReady()) {
            Log.e(pluginName, "Billing client is not ready.")
            val returnDict = Dictionary().apply {
                put("response_code", BillingClient.BillingResponseCode.ERROR)
                put("debug_message", "Billing client is not ready")
            }
            emitSignal(externalOfferReportingDetailsSignal.name, returnDict)
            return
        }
        billingClient.createExternalOfferReportingDetailsAsync { billingResult, reportingDetails ->
            val returnDict = billingResult.toDictionary()
            if (reportingDetails != null) {
                returnDict["external_transaction_token"] = reportingDetails.externalTransactionToken
            }
            emitSignal(externalOfferReportingDetailsSignal.name, returnDict)
        }
    }

    @Suppress("DEPRECATION")
    @UsedByGodot
    fun showExternalOfferInformationDialog() {
        val returnDict = Dictionary()
        val activity = activity
        if (activity == null || !isReady()) {
            Log.e(pluginName, "Cannot show external offer dialog: Activity is null or BillingClient is not ready.")
            returnDict["response_code"] = BillingClient.BillingResponseCode.ERROR
            returnDict["debug_message"] = "Activity is null or BillingClient is not ready"
            emitSignal(externalOfferInformationDialogSignal.name, returnDict)
            return
        }
        val billingResult = billingClient.showExternalOfferInformationDialog(activity) { dialogResult ->
            emitSignal(externalOfferInformationDialogSignal.name, dialogResult.toDictionary())
        }
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            emitSignal(externalOfferInformationDialogSignal.name, billingResult.toDictionary())
        }
    }

    @UsedByGodot
    fun isBillingProgramAvailable(programType: Int) {
        if (!isReady()) {
            Log.e(pluginName, "Billing client is not ready.")
            val returnDict = Dictionary().apply {
                put("response_code", BillingClient.BillingResponseCode.ERROR)
                put("debug_message", "Billing client is not ready")
            }
            emitSignal(billingProgramAvailabilitySignal.name, returnDict)
            return
        }
        billingClient.isBillingProgramAvailableAsync(programType) { billingResult, availabilityDetails ->
            val returnDict = billingResult.toDictionary()
            returnDict["billing_program"] = availabilityDetails?.billingProgram ?: programType
            emitSignal(billingProgramAvailabilitySignal.name, returnDict)
        }
    }

    @UsedByGodot
    fun createBillingProgramReportingDetails(programType: Int, developerBillingType: Int = 0) {
        if (!isReady()) {
            Log.e(pluginName, "Billing client is not ready.")
            val returnDict = Dictionary().apply {
                put("response_code", BillingClient.BillingResponseCode.ERROR)
                put("debug_message", "Billing client is not ready")
            }
            emitSignal(billingProgramReportingDetailsSignal.name, returnDict)
            return
        }
        val params = BillingProgramReportingDetailsParams.newBuilder()
            .setBillingProgram(programType)
            .setDeveloperBillingType(developerBillingType)
            .build()
        billingClient.createBillingProgramReportingDetailsAsync(params) { billingResult, reportingDetails ->
            val returnDict = billingResult.toDictionary()
            if (reportingDetails != null) {
                returnDict["external_transaction_token"] = reportingDetails.externalTransactionToken
                returnDict["billing_program"] = reportingDetails.billingProgram
            }
            emitSignal(billingProgramReportingDetailsSignal.name, returnDict)
        }
    }

    @UsedByGodot
    fun showBillingProgramInformationDialog(programType: Int, externalTransactionToken: String = "") {
        val returnDict = Dictionary()
        val activity = activity
        if (activity == null || !isReady()) {
            Log.e(pluginName, "Cannot show billing program dialog: Activity is null or BillingClient is not ready.")
            returnDict["response_code"] = BillingClient.BillingResponseCode.ERROR
            returnDict["debug_message"] = "Activity is null or BillingClient is not ready"
            emitSignal(billingProgramInformationDialogSignal.name, returnDict)
            return
        }
        val paramsBuilder = BillingProgramInformationDialogParams.newBuilder()
            .setBillingProgram(programType)
        if (externalTransactionToken.isNotEmpty()) {
            paramsBuilder.setExternalTransactionToken(externalTransactionToken)
        }
        billingClient.showBillingProgramInformationDialog(activity, paramsBuilder.build()) { dialogResult ->
            emitSignal(billingProgramInformationDialogSignal.name, dialogResult.toDictionary())
        }
    }

    @UsedByGodot
    fun getBillingChoiceInfo(programType: Int = 0) {
        if (!isReady()) {
            Log.e(pluginName, "Billing client is not ready.")
            val returnDict = Dictionary().apply {
                put("response_code", BillingClient.BillingResponseCode.ERROR)
                put("debug_message", "Billing client is not ready")
            }
            emitSignal(billingChoiceInfoSignal.name, returnDict)
            return
        }
        val params = GetBillingChoiceInfoParams.newBuilder()
            .setBillingProgram(programType)
            .build()
        billingClient.getBillingChoiceInfoAsync(params) { billingResult, choiceInfo ->
            val returnDict = billingResult.toDictionary()
            if (choiceInfo != null) {
                returnDict["play_billing_choice_image_url"] = choiceInfo.playBillingChoiceImageUrl
                returnDict["play_billing_loyalty_info"] = choiceInfo.playBillingLoyaltyInfo
            }
            emitSignal(billingChoiceInfoSignal.name, returnDict)
        }
    }

    @UsedByGodot
    fun launchExternalLink(
        linkUri: String,
        linkType: Int = 0,
        launchMode: Int = 0,
        programType: Int = 0,
        externalTransactionToken: String = ""
    ) {
        val returnDict = Dictionary()
        val activity = activity
        if (activity == null || !isReady()) {
            Log.e(pluginName, "Cannot launch external link: Activity is null or BillingClient is not ready.")
            returnDict["response_code"] = BillingClient.BillingResponseCode.ERROR
            returnDict["debug_message"] = "Activity is null or BillingClient is not ready"
            emitSignal(launchExternalLinkSignal.name, returnDict)
            return
        }
        try {
            val paramsBuilder = LaunchExternalLinkParams.newBuilder()
                .setLinkUri(Uri.parse(linkUri))
                .setLinkType(linkType)
                .setLaunchMode(launchMode)
                .setBillingProgram(programType)
            if (externalTransactionToken.isNotEmpty()) {
                paramsBuilder.setExternalTransactionToken(externalTransactionToken)
            }
            billingClient.launchExternalLink(activity, paramsBuilder.build()) { linkResult ->
                emitSignal(launchExternalLinkSignal.name, linkResult.toDictionary())
            }
        } catch (e: Exception) {
            Log.e(pluginName, "Error launching external link: ${e.message}", e)
            returnDict["response_code"] = BillingClient.BillingResponseCode.ERROR
            returnDict["debug_message"] = "Error launching external link: ${e.message}"
            emitSignal(launchExternalLinkSignal.name, returnDict)
        }
    }
}
