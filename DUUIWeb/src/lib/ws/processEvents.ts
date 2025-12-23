import type { WsEventEnvelope } from './eventDispatcher'

export type WebSocketStatus = 'connecting' | 'open' | 'closed' | 'error' | 'reconnecting'

type WebSocketMessage = WsEventEnvelope

type ProcessEventHandlers = {
	onEvent?: (message: WsEventEnvelope) => void
	onStatus?: (status: WebSocketStatus) => void
}

type Connection = {
	processId: string
	ws: WebSocket | null
	subscribers: Set<ProcessEventHandlers>
	reconnectTimer: ReturnType<typeof setTimeout> | null
	reconnectAttempts: number
	connecting: boolean
	manualClose: boolean
}

const connections = new Map<string, Connection>()
const RECONNECT_BASE_MS = 1000
const RECONNECT_MAX_MS = 30000

function notifyStatus(conn: Connection, status: WebSocketStatus) {
	for (const subscriber of conn.subscribers) {
		subscriber.onStatus?.(status)
	}
}

async function resolveWsUrl(processId: string): Promise<string> {
	const response = await fetch(`/api/processes/events?process_id=${encodeURIComponent(processId)}`, {
		method: 'GET'
	})
	if (!response.ok) {
		throw new Error(`Failed to resolve WebSocket URL for process ${processId}`)
	}
	const { url } = (await response.json()) as { url: string }
	return url
}

function parseMessage(data: string): WebSocketMessage | null {
	try {
		const parsed = JSON.parse(data)
		if (!parsed || typeof parsed !== 'object') return null
		return parsed as WebSocketMessage
	} catch {
		return null
	}
}

function dispatchMessage(conn: Connection, message: WebSocketMessage) {
	for (const subscriber of conn.subscribers) {
		subscriber.onEvent?.(message)
	}
}

function scheduleReconnect(conn: Connection) {
	if (conn.reconnectTimer || conn.subscribers.size === 0 || conn.manualClose) {
		return
	}

	conn.reconnectAttempts += 1
	const baseDelay = Math.min(
		RECONNECT_MAX_MS,
		RECONNECT_BASE_MS * Math.pow(2, conn.reconnectAttempts)
	)
	const jitter = 0.5 + Math.random()
	const delay = Math.round(baseDelay * jitter)

	notifyStatus(conn, 'reconnecting')
	conn.reconnectTimer = setTimeout(() => {
		conn.reconnectTimer = null
		ensureConnected(conn)
	}, delay)
}

function ensureConnected(conn: Connection) {
	if (typeof window === 'undefined') return
	if (conn.connecting) return
	if (conn.ws && (conn.ws.readyState === WebSocket.OPEN || conn.ws.readyState === WebSocket.CONNECTING)) {
		return
	}

	conn.connecting = true
	conn.manualClose = false
	notifyStatus(conn, 'connecting')

	resolveWsUrl(conn.processId)
		.then((url) => {
			if (conn.subscribers.size === 0) {
				conn.connecting = false
				return
			}

			const ws = new WebSocket(url)
			conn.ws = ws

			ws.onopen = () => {
				conn.connecting = false
				conn.reconnectAttempts = 0
				notifyStatus(conn, 'open')
			}

			ws.onmessage = (event) => {
				if (typeof event.data !== 'string') return
				const message = parseMessage(event.data)
				if (message) {
					dispatchMessage(conn, message)
				}
			}

			ws.onerror = () => {
				notifyStatus(conn, 'error')
				if (ws.readyState !== WebSocket.CLOSING && ws.readyState !== WebSocket.CLOSED) {
					ws.close()
				}
			}

			ws.onclose = () => {
				conn.ws = null
				conn.connecting = false
				notifyStatus(conn, 'closed')
				scheduleReconnect(conn)
			}
		})
		.catch(() => {
			conn.connecting = false
			scheduleReconnect(conn)
		})
}

function getConnection(processId: string): Connection {
	let conn = connections.get(processId)
	if (!conn) {
		conn = {
			processId,
			ws: null,
			subscribers: new Set(),
			reconnectTimer: null,
			reconnectAttempts: 0,
			connecting: false,
			manualClose: false
		}
		connections.set(processId, conn)
	}
	return conn
}

function teardownConnection(conn: Connection) {
	conn.manualClose = true
	if (conn.reconnectTimer) {
		clearTimeout(conn.reconnectTimer)
		conn.reconnectTimer = null
	}
	if (conn.ws && conn.ws.readyState !== WebSocket.CLOSED) {
		conn.ws.close()
	}
	conn.ws = null
	conn.connecting = false
	conn.reconnectAttempts = 0
	notifyStatus(conn, 'closed')
}

export function subscribeProcessEvents(
	processId: string,
	handlers: ProcessEventHandlers
): () => void {
	const conn = getConnection(processId)
	conn.subscribers.add(handlers)
	ensureConnected(conn)

	return () => {
		conn.subscribers.delete(handlers)
		if (conn.subscribers.size === 0) {
			teardownConnection(conn)
			connections.delete(processId)
		}
	}
}
