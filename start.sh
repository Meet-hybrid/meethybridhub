#!/bin/bash
echo "Starting Divine'zSignatures servers..."
echo ""

# Start storefront
echo "Starting Storefront on http://localhost:3000 ..."
cd "$(dirname "$0")/storefront" && npm run dev &
STOREFRONT_PID=$!

# Start dashboard
echo "Starting Dashboard on http://localhost:3001 ..."
cd "$(dirname "$0")/dashboard" && npm run dev -- -p 3001 &
DASHBOARD_PID=$!

echo ""
echo "✅ Servers started!"
echo "   🌐 Storefront: http://localhost:3000"
echo "   🔧 Dashboard:  http://localhost:3001"
echo ""
echo "Press Ctrl+C to stop both servers"
echo ""

# Wait for both processes
wait $STOREFRONT_PID $DASHBOARD_PID
