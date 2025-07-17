import axios from 'axios';

// 创建axios实例
const api = axios.create({
  baseURL: 'http://localhost:10086/api',
  timeout: 5000
});

// 请求拦截器
api.interceptors.request.use(
  config => {
    const userStr = localStorage.getItem('user');
    if (userStr) {
      const user = JSON.parse(userStr);
      if (user && user.accessToken) {
        config.headers['Authorization'] = 'Bearer ' + user.accessToken;
      }
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// 响应拦截器
api.interceptors.response.use(
  response => {
    // 拦截器直接返回 `response.data`，后续调用链可以直接使用数据
    return response.data;
  },
  error => {
    console.error('API请求错误', error);
    return Promise.reject(error);
  }
);

export { api }; 