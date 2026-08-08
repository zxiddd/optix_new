import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { businessService } from '@/services/business.service';

export const useBusinesses = (params: any) => {
  return useQuery({
    queryKey: ['businesses', params],
    queryFn: () => businessService.getBusinesses(params),
    placeholderData: (previousData: any) => previousData,
  });
};

export const useBusinessDetail = (id: string) => {
  return useQuery({
    queryKey: ['business', id],
    queryFn: () => businessService.getBusinessDetail(id),
    enabled: !!id,
  });
};

export const useUpdateBusinessStatus = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: string }) =>
      businessService.updateStatus(id, status),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['businesses'] });
      queryClient.invalidateQueries({ queryKey: ['business', variables.id] });
    },
  });
};
