/**
 * API Client wrapper for consistent API calls across services.
 * Provides a unified interface for making HTTP requests.
 */

import api from './api';
import type { AxiosRequestConfig } from 'axios';

/**
 * Standardized API client for service modules.
 * Wraps the base axios instance with typed methods.
 */
export const apiClient = {
  /**
   * Make a GET request
   */
  get: async <T>(url: string, config?: AxiosRequestConfig): Promise<T> => {
    const response = await api.get<T>(url, config);
    return response.data;
  },

  /**
   * Make a POST request
   */
  post: async <T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> => {
    const response = await api.post<T>(url, data, config);
    return response.data;
  },

  /**
   * Make a PUT request
   */
  put: async <T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> => {
    const response = await api.put<T>(url, data, config);
    return response.data;
  },

  /**
   * Make a PATCH request
   */
  patch: async <T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> => {
    const response = await api.patch<T>(url, data, config);
    return response.data;
  },

  /**
   * Make a DELETE request
   */
  delete: async <T>(url: string, config?: AxiosRequestConfig): Promise<T> => {
    const response = await api.delete<T>(url, config);
    return response.data;
  }
};

export default apiClient;
