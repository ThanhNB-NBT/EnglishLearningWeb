import { authAPI } from "../api/modules/auth.api";

// ===== HELPER: Lấy storage keys theo role =====
const getStorageKeys = (role) => {
    const prefix = role === 'ADMIN' ? 'admin_' : 'user_';
    return {
        token: `${prefix}authToken`,
        user: `${prefix}user`
    };
};

// ===== HELPER: Detect role từ URL path =====
const detectRoleFromPath = () => {
    return window.location.pathname.startsWith('/admin') ? 'ADMIN' : 'USER';
};

// ===== SAVE AUTH DATA =====
export const saveAuthData = (token, user) => {
    const keys = getStorageKeys(user.role);
    localStorage.setItem(keys.token, token);
    localStorage.setItem(keys.user, JSON.stringify(user));
};

// ===== GET TOKEN (dựa vào path hiện tại) =====
export const getToken = () => {
    const role = detectRoleFromPath();
    const key = role === 'ADMIN' ? 'admin_authToken' : 'user_authToken';
    return localStorage.getItem(key);
};

// ===== GET USER (dựa vào path hiện tại) =====
export const getUser = () => {
    const role = detectRoleFromPath();
    const key = role === 'ADMIN' ? 'admin_user' : 'user_user';
    const user = localStorage.getItem(key);
    return user ? JSON.parse(user) : null;
};

// ===== IS AUTHENTICATED =====
export const isAuthenticated = () => {
    const token = getToken();
    const user = getUser();
    
    // Phải có cả token và user, và token chưa hết hạn
    if (!token || !user) return false;
    
    return !isTokenExpired();
};

// ===== IS ADMIN =====
export const isAdmin = () => {
    const user = getUser();
    return user?.role === 'ADMIN';
};

// ===== IS USER =====
export const isUser = () => {
    const user = getUser();
    return user?.role === 'USER';
};

// ===== LOGOUT (FIX: Gọi API trước, xóa storage sau) =====
export const logout = async () => {
    const user = getUser();
    const token = getToken();
    const role = user?.role || detectRoleFromPath();
    
    try {
        // 1. Gọi API logout trước (khi còn token)
        if (token) {
            await authAPI.logout();
        }
    } catch (err) {
        console.error('Logout API error:', err);
    } finally {
        // 2. Sau đó mới xóa storage
        const keys = getStorageKeys(role);
        localStorage.removeItem(keys.token);
        localStorage.removeItem(keys.user);
        
        // 3. Redirect
        window.location.href = `${role === 'ADMIN' ? '/admin/login' : '/user/login'}?loggedOut=true`;
    }
};

// ===== IS TOKEN EXPIRED =====
export const isTokenExpired = () => {
    const token = getToken();
    if (!token) return true;

    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        const currentTime = Math.floor(Date.now() / 1000);
        return payload.exp < currentTime;
    } catch (error) {
        console.error('Error decoding token:', error);
        return true;
    }
};

// ===== GET USER FROM TOKEN =====
export const getUserFromToken = () => {
    const token = getToken();
    if (!token) return null;

    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return {
            id: payload.sub,
            username: payload.username,
            email: payload.email,
            fullname: payload.fullname,
            role: payload.role,
            exp: payload.exp,
            iat: payload.iat
        };
    } catch (error) {
        console.error('Error decoding token:', error);
        return null;
    }
};

// ===== CHECK TOKEN AND AUTO LOGOUT =====
export const checkTokenAndAutoLogout = () => {
    if (isAuthenticated() && isTokenExpired()) {
        logout();
        return false;
    }
    return true;
};

// ===== GET USER ROLE =====
export const getUserRole = () => {
    const user = getUser();
    return user?.role || null;
};

// ===== HAS ROLE =====
export const hasRole = (role) => {
    const user = getUser();
    return user?.role === role;
};

// ===== GET USER DISPLAY NAME =====
export const getUserDisplayName = () => {
    const user = getUser();
    return user?.fullname || user?.username || 'Người dùng ẩn danh';
};

// ===== GET USER EMAIL =====
export const getUserEmail = () => {
    const user = getUser();
    return user?.email || 'No Email';
};

// ===== INIT AUTH CHECK =====
export const initAuthCheck = () => {
    // Check ngay khi init
    if (isAuthenticated() && isTokenExpired()) {
        logout();
    }

    // Check định kỳ mỗi 5s
    setInterval(() => {
        checkTokenAndAutoLogout();
    }, 5000);
};

// ===== LOGIN =====
export const login = (token, user) => {
    saveAuthData(token, user);
    const role = user.role;
    window.location.href = role === 'ADMIN' ? '/admin/dashboard' : '/user/home';
};

// ===== REGISTER =====
export const register = (token, user) => {
    saveAuthData(token, user);
    const role = user.role;
    window.location.href = role === 'ADMIN' ? '/admin/login' : '/user/login';
};