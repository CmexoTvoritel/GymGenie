package com.asc.gymgenie.common

class ApiException(val statusCode: Int, val errorBody: String) :
    Exception("HTTP $statusCode: $errorBody")
