import axios from 'axios';

const API_URL = 'https://api.optixapp.in/api/v1/super-admin';

const auth = () => ({ headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });

export const paymentService = {
  getAll: async (params: Record<string, any> = {}) => {
    const { data } = await axios.get(`${API_URL}/payments`, { params, ...auth() });
    return data;
  },
  getDetail: async (id: string) => {
    const { data } = await axios.get(`${API_URL}/payments/${id}`, auth());
    return data;
  },
  refund: async (id: string, reason?: string, partial?: number) => {
    const { data } = await axios.post(`${API_URL}/payments/${id}/refund`, { reason, partial }, auth());
    return data;
  },
  getRevenueStats: async () => {
    const { data } = await axios.get(`${API_URL}/revenue-stats`, auth());
    return data;
  },
};
