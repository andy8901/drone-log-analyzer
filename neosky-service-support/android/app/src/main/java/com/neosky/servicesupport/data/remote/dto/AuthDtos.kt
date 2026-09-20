package com.neosky.servicesupport.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val fullName: String,
    val email: String,
    val phone: String? = null,
    val role: String,
)

@Serializable
data class RegisterRequestDto(
    val fullName: String,
    val email: String,
    val phone: String,
    val password: String,
    val companyName: String? = null,
)

@Serializable
data class RegisterResponseDto(
    val user: UserDto,
    val message: String,
)

@Serializable
data class VerifyOtpRequestDto(
    val email: String,
    val otp: String,
)

@Serializable
data class VerifyOtpResponseDto(
    val verified: Boolean,
)

@Serializable
data class LoginRequestDto(
    val identifier: String,
    val password: String,
)

@Serializable
data class LoginResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Int,
    val user: UserDto,
)

@Serializable
data class RefreshRequestDto(
    val refreshToken: String,
)

@Serializable
data class RefreshResponseDto(
    val accessToken: String,
    val expiresIn: Int,
)

@Serializable
data class ForgotPasswordRequestDto(
    val identifier: String,
)

@Serializable
data class ResetPasswordRequestDto(
    val identifier: String,
    val otp: String,
    val newPassword: String,
)
