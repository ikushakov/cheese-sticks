package com.example.semimanufactures

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class LogisticsItem(
    val id: String,
    val created_by: String,
    var created_by_name: String,
    val send_from: String,
    val send_from_title: String,
    val send_to: String,
    val send_to_title: String,
    val type: String,
    var comment: String,
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
    var executor: String,
    var executor_name: String,
    @SerializedName("dn_object_names")
    val dnObjectNames: String? = null,
    @SerializedName("objects")
    var objectsRaw: Any? = null,

    // ИЗМЕНЯЕМ ЭТУ СТРОКУ:
    // Было: @SerializedName("docs") var docs: Map<String, DocsObject.Docs>? = null,
    // Стало:
    @SerializedName("docs")
    var docs: List<Doc>? = null,

    @SerializedName("spros")
    var spros: String,
    var object_id: String,
    val object_name: String?
) {
    override fun toString(): String {
        return "LogisticsItem(id='$id', sklad_from_purpose='$sklad_from_purpose')"
    }

    fun getObjects(): List<LogisticsObject> {
        return when (type) {
            "stanok" -> {
                (objectsRaw as? Map<String, Any>)?.map { (_, value) ->
                    when (value) {
                        is Map<*, *> -> {
                            Gson().fromJson(Gson().toJson(value), LogisticsObject::class.java)
                        }
                        else -> null
                    }
                }?.filterNotNull() ?: emptyList()
            }
            else -> {
                (objectsRaw as? List<Any>)?.mapNotNull { obj ->
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
        }
    }

    // ДОБАВЛЯЕМ ЭТИ МЕТОДЫ ДЛЯ СОВМЕСТИМОСТИ:

    // Получить docs как Map (для совместимости со старым кодом)
    fun getDocsMap(): Map<String, Doc>? {
        return docs?.associateBy { it.id ?: "unknown" }
    }

    // Проверить наличие фото
    fun hasPhotos(): Boolean {
        return docs?.any { it.md5Name?.isNotBlank() == true } == true
    }

    // Получить список файлов URL
    fun getPhotoUrls(): List<String> {
        return docs?.mapNotNull { it.fileUrl } ?: emptyList()
    }

    fun getFirstDoc(): Doc? = docs?.firstOrNull()

    // Безопасный геттер для документа по ID
    fun getDocById(id: String): Doc? = docs?.find { it.id == id }

    // Проверка, есть ли хоть один документ с фото
    fun hasAnyPhoto(): Boolean {
        return docs?.any {
            !it.md5Name.isNullOrBlank() || !it.fileUrl.isNullOrBlank()
        } == true
    }

    // Получение только документов с фото
    fun getPhotoDocs(): List<Doc> {
        return docs?.filter {
            !it.md5Name.isNullOrBlank() || !it.fileUrl.isNullOrBlank()
        } ?: emptyList()
    }

    // Получение статуса в читаемом виде
    fun getStatusText(): String {
        return when (status) {
            "0" -> "Зарегистрирована"
            "1" -> "Принята"
            "-1" -> "Аннулирована"
            "2" -> "Выполняется"
            "3" -> "На подтверждении"
            "4" -> "Выполнена"
            else -> "Неизвестно ($status)"
        }
    }

    // Проверка, можно ли менять статус
    fun canChangeStatus(): Boolean {
        return status !in listOf("-1", "4") // Нельзя менять аннулированные и выполненные
    }

    // Получение отформатированной даты
    fun getFormattedCreatedAt(): String {
        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
            val date = inputFormat.parse(created_at)
            outputFormat.format(date)
        } catch (e: Exception) {
            created_at // Возвращаем как есть в случае ошибки
        }
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
    val docNumber: String? = null,
    @SerializedName("Станок")
    val stanok: String? = null,
    @SerializedName("MachineID")
    val machineId: String? = null,
    @SerializedName("kEquipmentId")
    val kEquipmentId: String? = null,
    @SerializedName("Тип")
    val tip: String? = null,
    @SerializedName("СтанокПодразделениеMDMкод")
    val stanokPodrazdelenieMDMKod: String? = null,
    @SerializedName("ФондСтанка")
    val fondStanka: String? = null,
    @SerializedName("МесФондСтанка")
    val mesFondStanka: String? = null,
    @SerializedName("Vsrok")
    val vsrok: String? = null,
    @SerializedName("NeVsrok")
    val neVsrok: String? = null,
    @SerializedName("Стойка")
    val stoika: String? = null,
    @SerializedName("ОбъектГруппа")
    val objectGruppa: String? = null
)

data class Doc(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("mdm_sotrudnik")
    val mdmSotrudnik: String? = null,

    @SerializedName("name_file")
    val nameFile: String? = null,

    @SerializedName("md5_name")
    val md5Name: String? = null, // Может быть null, поэтому String?

    @SerializedName("file_url")
    val fileUrl: String? = null,

    @SerializedName("storage_type")
    val storageType: String? = null,

    @SerializedName("date_created_file")
    val dateCreatedFile: String? = null,

    @SerializedName("status_file")
    val statusFile: String? = null,

    @SerializedName("s3_key")
    val s3Key: String? = null,

    @SerializedName("s3_unavailable")
    val s3Unavailable: Any? = null // Может быть Boolean или String (URL)
)