import { useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../../store/hooks';
import { removeFromCart, updateQuantity } from '../../store/cartSlice';

const TAX_RATE = 0.08;

export default function Cart() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const items = useAppSelector((s) => s.cart.items);

  const subtotal = items.reduce((sum, i) => sum + i.product.price * i.quantity, 0);
  const tax = subtotal * TAX_RATE;
  const total = subtotal + tax;

  if (items.length === 0) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-12 text-center text-gray-500">
        Your cart is empty.
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto px-4 py-6">
      <h1 className="text-2xl font-bold mb-4">Your Cart</h1>
      <div className="bg-white rounded-lg shadow-sm divide-y">
        {items.map((item) => (
          <div key={item.product.id} className="p-4 flex items-center gap-4">
            <div className="flex-1">
              <h3 className="font-semibold">{item.product.name}</h3>
              <p className="text-sm text-gray-500">${item.product.price.toFixed(2)} each</p>
            </div>
            <input
              type="number"
              min={1}
              value={item.quantity}
              onChange={(e) =>
                dispatch(
                  updateQuantity({
                    productId: item.product.id,
                    quantity: parseInt(e.target.value) || 1,
                  })
                )
              }
              className="w-16 border rounded-md px-2 py-1"
            />
            <span className="w-24 text-right font-semibold">
              ${(item.product.price * item.quantity).toFixed(2)}
            </span>
            <button
              onClick={() => dispatch(removeFromCart(item.product.id))}
              className="text-danger hover:underline text-sm"
            >
              Remove
            </button>
          </div>
        ))}
      </div>

      <div className="bg-white rounded-lg shadow-sm mt-4 p-4 space-y-2">
        <div className="flex justify-between text-sm">
          <span>Subtotal</span>
          <span>${subtotal.toFixed(2)}</span>
        </div>
        <div className="flex justify-between text-sm">
          <span>Tax (8%)</span>
          <span>${tax.toFixed(2)}</span>
        </div>
        <div className="flex justify-between font-bold text-lg border-t pt-2">
          <span>Total</span>
          <span>${total.toFixed(2)}</span>
        </div>
        <button
          onClick={() => navigate('/checkout')}
          className="w-full bg-primary text-white py-2 rounded-md hover:bg-primary-dark mt-2"
        >
          Proceed to Checkout
        </button>
      </div>
    </div>
  );
}
