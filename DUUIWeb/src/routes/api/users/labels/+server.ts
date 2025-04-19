import {API_URL} from '$env/static/private'
import {error} from '@sveltejs/kit'

/**
 * Sends a put request to the backend to update or insert a label.
 * The requesting user must be an admin.
 */
export const GET = async ({locals, url }) => {
	const user = locals.user;
	const driver = url.searchParams.get('driver');

	if (!user) {
		return error(401, { message: 'Unauthorized' })
	}

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

	// console .log("PUT label data", data)

	if (data.label.driver === "" || data.label.label === "" || !data.label.groups) {
		return error(400, { message: 'The label, driver and groups are required.' })
	}

	if (!user || user.role !== 'Admin') {
		return error(401, { message: 'Unauthorized' })
	}

	// return error(400, { message: `${API_URL}/users/labels/${operation}${data.driver}/${data.label}${labelId}` })

	return await fetch(`${API_URL}/users/labels/${data.labelId}`, {
		method: 'PUT',
		body: JSON.stringify(data.label)
	})
}

/**
 * Sends a delete request to the backend to delete a label.
 * The requesting user must be an admin.
 */
export const DELETE = async ({locals, request }) => {
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
