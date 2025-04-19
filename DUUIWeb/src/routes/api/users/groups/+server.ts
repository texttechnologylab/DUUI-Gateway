import {API_URL} from '$env/static/private'
import {error} from '@sveltejs/kit'

/**
 * Sends a put request to the backend to update or insert a Group.
 * The requesting user must be an admin.
 */
export const GET = async ({locals}) => {
	const user = locals.user;

	if (!user) {
		return error(401, { message: 'Unauthorized' })
	}

	return await fetch(`${API_URL}/users/groups`, {
		method: 'GET'
	});
};


export const PUT = async ({ request, locals }) => {
	const data = await request.json()
	const user = locals.user

	if (!data.group.name || data.group.name === "") {
		return error(400, { message: 'The group name is required.' })
	}

	if (!user || user.role !== 'Admin') {
		return error(401, { message: 'Unauthorized' })
	}

	if (!data.group.members || !data.group.name) {
		return error(400, { message: 'The group members, name and labels are required.' })
	}

	console.log("PUT group data", data)
	// return error(400, { message: `${API_URL}/users/labels/${operation}${data.driver}/${data.label}${labelId}` })

	return await fetch(`${API_URL}/users/groups/${data.groupId}`, {
		method: 'PUT',
		body: JSON.stringify(data.group)
	})
}

/**
 * Sends a delete request to the backend to delete a Group.
 * The requesting user must be an admin.
 */
export const DELETE = async ({locals, request }) => {
	const data = await request.json()
	const user = locals.user

	if (!user || user.role !== 'Admin') {
		return error(401, { message: 'Unauthorized' })
	}

	return await fetch(`${API_URL}/users/groups/${data.groupId}`, {
		method: 'DELETE',
		body: JSON.stringify({})
	})
}
