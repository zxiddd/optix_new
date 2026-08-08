import axios from 'axios';

const API_URL = 'https://api.optixapp.in/api/v1/super-admin';
const auth = () => ({ headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });

export const subscriptionService = {
  getAll: async (params: Record<string, any> = {}) => {
    const { data } = await axios.get(`${API_URL}/subscriptions`, { params, ...auth() });
    return data;
  },
  getDetail: async (id: string) => {
    const { data } = await axios.get(`${API_URL}/subscriptions/${id}`, auth());
    return data;
  },
  changePlan: async (businessId: string, planId: string, billingCycle: string) => {
    const { data } = await axios.patch(`${API_URL}/subscriptions/${businessId}/plan`, { planId, billingCycle }, auth());
    return data;
  },
  extend: async (businessId: string, days: number) => {
    const { data } = await axios.patch(`${API_URL}/subscriptions/${businessId}/extend`, { days }, auth());
    return data;
  },
  updateStatus: async (businessId: string, status: string) => {
    const { data } = await axios.patch(`${API_URL}/subscriptions/${businessId}/status`, { status }, auth());
    return data;
  },
  resetTrial: async (businessId: string) => {
    const { data } = await axios.post(`${API_URL}/subscriptions/${businessId}/reset-trial`, {}, auth());
    return data;
  },
};
