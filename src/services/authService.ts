import axios from 'axios';

const API_URL = 'http://localhost:10086/api/auth/';

const register = (username: string, email: string, password: string, role: string) => {
    return axios.post(API_URL + 'register', {
        username,
        email,
        password,
        role
    });
};

const login = (username: string, password: string) => {
    return axios
        .post(API_URL + 'login', {
            username,
            password,
        })
        .then(response => {
            // Remove side-effect, just return the data
            // if (response.data.accessToken) {
            //     localStorage.setItem('user', JSON.stringify(response.data));
            // }
            return response.data;
        });
};

const logout = () => {
    localStorage.removeItem('user');
};

const authService = {
    register,
    login,
    logout,
};

export default authService; 