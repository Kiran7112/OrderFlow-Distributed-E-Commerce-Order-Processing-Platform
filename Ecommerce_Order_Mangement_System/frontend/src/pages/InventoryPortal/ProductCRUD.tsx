import { useState } from 'react';
import { MOCK_PRODUCTS } from '../../data/mockProducts';
import { useUpdateStockMutation, useGetStockQuery } from '../../services/api';
import type { Product } from '../../types';

function ProductRow({ product }: { product: Product }) {
  const { data: stock } = useGetStockQuery(product.id, { pollingInterval: 5000 });
  const [updateStock, { isLoading }] = useUpdateStockMutation();
  const [qty, setQty] = useState('');
  const [reorder, setReorder] = useState('');
  const [msg, setMsg] = useState('');

  const handleUpdate = async () => {
    setMsg('');
    try {
      await updateStock({
        productId: product.id,
        availableQty: parseInt(qty) || stock?.availableQty || 0,
        reorderLevel: parseInt(reorder) || stock?.reorderLevel || 10,
      }).unwrap();
      setMsg('Updated!');
      setQty('');
      setReorder('');
    } catch {
      setMsg('Failed to update');
    }
  };

  return (
    <tr className="border-b">
      <td className="py-2">
        <div className="font-semibold">{product.name}</div>
        <div className="text-xs text-gray-500">{product.sku}</div>
      </td>
      <td className="py-2 text-right">{stock?.availableQty ?? '-'}</td>
      <td className="py-2 text-right">{stock?.reservedQty ?? '-'}</td>
      <td className="py-2">
        <div className="flex gap-2 items-center justify-end">
          <input
            type="number"
            placeholder="Qty"
            value={qty}
            onChange={(e) => setQty(e.target.value)}
            className="w-20 border rounded px-2 py-1 text-sm"
          />
          <input
            type="number"
            placeholder="Reorder"
            value={reorder}
            onChange={(e) => setReorder(e.target.value)}
            className="w-20 border rounded px-2 py-1 text-sm"
          />
          <button
            onClick={handleUpdate}
            disabled={isLoading}
            className="bg-primary text-white px-3 py-1 rounded text-sm hover:bg-primary-dark disabled:bg-gray-300"
          >
            Update
          </button>
          {msg && <span className="text-xs text-gray-500">{msg}</span>}
        </div>
      </td>
    </tr>
  );
}

export default function ProductCRUD() {
  return (
    <div className="bg-white rounded-lg shadow-sm p-4">
      <h2 className="font-semibold mb-4">Product Stock Management</h2>
      <table className="w-full text-sm">
        <thead>
          <tr className="text-left text-gray-500 border-b">
            <th className="py-2">Product</th>
            <th className="py-2 text-right">Available</th>
            <th className="py-2 text-right">Reserved</th>
            <th className="py-2 text-right">Update Stock</th>
          </tr>
        </thead>
        <tbody>
          {MOCK_PRODUCTS.map((p) => (
            <ProductRow key={p.id} product={p} />
          ))}
        </tbody>
      </table>
    </div>
  );
}
