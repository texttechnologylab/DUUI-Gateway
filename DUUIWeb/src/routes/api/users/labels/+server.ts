import {API_URL} from '$env/static/private'
import {error} from '@sveltejs/kit'

/**
 * Sends a put request to the backend to update or insert a label.
 * The requesting user must be an admin.
 */
export const PUT = async ({ request, locals, fetch }) => {
	const data = await request.json()
	const user = locals.user

	if (!user || user.role !== 'Admin') {
		return error(401, { message: 'Unauthorized' })
	}

	let routeString = `${API_URL}/users/labels/${data.driver}/${data.label}/`

	if (data.labelId) routeString += `/${data.labelId}`

	return await fetch(routeString, {
		method: 'PUT',
		body: JSON.stringify({})
	})
}

/**
 * Sends a delete request to the backend to delete a label.
 * The requesting user must be an admin.
 */
export const DELETE = async ({ cookies, locals, request }) => {
	const data = await request.json()
	const user = locals.user

	if (!user || user.role !== 'Admin') {
		return error(401, { message: 'Unauthorized' })
	}

	return await fetch(`${API_URL}/users/labels/${data.labelId}`, {
		method: 'DELETE',
		body: JSON.stringify({})
	})
}
