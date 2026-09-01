"use client";

import { useState } from "react";
import { MessageSquare, Send, DollarSign } from "lucide-react";
import DataTable, { StatusBadge } from "@/components/DataTable";

const mockRequests = [
  { id: 1, customer: "Amara Okafor", title: "Custom wedding gown with lace details", status: "OPEN", budgetMax: 85000, createdAt: "2026-09-01T08:00:00Z" },
  { id: 2, customer: "Tunde Bakare", title: "Agbada set for traditional engagement", status: "IN_REVIEW", budgetMax: 120000, createdAt: "2026-08-31T15:30:00Z" },
  { id: 3, customer: "Nneka Eze", title: "Matching family outfits for Christmas", status: "QUOTED", budgetMax: 200000, createdAt: "2026-08-30T10:00:00Z" },
  { id: 4, customer: "Chidi Nwosu", title: "Bespoke suit with unique fabric", status: "ACCEPTED", budgetMax: 95000, createdAt: "2026-08-28T09:15:00Z" },
];

const mockMessages = [
  { id: 1, senderId: 2, content: "Hello! I can work with the lace design you described. What color palette are you thinking?", createdAt: "2026-09-01T10:00:00Z" },
  { id: 2, senderId: 1, content: "I was thinking champagne and gold. Can you share some examples?", createdAt: "2026-09-01T10:30:00Z" },
  { id: 3, senderId: 2, content: "Of course! I'll send some inspiration boards shortly.", createdAt: "2026-09-01T11:00:00Z" },
];

