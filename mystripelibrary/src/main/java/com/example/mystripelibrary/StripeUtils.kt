import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.example.mystripelibrary.CardObject
import com.example.mystripelibrary.R
import com.google.gson.annotations.SerializedName

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject


@SuppressLint("StaticFieldLeak")
class StripeUtils(private val STRIPE_PUBLISHABLE_KEY: String, private val STRIPE_SECRET_KEY: String) {
    private var dialog: AlertDialog? = null

    val MY_STRIPE_PUBLISHABLE_KEY = this.STRIPE_PUBLISHABLE_KEY
    val MY_STRIPE_SECRET_KEY = this.STRIPE_SECRET_KEY

    val base_url = "https://api.stripe.com/v1/"
    val TOKENS = "tokens"
    var tokenGeneratedId: String = ""
    lateinit var context: Context
    var error = ""


    suspend fun createStripeCustomerWithUserEmail(
        context: Context,
        card: CardObject,
        email: String
    ): String {
        this.context = context
        var customerID: String = ""
        CoroutineScope(Dispatchers.IO).async {
            val tokenId: String = generateCardtoken(card)
            Log.e("----afterscope----", "GenerateCardtoken: " + tokenId)
            tokenGeneratedId = tokenId
            withContext(Dispatchers.Main) {
                if (tokenGeneratedId.equals("")) {
                    customerID = ""
                } else {
                    val custId = apiCreateCustomerWithUserEmail(email, tokenGeneratedId)
//                    Log.e("TAG", "custId  >>$custId")
                    if (custId != "") {
                        customerID = custId
                    }
                }
            }
        }.await()

        return customerID
    }

