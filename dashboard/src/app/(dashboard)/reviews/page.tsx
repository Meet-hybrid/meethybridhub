"use client";

import { Star, Flag, Check } from "lucide-react";

const mockReviews = [
  { id: 1, customer: "Alice Johnson", rating: 5, title: "Absolutely stunning!", comment: "The gown exceeded my expectations. The lace detail is beautiful and the fit is perfect.", createdAt: "2026-08-30T10:00:00Z", flagged: false },
  { id: 2, customer: "Bob Smith", rating: 4, title: "Great quality", comment: "The agbada set is well-made. Delivery was a bit slow but worth the wait.", createdAt: "2026-08-28T14:30:00Z", flagged: false },
  { id: 3, customer: "Carol White", rating: 3, title: "Good but not perfect", comment: "The color was slightly different from what I expected, but overall decent quality.", createdAt: "2026-08-25T09:15:00Z", flagged: true },
  { id: 4, customer: "David Brown", rating: 5, title: "Will order again!", comment: "Fast delivery, excellent craftsmanship. My wife loved the gift.", createdAt: "2026-08-22T16:00:00Z", flagged: false },
  { id: 5, customer: "Eva Martinez", rating: 5, title: "Best fashion store", comment: "I've ordered 3 times now and every piece is beautiful. The custom order process was smooth.", createdAt: "2026-08-20T11:45:00Z", flagged: false },
];

function StarRating({ rating }: { rating: number }) {
  return (
    <div className="flex gap-0.5">
      {[1, 2, 3, 4, 5].map((i) => (
        <Star
          key={i}
          className={`w-4 h-4 ${i <= rating ? "text-amber-400 fill-amber-400" : "text-gray-200"}`}
        />
      ))}
    </div>
  );
}

export default function ReviewsPage() {
  const avgRating = mockReviews.reduce((sum, r) => sum + r.rating, 0) / mockReviews.length;

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold text-gray-900">Reviews</h2>
        <p className="text-sm text-gray-500 mt-1">Customer reviews and ratings for your store</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-white rounded-xl border border-gray-200 p-6 text-center">
          <p className="text-3xl font-bold text-gray-900">{avgRating.toFixed(1)}</p>
          <StarRating rating={Math.round(avgRating)} />
          <p className="text-sm text-gray-500 mt-1">Average rating</p>
        </div>
        <div className="bg-white rounded-xl border border-gray-200 p-6 text-center">
          <p className="text-3xl font-bold text-gray-900">{mockReviews.length}</p>
          <p className="text-sm text-gray-500 mt-2">Total reviews</p>
        </div>
        <div className="bg-white rounded-xl border border-gray-200 p-6 text-center">
          <p className="text-3xl font-bold text-amber-600">
            {mockReviews.filter((r) => r.rating === 5).length}
          </p>
          <p className="text-sm text-gray-500 mt-2">5-star reviews</p>
        </div>
      </div>

      <div className="space-y-4">
        {mockReviews.map((review) => (
          <div key={review.id} className="bg-white rounded-xl border border-gray-200 p-5">
            <div className="flex items-start justify-between">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 bg-indigo-100 rounded-full flex items-center justify-center text-indigo-600 text-sm font-bold">
                  {review.customer.split(" ").map((n) => n[0]).join("")}
                </div>
                <div>
                  <p className="font-medium text-sm">{review.customer}</p>
                  <div className="flex items-center gap-2">
                    <StarRating rating={review.rating} />
                    <span className="text-xs text-gray-400">
                      {new Date(review.createdAt).toLocaleDateString()}
                    </span>
                  </div>
                </div>
              </div>
              <div className="flex gap-1">
                {review.flagged && (
                  <span className="px-2 py-1 bg-red-50 text-red-600 text-xs font-medium rounded-full flex items-center gap-1">
                    <Flag className="w-3 h-3" /> Flagged
                  </span>
                )}
              </div>
            </div>
            <div className="mt-3">
              <p className="font-medium text-sm">{review.title}</p>
              <p className="text-sm text-gray-600 mt-1">{review.comment}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
