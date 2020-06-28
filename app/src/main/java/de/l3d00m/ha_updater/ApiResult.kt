package de.l3d00m.ha_updater

object ApiResult {
    data class HomeassistantResult(
        val result: ResultCode,
        val message: String? = null,
        val newState: String? = null
    )

    enum class ResultCode {
        SUCCESS,
        MISSING_FIELD,
        INVALID_ENTITY,
        NETWORK_ERROR,
        API_ERROR,
        INVALID_TOKEN,
        UNKNOWN_ERROR
    }
}
