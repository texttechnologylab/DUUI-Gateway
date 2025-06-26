import {API_URL} from '$env/static/private'
import {error} from '@sveltejs/kit'

/**
 * Sends a get request to the backend to retrieve registry entries filtered by user scope.
 * The requesting user must be logged in.
 */
export const GET = async ({ locals, url }) => {
	const user = locals.user;
	// console.log("User:", user);

	if (!user) {
		// console.error("Unauthorized access detected");
		return error(401, { message: "Unauthorized" });
	}

	const filter = url.searchParams.get("filter") || "False";
	// console.log("Filter:", filter);

	try {
		const response = await fetch(`${API_URL}/users/registries/${user.oid}`, {
			method: "GET",
			headers: {
				"Content-Type": "application/json",
				"x-user-role": user.role,
				"x-user-oid": user.oid,
			}
		});
		// console.log("Response Status:", response.status);
		//
		// const data = await response.json();
		// console.log("Response Data:", data);

		return response;
	} catch (err) {
		// console.error("Fetch Error:", err);
		return error(500, { message: "Internal Server Error" });
	}
};


/**
 * Sends a put request to the backend to update or insert a registry entry.
 * The requesting user must be an admin.
 */
export const PUT = async ({ request, locals }) => {
	const data = await request.json()
	const user = locals.user

	// console .log("PUT registry data", data)

	if (data.registry.scope === "" || data.registry.name === "" || !data.registry.endpoint) {
		return error(400, { message: 'The name, scope and endpoint are required.' })
	}

	if (!user || user.role !== 'Admin') {
		return error(401, { message: 'Unauthorized' })
	}

	return await fetch(`${API_URL}/users/registries/${data.registryId}`, {
		method: 'PUT',
		body: JSON.stringify(data.registry)
	})
}

/**
 * Sends a delete request to the backend to delete a registry entry.
 * The requesting user must be an admin.
 */
export const DELETE = async ({locals, request }) => {
	const data = await request.json()
	const user = locals.user

	if (!user || user.role !== 'Admin') {
		return error(401, { message: 'Unauthorized' })
	}

	return await fetch(`${API_URL}/users/registries/${data.registryId}`, {
		method: 'DELETE',
		body: JSON.stringify({})
	})
}
