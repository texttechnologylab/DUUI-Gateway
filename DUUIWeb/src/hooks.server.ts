import { API_URL } from '$env/static/private'
import type { Handle, RequestEvent } from '@sveltejs/kit'
import { defaultGlobals } from '$lib/store'

/**
 * Fetch a user from the databse to verify the user is authorized.
 *
 * @param session the session id to authorize the user.
 * @param event a RequestEvent that is being handled.
 */
const fetchUser = async (
	session: string,
	event: RequestEvent<Partial<Record<string, string>>, string | null>
) => {
	const response = await fetch(`${API_URL}/users/auth/`, {
		method: 'GET',
		headers: {
			Authorization: session
		}
	})

	if (response.ok) {
		const json = await response.json()
		event.locals.user = json.user
	}
}

const fetchGlobals = async (event: RequestEvent) => {
	const role: string = event.locals.user?.role || ""
	const response = await fetch(`${API_URL}/globals/${role}`, {
		method: 'GET'
	})

	if (response.ok) {
		const json = await response.json()
		event.locals.globals = json
	} else {
		event.locals.globals = defaultGlobals
	}

}

/**
 * This function is called every time a request is made to any part of the web interface.
 * The request is verified through this function before being passed to the appropriate page / endpoint.
 */
export const handle: Handle = async ({ event, resolve }) => {
	const { cookies } = event
	const session = cookies.get('session') || event.url.searchParams.get('state') || ''

	if (session) {
		try {
			await fetchUser(session, event)
		} catch (err) {}
	}

	await fetchGlobals(event)

	// The user is not authorized.
	if (!event.locals.user) cookies.delete('session', { path: '/' })

	return await resolve(event)
}
