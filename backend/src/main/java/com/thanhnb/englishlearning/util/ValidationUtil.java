package com.thanhnb.englishlearning.util;

import com.thanhnb.englishlearning.exception.InvalidPasswordException;
import com.thanhnb.englishlearning.exception.InvalidEmailException;

import java.util.regex.Pattern;

public class ValidationUtil {

    // ==================== PASSWORD VALIDATION ====================
    
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 50;
    
    // Password phải có: chữ thường, chữ hoa, số, ký tự đặc biệt
    private static final String PASSWORD_PATTERN = 
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,50}$";
    
    private static final Pattern passwordPattern = Pattern.compile(PASSWORD_PATTERN);

    /**
     * Validate password với các yêu cầu:
     * - Tối thiểu 8 ký tự, tối đa 50 ký tự
     * - Có ít nhất 1 chữ hoa
     * - Có ít nhất 1 chữ thường
     * - Có ít nhất 1 số
     * - Có ít nhất 1 ký tự đặc biệt (@#$%^&+=!)
     * - Không có khoảng trắng
     */
    public static void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new InvalidPasswordException("Mật khẩu không được để trống");
        }
        
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new InvalidPasswordException("Mật khẩu phải có ít nhất " + MIN_PASSWORD_LENGTH + " ký tự");
        }
        
        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new InvalidPasswordException("Mật khẩu không được vượt quá " + MAX_PASSWORD_LENGTH + " ký tự");
        }
        
        if (!passwordPattern.matcher(password).matches()) {
            throw new InvalidPasswordException(
                "Mật khẩu phải chứa ít nhất: 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt (@#$%^&+=!)"
            );
        }
    }

    /**
     * Kiểm tra password có đủ mạnh không (không throw exception)
     */
    public static boolean isPasswordStrong(String password) {
        try {
            validatePassword(password);
            return true;
        } catch (InvalidPasswordException e) {
            return false;
        }
    }

    // ==================== EMAIL VALIDATION ====================
    
    private static final String EMAIL_PATTERN = 
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    
    private static final Pattern emailPattern = Pattern.compile(EMAIL_PATTERN);

    /**
     * Validate email format
     */
    public static void validateEmail(String email) {
        if (email == null || email.isEmpty()) {
            throw new InvalidEmailException("Email không được để trống");
        }
        
        if (email.length() > 100) {
            throw new InvalidEmailException("Email không được vượt quá 100 ký tự");
        }
        
        if (!emailPattern.matcher(email).matches()) {
            throw new InvalidEmailException("Email không đúng định dạng");
        }
    }

    /**
     * Kiểm tra email có hợp lệ không (không throw exception)
     */
    public static boolean isEmailValid(String email) {
        try {
            validateEmail(email);
            return true;
        } catch (InvalidEmailException e) {
            return false;
        }
    }

    // ==================== USERNAME VALIDATION ====================
    
    private static final String USERNAME_PATTERN = "^[a-zA-Z0-9_]{3,20}$";
    private static final Pattern usernamePattern = Pattern.compile(USERNAME_PATTERN);

    /**
     * Validate username:
     * - Từ 3-20 ký tự
     * - Chỉ chứa chữ, số và dấu gạch dưới
     * - Không có ký tự đặc biệt, khoảng trắng
     */
    public static void validateUsername(String username) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username không được để trống");
        }
        
        if (username.length() < 3) {
            throw new IllegalArgumentException("Username phải có ít nhất 3 ký tự");
        }
        
        if (username.length() > 20) {
            throw new IllegalArgumentException("Username không được vượt quá 20 ký tự");
        }
        
        if (!usernamePattern.matcher(username).matches()) {
            throw new IllegalArgumentException(
                "Username chỉ được chứa chữ cái, số và dấu gạch dưới (_)"
            );
        }
    }

    // ==================== OTP VALIDATION ====================
    
    /**
     * Validate OTP format (6 chữ số)
     */
    public static void validateOtp(String otp) {
        if (otp == null || otp.isEmpty()) {
            throw new IllegalArgumentException("Mã OTP không được để trống");
        }
        
        if (!otp.matches("^\\d{6}$")) {
            throw new IllegalArgumentException("Mã OTP phải là 6 chữ số");
        }
    }
}