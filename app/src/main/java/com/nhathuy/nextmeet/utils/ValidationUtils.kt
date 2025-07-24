package com.nhathuy.nextmeet.utils

import android.content.Context
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.model.ValidationResult

/**
 * Utility class for validating user input
 */
object ValidationUtils {

    /**
     * Xác thực phone
     *
     * @param phone Phone để xác thực
     * @return ValidationResult chứa trạng thái xác thực và thông báo lỗi nếu có
     */
    fun validatePhone(context: Context,phone: String): ValidationResult {
        return when {
            phone.isEmpty() -> ValidationResult(false, context.getString(R.string.error_phone_required))
            phone.length != 10 -> ValidationResult(false, context.getString(R.string.error_phone_length))
            !phone.all { it.isDigit() } -> ValidationResult(
                false,
                context.getString(R.string.error_phone_digits)
            )

            else -> ValidationResult(true)
        }
    }

    /**
     * Xác thực name
     *
     * @param name Name để xác thực
     * @return ValidationResult chứa trạng thái xác thực và thông báo lỗi nếu có
     */
    fun validateName(context: Context,name: String): ValidationResult {
        return when {
            name.isEmpty() -> ValidationResult(false, context.getString(R.string.error_name_required))
            name.length > 25 -> ValidationResult(false, context.getString(R.string.error_name_length))
            else -> ValidationResult(true)
        }
    }

    /**
     * Xác thực address
     *
     * @param address Address để xác thực
     * @return ValidationResult chứa trạng thái xác thực và thông báo lỗi nếu có
     */
    fun validateAddress(context: Context,address:String) :ValidationResult {
        return when {
            address.isEmpty() || address == "Add address" -> ValidationResult(false,context.getString(R.string.error_address_required))
            else -> ValidationResult(true)
        }
    }

    /**
     * Xác thực password
     *
     * @param password Password để xác thực
     * @return ValidationResult chứa trạng thái xác thực và thông báo lỗi nếu có
     */
    fun validatePassword(context: Context,password: String): ValidationResult {

        return when {
            password.isEmpty() -> ValidationResult(false, context.getString(R.string.error_password_required))
            password.length < 6 -> ValidationResult(
                false,
                context.getString(R.string.error_password_length)
            )
            else -> ValidationResult(true)
        }
    }
    /**
     * Xác thực email
     *
     * @param email Email để xác thực
     * @return ValidationResult chứa trạng thái xác thực và thông báo lỗi nếu có
     */
    fun validateEmail(context: Context,email: String): ValidationResult {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return when {
            email.isEmpty() -> ValidationResult(false, context.getString(R.string.error_email_required))
            !email.matches(emailPattern.toRegex()) -> ValidationResult(
                false,
                context.getString(R.string.error_email_invalid)
            )

            else -> ValidationResult(true)
        }
    }

    /**
     * Xác thực mật khẩu khớp
     * @param password Mật khẩu mới
     * @param confirmPassword Mật khẩu xác thực
     * @return ValidationResult chứa trạng thái xác thực và thông báo lỗi nếu có
     */
    fun validatePasswordMatch(context: Context,password: String, confirmPassword: String): ValidationResult {
        return when {
            password != confirmPassword -> ValidationResult(false, context.getString(R.string.error_password_mismatch))
            else -> ValidationResult(true)
        }
    }
}