export default function CustomOrdersPage() {
  const [selectedRequest, setSelectedRequest] = useState<any>(null);
  const [activeTab, setActiveTab] = useState<"details" | "chat">("details");
  const [newMessage, setNewMessage] = useState("");
  const [quoteForm, setQuoteForm] = useState({ price: "", estimatedDays: "", notes: "" });
  const [showQuoteForm, setShowQuoteForm] = useState(false);

  const formatCurrency = (amount: number) =>
    new Intl.NumberFormat("en-NG", { style: "currency", currency: "NGN" }).format(amount);

  const columns = [
    { key: "id", label: "#", render: (v: number) => `#${v}`, sortable: true },
    { key: "customer", label: "Customer", sortable: true },
    { key: "title", label: "Request", sortable: true },
    { key: "status", label: "Status", render: (v: string) => <StatusBadge status={v} /> },
    {
      key: "budgetMax",
      label: "Budget",
      render: (v: number | null) => (v ? formatCurrency(v) : "—"),
    },
    {
      key: "createdAt",
      label: "Date",
      sortable: true,
      render: (v: string) => new Date(v).toLocaleDateString(),
    },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold text-gray-900">Custom Orders</h2>
        <p className="text-sm text-gray-500 mt-1">Manage custom requests, quotes, and customer conversations</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-xl border border-gray-200 p-6">
          <DataTable columns={columns} data={mockRequests} onRowClick={setSelectedRequest} />
        </div>

        {selectedRequest ? (
          <div className="bg-white rounded-xl border border-gray-200 flex flex-col" style={{ minHeight: 480 }}>
            <div className="flex items-center justify-between p-4 border-b border-gray-200">
              <div>
                <h3 className="font-semibold text-gray-900">Request #{selectedRequest.id}</h3>
                <p className="text-sm text-gray-500 truncate max-w-xs">{selectedRequest.title}</p>
              </div>
              <StatusBadge status={selectedRequest.status} />
            </div>

            <div className="flex border-b border-gray-200">
              <button
                onClick={() => setActiveTab("details")}
                className={`flex-1 py-2.5 text-sm font-medium transition-colors ${
                  activeTab === "details" ? "text-indigo-600 border-b-2 border-indigo-600" : "text-gray-500 hover:text-gray-700"
                }`}
              >
                Details
              </button>
              <button
                onClick={() => setActiveTab("chat")}
                className={`flex-1 py-2.5 text-sm font-medium transition-colors ${
                  activeTab === "chat" ? "text-indigo-600 border-b-2 border-indigo-600" : "text-gray-500 hover:text-gray-700"
                }`}
              >
                <MessageSquare className="w-4 h-4 inline mr-1" /> Chat
              </button>
            </div>

            {activeTab === "details" ? (
              <div className="p-4 space-y-4 flex-1">
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div><span className="text-gray-500">Customer</span><p className="font-medium">{selectedRequest.customer}</p></div>
                  <div><span className="text-gray-500">Budget</span><p className="font-medium">{formatCurrency(selectedRequest.budgetMax)}</p></div>
                  <div className="col-span-2"><span className="text-gray-500">Description</span><p className="mt-1">{selectedRequest.title}</p></div>
                </div>

                {selectedRequest.status === "OPEN" || selectedRequest.status === "IN_REVIEW" ? (
                  <button
                    onClick={() => setShowQuoteForm(true)}
                    className="w-full flex items-center justify-center gap-2 py-2.5 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700 transition-colors"
                  >
                    <DollarSign className="w-4 h-4" /> Send Quote
                  </button>
                ) : selectedRequest.status === "QUOTED" ? (
                  <div className="bg-emerald-50 border border-emerald-200 rounded-lg p-3 text-sm text-emerald-700">
                    Quote sent — awaiting customer response
                  </div>
                ) : selectedRequest.status === "ACCEPTED" ? (
                  <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 text-sm text-blue-700">
                    Quote accepted — ready to convert to order
                  </div>
                ) : null}
              </div>
            ) : (
              <div className="flex flex-col flex-1">
                <div className="flex-1 p-4 space-y-3 overflow-auto" style={{ maxHeight: 300 }}>
                  {mockMessages.map((msg) => (
                    <div
                      key={msg.id}
                      className={`flex ${msg.senderId === 2 ? "justify-end" : "justify-start"}`}
                    >
                      <div
                        className={`max-w-xs px-4 py-2.5 rounded-2xl text-sm ${
                          msg.senderId === 2
                            ? "bg-indigo-600 text-white rounded-br-md"
                            : "bg-gray-100 text-gray-900 rounded-bl-md"
                        }`}
                      >
                        {msg.content}
                        <div className={`text-xs mt-1 ${msg.senderId === 2 ? "text-indigo-200" : "text-gray-400"}`}>
                          {new Date(msg.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
                <div className="p-3 border-t border-gray-200">
                  <div className="flex gap-2">
                    <input
                      type="text"
                      value={newMessage}
                      onChange={(e) => setNewMessage(e.target.value)}
                      placeholder="Type a message..."
                      className="flex-1 px-4 py-2 border border-gray-300 rounded-full text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
                    />
                    <button className="p-2 bg-indigo-600 text-white rounded-full hover:bg-indigo-700 transition-colors">
                      <Send className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </div>
            )}
          </div>
        ) : (
          <div className="bg-white rounded-xl border border-gray-200 flex items-center justify-center text-gray-400 text-sm" style={{ minHeight: 480 }}>
            Select a request to view details
          </div>
        )}
      </div>

      {showQuoteForm && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl w-full max-w-md mx-4 p-6">
            <h3 className="text-lg font-semibold mb-4">Send Quote</h3>
            <form className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Price (NGN)</label>
                <input
                  type="number"
                  value={quoteForm.price}
                  onChange={(e) => setQuoteForm({ ...quoteForm, price: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none"
                  placeholder="0"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Estimated Days</label>
                <input
                  type="number"
                  value={quoteForm.estimatedDays}
                  onChange={(e) => setQuoteForm({ ...quoteForm, estimatedDays: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none"
                  placeholder="14"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Notes</label>
                <textarea
                  rows={3}
                  value={quoteForm.notes}
                  onChange={(e) => setQuoteForm({ ...quoteForm, notes: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none resize-none"
                  placeholder="Material details, delivery terms..."
                />
              </div>
              <div className="flex justify-end gap-3 pt-2">
                <button type="button" onClick={() => setShowQuoteForm(false)} className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200">
                  Cancel
                </button>
                <button type="submit" className="px-4 py-2 text-sm font-medium text-white bg-indigo-600 rounded-lg hover:bg-indigo-700">
                  Send Quote
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
