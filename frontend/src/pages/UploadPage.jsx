import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import axios from 'axios'
import toast from 'react-hot-toast'
import { Upload, Lock, Clock, Download, Flame, Copy } from 'lucide-react'
import { QRCodeSVG as QRCode } from 'qrcode.react'

const API = import.meta.env.VITE_API_URL || 'http://localhost:8080'

export default function UploadPage() {
  const [file, setFile]               = useState(null)
  const [password, setPassword]       = useState('')
  const [email, setEmail]             = useState('')
  const [expiry, setExpiry]           = useState(5)
  const [maxDownloads, setMaxDownloads] = useState(0)
  const [burnAfterRead, setBurnAfterRead] = useState(false)
  const [loading, setLoading]         = useState(false)
  const [result, setResult]           = useState(null)
  const navigate = useNavigate()

  const strength = password.length === 0 ? 0
    : password.length < 6 ? 1
    : password.length < 10 ? 2
    : /[A-Z]/.test(password) && /[0-9]/.test(password) ? 4 : 3

  const strengthLabel = ['', 'Weak', 'Fair', 'Good', 'Strong']
  const strengthColor = ['', 'bg-red-500', 'bg-yellow-500', 'bg-blue-500', 'bg-green-500']

  async function handleUpload(e) {
    e.preventDefault()
    if (!file || !password) return toast.error('File and password required')
    setLoading(true)

    const form = new FormData()
    form.append('file', file)
    form.append('password', password)
    form.append('senderId', 'hirticc')
    form.append('senderEmail', email)
    form.append('expiryMinutes', expiry)
    form.append('maxDownloads', maxDownloads)
    form.append('burnAfterRead', burnAfterRead)

    try {
      const { data } = await axios.post(`${API}/api/upload`, form)
      setResult(data)
      toast.success('File uploaded successfully!')
    } catch {
      toast.error('Upload failed. Check your backend.')
    } finally {
      setLoading(false)
    }
  }

  function copyLink() {
    navigator.clipboard.writeText(result.link)
    toast.success('Link copied!')
  }

  if (result) return (
    <div className="min-h-screen bg-gray-950 text-white flex items-center justify-center p-6">
      <div className="bg-gray-900 rounded-2xl p-8 max-w-md w-full space-y-6">
        <h2 className="text-2xl font-bold text-green-400">File Uploaded!</h2>
        <div className="bg-gray-800 rounded-xl p-4 space-y-2 text-sm">
          <p className="text-gray-400">Share this link:</p>
          <p className="text-white break-all font-mono">{result.link}</p>
        </div>
        <div className="flex gap-2">
          <button onClick={copyLink}
            className="flex-1 flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-700 rounded-xl py-3 font-semibold transition">
            <Copy size={16} /> Copy Link
          </button>
          <button onClick={() => navigate('/dashboard')}
            className="flex-1 bg-gray-700 hover:bg-gray-600 rounded-xl py-3 font-semibold transition">
            Dashboard
          </button>
        </div>
        <div className="flex flex-col items-center gap-2">
          <p className="text-gray-400 text-sm">QR Code</p>
          <div className="bg-white p-3 rounded-xl">
            <QRCode value={result.link} size={150} />
          </div>
        </div>
        <div className="grid grid-cols-2 gap-3 text-sm">
          <div className="bg-gray-800 rounded-xl p-3">
            <p className="text-gray-400">Expires in</p>
            <p className="font-semibold">{result.expiresIn}</p>
          </div>
          <div className="bg-gray-800 rounded-xl p-3">
            <p className="text-gray-400">Max downloads</p>
            <p className="font-semibold">{result.maxDownloads}</p>
          </div>
        </div>
        <button onClick={() => setResult(null)}
          className="w-full bg-gray-700 hover:bg-gray-600 rounded-xl py-3 font-semibold transition">
          Upload Another
        </button>
      </div>
    </div>
  )

  return (
    <div className="min-h-screen bg-gray-950 text-white flex items-center justify-center p-6">
      <div className="bg-gray-900 rounded-2xl p-8 max-w-md w-full space-y-6">
        <div className="text-center">
          <h1 className="text-3xl font-bold">VanishVault</h1>
          <p className="text-gray-400 mt-1">Share files that self-destruct</p>
        </div>
        <form onSubmit={handleUpload} className="space-y-4">
          <label className="block border-2 border-dashed border-gray-700 hover:border-blue-500 rounded-xl p-6 text-center cursor-pointer transition">
            <Upload className="mx-auto mb-2 text-gray-400" />
            <p className="text-gray-400 text-sm">
              {file ? file.name : 'Click to select a file'}
            </p>
            <input type="file" className="hidden" onChange={e => setFile(e.target.files[0])} />
          </label>
          <div>
            <div className="flex items-center gap-2 bg-gray-800 rounded-xl px-4 py-3">
              <Lock size={16} className="text-gray-400" />
              <input
                type="password"
                placeholder="Set a password"
                value={password}
                onChange={e => setPassword(e.target.value)}
                className="bg-transparent flex-1 outline-none text-white"
              />
            </div>
            {password.length > 0 && (
              <div className="mt-2 space-y-1">
                <div className="h-1.5 bg-gray-700 rounded-full">
                  <div
                    className={['', 'bg-red-500', 'bg-yellow-500', 'bg-blue-500', 'bg-green-500'][strength] + ' h-1.5 rounded-full transition-all'}
                    style={{ width: strength * 25 + '%' }}
                  />
                </div>
                <p className="text-xs text-gray-400">Strength: {strengthLabel[strength]}</p>
              </div>
            )}
          </div>
          <input
            type="email"
            placeholder="Your email for alerts (optional)"
            value={email}
            onChange={e => setEmail(e.target.value)}
            className="w-full bg-gray-800 rounded-xl px-4 py-3 outline-none text-white"
          />
          <div className="flex items-center gap-3 bg-gray-800 rounded-xl px-4 py-3">
            <Clock size={16} className="text-gray-400" />
            <span className="text-gray-400 text-sm">Expires in</span>
            <select value={expiry} onChange={e => setExpiry(e.target.value)}
              className="bg-transparent flex-1 outline-none text-white">
              <option value={5}>5 minutes</option>
              <option value={30}>30 minutes</option>
              <option value={60}>1 hour</option>
              <option value={1440}>24 hours</option>
            </select>
          </div>
          <div className="flex items-center gap-3 bg-gray-800 rounded-xl px-4 py-3">
            <Download size={16} className="text-gray-400" />
            <span className="text-gray-400 text-sm">Download limit</span>
            <select value={maxDownloads} onChange={e => setMaxDownloads(e.target.value)}
              className="bg-transparent flex-1 outline-none text-white">
              <option value={0}>Unlimited</option>
              <option value={1}>1 time</option>
              <option value={3}>3 times</option>
              <option value={5}>5 times</option>
            </select>
          </div>
          <label className="flex items-center gap-3 bg-gray-800 rounded-xl px-4 py-3 cursor-pointer">
            <Flame size={16} className={burnAfterRead ? 'text-orange-400' : 'text-gray-400'} />
            <span className="text-gray-400 text-sm flex-1">Burn after read</span>
            <div onClick={() => setBurnAfterRead(!burnAfterRead)}
              className={'w-10 h-6 rounded-full transition-colors ' + (burnAfterRead ? 'bg-orange-500' : 'bg-gray-600') + ' relative'}>
              <div className={'absolute top-1 w-4 h-4 bg-white rounded-full transition-all ' + (burnAfterRead ? 'left-5' : 'left-1')} />
            </div>
          </label>
          <button type="submit" disabled={loading}
            className="w-full bg-blue-600 hover:bg-blue-700 disabled:opacity-50 rounded-xl py-3 font-semibold transition">
            {loading ? 'Uploading...' : 'Generate Secure Link'}
          </button>
        </form>
      </div>
    </div>
  )
}
