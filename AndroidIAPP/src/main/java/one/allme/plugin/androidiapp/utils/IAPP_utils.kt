package one.allme.plugin.androidiapp.utils

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import org.godotengine.godot.Dictionary

/**
 * A utility object for converting Google Play Billing Library objects to Godot Dictionaries.
 */
object IAPP_utils {

    /**
     * Converts a list of [Purchase] objects to a Godot Array of Dictionaries.
     * @param purchasesList The list of purchases to convert.
     * @return An Array of Dictionaries, where each Dictionary represents a purchase.
     */
    fun convertPurchasesListToArray(purchasesList: List<Purchase>): Array<Any> {
        return purchasesList.map { convertPurchaseToDictionary(it) }.toTypedArray()
    }

    /**
     * Converts a single [Purchase] object to a Godot Dictionary.
     * @param purchase The purchase object to convert.
     * @return A Dictionary representing the purchase.
     */
    private fun convertPurchaseToDictionary(purchase: Purchase): Dictionary {
        return Dictionary().apply {
            put("account_identifiers", purchase.accountIdentifiers)
            put("developer_payload", purchase.developerPayload)
            put("order_id", purchase.orderId)
            put("original_json", purchase.originalJson)
            put("package_name", purchase.packageName)
            put("pending_purchase_update", purchase.pendingPurchaseUpdate)
            put("products", purchase.products.toTypedArray())
            put("purchase_state", purchase.purchaseState)
            put("purchase_time", purchase.purchaseTime)
            put("purchase_token", purchase.purchaseToken)
            put("quantity", purchase.quantity)
            put("signature", purchase.signature)
            put("hash_code", purchase.hashCode())
            put("is_acknowledged", purchase.isAcknowledged)
            put("is_auto_renewing", purchase.isAutoRenewing)
            put("to_string", purchase.toString())
        }
    }

    /**
     * Converts a list of [ProductDetails] objects to a Godot Array of Dictionaries.
     * @param productDetailsList The list of product details to convert.
     * @return An Array of Dictionaries, where each Dictionary represents a product's details.
     */
    fun convertProductDetailsListToArray(productDetailsList: List<ProductDetails>): Array<Any> {
        return productDetailsList.map { convertProductDetailsToDictionary(it) }.toTypedArray()
    }

    /**
     * Converts a single [ProductDetails] object to a Godot Dictionary.
     * @param productsDetails The product details object to convert.
     * @return A Dictionary representing the product's details.
     */
    private fun convertProductDetailsToDictionary(productsDetails: ProductDetails): Dictionary {
        return Dictionary().apply {
            put("description", productsDetails.description)
            put("name", productsDetails.name)
            put("product_id", productsDetails.productId)
            put("product_type", productsDetails.productType)
            put("title", productsDetails.title)
            put("hash_code", productsDetails.hashCode())
            put("to_string", productsDetails.toString())
            if (productsDetails.productType == BillingClient.ProductType.INAPP) {
                put("one_time_purchase_offer_details", convertPurchaseOfferToDict(productsDetails.oneTimePurchaseOfferDetails))
            } else if (productsDetails.productType == BillingClient.ProductType.SUBS) {
                put("subscription_offer_details", convertSubscriptionsDetailsListToArray(productsDetails.subscriptionOfferDetails))
            }
        }
    }

    /**
     * Converts a [ProductDetails.OneTimePurchaseOfferDetails] object to a Godot Dictionary.
     * @param offerDetails The one-time purchase offer details to convert.
     * @return A Dictionary representing the one-time purchase offer details.
     */
    private fun convertPurchaseOfferToDict(offerDetails: ProductDetails.OneTimePurchaseOfferDetails?): Dictionary {
        return Dictionary().apply {
            offerDetails?.let {
                put("formatted_price", it.formattedPrice)
                put("price_currency_code", it.priceCurrencyCode)
                put("price_amount_micros", it.priceAmountMicros)
            }
        }
    }

    /**
     * Converts a list of [ProductDetails.SubscriptionOfferDetails] objects to a Godot Array of Dictionaries.
     * @param subscriptionsOffersList The list of subscription offer details to convert.
     * @return An Array of Dictionaries, where each Dictionary represents a subscription offer's details.
     */
    private fun convertSubscriptionsDetailsListToArray(subscriptionsOffersList: List<ProductDetails.SubscriptionOfferDetails>?): Array<Any>? {
        return subscriptionsOffersList?.map { convertSubscriptionDetailsToDictionary(it) }?.toTypedArray()
    }

    /**
     * Converts a single [ProductDetails.SubscriptionOfferDetails] object to a Godot Dictionary.
     * @param offerDetails The subscription offer details to convert.
     * @return A Dictionary representing the subscription offer's details.
     */
    private fun convertSubscriptionDetailsToDictionary(offerDetails: ProductDetails.SubscriptionOfferDetails): Dictionary {
        return Dictionary().apply {
            put("base_plan_id", offerDetails.basePlanId)
            put("installment_plan_details", convertInstallmentPlanDetailsToDictionary(offerDetails.installmentPlanDetails))
            put("offer_id", offerDetails.offerId)
            put("offer_tags", offerDetails.offerTags.toTypedArray())
            put("offer_token", offerDetails.offerToken)
            put("pricing_phases", convertPricingPhasesListToArray(offerDetails.pricingPhases.pricingPhaseList))
        }
    }

    /**
     * Converts a list of [ProductDetails.PricingPhase] objects to a Godot Array of Dictionaries.
     * @param phasesList The list of pricing phases to convert.
     * @return An Array of Dictionaries, where each Dictionary represents a pricing phase.
     */
    private fun convertPricingPhasesListToArray(phasesList: List<ProductDetails.PricingPhase>?): Array<Any>? {
        return phasesList?.map { convertPricingPhaseToDictionary(it) }?.toTypedArray()
    }

    /**
     * Converts a single [ProductDetails.PricingPhase] object to a Godot Dictionary.
     * @param phase The pricing phase to convert.
     * @return A Dictionary representing the pricing phase.
     */
    private fun convertPricingPhaseToDictionary(phase: ProductDetails.PricingPhase): Dictionary {
        return Dictionary().apply {
            put("billing_cycle_count", phase.billingCycleCount)
            put("billing_period", phase.billingPeriod)
            put("formatted_price", phase.formattedPrice)
            put("price_amount_micros", phase.priceAmountMicros)
            put("price_currency_code", phase.priceCurrencyCode)
            put("recurrence_mode", phase.recurrenceMode)
        }
    }

    /**
     * Converts a [ProductDetails.InstallmentPlanDetails] object to a Godot Dictionary.
     * @param planDetails The installment plan details to convert.
     * @return A Dictionary representing the installment plan details.
     */
    private fun convertInstallmentPlanDetailsToDictionary(planDetails: ProductDetails.InstallmentPlanDetails?): Dictionary {
        return Dictionary().apply {
            planDetails?.let {
                put("installment_plan_commitment_payments_count", it.installmentPlanCommitmentPaymentsCount)
                put("subsequent_installment_plan_commitment_payments_count", it.subsequentInstallmentPlanCommitmentPaymentsCount)
            }
        }
    }
}
