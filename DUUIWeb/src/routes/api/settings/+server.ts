import { API_URL } from '$env/static/private'
import { error } from '@sveltejs/kit'

/**
 * Sends a put request to the backend to update the global config settings.
 */
export const PUT = async ({ request, locals }) => {
	const user = locals.user
	if (!user) {
		return error(401, { message: 'Unauthorized' })
	}

	const response = await fetch(`${API_URL}/settings/`, {
		method: 'PUT',
		body: JSON.stringify(await request.json()),
		headers: {
			Authorization: user.session || ''
		}
	})

	return response
}