import axios from 'axios';

const API_URL = 'https://api.optixapp.in/api/v1/super-admin';
const auth = () => ({ headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });

export const deviceService = {
  getDevices: async (params: Record<string, any> = {}) => {
    const { data } = await axios.get(`${API_URL}/devices`, { params, ...auth() });
    return data;
  },
  remoteLogout: async (deviceId: string) => {
    const { data } = await axios.post(`${API_URL}/devices/${deviceId}/remote-logout`, {}, auth());
    return data;
  },
};
