package com.example.mystripelibrary

import com.google.gson.annotations.SerializedName

data class StripeCardData(
    @SerializedName("data")
    val `data`: List<Data>,
    @SerializedName("has_more")
    val hasMore: Boolean,
    @SerializedName("object")
    val objectX: String,
    @SerializedName("url")
    val url: String
) {
    data class Data(
        @SerializedName("address_city")
        val addressCity: Any,
        @SerializedName("address_country")
        val addressCountry: Any,
        @SerializedName("address_line1")
        val addressLine1: Any,
        @SerializedName("address_line1_check")
        val addressLine1Check: Any,
        @SerializedName("address_line2")
        val addressLine2: Any,
        @SerializedName("address_state")
        val addressState: Any,
        @SerializedName("address_zip")
        val addressZip: Any,
        @SerializedName("address_zip_check")
        val addressZipCheck: Any,
        @SerializedName("brand")
        val brand: String,
        @SerializedName("country")
        val country: String,
        @SerializedName("customer")
        val customer: String,
        @SerializedName("cvc_check")
        val cvcCheck: String,
        @SerializedName("dynamic_last4")
        val dynamicLast4: Any,
        @SerializedName("exp_month")
        val expMonth: Int,
        @SerializedName("exp_year")
        val expYear: Int,
        @SerializedName("fingerprint")
        val fingerprint: String,
        @SerializedName("funding")
        val funding: String,
        @SerializedName("id")
        val id: String,
        @SerializedName("last4")
        val last4: String,
        @SerializedName("metadata")
        val metadata: Metadata,
        @SerializedName("name")
        val name: Any,
        @SerializedName("object")
        val objectX: String,
        @SerializedName("tokenization_method")
        val tokenizationMethod: Any
    ) {
        class Metadata
    }
}