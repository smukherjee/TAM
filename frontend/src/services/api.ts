import axios from 'axios';

// Use relative URL to leverage Vite Proxy in Dev and Nginx Proxy in Prod
const API_URL = '/api';

const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const userStr = localStorage.getItem('user');
  if (userStr) {
    try {
      const user = JSON.parse(userStr);
      if (user.icaoCode) {
        config.headers['X-User-ICAO'] = user.icaoCode;
      }
    } catch (e) {
      console.error("Failed to parse user from local storage", e);
    }
  }
  return config;
});

export const loginUser = async (credentials: any) => {
  const response = await api.post('/auth/login', credentials);
  return response.data;
};

export const getTurnaroundEvents = async () => {
  const response = await api.get('/turnaround/events');
  return response.data;
};

export default api;
