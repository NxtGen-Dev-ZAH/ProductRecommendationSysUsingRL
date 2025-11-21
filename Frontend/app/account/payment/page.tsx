'use client';

import { useAuth } from '../../context/AuthContext';
import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { 
  FaPlus, 
  FaEdit, 
  FaTrash, 
  FaStar, 
  FaRegStar,
  FaCreditCard,
  FaTimes,
  FaPaypal 
} from 'react-icons/fa';
import AccountNav from '../../../components/account/AccountNav';
import { Button } from '../../../components/ui/button';
import { Input } from '../../../components/ui/input';
import { 
  getPaymentMethods, 
  addPaymentMethod, 
  updatePaymentMethod, 
  deletePaymentMethod, 
  setDefaultPaymentMethod,
  PaymentMethod 
} from '../../api/services/user';

const paymentMethodSchema = z.object({
  type: z.enum(['credit_card', 'debit_card', 'paypal'], {
    required_error: 'Payment method type is required',
  }),
  cardNumber: z.string().optional(),
  expiryMonth: z.number().min(1).max(12).optional(),
  expiryYear: z.number().min(new Date().getFullYear()).optional(),
  cardholderName: z.string().optional(),
}).refine((data) => {
  if (data.type === 'credit_card' || data.type === 'debit_card') {
    return data.cardNumber && data.expiryMonth && data.expiryYear && data.cardholderName;
  }
  return true;
}, {
  message: 'Card details are required for credit/debit cards',
});

type PaymentMethodFormValues = z.infer<typeof paymentMethodSchema>;

