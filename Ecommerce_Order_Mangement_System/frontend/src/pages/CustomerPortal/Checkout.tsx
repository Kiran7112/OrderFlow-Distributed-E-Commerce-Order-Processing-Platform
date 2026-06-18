import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../../store/hooks';
import { clearCart } from '../../store/cartSlice';
import { useCreateOrderMutation } from '../../services/api';
import { DEMO_CUSTOMER_ID } from '../../data/mockProducts';

export default function Checkout() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const items = useAppSelector((s) => s.cart.items);
  const [createOrder, { isLoading }] = useCreateOrderMutation();
  const [address, setAddress] = useState({ name: '', line1: '', city: '', postalCode: '' });
  const [error, setError] = useState('');

  const total = items.reduce((sum, i) => sum + i.product.price * i.quantity, 0);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      const order = await createOrder({
        customerId: DEMO_CUSTOMER_ID,
        items: items.map((i) => ({
          productId: i.product.id,
          quantity: i.quantity,
          unitPrice: i.product.price,
        })),
      }).unwrap();
      dispatch(clearCart());
      navigate(`/track/${order.id}`);
    } catch {
      setError('Failed to place order. Please try again.');
    }
  };

  if (items.length === 0) {
    return <div className="max-w-3xl mx-auto px-4 py-12 text-center text-gray-500">Cart is empty.</div>;
  }

  return (
    <div className="max-w-3xl mx-auto px-4 py-6">
      <h1 className="text-2xl font-bold mb-4">Checkout</h1>
      <form onSubmit={handleSubmit} className="bg-white rounded-lg shadow-sm p-4 space-y-4">
        <h2 className="font-semibold">Shipping Address</h2>
        <input
          required
          placeholder="Full Name"
          value={address.name}
          onChange={(e) => setAddress({ ...address, name: e.target.value })}
          className="w-full border rounded-md px-3 py-2"
        />
        <input
          required
          placeholder="Address Line 1"
          value={address.line1}
          onChange={(e) => setAddress({ ...address, line1: e.target.value })}
          className="w-full border rounded-md px-3 py-2"
        />
        <div className="flex gap-4">
          <input
            required
            placeholder="City"
            value={address.city}
            onChange={(e) => setAddress({ ...address, city: e.target.value })}
            className="flex-1 border rounded-md px-3 py-2"
          />
          <input
            required
            placeholder="Postal Code"
            value={address.postalCode}
            onChange={(e) => setAddress({ ...address, postalCode: e.target.value })}
            className="flex-1 border rounded-md px-3 py-2"
          />
        </div>

        <div className="border-t pt-4 flex justify-between font-bold text-lg">
          <span>Order Total</span>
          <span>${total.toFixed(2)}</span>
        </div>

        {error && <p className="text-danger text-sm">{error}</p>}

        <button
          type="submit"
          disabled={isLoading}
          className="w-full bg-primary text-white py-2 rounded-md hover:bg-primary-dark disabled:bg-gray-300"
        >
          {isLoading ? 'Placing Order...' : 'Place Order'}
        </button>
      </form>
    </div>
  );
}
