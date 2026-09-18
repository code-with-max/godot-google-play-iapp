package one.allme.plugin.androidiapp.utils

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsResult
import org.godotengine.godot.Dictionary

/**
 * Legacy utility object for backward compatibility.
 * Delegates all calls to [IappUtils].
 */
@Suppress("ClassName")
object IAPP_utils {
    fun convertPurchasesListToDictionary(purchasesList: List<Purchase>?): Dictionary =
        IappUtils.convertPurchasesListToDictionary(purchasesList)

    fun convertPurchasesListToArray(purchasesList: List<Purchase>?): Array<Any> =
        IappUtils.convertPurchasesListToArray(purchasesList)

    fun convertQueryProductDetailsResultToDictionary(queryProductDetailsResult: QueryProductDetailsResult): Dictionary =
        IappUtils.convertQueryProductDetailsResultToDictionary(queryProductDetailsResult)

    fun convertProductDetailsListToArray(productDetailsList: List<ProductDetails>?): Array<Any> =
        IappUtils.convertProductDetailsListToArray(productDetailsList)
}
