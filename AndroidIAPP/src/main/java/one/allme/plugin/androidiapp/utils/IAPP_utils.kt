package one.allme.plugin.androidiapp.utils

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import org.godotengine.godot.Dictionary


object IAPP_utils {

    // Convert list of purchases to array
    // https://developer.android.com/reference/com/android/billingclient/api/PurchasesUpdatedListener
    fun convertPurchasesListToArray(purchasesList: List<Purchase>): Array<Any?> {
        val purchasesArray = arrayOfNulls<Any>(purchasesList.size)
        for (i in purchasesList.indices) {
            purchasesArray[i] = convertPurchaseToDictionary(purchasesList[i])
        }
        return purchasesArray
    }

    // https://developer.android.com/reference/com/android/billingclient/api/Purchase
    private fun convertPurchaseToDictionary(purchase: Purchase): Dictionary {
        val dictionary = Dictionary() // from Godot type Dictionary
//        dictionary["equals"] = purchase.equals() // TODO check this method
        dictionary["account_identifiers"] = purchase.accountIdentifiers
        dictionary["developer_payload"] = purchase.developerPayload
        dictionary["order_id"] = purchase.orderId
        dictionary["original_json"] = purchase.originalJson
        dictionary["package_name"] = purchase.packageName
        dictionary["pending_purchase_update"] = purchase.pendingPurchaseUpdate
        dictionary["products"] = convertPurchaseProductsIdsListToArray(purchase.products) // list of string
        dictionary["purchase_state"] = purchase.purchaseState
        dictionary["purchase_time"] = purchase.purchaseTime
        dictionary["purchase_token"] = purchase.purchaseToken
        dictionary["quantity"] = purchase.quantity
        dictionary["signature"] = purchase.signature
        dictionary["hash_code"] = purchase.hashCode()
        dictionary["is_acknowledged"] = purchase.isAcknowledged
        dictionary["is_auto_renewing"] = purchase.isAutoRenewing
        dictionary["to_string"] = purchase.toString()
        return dictionary
    }


    // Returns the product Ids.
    // Convert list of purchase products to array
    // https://developer.android.com/reference/com/android/billingclient/api/Purchase#getProducts()
    private fun convertPurchaseProductsIdsListToArray(purchaseProductsList: List<String>): Array<Any?> {
        val purchaseProductsArray = arrayOfNulls<Any>(purchaseProductsList.size)
        for (i in purchaseProductsList.indices) {
            purchaseProductsArray[i] = purchaseProductsList[i]
        }
        return purchaseProductsArray
    }


    // Product Details utils below
    // Convert list of detailed product details to array
    // Called from: queryProductDetails
    fun convertProductDetailsListToArray(productDetailsList: List<ProductDetails>): Array<Any?> {
        val productDetailsArray = arrayOfNulls<Any>(productDetailsList.size)
        for (i in productDetailsList.indices) {
            productDetailsArray[i] = convertProductDetailsToDictionary(productDetailsList[i])
        }
        return productDetailsArray
    }


    // Convert product details to dictionary
    // https://developer.android.com/reference/com/android/billingclient/api/ProductDetails
    private fun convertProductDetailsToDictionary(productsDetails: ProductDetails): Dictionary {
        val dictionary = Dictionary()  // from Godot type Dictionary
        dictionary["description"] = productsDetails.description
        dictionary["name"] = productsDetails.name
        dictionary["product_id"] = productsDetails.productId
        dictionary["product_type"] = productsDetails.productType
        dictionary["title"] = productsDetails.title
        dictionary["hash_code"] = productsDetails.hashCode()
        dictionary["to_string"] = productsDetails.toString()
        if (productsDetails.productType == BillingClient.ProductType.INAPP) {
            dictionary["one_time_purchase_offer_details"] = convertPurchaseOfferToDict(productsDetails.oneTimePurchaseOfferDetails)
        } else if (productsDetails.productType == BillingClient.ProductType.SUBS) {
            dictionary["subscription_offer_details"] = convertSubscriptionsDetailsListToArray(productsDetails.subscriptionOfferDetails)
        }
        return dictionary
    }


