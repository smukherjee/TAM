import axios from 'axios';

const API_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const getTurnaroundEvents = async () => {
  const response = await api.get('/turnaround/events');
  return response.data;
};

export default api;
