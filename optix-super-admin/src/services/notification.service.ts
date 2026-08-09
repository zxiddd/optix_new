import axios from 'axios';

const API_URL = 'https://api.optixapp.in/api/v1/super-admin';
const auth = () => ({ headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });

export const notificationService = {
  sendNotification: async (payload: {
    targetType: 'ALL' | 'BUSINESS' | 'PLAN';
    businessId?: string;
    planId?: string;
    title: string;
    message: string;
    type?: string;
    severity?: string;
  }) => {
    const { data } = await axios.post(`${API_URL}/notifications/send`, payload, auth());
    return data;
  },
};
