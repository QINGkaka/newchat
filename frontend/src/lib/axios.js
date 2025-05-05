import axios from "axios";

const axiosInstance = axios.create({
  baseURL: "http://localhost:19096",
  withCredentials: true,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  }
});

axiosInstance.interceptors.request.use(
  config => {
    const authUser = JSON.parse(localStorage.getItem('authUser'));
    if (authUser?.token) {
      config.headers.Authorization = `Bearer ${authUser.token}`;
    }
    console.log('Request config:', {
      url: config.url,
      method: config.method,
      headers: config.headers,
      data: config.data
    });
    return config;
  },
  error => {
    console.error('Request error:', error);
    return Promise.reject(error);
  }
);

axiosInstance.interceptors.response.use(
  response => {
    console.log('Response:', response.data);
    return response;
  },
  error => {
    console.error('Response error:', error.response || error);
    if (error.response?.status === 401) {
      localStorage.removeItem('authUser');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default axiosInstance;