    @SuppressLint("LongLogTag")
    private suspend fun apiCreateCustomerWithUserEmail(email: String, cardToken: String): String {
        var customerID: String = ""
        CoroutineScope(Dispatchers.IO).async {

            var postBody =
                "email=$email&source=$cardToken"
            val client = OkHttpClient().newBuilder()
                .build()
            val mediaType: MediaType? = "application/x-www-form-urlencoded".toMediaTypeOrNull()
            val body: RequestBody =
                postBody
                    .toRequestBody(mediaType)
            val request: Request = Request.Builder()
                .url(base_url + "customers")
                .method("POST", body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader(
                    "Authorization",
                    "Bearer " + MY_STRIPE_SECRET_KEY
                )
                .build()
            val responseData: Response = client.newCall(request).execute()

            val jsonData: String = responseData.body!!.string()
            val Jobject = JSONObject(jsonData)
            customerID = Jobject.getString("id")
            Log.e("----customerResponse----", "GeneratCustomerid" + Jobject)
        }.await()
        return customerID
    }


    suspend fun addNewCardToStripeCustomerWithCustomerID(
        context: Context,
        card: CardObject,
        stripeCustomerId: String
    ): String {
        this.context = context
        var customerID: String = ""
        CoroutineScope(Dispatchers.IO).async {
            val tokenId: String = generateCardtoken(card)
            Log.e("----afterscope----", "GenerateCardtoken: " + tokenId)
            tokenGeneratedId = tokenId

            withContext(Dispatchers.Main) {
                if (tokenGeneratedId.equals("")) {
                    customerID = ""
                } else {
                    apiCallForAddNewCardWithCustomerID(tokenGeneratedId, stripeCustomerId)
                    customerID = "yes"
                }
            }
        }.await()

        return customerID
    }

    suspend fun apiCallForAddNewCardWithCustomerID(cardToken: String, stripeCustomerId: String) {
        CoroutineScope(Dispatchers.IO).async {
            var URLStripe = base_url + "customers/${stripeCustomerId}/sources";
            var postBody =
                "source=$cardToken"
            val client = OkHttpClient().newBuilder()
                .build()
            val mediaType: MediaType? = "application/x-www-form-urlencoded".toMediaTypeOrNull()
            val body: RequestBody =
                postBody
                    .toRequestBody(mediaType)
            val request: Request = Request.Builder()
                .url(URLStripe)
                .method("POST", body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader(
                    "Authorization",
                    "Bearer $MY_STRIPE_SECRET_KEY"
                )
                .build()
            val responseData: Response = client.newCall(request).execute()

            val jsonData: String = responseData.body!!.string()
            val Jobject = JSONObject(jsonData)
            Log.e("TAG", "apiCallForAddNewCardWithCustomerID:... " + Jobject)
        }.await()
//        cardToken = Jobject.getString("id")
    }


    @SuppressLint("LogNotTimber")
    private fun generateCardtoken(card: CardObject): String {

        var cardToken: String = ""

        var postBody =
            "card[number]=${card.cardNumber}&card[exp_month]=${card.cardExpiryMonth}&card[exp_year]=${card.cardExpiryYear}&card[cvc]=${card.cardCVV}"
        val client = OkHttpClient().newBuilder()
            .build()
        val mediaType: MediaType? = "application/x-www-form-urlencoded".toMediaTypeOrNull()
        val body: RequestBody =
            postBody
                .toRequestBody(mediaType)
        val request: Request = Request.Builder()
            .url(base_url + TOKENS)
            .method("POST", body)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader(
                "Authorization",
                "Bearer $MY_STRIPE_PUBLISHABLE_KEY"
            )
            .build()
        val responseData: Response = client.newCall(request).execute()

        val jsonData: String = responseData.body!!.string()
        val Jobject = JSONObject(jsonData)

//        If ERROR come look like below
//
//
//        {
//            "error": {
//            "code": "incorrect_number",
//            "doc_url": "https://stripe.com/docs/error-codes/incorrect-number",
//            "message": "Your card number is incorrect.",
//            "param": "number",
//            "type": "card_error"
//        }
//        }

        if (Jobject.has("error")) {
            cardToken = ""
            val error: JSONObject = Jobject.get("error") as JSONObject
            val msg = error.get("message")
            val cardError = msg.toString() //app specified variable to store error massage
            this.error = cardError
            showAlertDialogForstripe(context){
                setTitle("STRIPE")
                setCancelable(false)
                setMessage(cardError)
                positiveButton("OK"){
                }
            }
        } else if (Jobject.has("id")) {
            cardToken = Jobject.getString("id")
        }
        Log.e("TAG---cardtokenData...", ":... " + Jobject)
        return cardToken
    }


    fun getAllStripeCard(stripeCustomerId: String): JSONObject? {

        var URLStripe = base_url + "customers/${stripeCustomerId}/sources"
        val client = OkHttpClient().newBuilder()
            .build()
        var postBody = ""

        val mediaType: MediaType? = "application/x-www-form-urlencoded".toMediaTypeOrNull()
        val body: RequestBody =
            postBody
                .toRequestBody(mediaType)

        val request: Request = Request.Builder()
            .url(URLStripe)
            .method("GET", null)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader(
                "Authorization",
                "Bearer $MY_STRIPE_SECRET_KEY"
            )
            .build()
        val responseData: Response = client.newCall(request).execute()

        val jsonData: String = responseData.body!!.string()
        val Jobject = JSONObject(jsonData)
//        Log.e("---card List----", "getAllStripeCard: " + Jobject)
        return Jobject
    }

    fun updateDefaultcardWithCustomerId(
        context: Context,
        cardToken: String,
        customerId: String
    ): Boolean {
        this.context = context

        var URLStripe = base_url + "customers/${customerId}";
        var postBody =
            "default_source=$cardToken"
        val client = OkHttpClient().newBuilder()
            .build()
        val mediaType: MediaType? = "application/x-www-form-urlencoded".toMediaTypeOrNull()
        val body: RequestBody =
            postBody
                .toRequestBody(mediaType)
        val request: Request = Request.Builder()
            .url(URLStripe)
            .method("POST", body)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader(
                "Authorization",
                "Bearer $MY_STRIPE_SECRET_KEY"
            )
            .build()
        val responseData: Response = client.newCall(request).execute()
        val jsonData: String = responseData.body!!.string()
        val Jobject = JSONObject(jsonData)
        Log.e("TAG---defaultcard", "apiCallForAddNewCardWithCustomerID:... " + Jobject)
        return true
    }

    fun deleteCard(context: Context, stripeCustomerId: String, cardToken: String): Boolean {
        this.context = context

        var URLStripe = base_url + "customers/${stripeCustomerId}/sources/${cardToken}"

        val client = OkHttpClient().newBuilder()
            .build()
        var postBody = ""

        val mediaType: MediaType? = "application/x-www-form-urlencoded".toMediaTypeOrNull()
        val body: RequestBody =
            postBody
                .toRequestBody(mediaType)

        val request: Request = Request.Builder()
            .url(URLStripe)
            .method("DELETE", null)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader(
                "Authorization",
                "Bearer $MY_STRIPE_SECRET_KEY"
            )
            .build()
        val responseData: Response = client.newCall(request).execute()

        val jsonData: String = responseData.body!!.string()
        val Jobject = JSONObject(jsonData)
        return true
    }

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


    fun showAlertDialogForstripe(context: Context, dialogBuilder: AlertDialog.Builder.() -> Unit) {
        val builder = AlertDialog.Builder(context)
        builder.dialogBuilder()
        builder.setCancelable(false)
        val dialog = builder.create()

        dialog.setOnShowListener {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(context, R.color.green_400))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.black
                )
            )
        }
        dialog.show()
    }

    fun AlertDialog.Builder.positiveButton(text: String, handleClick: (which: Int) -> Unit = {}) {
        this.setPositiveButton(text) { _, which -> handleClick(which) }
    }

    fun AlertDialog.Builder.negativeButton(text: String, handleClick: (which: Int) -> Unit = {}) {
        this.setNegativeButton(text) { _, which -> handleClick(which) }
    }


    fun showProgressDialogStripe(context: Context) {
        hideProgressDialogStripe()
        val progressDialog = ProgressDialog(context)
        progressDialog.setTitle("Kotlin Progress Bar")
        progressDialog.setMessage("Application is loading, please wait")
        progressDialog.show()


        val builder = AlertDialog.Builder(context)
        builder.setCancelable(false) // if you want user to wait for some process to finish,
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val v = inflater.inflate(R.layout.layout_loading_dialog, null)
        builder.setView(v)
        dialog = builder.create()
        dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog!!.window!!.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog!!.show()

    }

    fun hideProgressDialogStripe() {
        if (isProgressDialogShownStripe())
            dialog?.dismiss()
    }

    fun isProgressDialogShownStripe(): Boolean {
        if (dialog != null)
            return dialog!!.isShowing
        else
            return false
    }
}