    // (INAPP)
    // Convert One Time Purchase Offer Details to Dictionary
    // https://developer.android.com/reference/com/android/billingclient/api/ProductDetails.OneTimePurchaseOfferDetails
    private fun convertPurchaseOfferToDict(offerDetails: ProductDetails.OneTimePurchaseOfferDetails?): Dictionary {
        val dictionary = Dictionary()  // from Godot type Dictionary
        if (offerDetails != null) {
            dictionary["formatted_price"] = offerDetails.formattedPrice
            dictionary["price_currency_code"] = offerDetails.priceCurrencyCode
            dictionary["price_amount_micros"] = offerDetails.priceAmountMicros
        }
        return dictionary
    }


    // (SUBS)
    // Convert list of subscriptions offers to array
    // yeah, lot of not Null checks :(
    private fun convertSubscriptionsDetailsListToArray(subscriptionsOffersList: List<ProductDetails.SubscriptionOfferDetails>?): Array<Any?>? {
        val subscriptionsOffersArray = subscriptionsOffersList?.let { arrayOfNulls<Any>(it.size) }
        if (subscriptionsOffersList != null) {
            for (i in subscriptionsOffersList.indices) {
                subscriptionsOffersArray?.set(i, convertSubscriptionDetailsToDictionary(subscriptionsOffersList[i]))
            }
        }
        return subscriptionsOffersArray
    }

    // Convert subscription offer details to dictionary
    // https://developer.android.com/reference/com/android/billingclient/api/ProductDetails.SubscriptionOfferDetails
    private fun convertSubscriptionDetailsToDictionary(offerDetails: ProductDetails.SubscriptionOfferDetails): Dictionary {
        val dictionary = Dictionary()  // from Godot type Dictionary
        dictionary["base_plan_id"] = offerDetails.basePlanId
        dictionary["installment_plan_details"] = offerDetails.installmentPlanDetails
        dictionary["offer_id"] = offerDetails.offerId
        dictionary["offer_tags"] = convertOfferTagsListToArray(offerDetails.offerTags) // list of String
        dictionary["offer_token"] = offerDetails.offerToken
        dictionary["pricing_phases"] = convertPricingPhasesListToArray(offerDetails.pricingPhases.pricingPhaseList)
        return dictionary
    }

    // Convert list of subscriptions offers to array
    // https://developer.android.com/reference/com/android/billingclient/api/ProductDetails.SubscriptionOfferDetails#getOfferTags()
    // yeah, lot of not Null checks :(
    private fun convertOfferTagsListToArray(offerTagsList: List<String>?): Array<Any?>? {
        val offerTagsArray = offerTagsList?.let { arrayOfNulls<Any>(it.size) }
        if (offerTagsList != null){
            for (i in offerTagsList.indices) {
                offerTagsArray?.set(i, (offerTagsList[i]))
            }
        }
        return offerTagsArray
    }


    // https://developer.android.com/reference/com/android/billingclient/api/ProductDetails.PricingPhases
    private fun convertPricingPhasesListToArray(phasesList: MutableList<ProductDetails.PricingPhase>?): Array<Any?>? {
        val phasesArray = phasesList?.let { arrayOfNulls<Any>(it.size) }
        if (phasesList != null) {
            for (i in phasesList.indices) {
                phasesArray?.set(i, convertPricingPhaseToDictionary(phasesList[i]))
            }
        }
        return phasesArray
    }


    // https://developer.android.com/reference/com/android/billingclient/api/ProductDetails.PricingPhase
    private fun convertPricingPhaseToDictionary(phase: ProductDetails.PricingPhase): Dictionary {
        val dictionary = Dictionary()  // from Godot type Dictionary
        dictionary["billing_cycle_count"] = phase.billingCycleCount
        dictionary["billing_period"] = phase.billingPeriod
        dictionary["formatted_price"] = phase.formattedPrice
        dictionary["price_amount_micros"] = phase.priceAmountMicros
        dictionary["price_currency_code"] = phase.priceCurrencyCode
        dictionary["recurrence_mode"] = phase.recurrenceMode
        return dictionary
    }

}


