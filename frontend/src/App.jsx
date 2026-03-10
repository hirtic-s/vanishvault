import { Routes, Route } from 'react-router-dom'
import { Toaster } from 'react-hot-toast'
import UploadPage from './pages/UploadPage'
import AccessPage from './pages/AccessPage'
import DashboardPage from './pages/DashboardPage'

export default function App() {
  return (
    <>
      <Toaster position="top-right" />
      <Routes>
        <Route path="/"            element={<UploadPage />} />
        <Route path="/file/:fileId" element={<AccessPage />} />
        <Route path="/dashboard"   element={<DashboardPage />} />
      </Routes>
    </>
  )
}
