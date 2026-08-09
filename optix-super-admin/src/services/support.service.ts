import axios from 'axios';

const API_URL = 'https://api.optixapp.in/api/v1/support';
const auth = () => ({ headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });

export const supportService = {
  getAllTickets: async (query?: { status?: string; search?: string }) => {
    const { data } = await axios.get(`${API_URL}/tickets/admin/all`, { params: query, ...auth() });
    return data;
  },

  getTicketDetails: async (id: string) => {
    const { data } = await axios.get(`${API_URL}/tickets/${id}`, auth());
    return data;
  },

  addMessage: async (id: string, senderName: string, message: string) => {
    const { data } = await axios.post(`${API_URL}/tickets/${id}/messages`, {
      senderType: 'ADMIN',
      senderName,
      message,
    }, auth());
    return data;
  },

  updateStatus: async (id: string, status: string, priority?: string) => {
    const { data } = await axios.patch(`${API_URL}/tickets/${id}/status`, { status, priority }, auth());
    return data;
  },
};
