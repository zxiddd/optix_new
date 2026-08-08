import React from 'react';
import { AlertTriangle, X } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

interface ConfirmDialogProps {
  open: boolean;
  title: string;
  description: string;
  confirmLabel?: string;
  confirmVariant?: 'danger' | 'warning' | 'primary';
  onConfirm: () => void;
  onCancel: () => void;
  loading?: boolean;
}

const ConfirmDialog: React.FC<ConfirmDialogProps> = ({
  open, title, description, confirmLabel = 'Confirm',
  confirmVariant = 'danger', onConfirm, onCancel, loading
}) => {
  const btnColors = {
    danger: 'bg-red-500 hover:bg-red-600 text-white',
    warning: 'bg-orange-500 hover:bg-orange-600 text-black',
    primary: 'bg-primary hover:bg-primary/90 text-black',
  };

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className="fixed inset-0 z-50 flex items-center justify-center"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
        >
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={onCancel} />
          <motion.div
            className="relative bg-card border border-border rounded-2xl p-6 w-full max-w-md shadow-2xl"
            initial={{ scale: 0.95, y: 10 }}
            animate={{ scale: 1, y: 0 }}
            exit={{ scale: 0.95, y: 10 }}
          >
            <button onClick={onCancel} className="absolute top-4 right-4 p-1 rounded-lg hover:bg-muted transition-colors">
              <X size={16} className="text-muted-foreground" />
            </button>

            <div className="flex items-start gap-4">
              <div className={`p-3 rounded-xl ${confirmVariant === 'danger' ? 'bg-red-500/15' : confirmVariant === 'warning' ? 'bg-orange-500/15' : 'bg-primary/15'}`}>
                <AlertTriangle size={20} className={confirmVariant === 'danger' ? 'text-red-400' : confirmVariant === 'warning' ? 'text-orange-400' : 'text-primary'} />
              </div>
              <div className="flex-1">
                <h3 className="text-base font-bold text-foreground mb-1">{title}</h3>
                <p className="text-sm text-muted-foreground leading-relaxed">{description}</p>
              </div>
            </div>

            <div className="flex gap-3 mt-6 justify-end">
              <button
                onClick={onCancel}
                className="px-4 py-2 text-sm font-semibold rounded-xl border border-border hover:bg-muted transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={onConfirm}
                disabled={loading}
                className={`px-4 py-2 text-sm font-bold rounded-xl transition-colors disabled:opacity-50 ${btnColors[confirmVariant]}`}
              >
                {loading ? 'Processing...' : confirmLabel}
              </button>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
};

export default ConfirmDialog;
