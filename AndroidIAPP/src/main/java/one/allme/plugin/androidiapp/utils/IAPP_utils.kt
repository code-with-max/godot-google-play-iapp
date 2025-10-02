package one.allme.plugin.androidiapp.utils

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsResult
import org.godotengine.godot.Dictionary

/**
 * A utility object for converting Google Play Billing Library objects to Godot Dictionaries.
 */
object IAPP_utils {

    fun convertPurchasesListToArray(purchasesList: List<Purchase>): Array<Any> {
        return purchasesList.map { convertPurchaseToDictionary(it) }.toTypedArray()
    }

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

    fun convertQueryProductDetailsResultToDictionary(queryProductDetailsResult: QueryProductDetailsResult): Dictionary {
        return Dictionary().apply {
            put(
                "product_details_list",
                convertProductDetailsListToArray(queryProductDetailsResult.productDetailsList)
            )
            put(
                "unfetched_product_list",
                convertUnfetchedProductListToArray(queryProductDetailsResult.unfetchedProductList)
            )
        }
    }

    private fun convertProductDetailsListToArray(productDetailsList: List<ProductDetails>?): Array<Any> {
        return productDetailsList?.map { convertProductDetailsToDictionary(it) }?.toTypedArray()
            ?: emptyArray()
    }

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

    private fun convertPurchaseOfferToDict(offerDetails: ProductDetails.OneTimePurchaseOfferDetails?): Dictionary {
        return Dictionary().apply {
            offerDetails?.let {
                put("formatted_price", it.formattedPrice)
                put("price_currency_code", it.priceCurrencyCode)
                put("price_amount_micros", it.priceAmountMicros)
            }
        }
    }

    private fun convertSubscriptionsDetailsListToArray(subscriptionsOffersList: List<ProductDetails.SubscriptionOfferDetails>?): Array<Any>? {
        return subscriptionsOffersList?.map { convertSubscriptionDetailsToDictionary(it) }?.toTypedArray()
    }

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

    private fun convertPricingPhasesListToArray(phasesList: List<ProductDetails.PricingPhase>?): Array<Any>? {
        return phasesList?.map { convertPricingPhaseToDictionary(it) }?.toTypedArray()
    }

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

    private fun convertInstallmentPlanDetailsToDictionary(planDetails: ProductDetails.InstallmentPlanDetails?): Dictionary {
        return Dictionary().apply {
            planDetails?.let {
                put("installment_plan_commitment_payments_count", it.installmentPlanCommitmentPaymentsCount)
                put("subsequent_installment_plan_commitment_payments_count", it.subsequentInstallmentPlanCommitmentPaymentsCount)
            }
        }
    }

    private fun convertUnfetchedProductListToArray(unfetchedProductList: List<QueryProductDetailsResult.UnfetchedProduct>?): Array<Any> {
        return unfetchedProductList?.map { convertUnfetchedProductToDictionary(it) }?.toTypedArray()
            ?: emptyArray()
    }

    private fun convertUnfetchedProductToDictionary(unfetchedProduct: QueryProductDetailsResult.UnfetchedProduct): Dictionary {
        return Dictionary().apply {
            put("product_id", unfetchedProduct.productId)
            put("reason", unfetchedProduct.reason)
        }
    }
}