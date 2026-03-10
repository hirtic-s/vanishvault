import { useEffect, useState } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { Shield, FileX, AlertTriangle, CheckCircle, Trash2 } from 'lucide-react'
import axios from 'axios'
import toast from 'react-hot-toast'

const SENDER_ID = 'hirticc'

const eventIcon = {
  FILE_OPENED:    <CheckCircle  size={16} className="text-green-400" />,
  WRONG_PASSWORD: <AlertTriangle size={16} className="text-yellow-400" />,
  FILE_DELETED:   <FileX        size={16} className="text-red-400" />,
}

const eventColor = {
  FILE_OPENED:    'border-green-800 bg-green-950',
  WRONG_PASSWORD: 'border-yellow-800 bg-yellow-950',
  FILE_DELETED:   'border-red-800 bg-red-950',
}

export default function DashboardPage() {
  const [alerts, setAlerts]     = useState([])
  const [connected, setConnected] = useState(false)

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      onConnect: () => {
        setConnected(true)
        client.subscribe(`/user/${SENDER_ID}/queue/alerts`, msg => {
          const data = JSON.parse(msg.body)
          setAlerts(prev => [{ ...data, id: Date.now() }, ...prev])
          toast(data.message)
        })
      },
      onDisconnect: () => setConnected(false),
      reconnectDelay: 3000,
    })

    client.activate()
    return () => client.deactivate()
  }, [])

  async function revokeFile(fileId) {
    try {
      await axios.delete(`/api/file/${fileId}/revoke`)
      toast.success('File revoked!')
      setAlerts(prev => prev.filter(a => a.fileId !== fileId))
    } catch {
      toast.error('Revoke failed')
    }
  }

  return (
    <div className="min-h-screen bg-gray-950 text-white p-6">
      <div className="max-w-2xl mx-auto space-y-6">

        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Shield size={28} className="text-blue-400" />
            <h1 className="text-2xl font-bold">VanishVault Dashboard</h1>
          </div>
          <div className="flex items-center gap-2">
            <div className={`w-2 h-2 rounded-full ${connected ? 'bg-green-400 animate-pulse' : 'bg-red-400'}`} />
            <span className="text-sm text-gray-400">{connected ? 'Live' : 'Disconnected'}</span>
          </div>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-3 gap-4">
          {[
            { label: 'Total Events',   value: alerts.length,                                  color: 'text-white' },
            { label: 'Files Opened',   value: alerts.filter(a => a.event === 'FILE_OPENED').length,   color: 'text-green-400' },
            { label: 'Failed Attempts', value: alerts.filter(a => a.event === 'WRONG_PASSWORD').length, color: 'text-yellow-400' },
          ].map(stat => (
            <div key={stat.label} className="bg-gray-900 rounded-xl p-4 text-center">
              <p className={`text-2xl font-bold ${stat.color}`}>{stat.value}</p>
              <p className="text-gray-400 text-sm">{stat.label}</p>
            </div>
          ))}
        </div>

        {/* Activity Feed */}
        <div className="bg-gray-900 rounded-2xl p-6 space-y-3">
          <h2 className="font-semibold text-gray-300">Live Activity Feed</h2>

          {alerts.length === 0 ? (
            <div className="text-center py-12 text-gray-600">
              <Shield size={40} className="mx-auto mb-3 opacity-30" />
              <p>Waiting for activity...</p>
              <p className="text-sm mt-1">Events will appear here in real time</p>
            </div>
          ) : (
            alerts.map(alert => (
              <div key={alert.id}
                className={`flex items-center gap-3 border rounded-xl px-4 py-3 ${eventColor[alert.event] ?? 'border-gray-700 bg-gray-800'}`}>
                {eventIcon[alert.event]}
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium">{alert.message}</p>
                  <p className="text-xs text-gray-500 font-mono truncate">{alert.fileId}</p>
                </div>
                {alert.event !== 'FILE_DELETED' && (
                  <button onClick={() => revokeFile(alert.fileId)}
                    className="text-gray-500 hover:text-red-400 transition">
                    <Trash2 size={14} />
                  </button>
                )}
              </div>
            ))
          )}
        </div>

      </div>
    </div>
  )
}
