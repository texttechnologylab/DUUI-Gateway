import { API_URL } from '$env/static/private'
import { v4 as uuidv4 } from 'uuid'
import type { PageServerLoad } from './$types'

export const load: PageServerLoad = async ({ url, cookies, fetch }) => {
	const code = url.searchParams.get('code')
	const name = uuidv4()

	const response = await fetch(`${API_URL}/users/auth/google?code=${code}&name=${name}`, {
		method: 'PUT',
		headers: {
			Authorization: cookies.get('session') || ''
		}
	})

	return {
		success: response.ok,
		name
	}
}
