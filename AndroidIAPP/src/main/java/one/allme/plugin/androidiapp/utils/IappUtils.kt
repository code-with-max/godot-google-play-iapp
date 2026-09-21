package one.allme.plugin.androidiapp.utils

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.UnfetchedProduct
import org.godotengine.godot.Dictionary

@Deprecated("Use IappUtils instead", ReplaceWith("IappUtils"))
typealias IAPP_utils = IappUtils

/**
 * Utility object for converting Google Play Billing objects to Godot Dictionaries.
 */
object IappUtils {

    fun convertPurchasesListToDictionary(purchasesList: List<Purchase>?): Dictionary {
        return Dictionary().apply {
            if (purchasesList == null) {
                put("error", "Purchase list is null")
                put("purchases_list", emptyArray<Any>())
            } else {
                put("purchases_list", convertPurchasesListToArray(purchasesList))
            }
        }
    }

    fun convertPurchasesListToArray(purchasesList: List<Purchase>?): Array<Any> {
        return purchasesList?.map { convertPurchaseToDictionary(it) }?.toTypedArray() ?: emptyArray()
    }

    private fun convertPurchaseToDictionary(purchase: Purchase): Dictionary {
        return Dictionary().apply {
            val accountIdentifiers = Dictionary().apply {
                val ai = purchase.accountIdentifiers
                put("obfuscated_account_id", ai?.obfuscatedAccountId.orEmpty())
                put("obfuscated_profile_id", ai?.obfuscatedProfileId.orEmpty())
            }
            put("account_identifiers", accountIdentifiers)

            put("developer_payload", purchase.developerPayload.orEmpty())
            put("order_id", purchase.orderId.orEmpty())
            put("original_json", purchase.originalJson.orEmpty())
            put("package_name", purchase.packageName.orEmpty())
            put("products", purchase.products.toTypedArray())
            put("purchase_state", purchase.purchaseState)
            put("purchase_time", purchase.purchaseTime)
            put("purchase_token", purchase.purchaseToken.orEmpty())
            put("quantity", purchase.quantity)
            put("signature", purchase.signature.orEmpty())
            put("is_acknowledged", purchase.isAcknowledged)
            put("is_auto_renewing", purchase.isAutoRenewing)
            put("is_suspended", purchase.isSuspended)
        }
    }

    fun convertQueryProductDetailsResultToDictionary(result: QueryProductDetailsResult): Dictionary {
        return Dictionary().apply {
            put("product_details_list", convertProductDetailsListToArray(result.productDetailsList))
            put("unfetched_product_list", convertUnfetchedProductListToArray(result.unfetchedProductList))
        }
    }

    fun convertProductDetailsListToArray(productDetailsList: List<ProductDetails>?): Array<Any> {
        return productDetailsList?.map { convertProductDetailsToDictionary(it) }?.toTypedArray() ?: emptyArray()
    }

    private fun convertProductDetailsToDictionary(details: ProductDetails): Dictionary {
        return Dictionary().apply {
            put("description", details.description.orEmpty())
            put("name", details.name.orEmpty())
            put("product_id", details.productId.orEmpty())
            put("product_type", details.productType.orEmpty())
            put("title", details.title.orEmpty())
            put("hash_code", details.hashCode())
            put("to_string", details.toString())

            when (details.productType) {
                BillingClient.ProductType.INAPP -> {
                    val offerList = details.oneTimePurchaseOfferDetailsList
                    put("one_time_purchase_offer_details", convertPurchaseOfferToDict(offerList?.firstOrNull()))
                    put("one_time_purchase_offer_details_list", convertOneTimePurchaseOfferListToArray(offerList))
                }
                BillingClient.ProductType.SUBS -> {
                    put("subscription_offer_details", convertSubscriptionsDetailsListToArray(details.subscriptionOfferDetails))
                }
            }
        }
    }

    private fun convertUnfetchedProductListToArray(list: List<UnfetchedProduct>?): Array<Any> {
        return list?.map { convertUnfetchedProductToDictionary(it) }?.toTypedArray() ?: emptyArray()
    }

    private fun convertUnfetchedProductToDictionary(unfetched: UnfetchedProduct): Dictionary {
        return Dictionary().apply {
            put("product_id", unfetched.productId.orEmpty())
            put("status_code", unfetched.statusCode)
        }
    }

    private fun convertOneTimePurchaseOfferListToArray(list: List<ProductDetails.OneTimePurchaseOfferDetails>?): Array<Any> {
        return list?.map { convertPurchaseOfferToDict(it) }?.toTypedArray() ?: emptyArray()
    }

    private fun convertPurchaseOfferToDict(offer: ProductDetails.OneTimePurchaseOfferDetails?): Dictionary {
        return Dictionary().apply {
            put("formatted_price", offer?.formattedPrice.orEmpty())
            put("price_currency_code", offer?.priceCurrencyCode.orEmpty())
            put("price_amount_micros", offer?.priceAmountMicros ?: 0L)
            put("offer_token", offer?.offerToken.orEmpty())
            put("offer_id_token", offer?.offerId.orEmpty())
            put("offer_tags", offer?.offerTags?.toTypedArray() ?: emptyArray<String>())
        }
    }

    private fun convertSubscriptionsDetailsListToArray(list: List<ProductDetails.SubscriptionOfferDetails>?): Array<Any> {
        return list?.map { convertSubscriptionDetailsToDictionary(it) }?.toTypedArray() ?: emptyArray()
    }

    private fun convertSubscriptionDetailsToDictionary(offer: ProductDetails.SubscriptionOfferDetails): Dictionary {
        return Dictionary().apply {
            put("base_plan_id", offer.basePlanId.orEmpty())
            put("installment_plan_details", convertInstallmentPlanDetailsToDictionary(offer.installmentPlanDetails))
            put("offer_id", offer.offerId.orEmpty())
            put("offer_tags", offer.offerTags?.toTypedArray() ?: emptyArray<String>())
            put("offer_token", offer.offerToken.orEmpty())
            put("pricing_phases", convertPricingPhasesListToArray(offer.pricingPhases.pricingPhaseList))
        }
    }

    private fun convertPricingPhasesListToArray(list: List<ProductDetails.PricingPhase>?): Array<Any> {
        return list?.map { convertPricingPhaseToDictionary(it) }?.toTypedArray() ?: emptyArray()
    }

    private fun convertPricingPhaseToDictionary(phase: ProductDetails.PricingPhase): Dictionary {
        return Dictionary().apply {
            put("billing_cycle_count", phase.billingCycleCount)
            put("billing_period", phase.billingPeriod.orEmpty())
            put("formatted_price", phase.formattedPrice.orEmpty())
            put("price_amount_micros", phase.priceAmountMicros)
            put("price_currency_code", phase.priceCurrencyCode.orEmpty())
            put("recurrence_mode", phase.recurrenceMode)
        }
    }

    private fun convertInstallmentPlanDetailsToDictionary(plan: ProductDetails.InstallmentPlanDetails?): Dictionary {
        return Dictionary().apply {
            put("installment_plan_commitment_payments_count", plan?.installmentPlanCommitmentPaymentsCount ?: 0)
            put("subsequent_installment_plan_commitment_payments_count", plan?.subsequentInstallmentPlanCommitmentPaymentsCount ?: 0)
        }
    }
}
