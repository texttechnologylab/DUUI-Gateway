import { API_URL } from '$env/static/private'
import type { Handle, RequestEvent } from '@sveltejs/kit'
import { env } from '$env/dynamic/public';


const requireVerification =
	(env.PUBLIC_VERIFICATION_REQUIRED ?? 'false').toLowerCase() !== 'false';

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

const ALLOW_UNVERIFIED = [
	'/account/login',
	'/account/logout',
	'/account/verify',
	'/account/register',
	'/auth',          // endpoint that checks the code
	'/api/users',
	'/assets', '/favicon.ico'
];

/**
 * This function is called every time a request is made to any part of the web interface.
 * The request is verified through this function before being passed to the appropriate page / endpoint.
 */
export const handle: Handle = async ({ event, resolve }) => {
	const { cookies, url } = event;
	const session = cookies.get('session') || event.url.searchParams.get('state') || '';

	if (session) {
		try {
			await fetchUser(session, event);
		} catch (err) { /* empty */ }
	}

	// The user is not authorized.
	if (!event.locals.user) cookies.delete('session', { path: '/' });
	// The user is not verified.
	else if (requireVerification && !event.locals.user.activated) {
		// allow only a small allowlist until verified
		const path = url.pathname;
		const allowed = ALLOW_UNVERIFIED.some((p) => path === p || path.startsWith(p + '/'));
		if (!allowed) {
			// keep session cookie; just detour to verification UI
			const next = encodeURIComponent(url.pathname + url.search);
			return new Response(null, {
				status: 303,
				headers: { location: `/account/verify` }
			});
		}
	}

	// Resolve the response
	const response = await resolve(event);

	return response;
};


