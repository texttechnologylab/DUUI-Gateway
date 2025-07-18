import { API_URL } from '$env/static/private'
import type { Handle, RequestEvent } from '@sveltejs/kit'


async function testBackendConnection() {
	try {
		console.log('Trying to reach: ', API_URL)
		const res = await fetch(`${API_URL}/alive`, {
			method: 'GET'
		});
		if (res.ok) {
			console.log('Backend reachable: ', API_URL)
		} else {
			console.error('Backend responds with: ', res.status)
		}
	} catch (error: any) {
		if (error instanceof Error) {
			console.error('Connecting to backend failed: ', error.toString(), error.stack);
		} else {
			console.error('Connecting to backend failed: ', error);
		}
	}
}

testBackendConnection()

/**
 * Fetch a user from the databse to verify the user is authorized.
 *
 * @param session the session id to authorize the user.
 * @param event a RequestEvent that is being handled.
 */
const fetchUser = async (
	session: string,
	event: RequestEvent
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

/**
 * This function is called every time a request is made to any part of the web interface.
 * The request is verified through this function before being passed to the appropriate page / endpoint.
 */
export const handle: Handle = async ({ event, resolve }) => {
	const { cookies } = event;
	const session = cookies.get('session') || event.url.searchParams.get('state') || '';

	if (session) {
		try {
			await fetchUser(session, event);
		} catch (err) { /* empty */ }
	}

	// The user is not authorized.
	if (!event.locals.user) cookies.delete('session', { path: '/' });

	// Resolve the response
	const response = await resolve(event);

	return response;
};


