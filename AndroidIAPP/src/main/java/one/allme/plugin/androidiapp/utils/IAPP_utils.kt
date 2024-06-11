package one.allme.plugin.androidiapp.utils

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import org.godotengine.godot.Dictionary


object IAPP_utils {

    fun convertPurchasesListToArray(purchasesList: List<Purchase>): Array<Any?> {
        val purchasesArray = arrayOfNulls<Any>(purchasesList.size)
        for (i in purchasesList.indices) {
            purchasesArray[i] = convertPurchaseToDictionary(purchasesList[i])
        }
        return purchasesArray
    }


    private fun convertPurchaseToDictionary(purchase: Purchase): Dictionary {
        val dictionary = Dictionary()
        dictionary["account_identifiers"] = purchase.accountIdentifiers
        dictionary["developer_payload"] = purchase.developerPayload
        dictionary["order_id"] = purchase.orderId
        dictionary["original_json"] = purchase.originalJson
        dictionary["package_name"] = purchase.packageName
        dictionary["pending_purchase_update"] = purchase.pendingPurchaseUpdate
        dictionary["products"] = purchase.products
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


    fun convertProductDetailsListToArray(productDetailsList: List<ProductDetails>): Array<Any?> {
        val productDetailsArray = arrayOfNulls<Any>(productDetailsList.size)
        for (i in productDetailsList.indices) {
            productDetailsArray[i] = convertProductDetailsToDictionary(productDetailsList[i])
        }
        return productDetailsArray
    }


    private fun convertProductDetailsToDictionary(productsDetails: ProductDetails): Dictionary {
        val dictionary = Dictionary()  // from Godot type Dictionary
        dictionary["description"] = productsDetails.description
        dictionary["name"] = productsDetails.name
        dictionary["product_id"] = productsDetails.productId
        dictionary["product_type"] = productsDetails.productType
        dictionary["subscription_offer_details"] = productsDetails.subscriptionOfferDetails
        dictionary["title"] = productsDetails.title
        dictionary["hash_code"] = productsDetails.hashCode()
        dictionary["to_string"] = productsDetails.toString()
        dictionary["one_time_purchase_offer_details"] = convertPurchaseOfferToDict(productsDetails.oneTimePurchaseOfferDetails)
        return dictionary
    }

//  Convert One Time Purchase Offer Details to Dictionary
    private fun convertPurchaseOfferToDict(offerDetails: ProductDetails.OneTimePurchaseOfferDetails?): Dictionary {
        val dictionary = Dictionary()  // from Godot type Dictionary
        if (offerDetails != null) {
            dictionary["formatted_price"] = offerDetails.formattedPrice
        }
        if (offerDetails != null) {
            dictionary["price_currency_code"] = offerDetails.priceCurrencyCode
        }
        if (offerDetails != null) {
            dictionary["price_amount_micros"] = offerDetails.priceAmountMicros
        }
            return dictionary
        }

}


