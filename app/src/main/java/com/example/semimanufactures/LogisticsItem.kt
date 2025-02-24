package com.example.semimanufactures

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class LogisticsItem(
    val id: String,
    var created_by_name: String,
    val send_from: String,
    val send_from_title: String,
    val send_to: String,
    val send_to_title: String,
    val type: String,
    val comment: String,
    val send_comment: String,
    val receive_comment: String,
    val created_at: String,
    val planned_date: String,
    val sender_phone: String,
    val receiver_phone: String,
    val loader: String,
    val sender_mdm: String,
    val receiver_mdm: String,
    var status: String,
    var is_doing_by: String,
    var is_doing_at: String,
    var is_check_by: String,
    var is_check_at: String,
    var is_ready_by: String,
    var is_ready_at: String,
    var is_decline_by: String?,
    var is_decline_at: String?,
    val sklad_from_name: String,
    val sklad_to_name: String,
    val creator: String,
    val sender_name: String,
    val receiver_name: String,
    val sklad_to_address: String,
    val sklad_from_address: String,
    val sklad_to_resp_name: String,
    val sklad_from_resp_name: String,
    val sklad_to_resp_dept_name: String,
    val sklad_from_resp_dept_name: String,
    val sklad_to_purpose: String,
    val sklad_from_purpose: String,
    var is_accepted_by: String,
    var is_accepted_at: String,
    @SerializedName("objects")
    var objectsRaw : List<Any>? = null,
    @SerializedName("docs")
    var docs: Map<String, DocsObject.Docs>? = null,
    @SerializedName("spros")
    var spros: String,
    var object_id: String
) {
    override fun toString(): String {
        return "LogisticsItem(id='$id', sklad_from_purpose='$sklad_from_purpose')"
    }
    fun getObjects(): List<LogisticsObject> {
        return objectsRaw?.mapNotNull { obj ->
            when (obj) {
                is Map<*, *> -> {
                    Gson().fromJson(Gson().toJson(obj), LogisticsObject::class.java)
                }
                is String -> {
                    LogisticsObject(id = "", index = "", demand = obj)
                }
                else -> null
            }
        }?.map { logisticsObject ->
            if (logisticsObject.type == "doc") {
                logisticsObject.copy(docNumber = logisticsObject.docNumber)
            } else {
                logisticsObject
            }
        } ?: emptyList()
    }
    data class DocsObject(
        @SerializedName("docs")
        val docs: Map<String, Docs>
    ) {
        data class Docs(
            val id: String,
            val nameFile: String,
            @SerializedName("md5_name")
            val md5Name: String,
            val mdm_sotrudnik: String
        )
    }
}
data class LogisticsObject(
    val id: String,
    val index: String,
    @SerializedName("Спрос")
    val demand: String? = null,
    @SerializedName("Операция")
    val operation: String? = null,
    @SerializedName("ЗаходНомер")
    val zahod: String? = null,
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("doc_number")
    val docNumber: String? = null
)