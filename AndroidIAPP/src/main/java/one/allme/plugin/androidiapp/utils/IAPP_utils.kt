package one.allme.plugin.androidiapp.utils

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.UnfetchedProduct
import org.godotengine.godot.Dictionary

/**
 * A utility object for converting Google Play Billing Library objects to Godot Dictionaries.
 */
object IAPP_utils {

    fun convertPurchasesListToArray(purchasesList: List<Purchase>?): Array<Any> {
        return purchasesList?.map { convertPurchaseToDictionary(it) }?.toTypedArray() ?: emptyArray()
    }

    private fun convertPurchaseToDictionary(purchase: Purchase): Dictionary {
        return Dictionary().apply {
            val accountIdentifiers = Dictionary()
            val ai = purchase.accountIdentifiers
            accountIdentifiers["obfuscated_account_id"] = ai?.obfuscatedAccountId ?: ""
            accountIdentifiers["obfuscated_profile_id"] = ai?.obfuscatedProfileId ?: ""
            put("account_identifiers", accountIdentifiers)

            put("developer_payload", purchase.developerPayload ?: "")
            put("order_id", purchase.orderId ?: "")
            put("original_json", purchase.originalJson ?: "")
            put("package_name", purchase.packageName ?: "")
            put("products", purchase.products.toTypedArray())
            put("purchase_state", purchase.purchaseState)
            put("purchase_time", purchase.purchaseTime)
            put("purchase_token", purchase.purchaseToken ?: "")
            put("quantity", purchase.quantity)
            put("signature", purchase.signature ?: "")
            put("is_acknowledged", purchase.isAcknowledged)
            put("is_auto_renewing", purchase.isAutoRenewing)
            put("is_suspended", purchase.isSuspended)
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

    fun convertProductDetailsListToArray(productDetailsList: List<ProductDetails>?): Array<Any> {
        return productDetailsList?.map { convertProductDetailsToDictionary(it) }?.toTypedArray()
            ?: emptyArray()
    }

    private fun convertProductDetailsToDictionary(productsDetails: ProductDetails): Dictionary {
        return Dictionary().apply {
            put("description", productsDetails.description ?: "")
            put("name", productsDetails.name ?: "")
            put("product_id", productsDetails.productId ?: "")
            put("product_type", productsDetails.productType ?: "")
            put("title", productsDetails.title ?: "")
            put("hash_code", productsDetails.hashCode())
            put("to_string", productsDetails.toString() ?: "")
            
            if (productsDetails.productType == BillingClient.ProductType.INAPP) {
                // Deprecated in Billing 7.0+, replaced by oneTimePurchaseOfferDetailsList
                // For backward compatibility, we can still populate it with the first item if available
                val offerList = productsDetails.oneTimePurchaseOfferDetailsList
                put("one_time_purchase_offer_details", convertPurchaseOfferToDict(offerList?.firstOrNull()))
                put("one_time_purchase_offer_details_list", convertOneTimePurchaseOfferListToArray(offerList))
            } else if (productsDetails.productType == BillingClient.ProductType.SUBS) {
                put("subscription_offer_details", convertSubscriptionsDetailsListToArray(productsDetails.subscriptionOfferDetails))
            }
        }
    }

    private fun convertUnfetchedProductListToArray(unfetchedProductList: List<UnfetchedProduct>?): Array<Any> {
        return unfetchedProductList?.map { convertUnfetchedProductToDictionary(it) }?.toTypedArray()
            ?: emptyArray()
    }

    private fun convertUnfetchedProductToDictionary(unfetchedProduct: UnfetchedProduct): Dictionary {
        return Dictionary().apply {
            put("product_id", unfetchedProduct.productId ?: "")
            put("status_code", unfetchedProduct.statusCode)
        }
    }

    private fun convertOneTimePurchaseOfferListToArray(offerDetailsList: List<ProductDetails.OneTimePurchaseOfferDetails>?): Array<Any> {
        return offerDetailsList?.map { convertPurchaseOfferToDict(it) }?.toTypedArray() ?: emptyArray()
    }

    private fun convertPurchaseOfferToDict(offerDetails: ProductDetails.OneTimePurchaseOfferDetails?): Dictionary {
        return Dictionary().apply {
            put("formatted_price", offerDetails?.formattedPrice ?: "")
            put("price_currency_code", offerDetails?.priceCurrencyCode ?: "")
            put("price_amount_micros", offerDetails?.priceAmountMicros ?: 0L)
            put("offer_token", offerDetails?.offerToken ?: "")
            // offerId is the correct property name for OneTimePurchaseOfferDetails
            put("offer_id_token", offerDetails?.offerId ?: "")
        }
    }

    private fun convertSubscriptionsDetailsListToArray(subscriptionsOffersList: List<ProductDetails.SubscriptionOfferDetails>?): Array<Any> {
        return subscriptionsOffersList?.map { convertSubscriptionDetailsToDictionary(it) }
            ?.toTypedArray() ?: emptyArray()
    }

    private fun convertSubscriptionDetailsToDictionary(offerDetails: ProductDetails.SubscriptionOfferDetails): Dictionary {
        return Dictionary().apply {
            put("base_plan_id", offerDetails.basePlanId ?: "")
            put("installment_plan_details", convertInstallmentPlanDetailsToDictionary(offerDetails.installmentPlanDetails))
            put("offer_id", offerDetails.offerId ?: "")
            put("offer_tags", offerDetails.offerTags.toTypedArray())
            put("offer_token", offerDetails.offerToken ?: "")
            put("pricing_phases", convertPricingPhasesListToArray(offerDetails.pricingPhases.pricingPhaseList))
        }
    }

    private fun convertPricingPhasesListToArray(phasesList: List<ProductDetails.PricingPhase>?): Array<Any> {
        return phasesList?.map { convertPricingPhaseToDictionary(it) }?.toTypedArray()
            ?: emptyArray()
    }

    private fun convertPricingPhaseToDictionary(phase: ProductDetails.PricingPhase): Dictionary {
        return Dictionary().apply {
            put("billing_cycle_count", phase.billingCycleCount)
            put("billing_period", phase.billingPeriod ?: "")
            put("formatted_price", phase.formattedPrice ?: "")
            put("price_amount_micros", phase.priceAmountMicros)
            put("price_currency_code", phase.priceCurrencyCode ?: "")
            put("recurrence_mode", phase.recurrenceMode)
        }
    }

    private fun convertInstallmentPlanDetailsToDictionary(planDetails: ProductDetails.InstallmentPlanDetails?): Dictionary {
        return Dictionary().apply {
            put("installment_plan_commitment_payments_count", planDetails?.installmentPlanCommitmentPaymentsCount ?: 0)
            put("subsequent_installment_plan_commitment_payments_count", planDetails?.subsequentInstallmentPlanCommitmentPaymentsCount ?: 0)
        }
    }
}
