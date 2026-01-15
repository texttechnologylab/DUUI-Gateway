import type { ProcessMessage } from './processMessages'

export type WebSocketStatus = 'connecting' | 'open' | 'closed' | 'error' | 'reconnecting'

type WebSocketMessage = ProcessMessage

type ProcessEventHandlers = {
	onMessage?: (message: ProcessMessage) => void
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
	console.debug('[process-ws] resolving WS URL', processId)
	const response = await fetch(`/api/processes/events?process_id=${encodeURIComponent(processId)}`, {
		method: 'GET'
	})
	if (!response.ok) {
		let body = ''
		try {
			body = await response.text()
		} catch {
			// ignore
		}
		console.debug('[process-ws] WS URL resolve failed', processId, response.status, body)
		throw new Error(`Failed to resolve WebSocket URL for process ${processId}`)
	}
	const { url } = (await response.json()) as { url: string }
	console.debug('[process-ws] resolved WS URL', processId, url)
	return url
}

function parseMessage(data: string): WebSocketMessage | null {
	try {
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		const parsed: any = JSON.parse(data)
		if (!parsed || typeof parsed !== 'object') return null
		if (typeof parsed.kind !== 'string') return null
		return parsed as WebSocketMessage
	} catch {
		console.debug('[process-ws] failed to parse message', data)
		return null
	}
}

function dispatchMessage(conn: Connection, message: WebSocketMessage) {
	// Debug: confirm WS messages arrive and are shaped as expected.
	// Keep as console.debug to avoid noisy prod logs.
	console.debug('[process-ws]', conn.processId, message.kind, message)
	for (const subscriber of conn.subscribers) {
		subscriber.onMessage?.(message)
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

			console.debug('[process-ws] connecting', conn.processId, url)
			const ws = new WebSocket(url)
			conn.ws = ws

			ws.onopen = () => {
				conn.connecting = false
				conn.reconnectAttempts = 0
				console.debug('[process-ws] socket open', conn.processId)
				notifyStatus(conn, 'open')
			}

			ws.onmessage = (event) => {
				if (typeof event.data !== 'string') return
				console.debug('[process-ws] raw message', conn.processId, event.data)
				const message = parseMessage(event.data)
				if (message) {
					dispatchMessage(conn, message)
				}
			}

			ws.onerror = () => {
				console.debug('[process-ws] socket error', conn.processId)
				notifyStatus(conn, 'error')
				if (ws.readyState !== WebSocket.CLOSING && ws.readyState !== WebSocket.CLOSED) {
					ws.close()
				}
			}

			ws.onclose = (event) => {
				console.debug('[process-ws] socket closed', conn.processId, event.code, event.reason)
				conn.ws = null
				conn.connecting = false
				notifyStatus(conn, 'closed')
				scheduleReconnect(conn)
			}
		})
		.catch(() => {
			console.debug('[process-ws] failed to resolve WS URL', conn.processId)
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
