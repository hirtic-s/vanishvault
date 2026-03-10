import { useState } from 'react'
import { useParams } from 'react-router-dom'
import axios from 'axios'
import toast from 'react-hot-toast'
import { Lock, Download, ShieldX } from 'lucide-react'

export default function AccessPage() {
  const { fileId } = useParams()
  const [password, setPassword] = useState('')
  const [loading, setLoading]   = useState(false)
  const [deleted, setDeleted]   = useState(false)
  const [attemptsLeft, setAttemptsLeft] = useState(3)

  async function handleVerify(e) {
    e.preventDefault()
    setLoading(true)

    try {
      const response = await axios.post(
        `/api/file/${fileId}/verify`,
        { password },
        { responseType: 'blob' }
      )

      // Download the file
      const url  = window.URL.createObjectURL(new Blob([response.data]))
      const link = document.createElement('a')
      link.href  = url

      const disposition = response.headers['content-disposition']
      const filename = disposition
        ? disposition.split('filename=')[1].replace(/"/g, '')
        : 'download'

      link.setAttribute('download', filename)
      document.body.appendChild(link)
      link.click()
      link.remove()
      toast.success('File downloaded!')

    } catch (err) {
      const status = err.response?.status
      const data   = await err.response?.data?.text()
      const parsed = data ? JSON.parse(data) : {}

      if (status === 401) {
        const left = parsed.attemptsLeft ?? attemptsLeft - 1
        setAttemptsLeft(left)
        toast.error(`Wrong password — ${left} attempt${left !== 1 ? 's' : ''} left`)
      } else if (status === 403) {
        setDeleted(true)
        toast.error('Too many failed attempts — file deleted')
      } else if (status === 410) {
        setDeleted(true)
        toast.error('This link has expired')
      } else if (status === 404) {
        setDeleted(true)
        toast.error('File not found or already deleted')
      } else {
        toast.error('Something went wrong')
      }
    } finally {
      setLoading(false)
    }
  }

  if (deleted) return (
    <div className="min-h-screen bg-gray-950 text-white flex items-center justify-center p-6">
      <div className="bg-gray-900 rounded-2xl p-8 max-w-sm w-full text-center space-y-4">
        <ShieldX size={48} className="mx-auto text-red-400" />
        <h2 className="text-2xl font-bold text-red-400">File Destroyed</h2>
        <p className="text-gray-400">This file no longer exists. It has been permanently deleted.</p>
      </div>
    </div>
  )

  return (
    <div className="min-h-screen bg-gray-950 text-white flex items-center justify-center p-6">
      <div className="bg-gray-900 rounded-2xl p-8 max-w-sm w-full space-y-6">

        <div className="text-center">
          <Lock size={40} className="mx-auto text-blue-400 mb-3" />
          <h2 className="text-2xl font-bold">Secure File</h2>
          <p className="text-gray-400 text-sm mt-1">Enter the password to download</p>
        </div>

        <form onSubmit={handleVerify} className="space-y-4">
          <input
            type="password"
            placeholder="Enter password"
            value={password}
            onChange={e => setPassword(e.target.value)}
            className="w-full bg-gray-800 rounded-xl px-4 py-3 outline-none text-white text-center text-lg tracking-widest"
            autoFocus
          />

          <div className="flex justify-center gap-2">
            {[...Array(3)].map((_, i) => (
              <div key={i}
                className={`w-3 h-3 rounded-full ${i < attemptsLeft ? 'bg-blue-500' : 'bg-gray-700'}`} />
            ))}
          </div>
          <p className="text-center text-xs text-gray-500">{attemptsLeft} attempt{attemptsLeft !== 1 ? 's' : ''} remaining</p>

          <button type="submit" disabled={loading || !password}
            className="w-full flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 rounded-xl py-3 font-semibold transition">
            <Download size={16} />
            {loading ? 'Verifying...' : 'Download File'}
          </button>
        </form>
      </div>
    </div>
  )
}
