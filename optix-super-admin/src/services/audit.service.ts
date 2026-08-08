import axios from 'axios';

const API_URL = 'https://api.optixapp.in/api/v1/super-admin';
const auth = () => ({ headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });

export const auditService = {
  getAll: async (params: Record<string, any> = {}) => {
    const { data } = await axios.get(`${API_URL}/audit-logs`, { params, ...auth() });
    return data;
  },
};
