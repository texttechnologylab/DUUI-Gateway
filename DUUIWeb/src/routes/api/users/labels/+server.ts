import {API_URL} from '$env/static/private'
import {error} from '@sveltejs/kit'

/**
 * Sends a put request to the backend to update or insert a label.
 * The requesting user must be an admin.
 */
export const GET = async ({ request, locals, url }) => {
	const user = locals.user;
	const driver = url.searchParams.get('driver');

	return await fetch(`${API_URL}/users/labels/driver-filter/${driver}`, {
		method: 'GET',
		headers: {
			'Content-Type': 'application/json',
			'x-user-role': user.role,
			'x-user-oid': user.oid
		}
	});
};


export const PUT = async ({ request, locals }) => {
	const data = await request.json()
	const user = locals.user

	if (data.driver === "" || data.label === "") {
		return error(400, { message: 'The label and driver are required.' })
	}

	if (!user || user.role !== 'Admin') {
		return error(401, { message: 'Unauthorized' })
	}

	let labelId = data.labelId ? "/" + data.labelId : ''
	let operation = data.labelId ? "update/" : "insert/"

	// return error(400, { message: `${API_URL}/users/labels/${operation}${data.driver}/${data.label}${labelId}` })

	return await fetch(`${API_URL}/users/labels/${operation}${data.driver}/${data.label}${labelId}`, {
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

	return await fetch(`${API_URL}/users/labels`, {
		method: 'DELETE',
		body: JSON.stringify(data["labels"])
	})
}
