import { API_URL } from '$env/static/private'
import { error, json, type RequestHandler } from '@sveltejs/kit'

/**
 * Provides the WebSocket URL for live process events.
 * This keeps the backend URL construction on the server side.
 */
export const GET: RequestHandler = async ({ url, cookies }) => {
	const processId = url.searchParams.get('process_id')

	if (!processId) {
		error(400, 'Missing process_id query parameter.')
	}

	const api = new URL(API_URL)
	const wsProtocol = api.protocol === 'https:' ? 'wss:' : 'ws:'

	const session = cookies.get('session') || ''
	const wsUrl = `${wsProtocol}//${api.host}/ws/processes/${processId}/events?token=${encodeURIComponent(session)}`

	console.log("WS-URL: " + wsUrl)

	return json({ url: wsUrl })
}
