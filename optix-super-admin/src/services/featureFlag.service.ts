import axios from 'axios';

const API_URL = 'https://api.optixapp.in/api/v1/super-admin';
const auth = () => ({ headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });

export const featureFlagService = {
  getAll: async (params: Record<string, any> = {}) => {
    const { data } = await axios.get(`${API_URL}/feature-flags`, { params, ...auth() });
    return data;
  },
  upsert: async (body: {
    featureKey: string;
    status: 'ON' | 'OFF' | 'BETA' | 'MAINTENANCE';
    level: 'GLOBAL' | 'COUNTRY' | 'PLAN' | 'BUSINESS';
    target?: string;
    notes?: string;
    businessId?: string;
  }) => {
    const { data } = await axios.post(`${API_URL}/feature-flags`, body, auth());
    return data;
  },
  delete: async (id: string) => {
    const { data } = await axios.delete(`${API_URL}/feature-flags/${id}`, auth());
    return data;
  },
  getEffective: async (businessId?: string) => {
    const { data } = await axios.get(`${API_URL}/effective-feature-flags`, {
      params: { businessId },
      ...auth(),
    });
    return data;
  },
};
