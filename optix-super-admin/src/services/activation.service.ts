import axios from 'axios';

const API_URL = 'https://api.optixapp.in/api/v1/super-admin';
const auth = () => ({ headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });

export const activationService = {
  getAll: async (params: Record<string, any> = {}) => {
    const { data } = await axios.get(`${API_URL}/activation-codes`, { params, ...auth() });
    return data;
  },
  create: async (body: {
    planId: string;
    billingCycle: string;
    maxUses: number;
    countryRestriction?: string;
    expiresAt?: string;
    notes?: string;
  }) => {
    const { data } = await axios.post(`${API_URL}/activation-codes`, body, auth());
    return data;
  },
  bulkCreate: async (body: {
    count: number;
    planId: string;
    billingCycle: string;
    maxUses: number;
    countryRestriction?: string;
    expiresAt?: string;
    notes?: string;
  }) => {
    const { data } = await axios.post(`${API_URL}/activation-codes/bulk`, body, auth());
    return data;
  },
  deactivate: async (id: string) => {
    const { data } = await axios.patch(`${API_URL}/activation-codes/${id}/deactivate`, {}, auth());
    return data;
  },
  delete: async (id: string) => {
    await axios.delete(`${API_URL}/activation-codes/${id}`, auth());
  },
};
