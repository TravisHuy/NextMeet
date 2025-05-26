package com.nhathuy.nextmeet.utils

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
    fun validatePhone(phone: String): ValidationResult {
        return when {
            phone.isEmpty() -> ValidationResult(false, "Phone number is required")
            phone.length != 10 -> ValidationResult(false, "Phone number must be 10 digits")
            !phone.all { it.isDigit() } -> ValidationResult(
                false,
                "Phone number must contain only digits"
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
    fun validateName(name: String): ValidationResult {
        return when {
            name.isEmpty() -> ValidationResult(false, "Name is required")
            name.length > 25 -> ValidationResult(false, "Name must be less than 25 characters")
            else -> ValidationResult(true)
        }
    }

    /**
     * Xác thực address
     *
     * @param address Address để xác thực
     * @return ValidationResult chứa trạng thái xác thực và thông báo lỗi nếu có
     */
    fun validateAddress(address:String) :ValidationResult {
        return when {
            address.isEmpty() || address == "Add address" -> ValidationResult(false,"Address is required")
            else -> ValidationResult(true)
        }
    }

//    fun validateCoordinate(latitude:Double,longitude:Double) : ValidationResult {
//        return when {
//            (latitude == null || longitude == null) -> ValidationResult(false, "Latitude or longitude required")
//            else -> ValidationResult(true)
//        }
//    }
//    /**
//     * Xác thực password
//     *
//     * @param password Password để xác thực
//     * @return ValidationResult chứa trạng thái xác thực và thông báo lỗi nếu có
//     */
//    fun validatePassword(password: String): ValidationResult {
//        val specialCharRegex = Regex("[!@#\$%^&*(),.?\":{}|<>]")
//        val uppercaseRegex = Regex("[A-Z]")
//
//        return when {
//            password.isEmpty() -> ValidationResult(false, "Password is required")
//            password.length < 6 -> ValidationResult(
//                false,
//                "Password must be less than 6 characters"
//            )
//
//            !specialCharRegex.containsMatchIn(password) -> ValidationResult(
//                false,
//                "Password must contain at least one character"
//            )
//
//            !uppercaseRegex.containsMatchIn(password) -> ValidationResult(
//                false,
//                "Password must be least one character uppercase letter"
//            )
//
//            else -> ValidationResult(true)
//        }
//    }

    /**
     * Xác thực password
     *
     * @param password Password để xác thực
     * @return ValidationResult chứa trạng thái xác thực và thông báo lỗi nếu có
     */
    fun validatePassword(password: String): ValidationResult {

        return when {
            password.isEmpty() -> ValidationResult(false, "Password is required")
            password.length < 6 -> ValidationResult(
                false,
                "Password must be less than 6 characters"
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
    fun validateEmail(email: String): ValidationResult {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return when {
            email.isEmpty() -> ValidationResult(false, "Email is required")
            !email.matches(emailPattern.toRegex()) -> ValidationResult(
                false,
                "Invalid email format"
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
    fun validatePasswordMatch(password: String, confirmPassword: String): ValidationResult {
        return when {
            password != confirmPassword -> ValidationResult(false, "Passwords do not match")
            else -> ValidationResult(true)
        }
    }
}

