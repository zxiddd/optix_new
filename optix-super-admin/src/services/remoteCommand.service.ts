import axios from 'axios';

const API_URL = 'https://api.optixapp.in/api/v1/super-admin';
const auth = () => ({ headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });

export const remoteCommandService = {
  sendCommand: async (body: {
    command: string;
    businessId: string;
    deviceId?: string;
    payload?: any;
  }) => {
    const { data } = await axios.post(`${API_URL}/remote-command`, body, auth());
    return data;
  },
  executeBulk: async (body: {
    action: string;
    businessIds: string[];
    payload?: any;
  }) => {
    const { data } = await axios.post(`${API_URL}/bulk-action`, body, auth());
    return data;
  },
};
