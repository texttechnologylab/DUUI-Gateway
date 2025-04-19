import { API_URL } from '$env/static/private'
import {json, type RequestHandler} from '@sveltejs/kit'

/**
 * Sends a get request to the backend to retrieve the folder structure of a database.
 */
export const POST: RequestHandler = async ({ request, cookies, fetch }) => {
	const data = await request.json()

	const response = await fetch(`${API_URL}/processes/folderstructure/${data.user}/${data.provider}/${data.reset}/${data.providerId}`, {
		method: 'GET',
		headers: {
			Authorization: cookies.get('session') || ''
		}
	})

	if (response.ok) {
		return json(await response.json(), {
			headers: {
				'Content-Type': 'application/json'
			},
			status: 200
		})
	}

	return response
}

