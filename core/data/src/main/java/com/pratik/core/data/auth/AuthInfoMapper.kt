package com.pratik.core.data.auth

import com.pratik.core.domain.authSession.AuthInfo

fun AuthInfo.toAuthInfoSerializable(): AuthInfoSerializable  {
    return AuthInfoSerializable(
        accessToken = accessToken,
        refreshToken = refreshToken,
        userId = userId
    )
}

fun AuthInfoSerializable.toAuthInfo(): AuthInfo {
    return AuthInfo(
        accessToken = accessToken,
        refreshToken = refreshToken,
        userId = userId
    )
}