import axios from 'axios';
import { useUserStore } from '../stores/userStore';

const API_URL = 'http://localhost:10086/api/admin/';

// Create an Axios instance for authenticated requests
const adminApi = axios.create({
  baseURL: API_URL,
});

// Add a request interceptor to include the auth token from the user store
adminApi.interceptors.request.use(
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

const getUsers = () => {
    return adminApi.get('users');
};

const updateUserRole = (userId: number, role: string) => {
    return adminApi.put(`users/${userId}/role`, { role });
};

const deleteUser = (userId: number) => {
    return adminApi.delete(`users/${userId}`);
};

const adminService = {
    getUsers,
    updateUserRole,
    deleteUser,
};

export default adminService; 