import { authAPI } from "../api";

export const saveAuthData = (token, user) => {
    localStorage.setItem('authToken', token);
    localStorage.setItem('user', JSON.stringify(user));
};

export const getToken = () => {
    return localStorage.getItem('authToken');
};

export const getUser = () => {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
};

export const isAuthenticated = () => {
    const token = getToken();
    const user = getUser();
    return token && user;
};

export const isAdmin = () => {
    const user = getUser();
    return user?.role === 'ADMIN';
};

export const isUser = () => {
    const user = getUser();
    return user?.role === 'USER';
};

export const logout = () => {
    const user = getUser();
    const role = user?.role || null;
    localStorage.removeItem('authToken');
    localStorage.removeItem('user');
    localStorage.clear();
    authAPI.logout().catch(err => {
        console.error('Logout API error:', err);
    });
    // Chuyển hướng với query param để thông báo trên trang login
    window.location.href = `${role === 'ADMIN' ? '/admin/login' : '/user/login'}?loggedOut=true`;
};

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

export const checkTokenAndAutoLogout = () => {
    if (isAuthenticated() && isTokenExpired()) {
        logout();
        return false;
    }
    return true;
};

export const getUserRole = () => {
    const user = getUser();
    return user?.role || null;
};

export const hasRole = (role) => {
    const user = getUser();
    return user?.role === role;
};

export const getUserDisplayName = () => {
    const user = getUser();
    return user?.fullname || user?.username || 'Người dùng ẩn danh';
};

export const getUserEmail = () => {
    const user = getUser();
    return user?.email || 'No Email';
};

export const initAuthCheck = () => {
    if (isAuthenticated() && isTokenExpired()) {
        logout();
    }

    setInterval(() => {
        checkTokenAndAutoLogout();
    }, 5000);
};

export const login = (token, user) => {
    saveAuthData(token, user);
    const role = getUserRole();
    window.location.href = role === 'ADMIN' ? '/admin/dashboard' : '/user/dashboard';
};

export const register = (token, user) => {
    saveAuthData(token, user);
    const role = getUserRole();
    window.location.href = role === 'ADMIN' ? '/admin/login' : '/user/login';
};