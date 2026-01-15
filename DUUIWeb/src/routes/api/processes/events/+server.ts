import { API_URL } from '$env/static/private'
import { error, json, type RequestHandler } from '@sveltejs/kit'

/**
 * Provides the WebSocket URL for live process events.
 * This keeps the backend URL construction on the server side.
 */
export const GET: RequestHandler = async ({ url, cookies, request }) => {
	const processId = url.searchParams.get('process_id')

	if (!processId) {
		error(400, 'Missing process_id query parameter.')
	}

	const api = new URL(API_URL)
	const wsProtocol = api.protocol === 'https:' ? 'wss:' : 'ws:'

	const session = cookies.get('session') || ''

	// API_URL is correct for server-to-server calls (Docker DNS),
	// but the returned ws URL must be reachable from the *browser*.
	//
	// In docker-compose, backend listens on 2605 internally but is published as 8085 on the host.
	// The browser can always reach the backend via "same host as the frontend, port 8085".
	const effectiveHostHeader =
		request?.headers.get('x-forwarded-host') || request?.headers.get('host') || url.host

	let requestHost = url.hostname
	let requestPort = url.port
	try {
		const parsed = new URL(`http://${effectiveHostHeader}`)
		requestHost = parsed.hostname
		requestPort = parsed.port
	} catch {
		// fall back to url.hostname/url.port
	}
	const apiHost = api.hostname

	const isApiLocalhost = apiHost === 'localhost' || apiHost === '127.0.0.1' || apiHost === '::1'

	const isDockerComposePublishedBackend =
		!isApiLocalhost && requestPort === '5173' && api.port === '2605' && apiHost !== requestHost

	const wsAuthority = isDockerComposePublishedBackend ? `${requestHost}:8085` : api.host
	const wsUrl = `${wsProtocol}//${wsAuthority}/ws/processes/${processId}/events?token=${encodeURIComponent(session)}`

	console.log('WS-URL:', wsUrl, {
		api: API_URL,
		urlHost: url.host,
		effectiveHostHeader,
		requestHost,
		requestPort
	})

	return json({ url: wsUrl })
}