export default function PaymentMethodsPage() {
  const { isAuthenticated, loading: authLoading } = useAuth();
  const router = useRouter();
  const [paymentMethods, setPaymentMethods] = useState<PaymentMethod[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingMethod, setEditingMethod] = useState<PaymentMethod | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors },
  } = useForm<PaymentMethodFormValues>({
    resolver: zodResolver(paymentMethodSchema),
  });

  const watchedType = watch('type');

  useEffect(() => {
    // Wait for auth to finish loading before checking authentication
    if (authLoading) {
      return;
    }

    if (!isAuthenticated) {
      router.push('/auth/login');
      return;
    }

    loadPaymentMethods();
  }, [isAuthenticated, authLoading, router]);

  const loadPaymentMethods = async () => {
    try {
      setLoading(true);
      const data = await getPaymentMethods();
      setPaymentMethods(data);
    } catch (error) {
      console.error('Error loading payment methods:', error);
      setMessage({ type: 'error', text: 'Failed to load payment methods' });
    } finally {
      setLoading(false);
    }
  };

  const handleAddNew = () => {
    setEditingMethod(null);
    reset();
    setShowForm(true);
    setMessage(null);
  };

  const handleEdit = (method: PaymentMethod) => {
    setEditingMethod(method);
    setValue('type', method.type);
    setValue('cardholderName', method.cardholderName || '');
    setValue('expiryMonth', method.expiryMonth);
    setValue('expiryYear', method.expiryYear);
    setShowForm(true);
    setMessage(null);
  };

  const handleCancel = () => {
    setShowForm(false);
    setEditingMethod(null);
    reset();
    setMessage(null);
  };

  const onSubmit = async (data: PaymentMethodFormValues) => {
    try {
      setSubmitting(true);
      setMessage(null);

      // Mask card number for display
      const maskedData = {
        ...data,
        cardNumber: data.cardNumber ? data.cardNumber.replace(/\d(?=\d{4})/g, '*') : undefined,
      };

      if (editingMethod) {
        await updatePaymentMethod(editingMethod.id!, maskedData);
        setMessage({ type: 'success', text: 'Payment method updated successfully!' });
      } else {
        await addPaymentMethod(maskedData);
        setMessage({ type: 'success', text: 'Payment method added successfully!' });
      }

      await loadPaymentMethods();
      setShowForm(false);
      setEditingMethod(null);
      reset();
    } catch (error) {
      console.error('Error saving payment method:', error);
      setMessage({ type: 'error', text: 'Failed to save payment method. Please try again.' });
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Are you sure you want to delete this payment method?')) return;

    try {
      await deletePaymentMethod(id);
      await loadPaymentMethods();
      setMessage({ type: 'success', text: 'Payment method deleted successfully!' });
    } catch (error) {
      console.error('Error deleting payment method:', error);
      setMessage({ type: 'error', text: 'Failed to delete payment method. Please try again.' });
    }
  };

  const handleSetDefault = async (id: number) => {
    try {
      await setDefaultPaymentMethod(id);
      await loadPaymentMethods();
      setMessage({ type: 'success', text: 'Default payment method updated!' });
    } catch (error) {
      console.error('Error setting default payment method:', error);
      setMessage({ type: 'error', text: 'Failed to set default payment method. Please try again.' });
    }
  };

  const formatCardNumber = (value: string) => {
    // Remove all non-digits
    const digits = value.replace(/\D/g, '');
    // Add spaces every 4 digits
    return digits.replace(/(\d{4})(?=\d)/g, '$1 ');
  };

  // Show loading state while auth is being checked
  if (authLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading...</p>
        </div>
      </div>
    );
  }

  // Show loading state if not authenticated
  if (!isAuthenticated) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-gray-50 min-h-screen">
      <div className="container mx-auto px-4 py-8">
        <div className="max-w-6xl mx-auto">
          <div className="mb-6">
            <h1 className="text-2xl font-bold text-gray-900">Payment Methods</h1>
            <p className="text-gray-600">Manage your payment cards and methods</p>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
            {/* Navigation Sidebar */}
            <div className="lg:col-span-1">
              <AccountNav activeItem="payment" />
            </div>

            {/* Main Content */}
            <div className="lg:col-span-3">
              {/* Message Display */}
              {message && (
                <div className={`mb-6 p-4 rounded-md ${
                  message.type === 'success' 
                    ? 'bg-green-50 text-green-700 border border-green-200' 
                    : 'bg-red-50 text-red-700 border border-red-200'
                }`}>
                  {message.text}
                </div>
              )}

              {/* Add New Payment Method Button */}
              {!showForm && (
                <div className="mb-6">
                  <Button onClick={handleAddNew} className="inline-flex items-center gap-2">
                    <FaPlus className="w-4 h-4" />
                    Add Payment Method
                  </Button>
                </div>
              )}

              {/* Payment Method Form */}
              {showForm && (
                <div className="bg-white rounded-lg shadow-sm border p-6 mb-6">
                  <div className="flex items-center justify-between mb-4">
                    <h3 className="text-lg font-medium text-gray-900">
                      {editingMethod ? 'Edit Payment Method' : 'Add New Payment Method'}
                    </h3>
                    <button
                      onClick={handleCancel}
                      className="text-gray-400 hover:text-gray-600"
                    >
                      <FaTimes className="w-5 h-5" />
                    </button>
                  </div>

                  <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Payment Method Type
                      </label>
                      <select
                        {...register('type')}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                      >
                        <option value="">Select payment method</option>
                        <option value="credit_card">Credit Card</option>
                        <option value="debit_card">Debit Card</option>
                        <option value="paypal">PayPal</option>
                      </select>
                      {errors.type && (
                        <p className="mt-1 text-sm text-red-600">{errors.type.message}</p>
                      )}
                    </div>

                    {(watchedType === 'credit_card' || watchedType === 'debit_card') && (
                      <>
                        <div>
                          <label className="block text-sm font-medium text-gray-700 mb-1">
                            Cardholder Name
                          </label>
                          <Input
                            {...register('cardholderName')}
                            placeholder="Enter cardholder name"
                            error={errors.cardholderName?.message}
                          />
                        </div>

                        <div>
                          <label className="block text-sm font-medium text-gray-700 mb-1">
                            Card Number
                          </label>
                          <Input
                            {...register('cardNumber')}
                            placeholder="1234 5678 9012 3456"
                            maxLength={19}
                            onChange={(e) => {
                              const formatted = formatCardNumber(e.target.value);
                              e.target.value = formatted;
                            }}
                            error={errors.cardNumber?.message}
                          />
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                          <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">
                              Expiry Month
                            </label>
                            <Input
                              {...register('expiryMonth', { valueAsNumber: true })}
                              type="number"
                              min="1"
                              max="12"
                              placeholder="MM"
                              error={errors.expiryMonth?.message}
                            />
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">
                              Expiry Year
                            </label>
                            <Input
                              {...register('expiryYear', { valueAsNumber: true })}
                              type="number"
                              min={new Date().getFullYear()}
                              placeholder="YYYY"
                              error={errors.expiryYear?.message}
                            />
                          </div>
                        </div>
                      </>
                    )}

                    <div className="flex justify-end gap-4 pt-4">
                      <Button
                        type="button"
                        variant="outline"
                        onClick={handleCancel}
                      >
                        Cancel
                      </Button>
                      <Button
                        type="submit"
                        loading={submitting}
                        disabled={submitting}
                      >
                        {submitting ? 'Saving...' : editingMethod ? 'Update Payment Method' : 'Add Payment Method'}
                      </Button>
                    </div>
                  </form>
                </div>
              )}

              {/* Payment Methods List */}
              <div className="space-y-4">
                {loading ? (
                  <div className="bg-white rounded-lg shadow-sm border p-8 text-center">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
                    <p className="text-gray-600">Loading payment methods...</p>
                  </div>
                ) : paymentMethods.length === 0 ? (
                  <div className="bg-white rounded-lg shadow-sm border p-8 text-center">
                    <FaCreditCard className="w-16 h-16 text-gray-300 mx-auto mb-4" />
                    <h3 className="text-lg font-medium text-gray-900 mb-2">No payment methods found</h3>
                    <p className="text-gray-600 mb-4">
                      Add your first payment method to get started.
                    </p>
                    <Button onClick={handleAddNew}>
                      <FaPlus className="w-4 h-4 mr-2" />
                      Add Payment Method
                    </Button>
                  </div>
                ) : (
                  paymentMethods.map((method) => (
                    <PaymentMethodCard
                      key={method.id}
                      method={method}
                      onEdit={handleEdit}
                      onDelete={handleDelete}
                      onSetDefault={handleSetDefault}
                    />
                  ))
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

interface PaymentMethodCardProps {
  method: PaymentMethod;
  onEdit: (method: PaymentMethod) => void;
  onDelete: (id: number) => void;
  onSetDefault: (id: number) => void;
}

function PaymentMethodCard({ method, onEdit, onDelete, onSetDefault }: PaymentMethodCardProps) {
  const getPaymentIcon = (type: string) => {
    switch (type) {
      case 'paypal':
        return <FaPaypal className="w-6 h-6 text-blue-600" />;
      default:
        return <FaCreditCard className="w-6 h-6 text-gray-600" />;
    }
  };

  const getPaymentTypeLabel = (type: string) => {
    switch (type) {
      case 'credit_card':
        return 'Credit Card';
      case 'debit_card':
        return 'Debit Card';
      case 'paypal':
        return 'PayPal';
      default:
        return type;
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-sm border p-6">
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-4">
          {getPaymentIcon(method.type)}
          <div>
            <div className="flex items-center gap-2 mb-1">
              <h3 className="font-medium text-gray-900">
                {getPaymentTypeLabel(method.type)}
              </h3>
              {method.isDefault && (
                <span className="inline-flex items-center gap-1 px-2 py-1 text-xs font-medium text-blue-700 bg-blue-100 rounded-full">
                  <FaStar className="w-3 h-3" />
                  Default
                </span>
              )}
            </div>
            {method.type !== 'paypal' && (
              <div className="text-sm text-gray-600 space-y-1">
                <p>{method.cardholderName}</p>
                <p>**** **** **** {method.cardNumber?.slice(-4)}</p>
                <p>Expires {method.expiryMonth?.toString().padStart(2, '0')}/{method.expiryYear}</p>
              </div>
            )}
          </div>
        </div>
        
        <div className="flex items-center gap-2">
          {!method.isDefault && (
            <button
              onClick={() => onSetDefault(method.id!)}
              className="text-gray-400 hover:text-blue-600 transition-colors"
              title="Set as default"
            >
              <FaRegStar className="w-4 h-4" />
            </button>
          )}
          <button
            onClick={() => onEdit(method)}
            className="text-gray-400 hover:text-blue-600 transition-colors"
            title="Edit payment method"
          >
            <FaEdit className="w-4 h-4" />
          </button>
          <button
            onClick={() => onDelete(method.id!)}
            className="text-gray-400 hover:text-red-600 transition-colors"
            title="Delete payment method"
          >
            <FaTrash className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
} 