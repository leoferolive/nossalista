import { useState } from 'react'

function App() {
  const [count, setCount] = useState(0)

  return (
    <div className="min-h-screen bg-gray-100 flex items-center justify-center">
      <div className="bg-white p-8 rounded-lg shadow-md">
        <h1 className="text-3xl font-bold text-gray-800 mb-4">
          NossaLista
        </h1>
        <p className="text-gray-600 mb-4">
          Listas compartilhadas em tempo real
        </p>
        <button
          className="bg-blue-500 hover:bg-blue-600 text-white font-bold py-2 px-4 rounded"
          onClick={() => setCount((count) => count + 1)}
        >
          Contagem: {count}
        </button>
        <p className="text-sm text-gray-500 mt-4">
          Frontend configurado com React 19 + TypeScript + Vite + Tailwind CSS
        </p>
      </div>
    </div>
  )
}

export default App
