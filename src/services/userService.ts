import axios from 'axios';
import { useUserStore } from '../stores/userStore';

const API_URL = 'http://localhost:10086/api/user/';

// Create an Axios instance for authenticated requests
const authApi = axios.create({
  baseURL: API_URL,
});

// Add a request interceptor to include the auth token
authApi.interceptors.request.use(
  config => {
    const userStore = useUserStore();
    const token = userStore.token;
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

const updatePassword = (oldPassword: string, newPassword: string) => {
    return authApi.post('password', {
        oldPassword,
        newPassword,
    });
};

const updateEmail = (password: string, newEmail: string) => {
    return authApi.post('email', {
        password,
        newEmail,
    });
};

const userService = {
    updatePassword,
    updateEmail,
};

export default userService; 