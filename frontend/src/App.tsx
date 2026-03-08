import { useState } from 'react'

function App() {
  const [count, setCount] = useState(0)

  return (
    <div className="min-h-screen bg-nl-bg-soft flex items-center justify-center">
      <div className="bg-nl-surface p-8 rounded-lg shadow-md">
        <h1 className="text-3xl font-bold text-nl-text mb-4">NossaLista</h1>
        <p className="text-nl-muted mb-4">Listas compartilhadas em tempo real</p>
        <button
          className="bg-blue-500 hover:bg-blue-600 text-white font-bold py-2 px-4 rounded"
          onClick={() => setCount((count) => count + 1)}
        >
          Contagem: {count}
        </button>
        <p className="text-sm text-nl-muted mt-4">
          Frontend configurado com React 19 + TypeScript + Vite + Tailwind CSS
        </p>
      </div>
    </div>
  )
}

export default App
