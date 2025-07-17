import { User } from '../types';
import { api } from '../api';

const register = (username: string, email: string, password: string, role: string, verificationCode: string): Promise<User> => {
    return api.post('/auth/register', {
        username,
        email,
        password,
        role: role ? [role] : ['user'], // Ensure role is an array
        verificationCode
    });
};

const login = (username: string, password: string): Promise<User> => {
    return api.post('/auth/login', {
        username,
        password,
    });
};

const logout = () => {
    localStorage.removeItem('user');
};

const sendVerificationCode = (email: string) => {
    return api.post('/auth/send-code', {
        email
    });
};

const authService = {
    register,
    login,
    logout,
    sendVerificationCode,
};

export default authService; 