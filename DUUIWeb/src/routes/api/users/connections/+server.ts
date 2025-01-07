import { API_URL } from '$env/static/private'
import { error } from '@sveltejs/kit'

/**
 * Sends a put request to the backend to update a user.
 */
export const PUT = async ({ request, url, locals, fetch }) => {
	const provider = url.searchParams.get('provider') || ''
	const user = locals.user
	if (!user) {
		return error(401, { message: 'Unauthorized' })
	}

	const response = await fetch(`${API_URL}/users/connections/${user.oid}/${provider}`, {
		method: 'PUT',
		body: JSON.stringify(await request.json())
	})

	return response
}
